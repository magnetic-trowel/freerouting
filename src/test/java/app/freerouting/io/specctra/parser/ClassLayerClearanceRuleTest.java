package app.freerouting.io.specctra.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.rules.ClearanceMatrix;
import app.freerouting.settings.GlobalSettings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** A clearance given in a class's {@code (layer_rule ...)} applies on that layer only. */
class ClassLayerClearanceRuleTest {

  private static final String BOARD =
      """
      (pcb class-layer-rule.dsn
        (parser (string_quote ") (space_in_quoted_tokens on))
        (resolution um 10)
        (unit um)
        (structure
          (layer F.Cu (type signal) (property (index 0)))
          (layer B.Cu (type signal) (property (index 1)))
          (boundary (path pcb 0  0 0  20000 0  20000 10000  0 10000  0 0))
          (rule (width 250) (clearance 200))
        )
        (placement
          (component PAD
            (place U1 2000 3000 front 0) (place U2 8000 3000 front 0) (place U3 14000 3000 front 0))
        )
        (library
          (image PAD (pin CirclePad_800_um 1 0 0))
          (padstack CirclePad_800_um
            (shape (circle F.Cu 800)) (shape (circle B.Cu 800)) (attach off))
        )
        (network
          (net A (pins U1-1))
          (net B (pins U2-1))
          (net C (pins U3-1))
          (class kicad_default A (rule (width 250) (clearance 200)))
          (class bottom_wide B (rule (width 250)) (layer_rule B.Cu (rule (clearance 800))))
          (class both C (rule (width 250) (clearance 300)) (layer_rule B.Cu (rule (clearance 700))))
        )
      )
      """;

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  private static RoutingBoard board() {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(BOARD.getBytes(StandardCharsets.UTF_8)), null, null);
    assertInstanceOf(BoardReadResult.Success.class, result);
    return (RoutingBoard) ((BoardReadResult.Success) result).board();
  }

  @Test
  void layerRuleClearanceDoesNotLeakToOtherLayers() {
    RoutingBoard board = board();
    ClearanceMatrix matrix = board.rules.clearanceMatrix;
    int wide = matrix.getNo("bottom_wide");
    int defaultClass = matrix.getNo("default");
    assertTrue(wide > 0 && defaultClass > 0);
    int frontLayer = board.layerStructure.getNo("F.Cu");
    int backLayer = board.layerStructure.getNo("B.Cu");
    // 800 um and 200 um at resolution um 10.
    assertEquals(8000, matrix.getValue(wide, defaultClass, backLayer, false));
    assertEquals(8000, matrix.getValue(wide, wide, backLayer, false));
    assertEquals(2000, matrix.getValue(wide, defaultClass, frontLayer, false));
    assertEquals(2000, matrix.getValue(wide, wide, frontLayer, false));
  }

  @Test
  void layerRuleOfAClassWithAGeneralRuleAppliesAgainstEveryClassOnItsLayer() {
    RoutingBoard board = board();
    ClearanceMatrix matrix = board.rules.clearanceMatrix;
    int both = matrix.getNo("both");
    int wide = matrix.getNo("bottom_wide");
    int defaultClass = matrix.getNo("default");
    int frontLayer = board.layerStructure.getNo("F.Cu");
    int backLayer = board.layerStructure.getNo("B.Cu");
    assertEquals(7000, matrix.getValue(both, both, backLayer, false));
    assertEquals(7000, matrix.getValue(both, defaultClass, backLayer, false));
    assertEquals(7000, matrix.getValue(defaultClass, both, backLayer, false));
    // the larger of the two classes on B.Cu
    assertEquals(8000, matrix.getValue(both, wide, backLayer, false));
    assertEquals(3000, matrix.getValue(both, both, frontLayer, false));
    assertEquals(3000, matrix.getValue(both, defaultClass, frontLayer, false));
    assertEquals(3000, matrix.getValue(both, wide, frontLayer, false));
  }
}
