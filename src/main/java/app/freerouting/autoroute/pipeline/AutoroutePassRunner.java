package app.freerouting.autoroute.pipeline;

import static java.util.Collections.shuffle;

import app.freerouting.autoroute.AutorouteAttemptResult;
import app.freerouting.autoroute.AutorouteAttemptState;
import app.freerouting.autoroute.DesignBlocks;
import app.freerouting.autoroute.PerformanceProfiler;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.items.Trace;
import app.freerouting.board.model.items.Via;
import app.freerouting.board.model.structure.Unit;
import app.freerouting.core.RouterCounters;
import app.freerouting.core.scoring.BoardStatistics;
import app.freerouting.drc.DesignRulesChecker;
import app.freerouting.drc.NetRoutingLedger;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.geometry.planar.Point;
import app.freerouting.logger.FRLogger;
import app.freerouting.rules.Net;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/** Executes one autoroute pass on the calling thread. */
final class AutoroutePassRunner {

  private final BatchAutorouter router;
  private Set<Integer> previousIncompleteNets = Collections.emptySet();
  private int previousIncompleteCount = -1;
  private int stagnationCount = 0;

  /**
   * Failed routing attempts per item id in the current pass loop. A pass loop starts with none, so
   * a later loop on the same or a copied board does not inherit the failures of an earlier one.
   */
  private final Map<Integer, Integer> failureCounts = new HashMap<>();

  AutoroutePassRunner(BatchAutorouter router) {
    this.router = router;
  }

