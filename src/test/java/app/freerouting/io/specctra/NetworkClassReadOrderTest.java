package app.freerouting.io.specctra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.ConductionArea;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.items.ObstacleArea;
import app.freerouting.io.BoardReadResult;
import app.freerouting.settings.GlobalSettings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The structure scope is read before the network scope, whose classes define clearance classes of
 * their own. Names and rules from the network must still reach what the structure declared.
 */
class NetworkClassReadOrderTest {

  private static final String DSN =
      """
      (pcb order.dsn
        (parser (string_quote ") (space_in_quoted_tokens on))
        (resolution um 10)
        (unit um)
        (structure
          (layer F.Cu (type signal) (property (index 0)))
          (layer B.Cu (type signal) (property (index 1)))
          (boundary (path pcb 0  0 0  20000 0  20000 10000  0 10000  0 0))
          (via "Via[0-1]_800:400_um")
          (keepout "ko" (polygon signal 0  9000 4000  11000 4000  11000 6000  9000 6000  9000 4000)
            (clearance_class HV))
          (plane GND (polygon B.Cu 0  0 0  5000 0  5000 2000  0 2000  0 0) (clearance_class HV))
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
            (shape (circle F.Cu 800)) (shape (circle B.Cu 800)) (attach off)))
        (network
          (net A (pins U1-1 U2-1))
          (net B (pins U3-1 U4-1))
          (class kicad_default A GND
            (circuit (use_via "Via[0-1]_800:400_um"))
            (rule (width 250) (clearance 200)))
          (class HV B
            (circuit (use_via "Via[0-1]_800:400_um"))
            (rule (width 300) (clearance 500))))
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
  void structureAreasMayNameAClassOfTheNetwork() {
    RoutingBoard board = load();
    int hv = board.rules.clearanceMatrix.getNo("HV");
    assertTrue(hv > 0);
    int keepouts = 0;
    int planes = 0;
    for (Item item : board.getItems()) {
      if (item instanceof ConductionArea) {
        assertEquals(hv, item.clearanceClassIndex());
        planes++;
      } else if (item instanceof ObstacleArea) {
        assertEquals(hv, item.clearanceClassIndex());
        keepouts++;
      }
    }
    assertEquals(2, keepouts);
    assertEquals(1, planes);
  }
}
