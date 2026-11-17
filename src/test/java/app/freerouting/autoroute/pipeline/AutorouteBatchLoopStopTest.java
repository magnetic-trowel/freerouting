package app.freerouting.autoroute.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.autoroute.events.TaskStateChangedEvent;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.StoppableThread;
import app.freerouting.core.results.RoutingResultManifest;
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

  /**
   * Net P has a pour on the bottom layer between its two through-hole pins, which its traces end
   * on; net Q's connection crosses the pour.
   */
  private static final String DSN_POUR =
      """
      (pcb loopfill (resolution um 1)
        (structure (layer top (type signal)) (layer bottom (type signal))
          (boundary (rect pcb 0 0 20000 10000))
          (plane P (polygon bottom 0 6000 3000 14000 3000 14000 7000 6000 7000))
          (via V) (rule (width 200) (clearance 200)))
        (placement (component C (place P1 3000 5000 front 0) (place P2 17000 5000 front 0)
          (place Q1 10000 1000 front 0) (place Q2 10000 9000 front 0)))
        (library (image C (pin T 1 0 0))
          (padstack T (shape (circle top 800)) (shape (circle bottom 800)) (attach off))
          (padstack V (shape (circle top 600)) (shape (circle bottom 600)) (attach off)))
        (network (net P (pins P1-1 P2-1)) (net Q (pins Q1-1 Q2-1))))
      """;

  private record Run(RoutingJob job, BatchAutorouter router, List<TaskStateChangedEvent> events) {}

  private static Run prepareOpen(Long maxWork) {
    return prepareOpen(maxWork, null, null);
  }

  private static Run prepareOpen(Long maxWork, Double passWeight, Double unroutedWeight) {
    Run run = prepareOpenJob(maxWork);
    run.job().routerSettings.autorouter.maxPasses = 6;
    run.job().routerSettings.autorouter.workPassWeight = passWeight;
    run.job().routerSettings.autorouter.workUnroutedWeight = unroutedWeight;
    return run;
  }

  /**
   * The open board as a job that stagnates (Y never routes) and so reaches the routing fallback:
   * the alternative 'continue', then the residual continuations.
   */
  private static Run prepareOpenJob(Long maxWork) {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(DSN_OPEN.getBytes(StandardCharsets.UTF_8)),
            null,
            null,
            "loopwork");
    assertInstanceOf(BoardReadResult.Success.class, result);
    RoutingBoard board = (RoutingBoard) ((BoardReadResult.Success) result).board();
    RouterSettings settings = new SettingsMerger(new DefaultSettings()).merge();
    settings.setLayerCount(board.getLayerCount());
    settings.applyBoardSpecificOptimizations(board);
    settings.setFanoutEnabled(false);
    settings.autorouter.maxPasses = null;
    settings.autorouter.stagnationPassLimit = 2;
    settings.autorouter.fallbackStrategies = "continue";
    settings.autorouter.maxWork = maxWork;
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

  /** The routing stage as RoutingPipeline runs it: the default pass loop, then the fallback. */
  private record Routed(
      long defaultWork,
      BatchAutorouter.StopReason defaultStop,
      RoutingResultManifest.FallbackDetail fallback,
      long jobWork) {}

  private static Routed route(Run run) {
    byte[] input = run.job().board.serialize(false);
    run.router().runBatchLoop();
    long defaultWork = run.job().workSpent;
    BatchAutorouter.StopReason defaultStop = run.router().lastStopReason;
    RoutingFallbackRunner fallback = new RoutingFallbackRunner(run.router());
    fallback.run(
        run.job(),
        run.router(),
        RoutingFallback.parse(run.job().routerSettings, name -> {}),
        input);
    return new Routed(defaultWork, defaultStop, fallback.detail(), run.job().workSpent);
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
  void theBudgetIsTheJobsAcrossPassLoops() {
    // a fallback attempt is another pass loop of the same job: it starts with what the earlier
    // loops spent, so a budget the first loop used up stops the second before its first pass
    Run run = prepareOpen(1L);
    run.job().workSpent = 5L;
    run.router().runBatchLoop();

    assertEquals(BatchAutorouter.StopReason.WORK_BUDGET, run.router().lastStopReason);
    assertEquals(0, run.router().phaseMetrics.autorouter.passesCompleted);
    assertEquals(5L, run.job().workSpent);
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
    assertEquals("work_budget", run.router().phaseMetrics.autorouter.stopReason);
    assertEquals(run.router().searchStepsUsed(), run.job().workSpent);
    assertFalse(RoutingFallback.isRetryable(BatchAutorouter.StopReason.WORK_BUDGET));
  }

  @Test
  void perPassWeightsCountTowardsTheBudget() {
    // the board has 4 pins: a pass weight of 1e9 per pin spends any budget below 4e9 in pass 1;
    // without the weight the same budget lets more passes run
    Run weighted = prepareOpen(1_000_000_000L, 1e9, null);
    weighted.router().runBatchLoop();
    assertEquals(BatchAutorouter.StopReason.WORK_BUDGET, weighted.router().lastStopReason);
    assertEquals(1, weighted.router().phaseMetrics.autorouter.passesCompleted);
    assertEquals(weighted.router().searchStepsUsed() + 4_000_000_000L, weighted.job().workSpent);

    Run plain = prepareOpen(1_000_000_000L, null, null);
    plain.router().runBatchLoop();
    assertTrue(plain.router().phaseMetrics.autorouter.passesCompleted > 1);

    // the unrouted term: 1 or 2 incomplete connections x 4 pins x 1e9 at pass 1
    Run unrouted = prepareOpen(1_000_000_000L, null, 1e9);
    unrouted.router().runBatchLoop();
    assertEquals(1, unrouted.router().phaseMetrics.autorouter.passesCompleted);
  }

  /**
   * Pour fill work (FillWorkMeter) counts autorouter.work_fill_weight work units per unit, the same
   * in every run; without the weight it counts nothing.
   */
  @Test
  void pourFillWorkCountsItsWeight() {
    long[] fill = new long[2];
    long[] work = new long[2];
    long[] steps = new long[2];
    for (int k = 0; k < 2; k++) {
      Run run = preparePour();
      run.job().routerSettings.autorouter.workFillWeight = k == 0 ? null : 1000.0;
      long before = app.freerouting.datastructures.FillWorkMeter.count();
      run.router().runBatchLoop();
      fill[k] = app.freerouting.datastructures.FillWorkMeter.count() - before;
      work[k] = run.job().workSpent;
      steps[k] = run.router().searchStepsUsed();
    }
    assertTrue(fill[0] > 0, "routing on a pour computes its fill");
    assertEquals(fill[0], fill[1], "the same run counts the same fill work");
    assertEquals(steps[0], work[0], "without the weight the fill work counts nothing");
    // the pass loop's share of the run's fill work, at 1000 work units each
    long fillWork = work[1] - steps[1];
    assertTrue(fillWork > 0 && fillWork % 1000L == 0 && fillWork <= 1000L * fill[1], "" + fillWork);
  }

  private static Run preparePour() {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(DSN_POUR.getBytes(StandardCharsets.UTF_8)),
            null,
            null,
            "loopfill");
    assertInstanceOf(BoardReadResult.Success.class, result);
    RoutingBoard board = (RoutingBoard) ((BoardReadResult.Success) result).board();
    RouterSettings settings = new SettingsMerger(new DefaultSettings()).merge();
    settings.setLayerCount(board.getLayerCount());
    settings.applyBoardSpecificOptimizations(board);
    settings.setFanoutEnabled(false);
    settings.autorouter.maxPasses = 3;
    RoutingJob job = new RoutingJob();
    job.thread =
        new StoppableThread() {
          @Override
          protected void threadAction() {}
        };
    job.board = board;
    job.routerSettings = settings;
    BatchAutorouter router = BatchAutorouter.createAttempt(job, board, settings, false);
    return new Run(job, router, new ArrayList<>());
  }

  @Test
  void theSameRunUsesTheSameSteps() {
    long[] used = new long[2];
    for (int k = 0; k < 2; k++) {
      Run run = prepareOpen(null);
      run.router().runBatchLoop();
      used[k] = run.router().searchStepsUsed();
      assertEquals(used[k], run.job().workSpent, "without weights the work is the search steps");
    }
    assertEquals(used[0], used[1]);
    assertTrue(used[0] > 0);
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
  void fallbackAttemptsDrawOnTheJobsBudget() {
    Routed free = route(prepareOpenJob(null));
    assertEquals(BatchAutorouter.StopReason.STAGNATION, free.defaultStop());
    assertTrue(free.fallback().attempts.size() >= 3, "default, 'continue' and a continuation");
    assertTrue(free.jobWork() > free.defaultWork(), "the attempts' work counts for the job");

    // One unit more than the default loop spent: the default loop runs as without a budget, the
    // first attempt starts with the job's work and stops after its first pass, and that stop ends
    // the job: no continuation starts.
    Run run = prepareOpenJob(free.defaultWork() + 1);
    Routed capped = route(run);
    assertEquals(BatchAutorouter.StopReason.STAGNATION, capped.defaultStop());
    assertEquals(free.defaultWork(), capped.defaultWork());
    assertEquals(2, capped.fallback().attempts.size());
    RoutingResultManifest.FallbackAttempt attempt = capped.fallback().attempts.get(1);
    assertEquals("continue", attempt.strategy);
    assertEquals("work_budget", attempt.stopReason);
    assertTrue(capped.jobWork() > free.defaultWork());
    assertTrue(capped.jobWork() < free.jobWork());
    assertFalse(run.job().thread.isStopAutoRouterRequested());
  }

  @Test
  void aSpentWorkBudgetStartsNoAttempt() {
    long defaultWork = route(prepareOpenJob(null)).defaultWork();

    // The default loop stops on its own (stagnation, retryable) just as the budget is spent: no
    // fallback attempt starts.
    Routed exact = route(prepareOpenJob(defaultWork));
    assertEquals(BatchAutorouter.StopReason.STAGNATION, exact.defaultStop());
    assertEquals(defaultWork, exact.jobWork());
    assertEquals(null, exact.fallback());

    // The default loop stops at the budget: WORK_BUDGET is not retried.
    Routed spent = route(prepareOpenJob(1L));
    assertEquals(BatchAutorouter.StopReason.WORK_BUDGET, spent.defaultStop());
    assertEquals(spent.defaultWork(), spent.jobWork());
    assertEquals(null, spent.fallback());
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

  @Test
  void searchBudgetStopsBeforeTheNextPassAndIsAnOwnStop() {
    Run run = prepare(5);
    run.router().setSearchStepLimit(1);
    run.router().runBatchLoop();

    assertEquals(BatchAutorouter.StopReason.SEARCH_BUDGET, run.router().lastStopReason);
    assertTrue(run.router().searchStepsUsed() >= 1, "the first pass counts its search steps");
    assertFalse(run.job().thread.isStopAutoRouterRequested());
    assertEquals(TaskState.FINISHED, lastState(run));
    assertEquals(0, run.job().board.getViaCount(), "the final cleanup removes the fanout stub");
    assertEquals(1, run.router().phaseMetrics.autorouter.passesCompleted);
    assertEquals("search_budget", run.router().phaseMetrics.autorouter.stopReason);
  }
}
