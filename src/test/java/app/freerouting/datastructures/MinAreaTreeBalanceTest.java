package app.freerouting.datastructures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.geometry.planar.FortyfiveDegreeBoundingDirections;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.geometry.planar.TileShape;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** The tree stays height-balanced and keeps the order of its leaves for any input. */
class MinAreaTreeBalanceTest {

  @Test
  void equalShapesGiveLogarithmicDepth() {
    MinAreaTree tree = newTree();
    for (int i = 0; i < 4000; i++) {
      tree.insert(new TestStorable(i, new IntBox(0, 0, 100, 100)));
    }
    assertBalanced(tree);
  }

  @Test
  void nestedShapesGiveLogarithmicDepth() {
    MinAreaTree tree = newTree();
    for (int i = 0; i < 4000; i++) {
      tree.insert(new TestStorable(i, new IntBox(-i, -i, i, i)));
    }
    assertBalanced(tree);
  }

  @Test
  void insertAndRemoveKeepLeafOrderAndBalance() {
    MinAreaTree tree = newTree();
    Random random = new Random(7);
    List<ShapeTree.Leaf> order = new ArrayList<>();
    for (int step = 0; step < 3000; step++) {
      if (order.isEmpty() || random.nextInt(3) != 0) {
        int x = random.nextInt(1000);
        int y = random.nextInt(1000);
        int size = random.nextInt(4) == 0 ? 500 : random.nextInt(20);
        TestStorable object = new TestStorable(step, new IntBox(x, y, x + size, y + size));
        tree.insert(object);
        List<ShapeTree.Leaf> now = Arrays.asList(tree.toArray());
        List<ShapeTree.Leaf> others = new ArrayList<>(now);
        others.remove(object.entries[0]);
        assertEquals(order, others, "an insertion reordered the other leaves");
        order = new ArrayList<>(now);
      } else {
        ShapeTree.Leaf removed = order.remove(random.nextInt(order.size()));
        tree.removeLeaf(removed);
        assertEquals(order, Arrays.asList(tree.toArray()), "a removal reordered the leaves");
      }
      if (step % 100 == 0) {
        assertBalanced(tree);
      }
    }
    assertBalanced(tree);
  }

  private static MinAreaTree newTree() {
    return new MinAreaTree(FortyfiveDegreeBoundingDirections.INSTANCE);
  }

  /** Checks links, bounds, layer masks, heights and the AVL balance of every node. */
  private static void assertBalanced(MinAreaTree tree) {
    if (tree.root == null) {
      return;
    }
    assertSame(null, tree.root.parent);
    int height = check(tree.root);
    // An AVL tree with n leaves has height below 1.45 log2(n + 2).
    double bound = 1.45 * Math.log(tree.size() + 2) / Math.log(2);
    assertTrue(height <= bound, "height " + height + " exceeds " + bound);
  }

  private static int check(ShapeTree.TreeNode node) {
    if (!(node instanceof ShapeTree.InnerNode inner)) {
      return 0;
    }
    assertSame(inner, inner.firstChild.parent);
    assertSame(inner, inner.secondChild.parent);
    assertTrue(inner.boundingShape.contains(inner.firstChild.boundingShape));
    assertTrue(inner.boundingShape.contains(inner.secondChild.boundingShape));
    assertEquals(inner.firstChild.layerMask | inner.secondChild.layerMask, inner.layerMask);
    int first = check(inner.firstChild);
    int second = check(inner.secondChild);
    assertTrue(Math.abs(first - second) <= 1, "unbalanced node");
    assertEquals(1 + Math.max(first, second), inner.height);
    return inner.height;
  }

  private static final class TestStorable implements ShapeTree.Storable {

    private final int id;
    private final TileShape shape;
    private ShapeTree.Leaf[] entries;

    private TestStorable(int id, TileShape shape) {
      this.id = id;
      this.shape = shape;
    }

    @Override
    public int compareTo(Object other) {
      return Integer.compare(id, ((TestStorable) other).id);
    }

    @Override
    public int treeShapeCount(ShapeTree shapeTree) {
      return 1;
    }

    @Override
    public TileShape getTreeShape(ShapeTree shapeTree, int index) {
      return shape;
    }

    @Override
    public void setSearchTreeEntries(ShapeTree.Leaf[] entries, ShapeTree tree) {
      this.entries = entries;
    }
  }
}
