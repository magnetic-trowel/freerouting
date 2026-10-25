package app.freerouting.core.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.model.structure.Unit;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The lengths of the board statistics are in the requested unit whatever the DSN resolution. */
class BoardStatisticsUnitsTest {

  /** A 50 mm x 40 mm board with one 20 mm trace, written in the given unit and resolution. */
  private static BasicBoard board(String unit, int resolution) {
    double perMm = Unit.scale(1, Unit.MM, Unit.fromString(unit));
    String dsn =
        String.format(
            Locale.ROOT,
            """
            (pcb units
              (resolution %1$s %2$d)
              (unit %1$s)
              (structure
                (layer F.Cu (type signal))
                (layer B.Cu (type signal))
                (boundary (rect pcb 0 0 %3$s %4$s))
                (rule (width %5$s) (clearance %5$s)))
              (network (net A))
              (wiring
                (wire (path F.Cu %5$s %6$s %6$s %7$s %6$s) (net A) (type route))))
            """,
            unit,
            resolution,
            50 * perMm,
            40 * perMm,
            0.2 * perMm,
            10 * perMm,
            30 * perMm);
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(dsn.getBytes(StandardCharsets.UTF_8)), null, null);
    assertTrue(result instanceof BoardReadResult.Success, result.toString());
    return ((BoardReadResult.Success) result).board();
  }

  @ParameterizedTest
  @CsvSource({"um, 10", "mm, 100", "mm, 1000000", "mil, 1000", "mil, 1", "inch, 100000"})
  void lengthsAreInMillimetres(String unit, int resolution) {
    BasicBoard board = board(unit, resolution);
    BoardStatistics stats = new BoardStatistics(board, null, false, false);
    double mmPerBoardUnit = board.communication.boardUnitSize(Unit.MM);

    assertEquals("mm", stats.unit);
    assertEquals(20.0, stats.traces.totalLength, 0.05);
    assertEquals(20.0, stats.traces.totalLengthMm, 0.05);
    assertEquals(20.0, stats.traces.totalHorizontalLength, 0.05);
    // the routable box is the outline plus a margin
    assertEquals(board.getBoundingBox().width() * mmPerBoardUnit, stats.board.size.width, 0.05);
    assertEquals(
        board.getBoundingBox().height() * mmPerBoardUnit, stats.board.boundingBox.height, 0.05);
  }

  @ParameterizedTest
  @CsvSource({"um, 10", "mil, 1000"})
  void lengthsFollowTheRequestedUnit(String unit, int resolution) {
    BoardStatistics stats = new BoardStatistics(board(unit, resolution), Unit.MIL, false, false);

    assertEquals("mil", stats.unit);
    assertEquals(20.0 / 0.0254, stats.traces.totalLength, 0.5);
    assertEquals(20.0, stats.traces.totalLengthMm, 0.05);
  }
}
