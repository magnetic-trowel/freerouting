package app.freerouting.autoroute.pipeline;

import app.freerouting.autoroute.AutorouteAttemptResult;
import app.freerouting.autoroute.AutorouteAttemptState;
import app.freerouting.autoroute.maze.AutorouteControl;
import app.freerouting.autoroute.maze.AutorouteEngine;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.ConductionArea;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.structure.Unit;
import app.freerouting.datastructures.TimeLimit;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.logger.FRLogger;
import app.freerouting.rules.Net;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/** Executes one autoroute connection, including necked retry and strict-DRC recovery. */
final class AutorouteConnectionRouter {

  private final BatchAutorouter router;

  AutorouteConnectionRouter(BatchAutorouter router) {
    this.router = router;
  }

  /** Set by {@link #routeAttempt} when it aimed at the plane only. */
  private boolean aimedAtPlaneOnly;

  /**
   * plane_as_priority: pads of the net already in the plane and at most plane_share_gap_um from
   * {@code item}, edge to edge, are accepted as destinations next to the plane. No via fits between
   * two such pads; sharing one is the usual practice for adjacent pins of a package.
   */
  private void addSharablePads(Item item, int netNo, Set<Item> destinations) {
    if (!(item instanceof Pin pin)
        || pin.firstLayer() != pin.lastLayer()
        || router.settings.autorouter == null
        || !Boolean.TRUE.equals(router.settings.autorouter.planeAsPriority)) {
      return;
    }
    Double gapUm = router.settings.autorouter.planeShareGapUm;
    double shareGapUm = gapUm != null ? gapUm : 500.0;
    if (shareGapUm <= 0) {
      return;
    }
    int resolution = Math.max(1, router.board.communication.resolution);
    double maxGap = Unit.scale(shareGapUm * resolution, Unit.UM, router.board.communication.unit);
    IntBox box = pin.boundingBox();
    Set<Item> inPlane = new java.util.TreeSet<>();
    for (Item plane : List.copyOf(destinations)) {
      inPlane.addAll(plane.getConnectedSet(netNo));
    }
    for (Item other : inPlane) {
      if (other instanceof Pin otherPin
          && otherPin != pin
          && otherPin.isOnLayer(pin.firstLayer())
          && boxGap(box, otherPin.boundingBox()) <= maxGap) {
        destinations.add(otherPin);
      }
    }
  }

  private Set<Item> planeEscapeObstacles(
      Item item, int netNo, Set<Item> connectedSet, Set<Item> destinations) {
    if (!(item instanceof Pin pin)
        || router.settings.autorouter == null
        || !Boolean.TRUE.equals(router.settings.autorouter.planeAsPriority)) {
      return Set.of();
    }
    return router.board.planeEscapeObstacles(pin, netNo, connectedSet, destinations);
  }

  private static double boxGap(IntBox a, IntBox b) {
    double dx = Math.max(0, Math.max(b.ll.x - a.ur.x, a.ll.x - b.ur.x));
    double dy = Math.max(0, Math.max(b.ll.y - a.ur.y, a.ll.y - b.ur.y));
    return Math.hypot(dx, dy);
  }

  /**
   * Routes one connection of {@code item}. A pad of a plane net is first routed to its planes on
   * power layers only, so that it drops into the plane through a via of its own instead of reaching
   * a neighbouring pad of the same net on the outer layers (a short trace costs less than a via,
   * and every unconnected pad of the net used to be a valid destination). Only when no route to the
   * plane is found is the rest of the net accepted as destination, so a pad with no room for a via
   * still gets connected.
   *
   * <p>With {@code router.strict_drc} the attempt runs inside an undo snapshot: the maze rips its
   * victims before it inserts the new connection, and if the attempt changed the board and still
   * did not route (the insertion failed, strict DRC rejected the new copper, or an exception
   * escaped), the snapshot is undone and the ripped connections are back. Without it, a rejected or
   * failed attempt leaves its victims ripped for later items and passes to route again, as before;
   * measured on the fixture suite that exploration completed more connections.
   */
  AutorouteAttemptResult route(
      Item item,
      int routeNetNo,
      SortedSet<Item> rippedItemList,
      Map<Item, Integer> ripupCosts,
      int ripupPassNo) {
    RoutingBoard board = router.board;
    Attempt attempt = new Attempt(board, rippedItemList, router.settings.isStrictDrc());
    AutorouteAttemptResult result = null;
    try {
      result =
          routeAttempt(item, routeNetNo, rippedItemList, ripupCosts, ripupPassNo, attempt, true);
      if (aimedAtPlaneOnly
          && (result.state == AutorouteAttemptState.FAILED
              || result.state == AutorouteAttemptState.INSERT_ERROR)) {
        if (attempt.rollsBack) {
          // The search to the whole net starts from the board before the failed attempt.
          attempt.rollbackForRetry();
        }
        result =
            routeAttempt(item, routeNetNo, rippedItemList, ripupCosts, ripupPassNo, attempt, false);
      }
      return result;
    } finally {
      attempt.finish(result != null && result.state == AutorouteAttemptState.ROUTED);
    }
  }

