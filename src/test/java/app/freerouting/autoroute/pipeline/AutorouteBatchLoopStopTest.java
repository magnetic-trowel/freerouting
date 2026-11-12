package app.freerouting.autoroute.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import app.freerouting.autoroute.events.TaskStateChangedEvent;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.StoppableThread;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Point;
import app.freerouting.geometry.planar.Polyline;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.settings.RouterSettings;
import app.freerouting.settings.SettingsMerger;
import app.freerouting.settings.sources.DefaultSettings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** How the autoroute pass loop ends and what it reports. */
class AutorouteBatchLoopStopTest {

  /** Net X needs one connection; net Z has a single pin with a fanout stub (trace and via). */
  private static final String DSN =
      """
      (pcb loopstop (resolution um 1)
        (structure (layer top (type signal)) (layer bottom (type signal))
          (boundary (rect pcb 0 0 20000 10000))
          (via V) (rule (width 200) (clearance 200)))
        (placement (component C (place A 3000 3000 front 0) (place B 17000 3000 front 0)
          (place F 10000 8000 front 0)))
        (library (image C (pin S 1 0 0)) (padstack S (shape (circle top 800)))
          (padstack V (shape (circle top 600)) (shape (circle bottom 600)) (attach off)))
        (network (net X (pins A-1 B-1)) (net Z (pins F-1))))
      """;

  /** Net X can be routed; net Y's pin B2 sits inside keepouts on both layers, so Y stays open. */
  private static final String DSN_OPEN =
      """
      (pcb loopwork (resolution um 1)
        (structure (layer top (type signal)) (layer bottom (type signal))
          (boundary (rect pcb 0 0 20000 10000))
          (keepout (rect top 15000 6000 19000 9500)) (keepout (rect bottom 15000 6000 19000 9500))
          (via V) (rule (width 200) (clearance 200)))
        (placement (component C (place A 3000 3000 front 0) (place B 17000 3000 front 0)
          (place A2 3000 8000 front 0) (place B2 17000 8000 front 0)))
        (library (image C (pin S 1 0 0)) (padstack S (shape (circle top 800)))
          (padstack V (shape (circle top 600)) (shape (circle bottom 600)) (attach off)))
        (network (net X (pins A-1 B-1)) (net Y (pins A2-1 B2-1))))
      """;

  private record Run(RoutingJob job, BatchAutorouter router, List<TaskStateChangedEvent> events) {}

  private static Run prepareOpen(Long maxWork) {
    return prepareOpen(maxWork, null, null);
  }

