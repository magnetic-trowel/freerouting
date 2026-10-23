package app.freerouting.board.actions;

import app.freerouting.datastructures.ShapeTree;
import app.freerouting.geometry.planar.TileShape;

/**
 * Stores the search tree entries and the precalculated tree shapes of an item for each search tree.
 * Array-backed: same lookup semantics (identity match on the tree, first match wins, insertion
 * order kept) as the former LinkedList implementation, without iterator allocation or per-node
 * pointer chasing.
 */
public class ItemSearchTreesInfo {

  private ShapeTree[] trees = new ShapeTree[2];
  private ShapeTree.Leaf[][] entries = new ShapeTree.Leaf[2][];
  private TileShape[][] shapes = new TileShape[2][];
  private int count;

  /** Creates a new instance of ItemSearchTreeEntries. */
  public ItemSearchTreesInfo() {}

  private int indexOf(ShapeTree tree) {
    ShapeTree[] t = this.trees;
    for (int i = 0; i < count; i++) {
      if (t[i] == tree) {
        return i;
      }
    }
    return -1;
  }

  private int add(ShapeTree tree) {
    if (count == trees.length) {
      int n = count * 2;
      trees = java.util.Arrays.copyOf(trees, n);
      entries = java.util.Arrays.copyOf(entries, n);
      shapes = java.util.Arrays.copyOf(shapes, n);
    }
    trees[count] = tree;
    entries[count] = null;
    shapes[count] = null;
    return count++;
  }

  /**
   * Returns the tree entries for the tree with identification number treeNo, or null, if for this
   * tree no entries of this item are inserted.
   */
  public ShapeTree.Leaf[] getTreeEntries(ShapeTree tree) {
    int i = indexOf(tree);
    return i < 0 ? null : entries[i];
  }

  /** Sets the item tree entries for the tree with identification number treeNo. */
  public void setTreeEntries(ShapeTree.Leaf[] treeEntries, ShapeTree tree) {
    int i = indexOf(tree);
    if (i < 0) {
      i = add(tree);
    }
    entries[i] = treeEntries;
  }

  /**
   * Returns the precalculated tiles shapes for the tree with identification number treeNo, or null,
   * if the tile shapes of this tree are not yet precalculated.
   */
  public TileShape[] getPrecalculatedTreeShapes(ShapeTree tree) {
    int i = indexOf(tree);
    return i < 0 ? null : shapes[i];
  }

  /** Sets the precalculated tree shapes for the tree with identification number treeNo. */
  public void setPrecalculatedTreeShapes(TileShape[] tileShapes, ShapeTree tree) {
    int i = indexOf(tree);
    if (i < 0) {
      i = add(tree);
    }
    shapes[i] = tileShapes;
  }

  /** Clears the stored information about the precalculated tree shapes for all search trees. */
  public void clearPrecalculatedTreeShapes() {
    for (int i = 0; i < count; i++) {
      shapes[i] = null;
    }
  }
}
