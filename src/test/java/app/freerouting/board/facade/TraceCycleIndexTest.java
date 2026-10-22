package app.freerouting.board.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.board.model.structure.FixedState;
import app.freerouting.board.model.structure.Layer;
import app.freerouting.board.model.structure.LayerStructure;
import app.freerouting.board.state.Communication;
import app.freerouting.board.trace.PolylineTrace;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Polyline;
import app.freerouting.geometry.planar.PolylineShape;
import app.freerouting.geometry.planar.TileShape;
import app.freerouting.rules.BoardRules;
import app.freerouting.rules.ClearanceMatrix;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Checks that {@link TraceCycleIndex} agrees with {@code Trace.isCycle()}. */
class TraceCycleIndexTest {

  private static final int NET = 1;

  private static BasicBoard createBoard() {
    LayerStructure layerStructure = new LayerStructure(new Layer[] {new Layer("Top", true)});
    ClearanceMatrix clearanceMatrix = ClearanceMatrix.getDefaultInstance(layerStructure, 10);
    BoardRules rules = new BoardRules(layerStructure, clearanceMatrix);
    rules.createDefaultNetClass();
    return new BasicBoard(
        new IntBox(0, 0, 100000, 100000),
        layerStructure,
        new PolylineShape[] {TileShape.getInstance(0, 0, 100000, 100000)},
        0,
        rules,
        new Communication());
  }

  private static PolylineTrace trace(BasicBoard board, int x1, int y1, int x2, int y2) {
    return board.insertTraceWithoutCleaning(
        new Polyline(new IntPoint(x1, y1), new IntPoint(x2, y2)),
        0,
        100,
        new int[] {NET},
        1,
        FixedState.UNFIXED);
  }

  private static void assertAgreesWithIsCycle(BasicBoard board, List<PolylineTrace> traces) {
    TraceCycleIndex index = new TraceCycleIndex(board);
    for (PolylineTrace trace : traces) {
      // On a symmetric contact graph the index is exact, not only a filter.
      assertEquals(trace.isCycle(), index.mayBeCycle(trace), "trace " + trace.getId());
    }
  }

  @Test
  void treeWithJunctionsHasNoCycle() {
    BasicBoard board = createBoard();
    // A star: three traces meet at (5000, 5000), so their contacts form a triangle in the item
    // graph, but no trace is on a cycle. One arm continues to a further junction.
    List<PolylineTrace> traces =
        List.of(
            trace(board, 1000, 5000, 5000, 5000),
            trace(board, 5000, 5000, 9000, 5000),
            trace(board, 5000, 5000, 5000, 9000),
            trace(board, 9000, 5000, 9000, 9000),
            trace(board, 9000, 5000, 13000, 5000));
    TraceCycleIndex index = new TraceCycleIndex(board);
    for (PolylineTrace trace : traces) {
      assertFalse(trace.isCycle());
      assertFalse(index.mayBeCycle(trace));
    }
  }

  @Test
  void loopWithTailMarksOnlyTheLoop() {
    BasicBoard board = createBoard();
    PolylineTrace bottom = trace(board, 1000, 1000, 5000, 1000);
    PolylineTrace right = trace(board, 5000, 1000, 5000, 5000);
    PolylineTrace top = trace(board, 5000, 5000, 1000, 5000);
    PolylineTrace left = trace(board, 1000, 5000, 1000, 1000);
    PolylineTrace tail = trace(board, 5000, 5000, 9000, 5000);
    PolylineTrace tailEnd = trace(board, 9000, 5000, 9000, 9000);
    TraceCycleIndex index = new TraceCycleIndex(board);
    for (PolylineTrace loopTrace : List.of(bottom, right, top, left)) {
      assertTrue(loopTrace.isCycle());
      assertTrue(index.mayBeCycle(loopTrace));
    }
    assertFalse(index.mayBeCycle(tail));
    assertFalse(index.mayBeCycle(tailEnd));
    assertAgreesWithIsCycle(board, List.of(bottom, right, top, left, tail, tailEnd));
  }

  @Test
  void indexFollowsBoardChanges() {
    BasicBoard board = createBoard();
    PolylineTrace first = trace(board, 1000, 1000, 5000, 1000);
    PolylineTrace second = trace(board, 5000, 1000, 5000, 5000);
    PolylineTrace third = trace(board, 5000, 5000, 1000, 5000);
    TraceCycleIndex index = new TraceCycleIndex(board);
    assertFalse(index.mayBeCycle(first));

    // Closing the loop must be seen by the same index instance.
    PolylineTrace fourth = trace(board, 1000, 5000, 1000, 1000);
    assertTrue(index.mayBeCycle(first));
    assertTrue(index.mayBeCycle(fourth));

    board.removeItem(second);
    assertFalse(index.mayBeCycle(first));
    assertAgreesWithIsCycle(board, List.of(first, third, fourth));
  }
}