  /**
   * Routes a connection of the net from {@code startSet} to the copper of {@code destSet}, not to
   * its pours, without ripping other connections. Returns the result of the maze search, or a
   * rejection if the new copper violates clearance; the caller undoes a connection it does not
   * keep. The connection is not pulled tight: the via optimizer keeps a via inside the pour
   * polygon, not inside the fill island it connects, and could move it off the copper it joins.
   */
  AutorouteAttemptResult routeWithoutRipup(int routeNetNo, Set<Item> startSet, Set<Item> destSet) {
    RoutingBoard board = router.board;
    AutorouteControl control =
        new AutorouteControl(
            board,
            routeNetNo,
            router.settings,
            router.settings.getViaCosts(),
            router.getTraceCosts());
    control.ripupAllowed = false;
    control.poursAreTargets = false;
    control.removeUnconnectedVias = router.isRemoveUnconnectedVias();
    int maxItemIdBefore = board.communication.idGenerator.maxGeneratedId();
    AutorouteEngine engine =
        board.initAutoroute(
            routeNetNo,
            control.traceClearanceClassIndex,
            router.thread,
            new TimeLimit(100000),
            router.isRetainAutorouteDatabase());
    AutorouteAttemptResult result =
        engine.autorouteConnection(startSet, destSet, control, new TreeSet<>(), new HashMap<>());
    if (result.state != AutorouteAttemptState.ROUTED) {
      return result;
    }
    AutorouteAttemptResult rejection =
        BatchAutorouter.strictDrcRejection(board, routeNetNo, maxItemIdBefore);
    return rejection != null ? rejection : result;
  }

  /** The undo snapshot around a strict-DRC connection attempt; see {@link #route}. */
  private static final class Attempt {
    private final RoutingBoard board;
    private final SortedSet<Item> rippedItemList;
    private final boolean rollsBack;
    private long contentVersionAtSnapshot;

    Attempt(RoutingBoard board, SortedSet<Item> rippedItemList, boolean rollsBack) {
      this.board = board;
      this.rippedItemList = rippedItemList;
      this.rollsBack = rollsBack;
      begin();
    }

    private void begin() {
      if (!rollsBack) {
        return;
      }
      contentVersionAtSnapshot = board.itemList.getContentVersion();
      board.generateSnapshot();
    }

    /** Undoes a failed attempt that changed the board and opens a new snapshot for a retry. */
    void rollbackForRetry() {
      finish(false);
      begin();
    }

    void finish(boolean keep) {
      if (!rollsBack) {
        return;
      }
      if (keep || board.itemList.getContentVersion() == contentVersionAtSnapshot) {
        board.popSnapshot();
      } else {
        board.undo(null);
        // The ripped connections are back on the board.
        rippedItemList.clear();
      }
    }
  }