  boolean runSingleThread(int passNo) {
    long passStartTime = System.currentTimeMillis();
    if (BatchAutorouter.isBenchmarkProfileEnabled()) {
      router.resetPassProfile();
    }
    try {
      long itemSelectionStart = BatchAutorouter.isBenchmarkProfileEnabled() ? System.nanoTime() : 0;
      List<Item> autorouteItemList = router.getAutorouteItems(router.board);
      dropDesignBlockedItems(autorouteItemList);
      if (BatchAutorouter.isBenchmarkProfileEnabled()) {
        router.profileItemSelectionNanos += System.nanoTime() - itemSelectionStart;
      }

      if (autorouteItemList.isEmpty()) {
        router.airLine = null;
        return false;
      }

      if (passNo > 1) {
        reorderSingleThreadItems(autorouteItemList, passNo);
      }

      long initialProgressStatisticsStart =
          BatchAutorouter.isBenchmarkProfileEnabled() ? System.nanoTime() : 0;
      // Statistics only feed board-updated listeners; a listener-less attempt (optimizer
      // candidate) discards them.
      router.progressStatistics =
          router.boardUpdatedEventListeners.isEmpty() ? null : router.board.getStatistics(false);
      router.progressItemsSinceStatistics = 0;
      final BoardStatistics stats = router.progressStatistics;
      if (BatchAutorouter.isBenchmarkProfileEnabled()) {
        router.profileBoardStatisticsNanos += System.nanoTime() - initialProgressStatisticsStart;
      }

      int itemsToGoCount = autorouteItemList.size();
      RouterCounters routerCounters = new RouterCounters();
      routerCounters.phase = "autoroute";
      routerCounters.passCount = passNo;
      routerCounters.queuedToBeRoutedCount = itemsToGoCount;
      routerCounters.skippedCount = 0;
      routerCounters.rippedCount = 0;
      routerCounters.failedToBeRoutedCount = 0;
      routerCounters.routedCount = 0;

      routerCounters.incompleteCount = router.calculateIncompleteCount(router.board);
      logIncompleteDetails(passNo, itemsToGoCount, routerCounters);
      router.fireBoardUpdatedEvent(stats, routerCounters, router.board);

      int rippedItemCount = 0;
      int notRouted = 0;
      int routed = 0;
      int skipped = 0;
      for (Item currentItem : autorouteItemList) {
        if (router.thread.isStopAutoRouterRequested()) {
          break;
        }

        for (int i = 0; i < currentItem.netCount(); i++) {
          if (router.thread.isStopAutoRouterRequested()) {
            break;
          }

          if (router.settings.autorouter.maxItems != null
              && router.settings.autorouter.maxItems > 0
              && router.totalItemsRouted >= router.settings.autorouter.maxItems) {
            router.job.logInfo(
                "Max items limit reached ("
                    + router.settings.autorouter.maxItems
                    + "). Stopping auto-router.");
            router.thread.requestStop();
            break;
          }
          router.totalItemsRouted++;
          router.board.startMarkingChangedArea();

          SortedSet<Item> rippedItemList = new TreeSet<>();
          Map<Item, Integer> rippedItemCosts = new LinkedHashMap<>();
          final int netItemsBefore =
              FRLogger.isTraceEnabled()
                  ? router.board.getConnectableItems(currentItem.getNetNumber(i)).size()
                  : 0;
          if (BatchAutorouter.isBenchmarkProfileEnabled()) {
            router.profileRouteItemCount++;
            Net routeNet = router.board.rules.nets.get(currentItem.getNetNumber(i));
            if (routeNet != null && routeNet.containsPlane()) {
              router.profilePlaneItemCount++;
            }
          }

          long routeItemStart = BatchAutorouter.isBenchmarkProfileEnabled() ? System.nanoTime() : 0;
          PerformanceProfiler.start("autoroute_item");
          final AutorouteAttemptResult autorouterResult =
              router.autorouteItem(
                  currentItem,
                  currentItem.getNetNumber(i),
                  rippedItemList,
                  rippedItemCosts,
                  passNo);
          PerformanceProfiler.end("autoroute_item");
          if (BatchAutorouter.isBenchmarkProfileEnabled()) {
            router.profileAutorouteItemNanos += System.nanoTime() - routeItemStart;
          }

          if (FRLogger.isTraceEnabled()) {
            logRippedItems(currentItem, i, rippedItemList, rippedItemCosts);
            logTraceRouteComparison(
                currentItem, i, autorouterResult, rippedItemList, netItemsBefore);
          }

          if (autorouterResult.state == AutorouteAttemptState.ROUTED) {
            ++routed;
          } else if ((autorouterResult.state == AutorouteAttemptState.ALREADY_CONNECTED)
              || (autorouterResult.state == AutorouteAttemptState.NO_UNCONNECTED_NETS)
              || (autorouterResult.state == AutorouteAttemptState.CONNECTED_TO_PLANE)) {
            ++skipped;
          } else {
            Net net = router.board.rules.nets.get(currentItem.getNetNumber(i));
            String netName = net != null ? net.name : "net#" + currentItem.getNetNumber(i);
            router.job.logDebug("Autorouter " + autorouterResult.details);
            int failureCount = failureCounts.merge(currentItem.getId(), 1, Integer::sum);
            int netNo = currentItem.getNetNumber(i);
            if (failureCount >= 2 && !designBlocked(router.board, currentItem, netNo)) {
              List<Item> tracesToRip = failedNetRipItems(router.board, currentItem, netNo);
              if (!tracesToRip.isEmpty()) {
                router.board.removeItems(tracesToRip);
              }
            }
            if (itemsToGoCount <= 5 || failureCount >= 3) {
              router.job.logDebug(
                  "Pass #"
                      + passNo
                      + ": Failed to route "
                      + currentItem.getClass().getSimpleName()
                      + " on net '"
                      + netName
                      + "' ("
                      + itemsToGoCount
                      + " items remaining, "
                      + failureCount
                      + " failures). State: "
                      + autorouterResult.state);
            }
            ++notRouted;
          }
          --itemsToGoCount;
          rippedItemCount += rippedItemList.size();
          updateProgress(
              routerCounters, itemsToGoCount, rippedItemCount, notRouted, routed, skipped);
        }
      }

      logTailRemoval(passNo, true);
      if (router.removeUnconnectedVias) {
        router.removeTails(Item.StopConnectionOption.NONE);
      } else {
        router.removeTails(Item.StopConnectionOption.FANOUT_VIA);
      }
      logTailRemoval(passNo, false);

      long finalStatisticsStart =
          BatchAutorouter.isBenchmarkProfileEnabled() ? System.nanoTime() : 0;
      long finalBoardStatisticsStart =
          BatchAutorouter.isBenchmarkProfileEnabled() ? System.nanoTime() : 0;
      final BoardStatistics boardStatistics =
          router.boardUpdatedEventListeners.isEmpty() ? null : router.board.getStatistics();
      if (BatchAutorouter.isBenchmarkProfileEnabled()) {
        router.profileBoardStatisticsNanos += System.nanoTime() - finalBoardStatisticsStart;
        router.profileIntermediateStatisticsNanos += System.nanoTime() - finalStatisticsStart;
      }
      routerCounters.passCount = passNo;
      routerCounters.queuedToBeRoutedCount = itemsToGoCount;
      routerCounters.skippedCount = skipped;
      routerCounters.rippedCount = rippedItemCount;
      routerCounters.failedToBeRoutedCount = notRouted;
      routerCounters.routedCount = routed;
      Set<Integer> currentIncompleteNets = new TreeSet<>();
      routerCounters.incompleteCount =
          router.calculateIncompleteCount(router.board, currentIncompleteNets);
      router.fireBoardUpdatedEvent(boardStatistics, routerCounters, router.board);

      if (this.previousIncompleteCount >= 0
          && routerCounters.incompleteCount >= this.previousIncompleteCount) {
        this.stagnationCount++;
      } else {
        this.stagnationCount = 0;
      }
      this.previousIncompleteCount = routerCounters.incompleteCount;
      this.previousIncompleteNets = currentIncompleteNets;

      long passDuration = System.currentTimeMillis() - passStartTime;
      int currentRipupCost = router.startRipupCosts * passNo;
      // Optimizer candidates run passes of their own on worker threads; the job's pass history
      // must not collect them (it is printed and cleared only when a pass loop ends).
      if (!router.isOptimizerAutorouter) {
        PerformanceProfiler.recordPass(
            passNo, routerCounters.incompleteCount, passDuration, currentRipupCost);
      }
      router.logBenchmarkProfile(passNo);

      router.airLine = null;
      return routed > 0 || notRouted > 0;
    } catch (Exception e) {
      router.job.logError("Something went wrong during the auto-routing", e);
      router.lastPassFailure = e;
      router.airLine = null;
      return false;
    }
  }

