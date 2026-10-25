package app.freerouting.io.specctra;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Pin;
import app.freerouting.settings.GlobalSettings;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * KiCad net names are case-sensitive. Two nets that differ only in case are different nets; the
 * pins of the second one were given to the first, so the router connected them.
 */
class DsnNetNameCaseTest {

  private static final String DSN =
      """
      (pcb case.dsn
        (parser (string_quote ") (space_in_quoted_tokens on))
        (resolution um 10)
        (unit um)
        (structure
          (layer F.Cu (type signal))
          (layer B.Cu (type signal))
          (boundary (path pcb 0  0 0  30000 0  30000 10000  0 10000  0 0))
          (via via_a)
          (rule (width 250) (clearance 200))
        )
        (placement
          (component PAD
            (place U1 5000 5000 front 0)
            (place U2 15000 5000 front 0)
            (place U3 25000 5000 front 0)
          )
        )
        (library
          (image PAD (pin round_pad 1 0 0))
          (padstack round_pad (shape (circle F.Cu 800)) (shape (circle B.Cu 800)) (attach off))
          (padstack via_a (shape (circle F.Cu 600)) (shape (circle B.Cu 600)) (attach off))
        )
        (network
          (net SIG (pins U1-1 U2-1))
          (net sig (pins U3-1))
          (class kicad_default SIG sig)
        )
      )
      """;

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  @Test
  void netsThatDifferOnlyInCaseKeepTheirOwnPins() throws Exception {
    RoutingBoard board = DsnTestFixtures.loadBoard(DSN.getBytes(StandardCharsets.UTF_8));

    assertEquals(List.of("SIG", "SIG", "sig"), pinNetNames(board));
    assertEquals("sig", board.rules.nets.get("sig", 1).name);
    assertEquals("SIG", board.rules.nets.get("SIG", 1).name);
    // a reference that matches no name exactly still finds the net
    assertEquals("SIG", board.rules.nets.get("Sig", 1).name);
  }

  private static List<String> pinNetNames(RoutingBoard board) {
    return board.getItems().stream()
        .filter(Pin.class::isInstance)
        .map(Pin.class::cast)
        .sorted(Comparator.comparingDouble(pin -> pin.getCenter().toFloat().x))
        .map(pin -> board.rules.nets.get(pin.getNetNumber(0)).name)
        .toList();
  }
}