  private static Run prepareOpen(Long maxWork, Double passWeight, Double unroutedWeight) {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(DSN_OPEN.getBytes(StandardCharsets.UTF_8)), null, null, "loopwork");
    assertInstanceOf(BoardReadResult.Success.class, result);
    RoutingBoard board = (RoutingBoard) ((BoardReadResult.Success) result).board();
    RouterSettings settings = new SettingsMerger(new DefaultSettings()).merge();
    settings.setLayerCount(board.getLayerCount());
    settings.applyBoardSpecificOptimizations(board);
    settings.setFanoutEnabled(false);
    settings.autorouter.maxPasses = 6;
    settings.autorouter.maxWork = maxWork;
    settings.autorouter.workPassWeight = passWeight;
    settings.autorouter.workUnroutedWeight = unroutedWeight;
    RoutingJob job = new RoutingJob();
    job.thread =
        new StoppableThread() {
          @Override
          protected void threadAction() {}
        };
    job.board = board;
    job.routerSettings = settings;
    BatchAutorouter router = BatchAutorouter.createAttempt(job, board, settings, false);
    List<TaskStateChangedEvent> events = new ArrayList<>();
    router.addTaskStateChangedEventListener(events::add);
    return new Run(job, router, events);
  }

  private static Run prepare(int maxPasses) {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(DSN.getBytes(StandardCharsets.UTF_8)), null, null, "loopstop");
    assertInstanceOf(BoardReadResult.Success.class, result);
    RoutingBoard board = (RoutingBoard) ((BoardReadResult.Success) result).board();

    Pin pinF =
        board.getPins().stream()
            .filter(p -> "F".equals(board.components.get(p.getComponentId()).name))
            .findFirst()
            .orElseThrow();
    int z = pinF.getNetNumber(0);
    IntPoint f = (IntPoint) pinF.getCenter();
    IntPoint viaCenter = new IntPoint(f.x + 600, f.y);
    board.insertTraceWithoutCleaning(
        new Polyline(new Point[] {f, viaCenter}), 0, 100, new int[] {z}, 1, FixedState.UNFIXED);
    board.insertVia(
        board.library.padstacks.get("V"), viaCenter, new int[] {z}, 1, FixedState.UNFIXED, true);
    assertEquals(1, board.getViaCount());

    RouterSettings settings = new SettingsMerger(new DefaultSettings()).merge();
    settings.setLayerCount(board.getLayerCount());
    settings.applyBoardSpecificOptimizations(board);
    settings.setFanoutEnabled(false);
    settings.autorouter.maxPasses = maxPasses;

    RoutingJob job = new RoutingJob();
    job.thread =
        new StoppableThread() {
          @Override
          protected void threadAction() {}
        };
    job.board = board;
    job.routerSettings = settings;
    // Like a continuation that keeps fanout vias: the passes keep the stub, the final cleanup
    // of a loop that ends on its own removes it.
    BatchAutorouter router = BatchAutorouter.createAttempt(job, board, settings, false);
    List<TaskStateChangedEvent> events = new ArrayList<>();
    router.addTaskStateChangedEventListener(events::add);
    return new Run(job, router, events);
  }

  private static TaskState lastState(Run run) {
    return run.events().get(run.events().size() - 1).getTaskState();
  }

  @Test
  void ownStopIsNoStopRequestAndCleansUpTheTails() {
    Run run = prepare(1);
    run.router().runBatchLoop();

    assertEquals(BatchAutorouter.StopReason.MAX_PASSES, run.router().lastStopReason);
    assertFalse(run.job().thread.isStopAutoRouterRequested());
    assertEquals(TaskState.FINISHED, lastState(run));
    assertEquals(0, run.job().board.getViaCount(), "the final cleanup removes the fanout stub");
    assertEquals(1, run.router().phaseMetrics.autorouter.passesCompleted);
    assertEquals("max_passes", run.router().phaseMetrics.autorouter.stopReason);
  }

  @Test
  void workBudgetStopsCleanlyBeforeThePassAfterItIsSpent() {
    // pass 1 searches (Y never routes), so any budget of one step is spent before pass 2
    Run run = prepareOpen(1L);
    run.router().runBatchLoop();

    assertEquals(BatchAutorouter.StopReason.WORK_BUDGET, run.router().lastStopReason);
    assertFalse(run.job().thread.isStopAutoRouterRequested());
    assertEquals(TaskState.FINISHED, lastState(run));
    assertEquals(1, run.router().phaseMetrics.autorouter.passesCompleted);
  }

  @Test
  void perPassWeightsCountTowardsTheBudget() {
    // the board has 4 pins: a pass weight of 1e9 per pin spends any budget below 4e9 in pass 1;
    // without the weight the same budget lets more passes run
    Run weighted = prepareOpen(1_000_000_000L, 1e9, null);
    weighted.router().runBatchLoop();
    assertEquals(BatchAutorouter.StopReason.WORK_BUDGET, weighted.router().lastStopReason);
    assertEquals(1, weighted.router().phaseMetrics.autorouter.passesCompleted);

    Run plain = prepareOpen(1_000_000_000L, null, null);
    plain.router().runBatchLoop();
    org.junit.jupiter.api.Assertions.assertTrue(
        plain.router().phaseMetrics.autorouter.passesCompleted > 1);

    // the unrouted term: 1 or 2 incomplete connections x 4 pins x 1e9 at pass 1
    Run unrouted = prepareOpen(1_000_000_000L, null, 1e9);
    unrouted.router().runBatchLoop();
    assertEquals(1, unrouted.router().phaseMetrics.autorouter.passesCompleted);
  }

  @Test
  void theSameRunUsesTheSameSteps() {
    long[] used = new long[2];
    for (int k = 0; k < 2; k++) {
      long t0 = app.freerouting.datastructures.TimeLimit.threadSteps();
      prepareOpen(null).router().runBatchLoop();
      used[k] = app.freerouting.datastructures.TimeLimit.threadSteps() - t0;
    }
    assertEquals(used[0], used[1]);
    org.junit.jupiter.api.Assertions.assertTrue(used[0] > 0);
  }

  @Test
  void anUnspentWorkBudgetChangesNothing() {
    Run capped = prepareOpen(1_000_000_000L);
    capped.router().runBatchLoop();
    Run plain = prepareOpen(null);
    plain.router().runBatchLoop();

    assertEquals(plain.router().lastStopReason, capped.router().lastStopReason);
    assertEquals(
        plain.router().phaseMetrics.autorouter.passesCompleted,
        capped.router().phaseMetrics.autorouter.passesCompleted);
    assertEquals(plain.job().board.getHash(), capped.job().board.getHash());
  }

  @Test
  void stopAfterTheTimeoutIsATimeoutBeforeTheJobStateSaysSo() {
    Run run = prepare(5);
    // What the scheduler's monitor does when timeoutAt passes; the job state stays RUNNING until
    // the pipeline returns.
    run.job().timeoutAt = Instant.now().minusSeconds(1);
    run.job().thread.requestStop();
    run.router().runBatchLoop();

    assertEquals(BatchAutorouter.StopReason.TIMED_OUT, run.router().lastStopReason);
    assertEquals(TaskState.TIMED_OUT, lastState(run));
    assertEquals(1, run.job().board.getViaCount(), "a stopped loop does not clean up");
    assertEquals(0, run.router().phaseMetrics.autorouter.passesCompleted);
    assertEquals("timed_out", run.router().phaseMetrics.autorouter.stopReason);
  }

  @Test
  void userCancelIsACancelEvenAfterTheTimeout() {
    Run run = prepare(5);
    run.job().timeoutAt = Instant.now().minusSeconds(1);
    run.job().setCancelledByUser(true);
    run.job().thread.requestStop();
    run.router().runBatchLoop();

    assertEquals(BatchAutorouter.StopReason.CANCELLED, run.router().lastStopReason);
    assertEquals(TaskState.CANCELLED, lastState(run));
  }
}