  private void reorderSingleThreadItems(List<Item> autorouteItemList, int passNo) {
    List<Item> planeItems = new ArrayList<>();
    List<Item> signalItems = new ArrayList<>();
    for (Item item : autorouteItemList) {
      if (router.isPlaneItem(item, router.board)) {
        planeItems.add(item);
      } else {
        signalItems.add(item);
      }
    }

    if (signalItems.isEmpty()) {
      return;
    }

    Comparator<Item> deterministicItemComparator =
        Comparator.comparingInt((Item item) -> item.netCount() > 0 ? item.getNetNumber(0) : 0)
            .thenComparingInt(Item::getId);

    Integer orderSeed =
        router.settings.autorouter != null ? router.settings.autorouter.itemOrderSeed : null;
    if (this.stagnationCount >= 2 || orderSeed != null) {
      signalItems.sort(deterministicItemComparator);
      long seed = (long) passNo * 1000003L + router.board.rules.nets.maxNetNumber();
      if (orderSeed != null) {
        seed = seed * 31L + orderSeed;
      }
      shuffle(signalItems, new Random(seed));
      router.job.logDebug(
          "Pass #"
              + passNo
              + ": applied deterministic permutation to "
              + signalItems.size()
              + " signal items (stagnation count: "
              + this.stagnationCount
              + ").");
    } else if (!this.previousIncompleteNets.isEmpty()) {
      List<Item> persistentItems = new ArrayList<>();
      List<Item> otherItems = new ArrayList<>();
      for (Item item : signalItems) {
        boolean isPersistent = false;
        for (int i = 0; i < item.netCount(); i++) {
          if (this.previousIncompleteNets.contains(item.getNetNumber(i))) {
            isPersistent = true;
            break;
          }
        }
        if (isPersistent) {
          persistentItems.add(item);
        } else {
          otherItems.add(item);
        }
      }
      persistentItems.sort(deterministicItemComparator);
      otherItems.sort(deterministicItemComparator);
      signalItems.clear();
      signalItems.addAll(persistentItems);
      signalItems.addAll(otherItems);
    } else {
      signalItems.sort(deterministicItemComparator);
    }

    autorouteItemList.clear();
    autorouteItemList.addAll(planeItems);
    autorouteItemList.addAll(signalItems);
  }

