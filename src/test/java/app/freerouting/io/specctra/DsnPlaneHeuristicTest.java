package app.freerouting.io.specctra;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.io.BoardReadResult;
import app.freerouting.settings.GlobalSettings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A copper pour that covers most of a signal layer makes its net a plane net, also on a board that
 * types another layer as a power layer.
 */
class DsnPlaneHeuristicTest {

  private static final String DSN =
      """
      (pcb planes.dsn
        (parser (string_quote ") (space_in_quoted_tokens on))
        (resolution um 10)
        (unit um)
        (structure
          (layer F.Cu (type signal) (property (index 0)))
          (layer In1.Cu (type power) (property (index 1)))
          (layer B.Cu (type signal) (property (index 2)))
          (boundary (path pcb 0  0 0  20000 0  20000 10000  0 10000  0 0))
          (plane GND (polygon In1.Cu 0  0 0  20000 0  20000 10000  0 10000  0 0))
          (via "Via[0-2]_800:400_um")
          (rule (width 250) (clearance 200))
        )
        (placement
          (component PAD
            (place U1 2000 5000 front 0)
            (place U2 18000 5000 front 0)))
        (library
          (image PAD (pin Round 1 0 0))
          (padstack Round (shape (circle F.Cu 800)) (attach off))
          (padstack "Via[0-2]_800:400_um"
            (shape (circle F.Cu 800)) (shape (circle In1.Cu 800)) (shape (circle B.Cu 800))
            (attach off)))
        (network
          (net VCC (pins U1-1 U2-1))
          (net GND)
          (class kicad_default VCC GND (circuit (use_via "Via[0-2]_800:400_um"))))
        (wiring
          (wire (polygon B.Cu 0  500 500  19500 500  19500 9500  500 9500  500 500)
            (net VCC) (type protect)))
      )
      """;

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  @Test
  void pourOnASignalLayerMakesAPlaneNetNextToAPowerLayer() {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(DSN.getBytes(StandardCharsets.UTF_8)), null, null);
    assertInstanceOf(BoardReadResult.Success.class, result);
    RoutingBoard board = (RoutingBoard) ((BoardReadResult.Success) result).board();
    assertTrue(!board.layerStructure.layers[1].isSignal, "In1.Cu stays a power layer");
    assertTrue(board.rules.nets.get("GND", 1).containsPlane());
    assertTrue(board.rules.nets.get("VCC", 1).containsPlane());
  }
}
