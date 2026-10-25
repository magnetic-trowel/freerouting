package app.freerouting.board.facade;

import static org.junit.jupiter.api.Assertions.assertSame;

import app.freerouting.Freerouting;
import app.freerouting.drc.ClearanceBaseline;
import app.freerouting.drc.DesignRulesChecker;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.settings.GlobalSettings;
import java.io.FileInputStream;
import org.junit.jupiter.api.Test;

/**
 * Board copies keep the baseline of pre-existing clearance violations. It is a transient field, and
 * a copy without it counted every violation of the input as new, so a routing fallback continuation
 * could never replace the incumbent on a board whose input had violations.
 */
class BoardCopyBaselineTest {

  @Test
  void deepCopyAndImageCopiesKeepTheClearanceBaseline() throws Exception {
    Freerouting.globalSettings = new GlobalSettings();
    RoutingBoard board;
    try (FileInputStream in = new FileInputStream("fixtures/Issue143-rpi_splitter.dsn")) {
      board =
          (RoutingBoard)
              ((BoardReadResult.Success) DsnReader.readBoard(in, null, null, "rpi")).board();
    }
    board.clearanceBaseline =
        new ClearanceBaseline(new DesignRulesChecker(board, null).getAllClearanceViolations());

    assertSame(board.clearanceBaseline, board.deepCopy().clearanceBaseline);
    assertSame(board.clearanceBaseline, board.image().copy().clearanceBaseline);
  }
}