  void resetAntiOscillationState() {
    this.previousIncompleteNets = Collections.emptySet();
    this.previousIncompleteCount = -1;
    this.stagnationCount = 0;
  }

  /** Starts a pass loop: no anti-oscillation state and no failure counts carried over. */
  void startPassLoop() {
    resetAntiOscillationState();
    this.failureCounts.clear();
  }

  private void logIncompleteDetails(int passNo, int itemsToGoCount, RouterCounters counters) {
    if (counters.incompleteCount <= 0) {
      return;
    }
    router.job.logDebug(
        "Pass #"
            + passNo
            + ": "
            + counters.incompleteCount
            + " incompletes across "
            + itemsToGoCount
            + " items to route");
    NetRoutingLedger ledger = router.board.routingLedger();
    for (int netNumber : ledger.incompleteNetNumbers()) {
      Net net = router.board.rules.nets.get(netNumber);
      String netName = net != null ? net.name : "net#" + netNumber;
      router.job.logDebug(
          "  Net '" + netName + "' has " + ledger.incompleteCount(netNumber) + " incomplete(s)");
    }
  }

  private void logRippedItems(
      Item currentItem,
      int netIndex,
      SortedSet<Item> rippedItemList,
      Map<Item, Integer> rippedItemCosts) {
    for (Item rippedItem : rippedItemList) {
      StringBuilder rippedNets = new StringBuilder();
      for (int netIx = 0; netIx < rippedItem.netCount(); netIx++) {
        if (netIx > 0) {
          rippedNets.append('|');
        }
        rippedNets.append(rippedItem.getNetNumber(netIx));
      }
      int ripupCost = rippedItemCosts.getOrDefault(rippedItem, -1);
      FRLogger.trace(
          "BatchAutorouter.autoroute_pass",
          "compare_trace_ripped_item",
          "source_item="
              + currentItem.getId()
              + ", source_net="
              + currentItem.getNetNumber(netIndex)
              + ", ripped_id="
              + rippedItem.getId()
              + ", ripped_type="
              + rippedItem.getClass().getSimpleName()
              + ", ripped_net_count="
              + rippedItem.netCount()
              + ", ripped_nets="
              + rippedNets
              + ", ripupCost="
              + ripupCost,
          "Net #" + currentItem.getNetNumber(netIndex) + ",Item #" + currentItem.getId(),
          router.getImpactedPoints(rippedItem));
    }
  }

  private void logTraceRouteComparison(
      Item currentItem,
      int netIndex,
      AutorouteAttemptResult result,
      SortedSet<Item> rippedItemList,
      int netItemsBefore) {
    DesignRulesChecker innerDrc = new DesignRulesChecker(router.board, null);
    innerDrc.calculateAllIncompletes();
    int tempIncomp = innerDrc.getIncompleteCount();
    int tempNetIncomp = innerDrc.getIncompleteCount(currentItem.getNetNumber(netIndex));
    int netItemsAfter = router.board.getConnectableItems(currentItem.getNetNumber(netIndex)).size();
    int maxItemId = router.board.communication.idGenerator.maxGeneratedId();
    FRLogger.trace(
        "BatchAutorouter.autoroute_pass",
        "compare_trace_route_item",
        "Routing "
            + currentItem.getClass().getSimpleName()
            + " -> result="
            + result.state
            + ", details="
            + result.details
            + ", incompletes="
            + tempIncomp
            + ", netIncomplete="
            + tempNetIncomp
            + ", ripped="
            + rippedItemList.size()
            + ", netItems="
            + netItemsBefore
            + "->"
            + netItemsAfter
            + ", maxItemId="
            + maxItemId,
        "Net #"
            + currentItem.getNetNumber(netIndex)
            + ",Item #"
            + currentItem.getId()
            + ",Type="
            + currentItem.getClass().getSimpleName(),
        router.getImpactedPoints(currentItem));
  }

