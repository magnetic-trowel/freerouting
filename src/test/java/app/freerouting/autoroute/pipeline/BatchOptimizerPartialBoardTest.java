package app.freerouting.autoroute.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Trace;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.StoppableThread;
import app.freerouting.core.scoring.BoardStatistics;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.settings.RouterSettings;
import app.freerouting.settings.sources.DefaultSettings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The optimizer must improve the routed connections of a board that is not completely routed. */
class BatchOptimizerPartialBoardTest {

  /**
   * Net DETOUR is routed with a 52 mm detour that a straight 34 mm trace can replace. Net WALLED
   * connects W0 and W1 with a 10 mm trace, but cannot reach W2, which sits inside a closed keepout
   * frame on both layers.
   */
  private static final String DSN =
      """
      (pcb partial (resolution um 10) (unit um)
        (structure (layer top (type signal)) (layer bottom (type signal))
          (boundary (rect pcb 0 0 40000 25000))
          (keepout (rect signal 26000 15000 34000 15500))
          (keepout (rect signal 26000 20500 34000 21000))
          (keepout (rect signal 26000 15000 26500 21000))
          (keepout (rect signal 33500 15000 34000 21000))
          (via V) (rule (width 200) (clearance 200)))
        (placement (component C (place D1 3000 3000 front 0) (place D2 37000 3000 front 0)
          (place W0 14000 18000 front 0) (place W1 24000 18000 front 0)
          (place W2 30000 18000 front 0)))
        (library (image C (pin P 1 0 0))
          (padstack P (shape (circle top 800)) (shape (circle bottom 800)) (attach off))
          (padstack V (shape (circle top 600)) (shape (circle bottom 600)) (attach off)))
        (network (net DETOUR (pins D1-1 D2-1)) (net WALLED (pins W0-1 W1-1 W2-1)))
        (wiring
          (wire (path top 200 3000 3000 3000 12000 37000 12000 37000 3000) (net DETOUR))
          (wire (path top 200 14000 18000 24000 18000) (net WALLED))))
      """;

  @Test
  void optimizesRoutedConnectionsWhileOthersStayUnrouted() {
    RoutingJob job = jobFor(loadBoard());
    BoardStatistics before = job.board.getStatistics();
    assertEquals(1, before.connections.incompleteCount, "W2 must start unrouted");
    assertEquals(62.0f, before.traces.totalLengthMm, 0.01f);

    new BatchOptimizer(job).runBatchLoop();

    BoardStatistics after = job.board.getStatistics();
    assertEquals(1, after.connections.incompleteCount, "W0 and W1 must stay connected");
    assertEquals(0, after.clearanceViolations.totalCount);
    assertEquals(
        34.0f + 10.0f,
        after.traces.totalLengthMm,
        0.5f,
        "the detour must become a straight trace, the trace of WALLED must stay");
  }

  @Test
  void aSpentWorkBudgetStartsNoOptimizerPass() {
    RoutingJob job = jobFor(loadBoard());
    job.routerSettings.autorouter.maxWork = 100L;
    job.workSpent = 100L;
    BatchOptimizer optimizer = new BatchOptimizer(job);
    optimizer.runBatchLoop();

    assertEquals(BatchOptimizer.StopReason.SKIPPED, optimizer.lastStopReason);
    assertEquals(62.0f, job.board.getStatistics().traces.totalLengthMm, 0.01f);

    // budget left: the optimizer runs as without one
    RoutingJob left = jobFor(loadBoard());
    left.routerSettings.autorouter.maxWork = 1_000_000_000L;
    left.workSpent = 99L;
    new BatchOptimizer(left).runBatchLoop();
    assertEquals(44.0f, left.board.getStatistics().traces.totalLengthMm, 0.5f);
  }

  /**
   * The optimizer's search steps count towards the job's work, the same in every run, and a work
   * budget that is never reached changes nothing: the same board and the same work as without it.
   */
  @Test
  void optimizerSearchStepsCountTowardsTheJobsWork() {
    RoutingJob free = jobFor(loadBoard());
    new BatchOptimizer(free).runBatchLoop();
    assertTrue(free.workSpent > 0, "the candidates' search steps must count");

    RoutingJob again = jobFor(loadBoard());
    new BatchOptimizer(again).runBatchLoop();
    assertEquals(free.workSpent, again.workSpent, "the same run must count the same work");

    RoutingJob unspent = jobFor(loadBoard());
    unspent.routerSettings.autorouter.maxWork = Long.MAX_VALUE;
    BatchOptimizer optimizer = new BatchOptimizer(unspent);
    optimizer.runBatchLoop();
    assertEquals(free.workSpent, unspent.workSpent);
    assertEquals(
        free.board.getStatistics().traces.totalLengthMm,
        unspent.board.getStatistics().traces.totalLengthMm,
        0.0f);
    assertNotEquals(BatchOptimizer.StopReason.WORK_BUDGET, optimizer.lastStopReason);
  }

  /**
   * autorouter.work_optimizer_step_weight prices each optimizer search step, and changes nothing
   * else.
   */
  @Test
  void optimizerStepsCountTheirWeight() {
    RoutingJob free = jobFor(loadBoard());
    new BatchOptimizer(free).runBatchLoop();
    RoutingJob weighted = jobFor(loadBoard());
    weighted.routerSettings.autorouter.workOptimizerStepWeight = 3.0;
    new BatchOptimizer(weighted).runBatchLoop();

    assertEquals(free.optimizerSearchSteps, free.workSpent, "unset, a step is one work unit");
    assertEquals(3 * free.workSpent, weighted.workSpent);
    assertEquals(
        free.board.getStatistics().traces.totalLengthMm,
        weighted.board.getStatistics().traces.totalLengthMm,
        0.0f);
  }

