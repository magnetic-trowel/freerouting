package app.freerouting.io.specctra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.geometry.planar.Point;
import app.freerouting.geometry.planar.Polyline;
import app.freerouting.io.BoardReadResult;
import app.freerouting.rules.NetClass;
import app.freerouting.settings.GlobalSettings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Non-positive wire widths must neither enter the rules nor abort the SES export. */
class InvalidWidthTest {

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  private static RoutingBoard load(String dsn) {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(dsn.getBytes(StandardCharsets.UTF_8)), null, null);
    assertInstanceOf(BoardReadResult.Success.class, result);
    return (RoutingBoard) ((BoardReadResult.Success) result).board();
  }

  private static String fixture() throws IOException {
    try (InputStream in = DsnTestFixtures.openResource("scoring-perfect-two-pin.dsn")) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  void negativeNetClassWidthFallsBackToTheStructureWidth() throws IOException {
    String dsn =
        fixture()
            .replaceFirst(
                "\\(class kicad_default PERFECT\\s*\\)",
                "(class kicad_default PERFECT (rule (width -0.001) (clearance 200)))");
    RoutingBoard board = load(dsn);

    int netNumber = board.getPins().iterator().next().getNetNumber(0);
    NetClass netClass = board.rules.nets.get(netNumber).getNetClass();
    // The structure rule (width 250) um at resolution 10 gives a half width of 1250.
    assertEquals(1250, netClass.getTraceHalfWidth(0));
  }

  @Test
  void zeroWidthWireIsSkippedAndTheRestOfTheSessionIsWritten() throws IOException {
    RoutingBoard board = load(fixture());
    List<Pin> pins = new ArrayList<>(board.getPins());
    Point first = pins.get(0).getCenter();
    Point second = pins.get(1).getCenter();
    int netNumber = pins.get(0).getNetNumber(0);
    NetClass netClass = board.rules.nets.get(netNumber).getNetClass();
    for (int halfWidth : new int[] {netClass.getTraceHalfWidth(0), 0}) {
      board.insertTraceWithoutCleaning(
          new Polyline(first, second),
          0,
          halfWidth,
          new int[] {netNumber},
          netClass.getTraceClearanceClass(),
          FixedState.UNFIXED);
    }
    assertEquals(2, board.getTraces().size());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SesWriter.write(board, out, "invalid-width");
    String ses = out.toString(StandardCharsets.UTF_8);

    assertEquals(1, ses.split("\\(wire", -1).length - 1, ses);
  }
}