  private void updateProgress(
      RouterCounters counters,
      int itemsToGoCount,
      int rippedItemCount,
      int notRouted,
      int routed,
      int skipped) {
    router.progressItemsSinceStatistics++;
    if (router.shouldFireBoardUpdate()) {
      if (router.progressItemsSinceStatistics
          >= BatchAutorouter.PROGRESS_STATISTICS_ITEM_INTERVAL) {
        long progressStatisticsStart =
            BatchAutorouter.isBenchmarkProfileEnabled() ? System.nanoTime() : 0;
        router.progressStatistics =
            router.boardUpdatedEventListeners.isEmpty() ? null : router.board.getStatistics(false);
        router.progressItemsSinceStatistics = 0;
        if (BatchAutorouter.isBenchmarkProfileEnabled()) {
          router.profileBoardStatisticsNanos += System.nanoTime() - progressStatisticsStart;
        }
      }
      counters.queuedToBeRoutedCount = itemsToGoCount;
      counters.skippedCount = skipped;
      counters.rippedCount = rippedItemCount;
      counters.failedToBeRoutedCount = notRouted;
      counters.routedCount = routed;
      counters.incompleteCount = router.calculateIncompleteCount(router.board);
      router.fireBoardUpdatedEvent(router.progressStatistics, counters, router.board);
    }
  }

  private void dropDesignBlockedItems(List<Item> items) {
    int before = items.size();
    Set<Integer> ruleBlockedNets = ruleBlockedNets(router.board);
    items.removeIf(
        item ->
            (failureCounts.containsKey(item.getId())
                    || (ruleBlockedNets != null
                        && item.netCount() == 1
                        && ruleBlockedNets.contains(item.getNetNumber(0))))
                && designBlocked(router.board, item, item.getNetNumber(0)));
    if (items.size() < before && router.job != null) {
      router.job.logDebug(
          "Not retrying "
              + (before - items.size())
              + " item(s) whose open connections the design blocks (pins on foreign copper).");
    }
  }

  static Set<Integer> ruleBlockedNets(RoutingBoard board) {
    if (board.ruleBlockedPins == null || board.ruleBlockedPins.isEmpty()) {
      return null;
    }
    Set<Integer> nets = new HashSet<>();
    for (Pin pin : board.getPins()) {
      if (pin.netCount() == 1 && board.ruleBlockedPins.contains(pin.getId())) {
        nets.add(pin.getNetNumber(0));
      }
    }
    return nets.isEmpty() ? null : nets;
  }

  static boolean designBlocked(RoutingBoard board, Item item, int netNumber) {
    return item.netCount() == 1 && DesignBlocks.connectionsBlocked(board, item, netNumber);
  }

  static List<Item> failedNetRipItems(RoutingBoard board, Item failedItem, int netNumber) {
    Collection<Item> items = board.getConnectableItems(netNumber);
    long pinCount = items.stream().filter(i -> i instanceof Pin).count();
    boolean branched = pinCount > 2;
    int offset =
        (int)
            Math.round(
                board.communication.coordinateTransform.dsnToBoard(
                    Unit.scale(1500, Unit.UM, board.communication.unit)));
    IntBox region = failedItem.boundingBox().offset(offset);
    List<Item> result = new ArrayList<>();
    for (Item i : items) {
      if ((i instanceof Trace || i instanceof Via)
          && !i.isUserFixed()
          && i.netCount() == 1
          && (!branched || region.intersects(i.boundingBox()))) {
        result.add(i);
      }
    }
    return result;
  }

  private void logTailRemoval(int passNo, boolean before) {
    if (!FRLogger.isTraceEnabled()) {
      return;
    }
    int incompleteCount = router.calculateIncompleteCount(router.board);
    FRLogger.trace(
        "BatchAutorouter.autoroute_pass",
        "compare_trace_remove_tails",
        "Incompletes " + (before ? "before" : "after") + " remove_tails=" + incompleteCount,
        "Autorouter pass #" + passNo,
        new Point[0]);
  }
}
