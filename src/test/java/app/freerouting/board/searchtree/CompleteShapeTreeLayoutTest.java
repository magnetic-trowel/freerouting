package app.freerouting.board.searchtree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import app.freerouting.Freerouting;
import app.freerouting.autoroute.expansion.IncompleteFreeSpaceExpansionRoom;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.items.Pin;
import app.freerouting.datastructures.ShapeTree;
import app.freerouting.geometry.planar.TileShape;
import app.freerouting.io.specctra.DsnTestFixtures;
import app.freerouting.settings.GlobalSettings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The free-space rooms of the 45-degree tree depend on the board, not on the layout of the tree:
 * the same items inserted in another order give a different leaf order but the same rooms.
 */
class CompleteShapeTreeLayoutTest {

  @Test
  void roomsDoNotDependOnInsertionOrder() throws Exception {
    Freerouting.globalSettings = new GlobalSettings();
    RoutingBoard board = DsnTestFixtures.loadBoard("Issue508-DAC2020_bm01.dsn");
    List<Item> items = new ArrayList<>(board.getItems());
    ShapeSearchTree45Degree forward = new ShapeSearchTree45Degree(board, 1);
    items.forEach(forward::insert);
    Collections.reverse(items);
    ShapeSearchTree45Degree backward = new ShapeSearchTree45Degree(board, 1);
    items.forEach(backward::insert);
    assertNotEquals(
        leafOrder(forward), leafOrder(backward), "the two trees should differ in leaf order");

    int rooms = 0;
    for (Pin pin : board.getPins()) {
      if (pin.netCount() == 0) {
        continue;
      }
      for (int i = 0; i < pin.treeShapeCount(forward); i++) {
        TileShape shape = pin.getTreeShape(forward, i);
        int layer = pin.shapeLayer(i);
        int net = pin.getNetNumber(0);
        assertEquals(
            rooms(forward, shape, layer, net),
            rooms(backward, shape, layer, net),
            "rooms around pin " + pin.getId());
        rooms++;
      }
    }
    assertNotEquals(0, rooms);
  }

  private static List<ShapeTree.Leaf> leafOrder(ShapeSearchTree tree) {
    return Arrays.asList(tree.toArray());
  }

  private static List<String> rooms(
      ShapeSearchTree45Degree tree, TileShape contained, int layer, int net) {
    List<String> result = new ArrayList<>();
    for (IncompleteFreeSpaceExpansionRoom room :
        tree.completeShape(
            new IncompleteFreeSpaceExpansionRoom(null, layer, contained), net, null, null)) {
      result.add(String.valueOf(room.getShape()));
    }
    return result;
  }
}
