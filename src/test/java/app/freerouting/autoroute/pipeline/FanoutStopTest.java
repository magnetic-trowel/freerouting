package app.freerouting.autoroute.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.TestFixtures;
import app.freerouting.board.actions.ItemIdGenerator;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.core.StoppableThread;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.settings.RouterSettings;
import app.freerouting.settings.SettingsMerger;
import app.freerouting.settings.sources.DefaultSettings;
import app.freerouting.settings.sources.DsnFileSettings;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** A stop request, such as the job timeout, ends the fanout without starting another pass. */
class FanoutStopTest {

  private static final String FIXTURE = "Issue508-SMD-routing-issue-demo.dsn";

  @Test
  void stoppedThreadStartsNoFanoutPass() throws Exception {
    RoutingBoard board = loadBoard();
    int viasBefore = board.getViaCount();
    int tracesBefore = board.getTraces().size();
    StoppableThread thread = new NoOpThread();
    thread.requestStop();
    List<BatchFanout.FanoutPassStatus> statuses = new ArrayList<>();

    BatchFanout.FanoutRunSummary summary =
        BatchFanout.fanoutBoard(board, loadSettings(), thread, statuses::add);

    assertEquals(0, summary.completedPassCount());
    assertTrue(statuses.isEmpty(), "no pass may start, publish progress or try a pin");
    assertEquals(viasBefore, board.getViaCount());
    assertEquals(tracesBefore, board.getTraces().size());
  }

  @Test
  void stopDuringAPassEndsTheFanoutAfterThatPass() throws Exception {
    RoutingBoard board = loadBoard();
    StoppableThread thread = new NoOpThread();
    List<BatchFanout.FanoutPassStatus> statuses = new ArrayList<>();

    BatchFanout.FanoutRunSummary summary =
        BatchFanout.fanoutBoard(
            board,
            loadSettings(),
            thread,
            status -> {
              statuses.add(status);
              // The first report comes at the start of pass 1, before its first pin.
              thread.requestStop();
            });

    assertEquals(1, summary.completedPassCount());
    assertTrue(statuses.stream().allMatch(status -> status.passNo() == 1));
  }

  private static RoutingBoard loadBoard() throws Exception {
    File dsn = TestFixtures.resolveFile(FIXTURE);
    try (FileInputStream in = new FileInputStream(dsn)) {
      BoardReadResult result = DsnReader.readBoard(in, null, new ItemIdGenerator());
      return (RoutingBoard) ((BoardReadResult.Success) result).board();
    }
  }

  private static RouterSettings loadSettings() throws Exception {
    File dsn = TestFixtures.resolveFile(FIXTURE);
    try (FileInputStream in = new FileInputStream(dsn)) {
      return new SettingsMerger(new DefaultSettings(), new DsnFileSettings(in, FIXTURE)).merge();
    }
  }

  private static final class NoOpThread extends StoppableThread {
    @Override
    protected void threadAction() {}
  }
}
