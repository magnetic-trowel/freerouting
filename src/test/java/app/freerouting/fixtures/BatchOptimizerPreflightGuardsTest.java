package app.freerouting.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.autoroute.pipeline.BatchOptimizer;
import app.freerouting.board.actions.ItemIdGenerator;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.items.Trace;
import app.freerouting.board.model.items.Via;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.StoppableThread;
import app.freerouting.core.results.RoutingResultManifest;
import app.freerouting.core.scoring.BoardStatistics;
import app.freerouting.core.scoring.BoardStatisticsBounds;
import app.freerouting.io.specctra.DsnTestFixtures;
import app.freerouting.io.specctra.SesReader;
import app.freerouting.management.HeadlessBoardManager;
import app.freerouting.settings.sources.TestingSettings;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link BatchOptimizer} pre-flight guards correctly identify un-improvable boards
 * and bypass the expensive optimization passes.
 */
class BatchOptimizerPreflightGuardsTest extends RoutingFixtureTest {

  private static final String DSN_BM08 = "Issue508-DAC2020_bm08.dsn";
  private static final String SES_BM08 = "Issue508-DAC2020_bm08-routed.ses";

  private RoutingBoard fullyRoutedBm08;

  @BeforeEach
  @Override
  protected void setUp() {
    super.setUp();
    fullyRoutedBm08 = loadRoutedBm08();
  }

  private RoutingBoard loadRoutedBm08() {
    TestingSettings settings = new TestingSettings();
    RoutingJob job = getRoutingJob(DSN_BM08, settings);
    HeadlessBoardManager boardManager = new HeadlessBoardManager(job);
    try {
      boardManager.loadFromSpecctraDsn(job.input.getData(), null, new ItemIdGenerator());
      RoutingBoard board = boardManager.getRoutingBoard();
      InputStream sesStream =
          app.freerouting.TestFixtures.resolvePath(SES_BM08).toUri().toURL().openStream();
      SesReader.read(sesStream, board);
      board.finishAutoroute();
      // The session is imported fixed; unfix it so that the optimizer has candidates.
      for (Item item : board.getItems()) {
        if (item instanceof Trace || item instanceof Via) {
          item.setFixedState(FixedState.UNFIXED);
        }
      }
      return board;
    } catch (Exception e) {
      throw new RuntimeException("Failed to load test board", e);
    }
  }

  @Test
  void guard1BypassesOptimizerWhenBoardHasIncompletes() {
    TestingSettings settings = new TestingSettings();
    RoutingJob job = getRoutingJob(DSN_BM08, settings);
    job.board = fullyRoutedBm08.deepCopy();
    job.thread = new NoOpStoppableThread();

    BatchOptimizer optimizer = BatchOptimizer.create(job);
    BoardStatistics stats = new BoardStatistics();
    stats.connections.incompleteCount = 5;
    stats.vias.totalCount = 10;

    String bypassReason = optimizer.evaluatePreFlightGuards(stats);
    assertNotNull(bypassReason);
    assertTrue(bypassReason.contains("unrouted connection(s)"));
  }

  @Test
  void guard2BypassesOptimizerWhenBoardHasZeroViasAndHighOptimizerScore() {
    TestingSettings settings = new TestingSettings();
    RoutingJob job = getRoutingJob(DSN_BM08, settings);
    job.board = fullyRoutedBm08.deepCopy();
    job.thread = new NoOpStoppableThread();

    BatchOptimizer optimizer = BatchOptimizer.create(job);
    BoardStatistics stats =
        new BoardStatistics() {
          @Override
          public float getOptimizerScore(app.freerouting.settings.RouterSettings routerSettings) {
            return 960.0f;
          }
        };
    stats.connections.incompleteCount = 0;
    stats.vias.totalCount = 0;

    String bypassReason = optimizer.evaluatePreFlightGuards(stats);
    assertNotNull(bypassReason);
    assertTrue(bypassReason.contains("no vias to eliminate"));
    assertTrue(bypassReason.contains("950.00"));
  }

