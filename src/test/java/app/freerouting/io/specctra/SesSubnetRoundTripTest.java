package app.freerouting.io.specctra;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.trace.PolylineTrace;
import app.freerouting.drc.DesignRulesChecker;
import app.freerouting.settings.GlobalSettings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A net with {@code fromto} scopes loads as one subnet per connection, all with the same name. The
 * session must give each wire back to its own subnet: on subnet 1 the U2-U3 wire touches a pin of
 * subnet 2 only, which is a clearance violation and leaves subnet 2 unrouted.
 */
class SesSubnetRoundTripTest {

  private static final String DSN =
      """
      (pcb subnet.dsn
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
          (net CLK (fromto U1-1 U2-1) (fromto U2-1 U3-1))
          (class kicad_default CLK)
        )
        (wiring
          (wire (path F.Cu 250 5000 5000 15000 5000) (net CLK 1))
          (wire (path F.Cu 250 15000 5000 25000 5000) (net CLK 2))
        )
      )
      """;

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  private static RoutingBoard load() throws Exception {
    return DsnTestFixtures.loadBoard(DSN.getBytes(StandardCharsets.UTF_8));
  }

  private static List<String> traceSubnets(RoutingBoard board) {
    return board.getItems().stream()
        .filter(PolylineTrace.class::isInstance)
        .map(PolylineTrace.class::cast)
        .map(
            trace ->
                trace.firstCorner()
                    + "-"
                    + trace.lastCorner()
                    + " subnet "
                    + board.rules.nets.get(trace.getNetNumber(0)).subnetNumber)
        .sorted()
        .toList();
  }

  @Test
  void eachWireKeepsItsSubnet() throws Exception {
    RoutingBoard design = load();
    ByteArrayOutputStream session = new ByteArrayOutputStream();
    SesWriter.write(design, session, "subnet.dsn");

    RoutingBoard reloaded = load();
    SesImportSummary summary =
        SesReader.read(new ByteArrayInputStream(session.toByteArray()), reloaded);

    assertEquals(2, summary.wiresImported());
    assertEquals(traceSubnets(design), traceSubnets(reloaded));
    assertEquals(0, new DesignRulesChecker(reloaded, null).getAllClearanceViolations().size());
  }

  @Test
  void subnetWithoutWiresKeepsTheOrderOfTheOthers() throws Exception {
    RoutingBoard design = load();
    design.getItems().stream()
        .filter(PolylineTrace.class::isInstance)
        .map(PolylineTrace.class::cast)
        .filter(trace -> design.rules.nets.get(trace.getNetNumber(0)).subnetNumber == 1)
        .toList()
        .forEach(
            trace -> {
              trace.unfix();
              design.removeItem(trace);
            });
    ByteArrayOutputStream session = new ByteArrayOutputStream();
    SesWriter.write(design, session, "subnet.dsn");

    RoutingBoard reloaded = load();
    SesReader.read(new ByteArrayInputStream(session.toByteArray()), reloaded);

    assertEquals(traceSubnets(design), traceSubnets(reloaded));
  }
}
