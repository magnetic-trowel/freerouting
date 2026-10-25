package app.freerouting.io.specctra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.io.BoardReadResult;
import app.freerouting.rules.NetClass;
import app.freerouting.rules.ViaRule;
import app.freerouting.settings.GlobalSettings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * KiCad exports a net class size that is not set as -0.001: "(width -0.001)", "(clearance -0.001)"
 * and a via padstack "(circle F.Cu -0.001)". An unset size is absent, so the default applies.
 */
class UnsetSizeTest {

  private static final String DSN =
      """
      (pcb unset.dsn
        (parser (string_quote ") (space_in_quoted_tokens on))
        (resolution um 10)
        (unit um)
        (structure
          (layer F.Cu (type signal) (property (index 0)))
          (layer B.Cu (type signal) (property (index 1)))
          (boundary (path pcb 0  0 0  20000 0  20000 10000  0 10000  0 0))
          (via "Via[0-1]_800:400_um" "Via[0-1]_-0.001:-0.001_um")
          (rule (width 250) (clearance 200))
        )
        (placement
          (component PAD
            (place U1 2000 5000 front 0)
            (place U2 18000 5000 front 0)
            (place U3 2000 8000 front 0)
            (place U4 18000 8000 front 0)))
        (library
          (image PAD (pin Round 1 0 0))
          (padstack Round (shape (circle F.Cu 800)) (attach off))
          (padstack "Via[0-1]_800:400_um"
            (shape (circle F.Cu 800)) (shape (circle B.Cu 800)) (attach off))
          (padstack "Via[0-1]_-0.001:-0.001_um"
            (shape (circle F.Cu -0.001)) (shape (circle B.Cu -0.001)) (attach off)))
        (network
          (net A (pins U1-1 U2-1))
          (net B (pins U3-1 U4-1))
          (class kicad_default A
            (circuit (use_via "Via[0-1]_800:400_um"))
            (rule (width 250) (clearance 200)))
          (class Unset B
            (circuit (use_via "Via[0-1]_-0.001:-0.001_um"))
            (rule (width -0.001) (clearance -0.001))))
      )
      """;

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  private static RoutingBoard load() {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(DSN.getBytes(StandardCharsets.UTF_8)), null, null);
    assertInstanceOf(BoardReadResult.Success.class, result);
    return (RoutingBoard) ((BoardReadResult.Success) result).board();
  }

  @Test
  void viaPadstackWithUnsetDiameterIsNotRead() {
    RoutingBoard board = load();
    assertNull(board.library.padstacks.get("Via[0-1]_-0.001:-0.001_um"));
    for (int i = 0; i < board.library.viaPadstackCount(); i++) {
      // 800 um at resolution 10: radius 4000; a -0.001 diameter used to give radius 1.
      assertEquals(
          4000,
          ((app.freerouting.geometry.planar.Circle) board.library.getViaPadstack(i).getShape(0))
              .radius);
    }
  }

  @Test
  void classWithUnsetSizesUsesTheDefaultViasWidthAndClearance() {
    RoutingBoard board = load();
    NetClass unset = board.rules.netClasses.get("Unset");
    NetClass defaultClass = board.rules.getDefaultNetClass();
    ViaRule viaRule = unset.getViaRule();
    assertEquals(1, viaRule.viaCount());
    assertEquals("Via[0-1]_800:400_um", viaRule.getVia(0).getPadstack().name);
    assertEquals(defaultClass.getTraceHalfWidth(0), unset.getTraceHalfWidth(0));
    int clearanceClass = unset.getTraceClearanceClass();
    // The class keeps the default clearance of 200 um (2000 board units) to itself.
    assertEquals(
        2000, board.rules.clearanceMatrix.getValue(clearanceClass, clearanceClass, 0, false));
  }
}