  private AutorouteAttemptResult routeAttempt(
      Item item,
      int routeNetNo,
      SortedSet<Item> rippedItemList,
      Map<Item, Integer> ripupCosts,
      int ripupPassNo,
      Attempt attempt,
      boolean planeOnly) {
    aimedAtPlaneOnly = false;
    try {
      Net routeNet = router.board.rules.nets.get(routeNetNo);
      boolean containsPlane = routeNet != null && routeNet.containsPlane();
      boolean routedFirst = routeNet != null && routeNet.isRoutedFirst();
      int signalViaCosts =
          routedFirst ? router.settings.getPriorityViaCosts() : router.settings.getViaCosts();
      int currentViaCosts =
          containsPlane
              ? Math.min(router.settings.getPlaneViaCosts(), signalViaCosts)
              : signalViaCosts;

      AutorouteControl autorouteControl =
          new AutorouteControl(
              router.board, routeNetNo, router.settings, currentViaCosts, router.getTraceCosts());
      autorouteControl.ripupAllowed = true;
      autorouteControl.ripupCosts = router.getStartRipupCosts() * ripupPassNo;
      autorouteControl.netsNotToRip =
          routedFirst
              ? router.ripupMemory.netsNotToRip(routeNetNo)
              : router.netsNotToRipFor(routeNetNo);
      autorouteControl.removeUnconnectedVias = router.isRemoveUnconnectedVias();

      Set<Item> unconnectedSet = item.getUnconnectedSet(routeNetNo);
      if (unconnectedSet.isEmpty()) {
        return new AutorouteAttemptResult(AutorouteAttemptState.NO_UNCONNECTED_NETS);
      }

      Set<Item> connectedSet = item.getConnectedSet(routeNetNo);
      Set<Item> routeStartSet;
      Set<Item> routeDestSet;
      Set<Item> planeEscapeObstacles = Set.of();
      if (containsPlane) {
        for (Item currentItem : connectedSet) {
          if (currentItem instanceof ConductionArea ca && !ca.isFractured()) {
            return new AutorouteAttemptResult(AutorouteAttemptState.CONNECTED_TO_PLANE);
          }
        }
        routeStartSet = connectedSet;
        routeDestSet = unconnectedSet;
        if (planeOnly) {
          Set<Item> planes = router.board.powerLayerPlanes(unconnectedSet);
          planes.removeIf(p -> p instanceof ConductionArea ca && ca.isFractured());
          if (!planes.isEmpty()) {
            addSharablePads(item, routeNetNo, planes);
            routeDestSet = planes;
            aimedAtPlaneOnly = true;
            planeEscapeObstacles = planeEscapeObstacles(item, routeNetNo, connectedSet, planes);
          }
        }
        Set<Item> filteredStartSet = new TreeSet<>();
        for (Item it : routeStartSet) {
          if (!(it instanceof ConductionArea ca && ca.isFractured())) {
            filteredStartSet.add(it);
          }
        }
        if (!filteredStartSet.isEmpty()) {
          routeStartSet = filteredStartSet;
        }
        Set<Item> filteredDestSet = new TreeSet<>();
        for (Item it : routeDestSet) {
          if (!(it instanceof ConductionArea ca && ca.isFractured())) {
            filteredDestSet.add(it);
          }
        }
        if (!filteredDestSet.isEmpty()) {
          routeDestSet = filteredDestSet;
        }
      } else {
        routeStartSet = unconnectedSet;
        routeDestSet = connectedSet;
      }
      router.setAirLine(AutorouteAirlineCalculator.calculateAirline(routeStartSet, routeDestSet));

      double maxMilliseconds = 100000 * Math.pow(2, ripupPassNo - 1);
      maxMilliseconds = Math.min(maxMilliseconds, Integer.MAX_VALUE);
      TimeLimit timeLimit = new TimeLimit((int) maxMilliseconds);

      // The obstacles of a plane escape are set around the class-width search below only. A necked
      // search builds its own engine without them and would reach the plane through the copper
      // they block, so a plane escape is never necked: when it fails, the search to the whole net
      // follows and may neck.
      boolean escaping = !planeEscapeObstacles.isEmpty();

      // start_necked: a net that needed the neck before routes at the neck width, ripping like any
      // search, and
      // that result stands. The class-width search it skips would use up the space before the
      // necked retry
      // and, when it routes, displace the neighbours the neck fits beside.
      if (router.startNecked
          && !escaping
          && router.settings.getNeckWidthUm() > 0
          && router.neckedRouteNets.contains(routeNetNo)) {
        int beforeStart = router.board.communication.idGenerator.maxGeneratedId();
        AutorouteAttemptResult started =
            retryConnectionNecked(
                routeNetNo,
                autorouteControl,
                currentViaCosts,
                routeStartSet,
                routeDestSet,
                rippedItemList,
                ripupCosts,
                ripupPassNo,
                timeLimit,
                /* planned= */ true,
                /* quiet= */ true);
        if (started != null) {
          if (started.state != AutorouteAttemptState.ROUTED) {
            router.startNeckedFailed++;
            return started;
          }
          AutorouteAttemptResult rejection =
              applyStrictDrcAfterRoute(routeNetNo, beforeStart, ripupPassNo);
          if (rejection != null) {
            return rejection;
          }
          router.startNeckedRouted++;
          return started;
        }
      }

      // The obstacles below hold for this search only: rooms and drill pages built with them must
      // not be retained for the next connection of the net.
      AutorouteEngine autorouteEngine =
          router.board.initAutoroute(
              routeNetNo,
              autorouteControl.traceClearanceClassIndex,
              router.thread,
              timeLimit,
              router.isRetainAutorouteDatabase() && !escaping);
      int maxItemIdBeforeRoute = router.board.communication.idGenerator.maxGeneratedId();

      long mazeSearchStart = BatchAutorouter.isBenchmarkProfileEnabled() ? System.nanoTime() : 0;
      AutorouteAttemptResult searchResult;
      planeEscapeObstacles.forEach(obstacle -> obstacle.setPlaneEscapeObstacle(true));
      try {
        searchResult =
            autorouteEngine.autorouteConnection(
                routeStartSet, routeDestSet, autorouteControl, rippedItemList, ripupCosts);
      } finally {
        planeEscapeObstacles.forEach(obstacle -> obstacle.setPlaneEscapeObstacle(false));
        if (escaping) {
          router.board.finishAutoroute();
        }
      }
      final AutorouteAttemptResult autorouteResult = searchResult;
      if (BatchAutorouter.isBenchmarkProfileEnabled()) {
        router.addProfileMazeSearchNanos(System.nanoTime() - mazeSearchStart);
      }

      if (autorouteResult.state == AutorouteAttemptState.ROUTED) {
        int maxItemIdBeforeOpt = router.board.communication.idGenerator.maxGeneratedId();
        FRLogger.trace(
            "compare_trace_opt_changed_area_before net="
                + routeNetNo
                + ", maxItemId="
                + maxItemIdBeforeOpt);
        long pullTightStart = BatchAutorouter.isBenchmarkProfileEnabled() ? System.nanoTime() : 0;
        router.board.optChangedArea(
            new int[0],
            null,
            router.getTracePullTightAccuracy(),
            autorouteControl.traceCosts,
            router.thread);
        if (BatchAutorouter.isBenchmarkProfileEnabled()) {
          router.addProfileOptChangedAreaNanos(System.nanoTime() - pullTightStart);
        }
        int maxItemIdAfterOpt = router.board.communication.idGenerator.maxGeneratedId();
        FRLogger.trace(
            "compare_trace_opt_changed_area_after net="
                + routeNetNo
                + ", maxItemId="
                + maxItemIdAfterOpt
                + ", delta="
                + (maxItemIdAfterOpt - maxItemIdBeforeOpt));
      }

      if ((autorouteResult.state == AutorouteAttemptState.FAILED
              || autorouteResult.state == AutorouteAttemptState.INSERT_ERROR)
          && !escaping
          && router.settings.getNeckWidthUm() > 0) {
        if (attempt.rollsBack) {
          // The necked retry starts from the board before the failed attempt.
          attempt.rollbackForRetry();
          maxItemIdBeforeRoute = router.board.communication.idGenerator.maxGeneratedId();
        }
        AutorouteAttemptResult neckedResult =
            retryConnectionNecked(
                routeNetNo,
                autorouteControl,
                currentViaCosts,
                routeStartSet,
                routeDestSet,
                rippedItemList,
                ripupCosts,
                ripupPassNo,
                timeLimit,
                /* planned= */ false,
                /* quiet= */ false);
        if (neckedResult != null) {
          AutorouteAttemptResult strictResult =
              applyStrictDrcAfterRoute(routeNetNo, maxItemIdBeforeRoute, ripupPassNo);
          if (strictResult != null) {
            return strictResult;
          }
          return neckedResult;
        }
      }

      if (autorouteResult.state == AutorouteAttemptState.ROUTED) {
        AutorouteAttemptResult strictResult =
            applyStrictDrcAfterRoute(routeNetNo, maxItemIdBeforeRoute, ripupPassNo);
        if (strictResult != null) {
          return strictResult;
        }
      }

      return autorouteResult;
    } catch (Exception e) {
      FRLogger.error("Error during routing passes", e);
      return new AutorouteAttemptResult(AutorouteAttemptState.FAILED);
    }
  }

