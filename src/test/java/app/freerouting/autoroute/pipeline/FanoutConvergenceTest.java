package app.freerouting.autoroute.pipeline;

import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.TestFixtures;
import app.freerouting.board.actions.ItemIdGenerator;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.core.StoppableThread;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.logger.FRLogger;
import app.freerouting.settings.RouterSettings;
import app.freerouting.settings.SettingsMerger;
import app.freerouting.settings.sources.DefaultSettings;
import app.freerouting.settings.sources.DsnFileSettings;
import java.io.File;
import java.io.FileInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue690 is a routed board with planes. Before the fix, fanout reported most of its pins as
 * routed although the search ended on copper the net already had, and trace normalization looped on
 * split/combine no-ops until its 2000-iteration cap.
 */
@Tag("slow")
class FanoutConvergenceTest {

  private static final String FIXTURE = "Issue690-kit-dev-coldfire-xilinx_5213.dsn";

  @Test
  void firstFanoutPassCountsOnlyRealEscapesAndNormalizationConverges() throws Exception {
    File dsn = TestFixtures.resolveFile(FIXTURE);
    BoardReadResult result =
        DsnReader.readBoard(new FileInputStream(dsn), null, new ItemIdGenerator());
    RoutingBoard board = (RoutingBoard) ((BoardReadResult.Success) result).board();
    RouterSettings settings =
        new SettingsMerger(
                new DefaultSettings(), new DsnFileSettings(new FileInputStream(dsn), FIXTURE))
            .merge();
    settings.fanout.maxPasses = 1;

    Instant start = Instant.now();
    List<BatchFanout.FanoutPassStatus> passes = new ArrayList<>();
    BatchFanout.fanoutBoard(board, settings, new NoOpThread(), passes::add);

    BatchFanout.FanoutPassStatus firstPass =
        passes.stream()
            .filter(BatchFanout.FanoutPassStatus::passCompleted)
            .findFirst()
            .orElseThrow();
    assertTrue(
        firstPass.notRoutedCount() > 0,
        "pins whose search ends on existing copper must not count as fanned out");
    assertTrue(
        Arrays.stream(FRLogger.getLogEntries().getEntries(start, null))
            .noneMatch(e -> e.getMessage().contains("iterations for net")),
        "normalization must converge without hitting its iteration cap");
  }

  private static final class NoOpThread extends StoppableThread {
    @Override
    protected void threadAction() {}
  }
}
