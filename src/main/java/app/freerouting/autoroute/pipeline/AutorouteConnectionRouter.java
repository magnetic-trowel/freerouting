package app.freerouting.autoroute.pipeline;

import app.freerouting.autoroute.AutorouteAttemptResult;
import app.freerouting.autoroute.AutorouteAttemptState;
import app.freerouting.autoroute.maze.AutorouteControl;
import app.freerouting.autoroute.maze.AutorouteEngine;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.ConductionArea;
import app.freerouting.board.model.items.Item;
import app.freerouting.datastructures.TimeLimit;
import app.freerouting.logger.FRLogger;
import app.freerouting.rules.Net;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/** Executes one autoroute connection, including necked retry and strict-DRC recovery. */
final class AutorouteConnectionRouter {

  private static final int TIME_LIMIT_TO_PREVENT_ENDLESS_LOOP = 1000;

  private final BatchAutorouter router;

  AutorouteConnectionRouter(BatchAutorouter router) {
    this.router = router;
  }

  /**
   * Routes one connection. With {@code router.strict_drc} the attempt runs inside an undo snapshot:
   * the maze rips its victims before it inserts the new connection, and if the attempt changed the
   * board and still did not route (the insertion failed, strict DRC rejected the new copper, or an
   * exception escaped), the snapshot is undone and the ripped connections are back. Without it, a
   * rejected or failed attempt leaves its victims ripped for later items and passes to route again,
   * as before; measured on the fixture suite that exploration completed more connections.
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
      result = routeAttempt(item, routeNetNo, rippedItemList, ripupCosts, ripupPassNo, attempt);
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
      Attempt attempt) {
    try {
      Net routeNet = router.board.rules.nets.get(routeNetNo);
      boolean containsPlane = routeNet != null && routeNet.containsPlane();
      int currentViaCosts =
          containsPlane ? router.settings.getPlaneViaCosts() : router.settings.getViaCosts();

      AutorouteControl autorouteControl =
          new AutorouteControl(
              router.board, routeNetNo, router.settings, currentViaCosts, router.getTraceCosts());
      autorouteControl.ripupAllowed = true;
      autorouteControl.ripupCosts = router.getStartRipupCosts() * ripupPassNo;
      autorouteControl.removeUnconnectedVias = router.isRemoveUnconnectedVias();

      Set<Item> unconnectedSet = item.getUnconnectedSet(routeNetNo);
      if (unconnectedSet.isEmpty()) {
        return new AutorouteAttemptResult(AutorouteAttemptState.NO_UNCONNECTED_NETS);
      }

      Set<Item> connectedSet = item.getConnectedSet(routeNetNo);
      Set<Item> routeStartSet;
      Set<Item> routeDestSet;
      if (containsPlane) {
        for (Item currentItem : connectedSet) {
          if (currentItem instanceof ConductionArea) {
            return new AutorouteAttemptResult(AutorouteAttemptState.CONNECTED_TO_PLANE);
          }
        }
        routeStartSet = connectedSet;
        routeDestSet = unconnectedSet;
      } else {
        routeStartSet = unconnectedSet;
        routeDestSet = connectedSet;
      }

      router.setAirLine(AutorouteAirlineCalculator.calculateAirline(routeStartSet, routeDestSet));

      double maxMilliseconds = 100000 * Math.pow(2, ripupPassNo - 1);
      maxMilliseconds = Math.min(maxMilliseconds, Integer.MAX_VALUE);
      TimeLimit timeLimit = new TimeLimit((int) maxMilliseconds);

      // start_necked: a net that needed the neck before routes at the neck width, ripping like any
      // search, and
      // that result stands. The class-width search it skips would use up the space before the
      // necked retry
      // and, when it routes, displace the neighbours the neck fits beside.
      if (router.startNecked
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

      AutorouteEngine autorouteEngine =
          router.board.initAutoroute(
              routeNetNo,
              autorouteControl.traceClearanceClassIndex,
              router.thread,
              timeLimit,
              router.isRetainAutorouteDatabase());
      int maxItemIdBeforeRoute = router.board.communication.idGenerator.maxGeneratedId();

      long mazeSearchStart = BatchAutorouter.isBenchmarkProfileEnabled() ? System.nanoTime() : 0;
      final AutorouteAttemptResult autorouteResult =
          autorouteEngine.autorouteConnection(
              routeStartSet, routeDestSet, autorouteControl, rippedItemList, ripupCosts);
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
            router.thread,
            TIME_LIMIT_TO_PREVENT_ENDLESS_LOOP);
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
        router.thread,
        TIME_LIMIT_TO_PREVENT_ENDLESS_LOOP);
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
   * Returns a rejection if strict DRC applies and the new copper of the connection violates
   * clearance, or null if the connection may be kept. With {@code router.strict_drc} the caller's
   * attempt snapshot rolls a rejected connection back. From pass 3 without it, the new traces and
   * vias are removed and the connections the attempt ripped stay ripped.
   */
  private AutorouteAttemptResult applyStrictDrcAfterRoute(
      int routeNetNo, int maxItemIdBefore, int ripupPassNo) {
    boolean isStrictPass = router.settings.isStrictDrc() || ripupPassNo >= 3;
    if (!isStrictPass) {
      return null;
    }
    AutorouteAttemptResult rejection =
        router.settings.isStrictDrc()
            ? BatchAutorouter.strictDrcRejection(router.board, routeNetNo, maxItemIdBefore)
            : BatchAutorouter.enforceStrictDrc(router.board, routeNetNo, maxItemIdBefore);
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
