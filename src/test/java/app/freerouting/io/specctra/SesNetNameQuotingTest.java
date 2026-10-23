package app.freerouting.io.specctra;

import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.board.model.structure.Layer;
import app.freerouting.board.model.structure.LayerStructure;
import app.freerouting.board.model.structure.Unit;
import app.freerouting.board.state.Communication;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Polyline;
import app.freerouting.geometry.planar.PolylineShape;
import app.freerouting.io.CoordinateTransform;
import app.freerouting.rules.BoardRules;
import app.freerouting.rules.ClearanceMatrix;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** KiCad 10 rejects a whole session that contains a bare net name such as ADD|SUB. */
class SesNetNameQuotingTest {

  private static String sessionWithNet(String netName) throws Exception {
    LayerStructure layers =
        new LayerStructure(new Layer[] {new Layer("F.Cu", true), new Layer("B.Cu", true)});
    BoardRules rules = new BoardRules(layers, ClearanceMatrix.getDefaultInstance(layers, 1));
    rules.createDefaultNetClass();
    Communication defaults = new Communication();
    Communication communication =
        new Communication(
            Unit.UM,
            10,
            defaults.specctraParserInfo,
            new CoordinateTransform(1, 0, 0),
            defaults.idGenerator,
            defaults.observers);
    BasicBoard board =
        new BasicBoard(
            new IntBox(-10000, -10000, 10000, 10000),
            layers,
            new PolylineShape[0],
            0,
            rules,
            communication);
    rules.nets.add(netName, 1, false);
    board.insertTraceWithoutCleaning(
        new Polyline(new IntPoint(0, 0), new IntPoint(1000, 0)),
        0,
        50,
        new int[] {1},
        1,
        FixedState.UNFIXED);
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    SesWriter.write(board, bytes, "quoting");
    return bytes.toString(StandardCharsets.UTF_8);
  }

  @Test
  void netNamesWithPunctuationAreQuoted() throws Exception {
    assertTrue(sessionWithNet("ADD|SUB").contains("(net \"ADD|SUB\""));
    assertTrue(sessionWithNet("+5V").contains("(net \"+5V\""));
  }

  @Test
  void plainNetNamesStayBareAndLayerNamesAreUnchanged() throws Exception {
    String session = sessionWithNet("GND");
    assertTrue(session.contains("(net GND"), session);
    assertTrue(session.contains("(path F.Cu "), session);
  }
}
