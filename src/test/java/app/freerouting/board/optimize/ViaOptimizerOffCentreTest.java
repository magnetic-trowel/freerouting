package app.freerouting.board.optimize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.items.Trace;
import app.freerouting.board.model.items.Via;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.board.model.structure.Layer;
import app.freerouting.board.model.structure.LayerStructure;
import app.freerouting.board.state.Communication;
import app.freerouting.board.trace.PolylineTrace;
import app.freerouting.core.library.Padstack;
import app.freerouting.geometry.planar.ConvexShape;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Polyline;
import app.freerouting.geometry.planar.PolylineShape;
import app.freerouting.geometry.planar.TileShape;
import app.freerouting.rules.BoardRules;
import app.freerouting.rules.ClearanceMatrix;
import app.freerouting.rules.ViaRule;
import app.freerouting.settings.GlobalSettings;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * A trace that runs over a via's centre and ends inside its pad counts as connected to the via.
 * Moving the via must keep that trace connected. (A trace that ends in the pad without covering the
 * centre is not a contact of the via at all, so ViaOptimizer never sees it.)
 */
class ViaOptimizerOffCentreTest {

  private RoutingBoard board;
  private Padstack padstack;

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
    LayerStructure layers =
        new LayerStructure(new Layer[] {new Layer("Top", true), new Layer("Bottom", true)});
    BoardRules rules = new BoardRules(layers, ClearanceMatrix.getDefaultInstance(layers, 10));
    rules.createDefaultNetClass();
    board =
        new RoutingBoard(
            new IntBox(0, 0, 200000, 200000),
            layers,
            new PolylineShape[] {TileShape.getInstance(0, 0, 200000, 200000)},
            0,
            rules,
            new Communication());
    board.rules.nets.add("Net1", 1, false);
    board.library.padstacks = new app.freerouting.core.library.Padstacks(layers);
    ConvexShape[] shapes =
        new ConvexShape[] {
          TileShape.getInstance(-500, -500, 500, 500), TileShape.getInstance(-500, -500, 500, 500)
        };
    padstack = board.library.padstacks.add("Via", shapes, true, false);
    board.library.addViaPadstack(padstack);
    ViaRule viaRule = new ViaRule("Default");
    rules.viaRules.add(viaRule);
    rules.getDefaultNetClass().setViaRule(viaRule);
  }

  private PolylineTrace trace(int layer, IntPoint... corners) {
    return board.insertTraceWithoutCleaning(
        new Polyline(corners), layer, 100, new int[] {1}, 1, FixedState.UNFIXED);
  }

  /** Two traces on different layers meet at a via; the top one overshoots the centre. */
  @ParameterizedTest
  @CsvSource({"0, 0", "300, 0"})
  void movedViaKeepsOffCentreTraceConnected(int dx, int dy) {
    IntPoint center = new IntPoint(100000, 100000);
    Via via = board.insertVia(padstack, center, new int[] {1}, 1, FixedState.UNFIXED, false);
    PolylineTrace top =
        trace(0, new IntPoint(20000, 100000), new IntPoint(100000 + dx, 100000 + dy));
    PolylineTrace bottom = trace(1, center, new IntPoint(100000, 180000));
    assertTrue(via.getNormalContacts().contains(top), "setup: top trace must touch the via");
    assertTrue(via.getNormalContacts().contains(bottom), "setup: bottom trace must touch the via");
    assertEquals(1, board.getConnectedSets(1).size(), "setup: one connected set");

    boolean moved = ViaOptimizer.optViaLocation(board, via, null, 100, 10);

    List<Item> copper = new ArrayList<>();
    for (Item item : board.getItems()) {
      if (item instanceof Trace || item instanceof Via) {
        copper.add(item);
      }
    }
    assertEquals(
        1,
        board.getConnectedSets(1).size(),
        "via moved="
            + moved
            + ", via now at "
            + via.getCenter()
            + "; copper after the move: "
            + copper);
  }
}