  @Test
  void guard2BypassesOptimizerWhenBoardHasZeroViasAndTraceLengthNearBound() {
    TestingSettings settings = new TestingSettings();
    RoutingJob job = getRoutingJob(DSN_BM08, settings);
    job.board = fullyRoutedBm08.deepCopy();
    job.thread = new NoOpStoppableThread();

    BatchOptimizer optimizer = BatchOptimizer.create(job);
    BoardStatistics stats =
        new BoardStatistics() {
          @Override
          public float getOptimizerScore(app.freerouting.settings.RouterSettings routerSettings) {
            return 900.0f;
          }
        };
    stats.connections.incompleteCount = 0;
    stats.vias.totalCount = 0;
    stats.bounds = new BoardStatisticsBounds();
    stats.bounds.minTraceLengthMm = 100.0f;
    stats.traces.totalLengthMm = 104.0f; // within 1.05 * 100 = 105.0

    String bypassReason = optimizer.evaluatePreFlightGuards(stats);
    assertNotNull(bypassReason);
    assertTrue(bypassReason.contains("within 5% of theoretical minimum"));
  }

  @Test
  void guard2DoesNotBypassWhenZeroViasButTraceLengthSuboptimal() {
    TestingSettings settings = new TestingSettings();
    RoutingJob job = getRoutingJob(DSN_BM08, settings);
    job.board = fullyRoutedBm08.deepCopy();
    job.thread = new NoOpStoppableThread();

    BatchOptimizer optimizer = BatchOptimizer.create(job);
    BoardStatistics stats =
        new BoardStatistics() {
          @Override
          public float getOptimizerScore(app.freerouting.settings.RouterSettings routerSettings) {
            return 800.0f;
          }
        };
    stats.connections.incompleteCount = 0;
    stats.vias.totalCount = 0;
    stats.bounds = new BoardStatisticsBounds();
    stats.bounds.minTraceLengthMm = 100.0f;
    stats.traces.totalLengthMm = 130.0f; // well above 1.05 * 100 = 105.0

    String bypassReason = optimizer.evaluatePreFlightGuards(stats);
    assertNull(bypassReason);
  }

  @Test
  void guard3BypassesOptimizerWhenScoreAtOrAboveCeiling() {
    TestingSettings settings = new TestingSettings();
    RoutingJob job = getRoutingJob(DSN_BM08, settings);
    job.board = fullyRoutedBm08.deepCopy();
    job.thread = new NoOpStoppableThread();

    BatchOptimizer optimizer = BatchOptimizer.create(job);
    BoardStatistics stats =
        new BoardStatistics() {
          @Override
          public float getOptimizerScore(app.freerouting.settings.RouterSettings routerSettings) {
            return 996.5f;
          }
        };
    stats.connections.incompleteCount = 0;
    stats.vias.totalCount = 10;

    String bypassReason = optimizer.evaluatePreFlightGuards(stats);
    assertNotNull(bypassReason);
    assertTrue(bypassReason.contains("995.00"));
  }

  @Test
  void guard3BypassesOptimizerWhenTraceLengthWithin2PercentOfTheoreticalMin() {
    TestingSettings settings = new TestingSettings();
    RoutingJob job = getRoutingJob(DSN_BM08, settings);
    job.board = fullyRoutedBm08.deepCopy();
    job.thread = new NoOpStoppableThread();

    BatchOptimizer optimizer = BatchOptimizer.create(job);
    BoardStatistics stats =
        new BoardStatistics() {
          @Override
          public float getOptimizerScore(app.freerouting.settings.RouterSettings routerSettings) {
            return 900.0f;
          }
        };
    stats.connections.incompleteCount = 0;
    stats.vias.totalCount = 10;
    stats.bounds = new BoardStatisticsBounds();
    stats.bounds.minTraceLengthMm = 100.0f;
    stats.bounds.minViaCount = 10;
    stats.traces.totalLengthMm = 101.5f; // within 1.02 * 100.0 = 102.0

    String bypassReason = optimizer.evaluatePreFlightGuards(stats);
    assertNotNull(bypassReason);
    assertTrue(bypassReason.contains("theoretical minimum"));
  }

