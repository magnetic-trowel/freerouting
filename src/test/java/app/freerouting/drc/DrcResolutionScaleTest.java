package app.freerouting.drc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.model.structure.Unit;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.settings.DesignRulesCheckerSettings;
import app.freerouting.settings.GlobalSettings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The DSN reader divides the coordinate scale by powers of ten when the declared resolution would
 * overflow int coordinates (LibrePCB writes {@code resolution mm 1000000}). DRC converted board
 * units to micrometres with the declared resolution, which then overstated the board units per
 * millimetre by 10x to 100x, so a real clearance shortfall fell under the 1 um tolerance.
 */
class DrcResolutionScaleTest {

  /** Two 0.25 mm traces 0.4 mm apart: a 0.15 mm gap against a 0.2 mm clearance rule. */
  private static String board(String resolution, String unit, double scale) {
    return """
        (pcb test
          (parser (host_cad "LibrePCB") (host_version "2.0.0"))
          (resolution %1$s)
          (unit %2$s)
          (structure
            (layer F.Cu (type signal))
            (layer B.Cu (type signal))
            (boundary (rect pcb 0 0 %3$s %3$s))
            (via "V1")
            (rule (width %4$s) (clearance %5$s))
          )
          (placement)
          (library
            (padstack "V1" (shape (circle F.Cu %6$s)) (shape (circle B.Cu %6$s)) (attach off))
          )
          (network
            (net A) (net B)
            (class default A B (circuit (use_via "V1")) (rule (width %4$s) (clearance %5$s)))
          )
          (wiring
            (wire (path F.Cu %4$s %7$s %7$s %8$s %7$s) (net A) (type route))
            (wire (path F.Cu %4$s %7$s %9$s %8$s %9$s) (net B) (type route))
          )
        )
        """
        .formatted(
            resolution,
            unit,
            n(100 * scale),
            n(0.25 * scale),
            n(0.2 * scale),
            n(0.6 * scale),
            n(10 * scale),
            n(50 * scale),
            n(10.4 * scale));
  }

  private static String n(double value) {
    return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
  }

  private static BasicBoard load(String dsn) {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(dsn.getBytes(StandardCharsets.UTF_8)), null, null, "t.dsn");
    return assertInstanceOf(BoardReadResult.Success.class, result).board();
  }

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  @Test
  void shortfallIsReportedAtEveryResolution() {
    BasicBoard micrometres = load(board("um 10", "um", 1000));
    BasicBoard nanometreGrid = load(board("mm 1000000", "mm", 1));
    assertTrue(
        nanometreGrid.communication.coordinateTransform.dsnToBoard(1)
            < nanometreGrid.communication.resolution,
        "the reader must have reduced the scale for this test to mean anything");

    for (BasicBoard board : new BasicBoard[] {micrometres, nanometreGrid}) {
      var violations =
          new DesignRulesChecker(board, new DesignRulesCheckerSettings())
              .getAllClearanceViolations();
      assertEquals(
          1, violations.size(), "violations at resolution " + board.communication.resolution);
    }
  }

  @Test
  void boardUnitSizeFollowsTheCoordinateTransform() {
    BasicBoard board = load(board("mm 1000000", "mm", 1));
    double boardUnitsPerMm = board.communication.coordinateTransform.dsnToBoard(1);
    assertEquals(1000.0 / boardUnitsPerMm, board.communication.boardUnitSize(Unit.UM), 1e-12);
    assertEquals(1.0, board.communication.boardUnitSize(Unit.MM) * boardUnitsPerMm, 1e-12);
  }
}
