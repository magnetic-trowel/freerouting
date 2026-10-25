package app.freerouting.autoroute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.autoroute.pipeline.BatchAutorouter;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.StoppableThread;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.settings.RouterSettings;
import app.freerouting.settings.SettingsMerger;
import app.freerouting.settings.sources.DefaultSettings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * The pass history is static. It used to survive {@link PerformanceProfiler#reset()} and collect
 * every pass of every optimizer candidate, so it grew for the life of the process.
 */
class PerformanceProfilerTest {

  private static final String DSN =
      """
      (pcb profiler (resolution um 1)
        (structure (layer top (type signal)) (layer bottom (type signal))
          (boundary (rect pcb 0 0 20000 10000))
          (via V) (rule (width 400) (clearance 200)))
        (placement (component C (place A 3000 5000 front 0) (place B 17000 5000 front 0)))
        (library (image C (pin S 1 0 0)) (padstack S (shape (circle top 800)))
          (padstack V (shape (circle top 600)) (shape (circle bottom 600)) (attach off)))
        (network (net X (pins A-1 B-1))))
      """;

  @Test
  void resetClearsThePassHistory() {
    PerformanceProfiler.reset();
    PerformanceProfiler.recordPass(1, 3, 10, 100);
    PerformanceProfiler.recordPass(2, 1, 10, 200);
    assertEquals(2, PerformanceProfiler.passHistorySize());

    PerformanceProfiler.reset();

    assertEquals(0, PerformanceProfiler.passHistorySize());
  }

  @Test
  void optimizerCandidatePassesAreNotRecorded() {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(DSN.getBytes(StandardCharsets.UTF_8)), null, null, "profiler");
    assertInstanceOf(BoardReadResult.Success.class, result);
    RoutingBoard board = (RoutingBoard) ((BoardReadResult.Success) result).board();
    RouterSettings settings = new SettingsMerger(new DefaultSettings()).merge();
    settings.setLayerCount(board.getLayerCount());
    settings.applyBoardSpecificOptimizations(board);
    RoutingJob job = new RoutingJob();
    job.thread =
        new StoppableThread() {
          @Override
          protected void threadAction() {}
        };
    job.board = board;
    job.routerSettings = settings;
    PerformanceProfiler.reset();

    int passes =
        BatchAutorouter.autoroutePassesForOptimizingItem(
            job, 2, settings.getStartRipupCosts(), 500, true, board, settings);

    assertTrue(passes >= 1, "the candidate router ran at least one pass");
    assertEquals(0, PerformanceProfiler.passHistorySize());
  }
}