  /** Each optimizer pass started also counts autorouter.work_pass_weight x the board's pins. */
  @Test
  void optimizerPassesCountTheirPassWeight() {
    RoutingJob free = jobFor(loadBoard());
    BatchOptimizer plain = new BatchOptimizer(free);
    plain.runBatchLoop();

    RoutingJob weighted = jobFor(loadBoard());
    weighted.routerSettings.autorouter.workPassWeight = 1000.0;
    BatchOptimizer optimizer = new BatchOptimizer(weighted);
    optimizer.runBatchLoop();
    int pins = weighted.board.getPins().size();
    int passes = weighted.resultPhaseMetrics.optimizer.passesCompleted;
    assertTrue(passes > 0);
    assertEquals(free.workSpent + passes * 1000L * pins, weighted.workSpent);
  }

  /**
   * With a pour of DETOUR on the bottom layer under its detour, the candidates compute its fill:
   * their pour fill work counts autorouter.work_fill_weight each, and changes nothing else.
   */
  @Test
  void optimizerPourFillWorkCountsItsWeight() {
    String withPour =
        DSN.replace(
            "(via V)",
            "(plane DETOUR (polygon bottom 0 10000 1000 30000 1000 30000 14000 10000 14000))"
                + " (via V)");
    RoutingJob free = jobFor(loadBoard(withPour));
    new BatchOptimizer(free).runBatchLoop();
    RoutingJob weighted = jobFor(loadBoard(withPour));
    weighted.routerSettings.autorouter.workFillWeight = 1000.0;
    new BatchOptimizer(weighted).runBatchLoop();

    long fillWork = weighted.workSpent - free.workSpent;
    assertTrue(fillWork > 0 && fillWork % 1000L == 0, "" + fillWork);
    assertEquals(
        free.board.getStatistics().traces.totalLengthMm,
        weighted.board.getStatistics().traces.totalLengthMm,
        0.0f);
  }

  /**
   * A budget spent during a pass stops it after the candidate that spent it: the pass commits what
   * it found, and no further pass starts (stop reason work_budget).
   */
  @Test
  void aWorkBudgetSpentInAPassStopsTheOptimizer() {
    RoutingJob job = jobFor(loadBoard());
    job.routerSettings.autorouter.maxWork = 1L;
    BatchOptimizer optimizer = new BatchOptimizer(job);
    optimizer.runBatchLoop();

    assertEquals(BatchOptimizer.StopReason.WORK_BUDGET, optimizer.lastStopReason);
    assertEquals(1, job.resultPhaseMetrics.optimizer.passesCompleted, "passes started");
    assertTrue(job.workSpent >= 1L);
    RoutingJob free = jobFor(loadBoard());
    new BatchOptimizer(free).runBatchLoop();
    assertTrue(job.workSpent < free.workSpent, "the stopped run must do less work");
    assertEquals(1, job.board.getStatistics().connections.incompleteCount);
  }

  /**
   * A candidate of WALLED rips the W0-W1 trace. Once it is back, only the W2 connection the routing
   * stage could not route is open: with the count from before the rip the candidate stops after
   * that pass instead of retrying W2 in every pass up to the limit.
   */
  @Test
  void candidateOfIncompleteNetStopsOnceRippedConnectionIsBack() {
    for (boolean stopEarly : new boolean[] {true, false}) {
      RoutingJob job = jobFor(loadBoard());
      RoutingBoard board = (RoutingBoard) job.board;
      int netNo = board.rules.nets.get("WALLED", 1).netNumber;
      Trace walledTrace =
          board.getTraces().stream().filter(t -> t.containsNet(netNo)).findFirst().orElseThrow();
      int incompleteBefore = board.routingLedger().incompleteCount();
      assertEquals(1, incompleteBefore);
      board.removeItems(List.of(walledTrace));
      assertEquals(2, board.routingLedger().incompleteCount());

      int passes =
          BatchAutorouter.autoroutePassesForOptimizingItem(
              job,
              6,
              job.routerSettings.getStartRipupCosts(),
              500,
              true,
              board,
              job.routerSettings,
              Set.of(),
              stopEarly ? incompleteBefore : -1);

      if (stopEarly) {
        assertEquals(1, board.routingLedger().incompleteCount(), "W0-W1 must be routed again");
      }
      assertEquals(stopEarly ? 1 : 7, passes, "passes run");
    }
  }

  private static RoutingBoard loadBoard() {
    return loadBoard(DSN);
  }

  private static RoutingBoard loadBoard(String dsn) {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(dsn.getBytes(StandardCharsets.UTF_8)), null, null, "partial");
    assertInstanceOf(BoardReadResult.Success.class, result);
    return (RoutingBoard) ((BoardReadResult.Success) result).board();
  }

  private static RoutingJob jobFor(RoutingBoard board) {
    RouterSettings settings = new DefaultSettings().getSettings();
    settings.setLayerCount(board.getLayerCount());
    settings.applyBoardSpecificOptimizations(board);
    settings.optimizer.enabled = true;
    settings.optimizer.maxThreads = 1;

    RoutingJob job = new RoutingJob();
    job.board = board;
    job.routerSettings = settings;
    job.thread =
        new StoppableThread() {
          @Override
          protected void threadAction() {}
        };
    return job;
  }
}
