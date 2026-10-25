package app.freerouting.board.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.board.model.structure.Layer;
import app.freerouting.board.model.structure.LayerStructure;
import app.freerouting.board.state.Communication;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Polyline;
import app.freerouting.geometry.planar.PolylineShape;
import app.freerouting.rules.BoardRules;
import app.freerouting.rules.ClearanceMatrix;
import app.freerouting.settings.GlobalSettings;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A trace that crosses a trace of its net which cannot be split is split only where the two are in
 * contact afterwards.
 */
class PolylineTraceSplitAtFixedTraceTest {
  private RoutingBoard board;
  private PolylineTrace fixedTrace;

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
    LayerStructure layers = new LayerStructure(new Layer[] {new Layer("Top", true)});
    BoardRules rules = new BoardRules(layers, ClearanceMatrix.getDefaultInstance(layers, 10));
    rules.createDefaultNetClass();
    board =
        new RoutingBoard(
            new IntBox(-10000, -10000, 10000, 10000),
            layers,
            new PolylineShape[0],
            0,
            rules,
            new Communication());
    board.rules.nets.add("Net1", 1, false);
    // A user-fixed trace at about 60 degrees; its integer points are the multiples of (56, 97).
    fixedTrace =
        insertTrace(
            new Polyline(new IntPoint(0, 0), new IntPoint(560, 970)), FixedState.USER_FIXED);
  }

  @Test
  void crossingAtRationalPointLeavesTheTraceWhole() {
    // The vertical line x = 100 meets the fixed trace at (100, 173.21...).
    PolylineTrace crossing =
        insertTrace(
            new Polyline(new IntPoint(100, -500), new IntPoint(100, 1500)), FixedState.UNFIXED);

    assertFalse(crossing.normalize(null), "nothing to normalize");
    assertTrue(crossing.isOnTheBoard());
    assertEquals(Set.of(fixedTrace, crossing), Set.copyOf(board.getTraces()));
  }

  @Test
  void crossingAtIntegerPointIsSplitThere() {
    PolylineTrace crossing =
        insertTrace(
            new Polyline(new IntPoint(56, -500), new IntPoint(56, 1500)), FixedState.UNFIXED);

    assertTrue(crossing.normalize(null));
    assertFalse(crossing.isOnTheBoard());
    List<PolylineTrace> pieces =
        board.getTraces().stream()
            .filter(trace -> trace != fixedTrace)
            .map(PolylineTrace.class::cast)
            .toList();
    assertEquals(2, pieces.size());
    IntPoint junction = new IntPoint(56, 97);
    for (PolylineTrace piece : pieces) {
      assertTrue(junction.equals(piece.firstCorner()) || junction.equals(piece.lastCorner()));
      assertTrue(piece.getNormalContacts().contains(fixedTrace), piece + " touches the fixed one");
    }
  }

  private PolylineTrace insertTrace(Polyline polyline, FixedState fixedState) {
    return board.insertTraceWithoutCleaning(polyline, 0, 10, new int[] {1}, 1, fixedState);
  }
}