  @Test
  void guard3DoesNotBypassWhenTraceLengthNearBoundButExcessViasRemain() {
    TestingSettings settings = new TestingSettings();
    RoutingJob job = getRoutingJob(DSN_BM08, settings);
    job.board = fullyRoutedBm08.deepCopy();
    job.thread = new NoOpStoppableThread();

    BatchOptimizer optimizer = BatchOptimizer.create(job);
    BoardStatistics stats =
        new BoardStatistics() {
          @Override
          public float getOptimizerScore(app.freerouting.settings.RouterSettings routerSettings) {
            return 900.0f;
          }
        };
    stats.connections.incompleteCount = 0;
    stats.vias.totalCount = 10;
    stats.bounds = new BoardStatisticsBounds();
    stats.bounds.minTraceLengthMm = 100.0f;
    stats.bounds.minViaCount = 2; // vias (10) > minViaCount (2)
    stats.traces.totalLengthMm = 101.5f;

    String bypassReason = optimizer.evaluatePreFlightGuards(stats);
    assertNull(bypassReason);
  }

  @Test
  void guard3BypassesOptimizerWhenViasAtBoundAndTraceLengthWithin5Percent() {
    TestingSettings settings = new TestingSettings();
    RoutingJob job = getRoutingJob(DSN_BM08, settings);
    job.board = fullyRoutedBm08.deepCopy();
    job.thread = new NoOpStoppableThread();

    BatchOptimizer optimizer = BatchOptimizer.create(job);
    BoardStatistics stats = statsWithScore(900.0f);
    stats.connections.incompleteCount = 0;
    stats.vias.totalCount = 4;
    stats.bounds = new BoardStatisticsBounds();
    stats.bounds.minTraceLengthMm = 100.0f;
    stats.bounds.minViaCount = 4;
    stats.traces.totalLengthMm = 104.0f; // the vias cannot improve, the length by under 5%

    String bypassReason = optimizer.evaluatePreFlightGuards(stats);
    assertNotNull(bypassReason);
    assertTrue(bypassReason.contains("within 5% of theoretical minimum"));
  }

  @Test
  void guard4BypassesOptimizerWhenEveryViaAndTraceIsFixed() {
    TestingSettings settings = new TestingSettings();
    RoutingJob job = getRoutingJob(DSN_BM08, settings);
    job.board = fullyRoutedBm08.deepCopy();
    job.thread = new NoOpStoppableThread();
    for (Item item : job.board.getItems()) {
      if (item instanceof Trace || item instanceof Via) {
        item.setFixedState(FixedState.USER_FIXED);
      }
    }

    BatchOptimizer optimizer = BatchOptimizer.create(job);
    BoardStatistics stats = statsWithScore(800.0f);
    stats.connections.incompleteCount = 0;
    stats.vias.totalCount = 10;

    String bypassReason = optimizer.evaluatePreFlightGuards(stats);
    assertNotNull(bypassReason);
    assertTrue(bypassReason.contains("no unfixed via or trace"));
  }

  /** Fixed vias used to make the "all vias are mandatory" guard skip the optimizer vacuously. */
  @Test
  void fixedViasDoNotBypassTheOptimizerWhenTheTracesCanImprove() throws Exception {
    TestingSettings settings = new TestingSettings();
    RoutingJob job = getRoutingJob(DSN_BM08, settings);
    job.board = DsnTestFixtures.loadBoard("Issue103-Board-Routed.dsn");
    job.thread = new NoOpStoppableThread();
    assertFalse(job.board.getVias().isEmpty());
    for (Via via : job.board.getVias()) {
      via.setFixedState(FixedState.USER_FIXED);
    }

    BatchOptimizer optimizer = BatchOptimizer.create(job);
    BoardStatistics stats = statsWithScore(800.0f);
    stats.connections.incompleteCount = 0;
    stats.vias.totalCount = 10;
    stats.bounds = new BoardStatisticsBounds();
    stats.bounds.minTraceLengthMm = 100.0f;
    stats.bounds.minViaCount = 0;
    stats.traces.totalLengthMm = 130.0f;

    assertNull(optimizer.evaluatePreFlightGuards(stats));
  }

