package app.freerouting.io.specctra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Via;
import app.freerouting.settings.GlobalSettings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Every via in a session must find its padstack in the session's {@code library_out}: KiCad stops
 * the whole import with "A wire_via references a missing padstack" otherwise. A design via whose
 * padstack is not in the structure's via list used to be written without its padstack.
 */
class SesLibraryOutTest {

  private static final String DSN =
      """
      (pcb library_out.dsn
        (parser (string_quote ") (space_in_quoted_tokens on))
        (resolution um 10)
        (unit um)
        (structure
          (layer F.Cu (type signal))
          (layer B.Cu (type signal))
          (boundary (path pcb 0  0 0  10000 0  10000 10000  0 10000  0 0))
          (via via_a)
          (rule (width 250) (clearance 200))
        )
        (placement
          (component PAD
            (place U1 2000 5000 front 0)
            (place U2 8000 5000 front 0)
          )
        )
        (library
          (image PAD (pin round_pad 1 0 0))
          (padstack round_pad (shape (circle F.Cu 800)) (shape (circle B.Cu 800)) (attach off))
          (padstack via_a (shape (circle F.Cu 600)) (shape (circle B.Cu 600)) (attach off))
          (padstack via_b (shape (circle F.Cu 900)) (shape (circle B.Cu 900)) (attach off))
        )
        (network
          (net SIG (pins U1-1 U2-1))
          (class kicad_default SIG)
        )
        (wiring
          (via via_b 5000 5000 (net SIG))
        )
      )
      """;

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  @Test
  void designViaPadstacksOutsideTheViaListAreWritten() throws Exception {
    RoutingBoard board = DsnTestFixtures.loadBoard(DSN.getBytes(StandardCharsets.UTF_8));
    assertEquals(1, board.getItems().stream().filter(Via.class::isInstance).count());

    ByteArrayOutputStream session = new ByteArrayOutputStream();
    SesWriter.write(board, session, "library_out.dsn");
    String text = session.toString(StandardCharsets.UTF_8);

    assertTrue(text.contains("(via \"via_b\" 50000"), text);
    assertTrue(text.contains("(padstack \"via_b\""), text);
    assertTrue(text.contains("(padstack \"via_a\""), "the via list is still written: " + text);

    RoutingBoard reloaded = DsnTestFixtures.loadBoard(DSN.getBytes(StandardCharsets.UTF_8));
    SesImportSummary summary =
        SesReader.read(new ByteArrayInputStream(session.toByteArray()), reloaded);
    assertEquals(1, summary.viasImported());
    assertEquals(0, summary.errorsEncountered());
  }
}
