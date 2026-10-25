package app.freerouting.io.specctra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.items.Trace;
import app.freerouting.board.model.structure.FixedState;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** KiCad names hierarchical nets "/SDA" or "/sheet/SDA"; the leading slash must survive SES. */
class SesHierarchicalNetNameTest {

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  private static RoutingBoard load(String netName) throws IOException {
    String dsn;
    try (InputStream in = DsnTestFixtures.openResource("scoring-perfect-two-pin.dsn")) {
      dsn = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    dsn = dsn.replace("PERFECT", "\"" + netName + "\"");
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(dsn.getBytes(StandardCharsets.UTF_8)), null, null);
    assertInstanceOf(BoardReadResult.Success.class, result);
    return (RoutingBoard) ((BoardReadResult.Success) result).board();
  }

  @ParameterizedTest
  @ValueSource(strings = {"/SDA", "/sheet/SDA"})
  void leadingSlashSurvivesTheSessionRoundTrip(String netName) throws IOException {
    RoutingBoard board = load(netName);
    List<Pin> pins = new ArrayList<>(board.getPins());
    int netNumber = pins.get(0).getNetNumber(0);
    assertEquals(netName, board.rules.nets.get(netNumber).name);
    NetClass netClass = board.rules.nets.get(netNumber).getNetClass();
    board.insertTraceWithoutCleaning(
        new Polyline(pins.get(0).getCenter(), pins.get(1).getCenter()),
        0,
        netClass.getTraceHalfWidth(0),
        new int[] {netNumber},
        netClass.getTraceClearanceClass(),
        FixedState.UNFIXED);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SesWriter.write(board, out, "hierarchical");
    String ses = out.toString(StandardCharsets.UTF_8);
    assertTrue(ses.contains("(net \"" + netName + "\""), ses);

    RoutingBoard reloaded = load(netName);
    SesImportSummary summary =
        SesReader.read(new ByteArrayInputStream(ses.getBytes(StandardCharsets.UTF_8)), reloaded);
    assertEquals(1, summary.wiresImported());
    assertEquals(0, summary.errorsEncountered());
    Trace trace = reloaded.getTraces().iterator().next();
    assertEquals(netName, reloaded.rules.nets.get(trace.getNetNumber(0)).name);
  }
}