  private static BoardStatistics statsWithScore(float score) {
    return new BoardStatistics() {
      @Override
      public float getOptimizerScore(app.freerouting.settings.RouterSettings routerSettings) {
        return score;
      }
    };
  }

  @Test
  void guardsCanBeDisabledViaSettings() {
    TestingSettings settings = new TestingSettings();
    settings.setOptimizerEnablePreflightGuards(false);
    RoutingJob job = getRoutingJob(DSN_BM08, settings);
    job.board = fullyRoutedBm08.deepCopy();
    job.thread = new NoOpStoppableThread();

    BatchOptimizer optimizer = BatchOptimizer.create(job);
    BoardStatistics stats = new BoardStatistics();
    stats.connections.incompleteCount = 5;
    stats.vias.totalCount = 0;

    String bypassReason = optimizer.evaluatePreFlightGuards(stats);
    assertNull(bypassReason);
  }

  @Test
  void runBatchLoopSetsManifestSnapshotsAndZeroPassesWhenBypassed() {
    TestingSettings settings = new TestingSettings();
    RoutingJob job = getRoutingJob(DSN_BM08, settings);
    job.board = fullyRoutedBm08.deepCopy();
    job.routerSettings.optimizerScoring.excessWireLengthWeight = 0.0f;
    job.routerSettings.optimizerScoring.excessViaWeight = 0.0f;
    job.routerSettings.optimizerScoring.excessBendWeight = 0.0f;
    job.thread = new NoOpStoppableThread();

    BatchOptimizer optimizer = BatchOptimizer.create(job);
    optimizer.runBatchLoop();

    RoutingResultManifest.PhaseDetail phase = job.resultPhaseMetrics.optimizer;
    assertNotNull(phase.before);
    assertNotNull(phase.after);
    assertEquals(0, phase.passesCompleted);
    assertEquals(0.0f, phase.durationSeconds, 0.001f);
    assertEquals(BatchOptimizer.StopReason.SKIPPED, optimizer.lastStopReason);
    assertEquals("skipped", phase.stopReason);
  }

  @Test
  void stopReasonTellsAPassLimitFromAStopAndATimeout() {
    RoutingJob limited = getRoutingJob(DSN_BM08, new TestingSettings());
    limited.board = fullyRoutedBm08.deepCopy();
    limited.routerSettings.optimizer.maxPasses = 1;
    limited.thread = new NoOpStoppableThread();
    BatchOptimizer.create(limited).runBatchLoop();
    assertEquals(1, limited.resultPhaseMetrics.optimizer.passesCompleted);
    assertEquals("max_passes", limited.resultPhaseMetrics.optimizer.stopReason);

    RoutingJob cancelled = getRoutingJob(DSN_BM08, new TestingSettings());
    cancelled.board = fullyRoutedBm08.deepCopy();
    cancelled.thread = new NoOpStoppableThread();
    cancelled.thread.requestStop();
    BatchOptimizer.create(cancelled).runBatchLoop();
    assertEquals(0, cancelled.resultPhaseMetrics.optimizer.passesCompleted);
    assertEquals("cancelled", cancelled.resultPhaseMetrics.optimizer.stopReason);

    // What the job monitor does when the job timeout passes.
    RoutingJob timedOut = getRoutingJob(DSN_BM08, new TestingSettings());
    timedOut.board = fullyRoutedBm08.deepCopy();
    timedOut.thread = new NoOpStoppableThread();
    timedOut.timeoutAt = java.time.Instant.now().minusSeconds(1);
    timedOut.thread.requestStop();
    BatchOptimizer.create(timedOut).runBatchLoop();
    assertEquals("timed_out", timedOut.resultPhaseMetrics.optimizer.stopReason);
  }

  private static final class NoOpStoppableThread extends StoppableThread {
    @Override
    protected void threadAction() {}
  }
}