  private AutorouteAttemptResult retryConnectionNecked(
      int routeNetNo,
      AutorouteControl originalControl,
      int viaCosts,
      Set<Item> routeStartSet,
      Set<Item> routeDestSet,
      SortedSet<Item> rippedItemList,
      Map<Item, Integer> ripupCosts,
      int ripupPassNo,
      TimeLimit timeLimit,
      boolean planned,
      boolean quiet) {
    int neckHalfWidth = AutorouteControl.neckHalfWidth(router.board, router.settings);
    boolean narrowerSomewhere = false;
    for (int i = 0; i < originalControl.layerCount; i++) {
      if (originalControl.layerActive[i] && originalControl.traceHalfWidth[i] > neckHalfWidth) {
        narrowerSomewhere = true;
        break;
      }
    }
    if (!narrowerSomewhere) {
      return null;
    }

    AutorouteControl neckControl =
        new AutorouteControl(
            router.board, routeNetNo, router.settings, viaCosts, router.getTraceCosts());
    neckControl.ripupAllowed = true;
    neckControl.ripupCosts = router.getStartRipupCosts() * ripupPassNo;
    neckControl.netsNotToRip = originalControl.netsNotToRip;
    neckControl.removeUnconnectedVias = router.isRemoveUnconnectedVias();
    for (int i = 0; i < neckControl.layerCount; i++) {
      int compensation = neckControl.compensatedTraceHalfWidth[i] - neckControl.traceHalfWidth[i];
      neckControl.traceHalfWidth[i] = Math.min(neckControl.traceHalfWidth[i], neckHalfWidth);
      neckControl.compensatedTraceHalfWidth[i] = neckControl.traceHalfWidth[i] + compensation;
      neckControl.updateTraceClearanceOffset(i, router.board);
    }

    AutorouteEngine neckEngine =
        router.board.initAutoroute(
            routeNetNo,
            neckControl.traceClearanceClassIndex,
            router.thread,
            timeLimit,
            router.isRetainAutorouteDatabase());
    long neckMazeSearchStart = BatchAutorouter.isBenchmarkProfileEnabled() ? System.nanoTime() : 0;
    AutorouteAttemptResult neckResult =
        neckEngine.autorouteConnection(
            routeStartSet, routeDestSet, neckControl, rippedItemList, ripupCosts);
    if (BatchAutorouter.isBenchmarkProfileEnabled()) {
      router.addProfileMazeSearchNanos(System.nanoTime() - neckMazeSearchStart);
    }
    if (neckResult.state != AutorouteAttemptState.ROUTED) {
      return planned ? neckResult : null;
    }

    router.neckedRouteNets.add(routeNetNo);

    long neckPullTightStart = BatchAutorouter.isBenchmarkProfileEnabled() ? System.nanoTime() : 0;
    router.board.optChangedArea(
        new int[0],
        null,
        router.getTracePullTightAccuracy(),
        neckControl.traceCosts,
        router.thread);
    if (BatchAutorouter.isBenchmarkProfileEnabled()) {
      router.addProfileOptChangedAreaNanos(System.nanoTime() - neckPullTightStart);
    }
    if (!quiet) {
      Net routeNet = router.board.rules.nets.get(routeNetNo);
      FRLogger.info(
          (planned ? "Retry plan routed net '" : "Necked retry routed net '")
              + (routeNet != null ? routeNet.name : "#" + routeNetNo)
              + "' at "
              + router.settings.getNeckWidthUm()
              + " um trace width.");
    }
    return neckResult;
  }

  /**
   * Returns a rejection if {@code router.strict_drc} is enabled and the new copper of the
   * connection violates clearance, or null if the connection may be kept. The caller's attempt
   * snapshot rolls a rejected connection back. Without strict DRC the route is kept: removing only
   * its new items would leave the connections it ripped up unrouted.
   */
  private AutorouteAttemptResult applyStrictDrcAfterRoute(
      int routeNetNo, int maxItemIdBefore, int ripupPassNo) {
    if (!router.settings.isStrictDrc()) {
      return null;
    }
    AutorouteAttemptResult rejection =
        BatchAutorouter.strictDrcRejection(router.board, routeNetNo, maxItemIdBefore);
    if (rejection != null) {
      FRLogger.trace(
          "AutorouteConnectionRouter.apply_strict_drc",
          "strict_drc_rejection",
          "pass=" + ripupPassNo + ", net=" + routeNetNo + ", reason=" + rejection.details,
          "",
          null);
    }
    return rejection;
  }
}
