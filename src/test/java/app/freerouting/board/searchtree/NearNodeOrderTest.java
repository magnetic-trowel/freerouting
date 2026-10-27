package app.freerouting.board.searchtree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.freerouting.Freerouting;
import app.freerouting.autoroute.expansion.CompleteFreeSpaceExpansionRoom;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.searchtree.ShapeSearchTree.NearNode;
import app.freerouting.datastructures.ShapeTree;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.io.specctra.DsnTestFixtures;
import app.freerouting.settings.GlobalSettings;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The nearest-first queue compares precomputed keys instead of calling {@link
 * ShapeTree.Leaf#compareTo}. At equal gap the keys must order every pair of nodes exactly as the
 * original comparison did (inner nodes first, rooms before items, descending id, ascending shape
 * index), or the walk would meet the leaves in another order and route differently.
 */
class NearNodeOrderTest {

  @Test
  void keysOrderNodesLikeTheLeafComparison() throws Exception {
    Freerouting.globalSettings = new GlobalSettings();
    RoutingBoard board = DsnTestFixtures.loadBoard("scoring-perfect-two-pin.dsn");
    IntBox box = new IntBox(0, 0, 10, 10);
    List<ShapeTree.Storable> objects = new ArrayList<>();
    for (Pin pin : board.getPins()) {
      objects.add(pin);
    }
    for (int id : new int[] {0, 1, 2, 7, Integer.MAX_VALUE}) {
      objects.add(new CompleteFreeSpaceExpansionRoom(box, 0, id));
    }
    // A room with a negative id has no key and falls back to the original comparison.
    objects.add(new CompleteFreeSpaceExpansionRoom(box, 0, -3));
    List<NearNode> nodes = new ArrayList<>();
    nodes.add(NearNode.of(new ShapeTree.InnerNode(box, null), box));
    nodes.add(NearNode.of(new ShapeTree.InnerNode(box, null), box));
    for (ShapeTree.Storable object : objects) {
      for (int index : new int[] {0, 1, 5}) {
        nodes.add(NearNode.of(new ShapeTree.Leaf(object, index, null, box), box));
      }
    }
    for (NearNode a : nodes) {
      for (NearNode b : nodes) {
        assertEquals(
            Integer.signum(originalCompare(a, b)),
            Integer.signum(a.compareTo(b)),
            () -> a.node() + " vs " + b.node());
      }
    }
  }

  /** The comparison of the queue before the keys: inner nodes first, then {@link Leaf} order. */
  private static int originalCompare(NearNode a, NearNode b) {
    int result = Long.compare(a.gap(), b.gap());
    if (result != 0) {
      return result;
    }
    boolean leaf = a.node() instanceof ShapeTree.Leaf;
    if (leaf != b.node() instanceof ShapeTree.Leaf) {
      return leaf ? 1 : -1;
    }
    return leaf ? ((ShapeTree.Leaf) a.node()).compareTo((ShapeTree.Leaf) b.node()) : 0;
  }
}
