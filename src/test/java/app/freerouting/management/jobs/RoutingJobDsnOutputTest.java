package app.freerouting.management.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.analytics.FRAnalytics;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.core.BoardFileDetails;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.RoutingJobState;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.FileFormat;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.io.specctra.DsnTestFixtures;
import app.freerouting.settings.GlobalSettings;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The command line documents {@code -do board.dsn}; the headless writer must produce it. */
class RoutingJobDsnOutputTest {

  @BeforeEach
  void setUp() {
    GlobalSettings.setCurrent(new GlobalSettings());
    app.freerouting.Freerouting.globalSettings = new GlobalSettings();
    FRAnalytics.setEnabled(false);
  }

  @Test
  void dsnOutputHoldsTheBoardWithItsWiring() throws Exception {
    RoutingJob job = new RoutingJob();
    job.name = "Issue153-wavefolder";
    job.state = RoutingJobState.COMPLETED;
    job.board = DsnTestFixtures.loadBoard("Issue153-wavefolder.dsn");
    job.output = new BoardFileDetails();
    job.output.format = FileFormat.DSN;

    assertTrue(RoutingJobOutputWriter.write(job));

    byte[] written = job.output.getData().readAllBytes();
    BoardReadResult result = DsnReader.readBoard(new ByteArrayInputStream(written), null, null);
    assertInstanceOf(BoardReadResult.Success.class, result);
    RoutingBoard reloaded = (RoutingBoard) ((BoardReadResult.Success) result).board();
    assertEquals(job.board.getVias().size(), reloaded.getVias().size());
    assertEquals(job.board.getTraces().size(), reloaded.getTraces().size());
  }
}
