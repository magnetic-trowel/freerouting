package app.freerouting.board.searchtree;

import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.autoroute.expansion.IncompleteFreeSpaceExpansionRoom;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Pin;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.io.specctra.DsnTestFixtures;
import app.freerouting.settings.GlobalSettings;
import org.junit.jupiter.api.Test;

/**
 * The 45-degree tree's room completion stops walking the tree once no room piece is left. The nodes
 * still in its thread-local queue must not stay there: they keep the tree, its board and the
 * autoroute engine reachable from the thread, so every engine an optimizer thread used leaked.
 */
class CompleteShapeQueueTest {

  @Test
  void earlyExitLeavesNoNodesInTheThreadLocalQueue() throws Exception {
    Freerouting.globalSettings = new GlobalSettings();
    RoutingBoard board = DsnTestFixtures.loadBoard("scoring-perfect-two-pin.dsn");
    ShapeSearchTree45Degree tree = new ShapeSearchTree45Degree(board, 0);
    Pin pin = null;
    for (Pin current : board.getPins()) {
      tree.insert(current);
      pin = current;
    }
    IntPoint center = (IntPoint) pin.getCenter();
    // A room that must contain a spot inside the pad of a pin of another net: the pad removes
    // every room piece, which ends the walk early.
    IntBox inside = new IntBox(center.x - 10, center.y - 10, center.x + 10, center.y + 10);
    int otherNet = pin.getNetNumber(0) + 1;
    var result =
        tree.completeShape(
            new IncompleteFreeSpaceExpansionRoom(null, 0, inside), otherNet, null, null);

    assertTrue(result.isEmpty(), "the pad must leave no room piece");
    assertTrue(tree.completeShapeQueue.get().isEmpty(), "nodes were left in the queue");
  }
}
