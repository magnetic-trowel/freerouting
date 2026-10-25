package app.freerouting.datastructures;

import app.freerouting.geometry.planar.IntOctagon;
import app.freerouting.geometry.planar.RegularTileShape;
import app.freerouting.geometry.planar.ShapeBoundingDirections;
import app.freerouting.logger.FRLogger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * Binary search tree for shapes in the plane. The shapes are stored in the leaves of the tree. A
 * new shape is paired with a leaf found by going down from the root to the child whose bounding
 * shape grows least. After every insertion and removal the nodes above the change are rebalanced by
 * rotations, as in an AVL tree, so the depth is logarithmic in the number of shapes for any input,
 * also for many equal or nested shapes.
 *
 * <p>The structure changes never reorder leaves: an inserted leaf directly follows the leaf it is
 * paired with, a removal leaves the others in place, and the rotations keep the leaf order.
 *
 * <p>The use of ShapeDirections to calculate the bounding shape is for historical reasons (coming
 * from a Kd-Tree). Instead, any algorithm to calculate a bounding shape of two input shapes can be
 * used.
 */
public class MinAreaTree extends ShapeTree {

  /** Reusable traversal stack for overlap queries, isolated per caller thread. */
  protected final ThreadLocal<ArrayStack<TreeNode>> nodeStack =
      ThreadLocal.withInitial(() -> new ArrayStack<>(10000));

  /** Constructor with a fixed set of directions defining the keys and the surrounding shapes. */
  public MinAreaTree(ShapeBoundingDirections directions) {
    super(directions);
  }

  /** Calculates the objects in this tree, which overlap with shape, in {@link Leaf} order. */
  public List<Leaf> overlaps(RegularTileShape shape) {
    Lock lock = readLock();
    lock.lock();
    try {
      return overlapsUnlocked(shape);
    } finally {
      lock.unlock();
    }
  }

  /** Calculates overlaps into {@code result} in {@link Leaf} order. */
  public void overlaps(RegularTileShape shape, List<Leaf> result) {
    Lock lock = readLock();
    lock.lock();
    try {
      overlapsUnlocked(shape, result);
    } finally {
      lock.unlock();
    }
  }

  /** Calculates overlaps while the read lock is already held by the caller. */
  protected final List<Leaf> overlapsUnlocked(RegularTileShape shape) {
    List<Leaf> foundOverlaps = new ArrayList<>();
    overlapsUnlocked(shape, foundOverlaps);
    return foundOverlaps;
  }

  /** Appends overlapping leaves into {@code result} while read lock is held. */
  protected final void overlapsUnlocked(RegularTileShape shape, List<Leaf> result) {
    recordQuery(shape, -1L);
    if (this.root == null) {
      return;
    }
    int first = result.size();
    ArrayStack<TreeNode> stack = nodeStack.get();
    stack.reset();
    stack.push(this.root);
    TreeNode currentNode;
    while ((currentNode = stack.pop()) != null) {
      onNodeVisited();
      if (currentNode.boundingShape.intersects(shape)) {
        if (currentNode instanceof Leaf leaf) {
          result.add(leaf);
        } else {
          stack.push(((InnerNode) currentNode).firstChild);
          stack.push(((InnerNode) currentNode).secondChild);
        }
      }
    }
    if (first == 0) {
      Collections.sort(result);
    } else {
      result.subList(first, result.size()).sort(null);
    }
  }

  /**
   * Appends the overlapping leaves in traversal order, without sorting, while the read lock is
   * held. Subtrees whose layer mask has no bit in common with {@code layerBits} are skipped; pass
   * -1 to visit all layers. Skipping a subtree keeps the relative order of the remaining leaves.
   */
  protected final void overlapsUnsortedUnlocked(
      RegularTileShape shape, long layerBits, List<Leaf> result) {
    recordQuery(shape, layerBits);
    if (this.root == null) {
      return;
    }
    ArrayStack<TreeNode> stack = nodeStack.get();
    stack.reset();
    stack.push(this.root);
    TreeNode currentNode;
    while ((currentNode = stack.pop()) != null) {
      onNodeVisited();
      if ((currentNode.layerMask & layerBits) != 0 && currentNode.boundingShape.intersects(shape)) {
        if (currentNode instanceof Leaf leaf) {
          result.add(leaf);
        } else {
          stack.push(((InnerNode) currentNode).firstChild);
          stack.push(((InnerNode) currentNode).secondChild);
        }
      }
    }
  }

  /** Extension hook for testing and instrumentation during tree traversal. */
  protected void onNodeVisited() {}

  /** Reports a query to this thread's {@link QueryRegionRecorder}. */
  protected void recordQuery(RegularTileShape shape, long layerBits) {
    QueryRegionRecorder.recordQuery(shape, layerBits);
  }

  @Override
  void insert(Leaf leaf) {
    Lock lock = writeLock();
    lock.lock();
    try {
      insertUnlocked(leaf);
    } finally {
      lock.unlock();
    }
  }

  /** Inserts a leaf while the write lock is already held. */
  private void insertUnlocked(Leaf leaf) {
    ++this.leafCount;
    if (root == null) {
      root = leaf;
      return;
    }
    // The new leaf and its sibling get a new parent, which takes the sibling's place. The new leaf
    // is the second child, so it follows the sibling's leaves and the order of the others stays.
    Leaf sibling = findSibling(leaf.boundingShape);
    InnerNode oldParent = sibling.parent;
    InnerNode newNode = new InnerNode(null, oldParent);
    if (oldParent == null) {
      root = newNode;
    } else if (oldParent.firstChild == sibling) {
      oldParent.firstChild = newNode;
    } else {
      oldParent.secondChild = newNode;
    }
    newNode.firstChild = sibling;
    newNode.secondChild = leaf;
    sibling.parent = newNode;
    leaf.parent = newNode;
    refitUpwards(newNode);
  }

  /**
   * The leaf the new leaf with shape {@code added} is paired with: going down from the root, take
   * the child whose bounding shape grows least, the first child on a tie.
   */
  private Leaf findSibling(RegularTileShape added) {
    TreeNode node = root;
    while (node instanceof InnerNode inner) {
      double firstIncrease = areaIncrease(added, inner.firstChild.boundingShape);
      double secondIncrease = areaIncrease(added, inner.secondChild.boundingShape);
      node = firstIncrease <= secondIncrease ? inner.firstChild : inner.secondChild;
    }
    return (Leaf) node;
  }

  /** Area growth of childShape when added is merged into it; octagons skip the union object. */
  private static double areaIncrease(RegularTileShape added, RegularTileShape childShape) {
    return unionArea(added, childShape) - childShape.area();
  }

  /** Area of the union of two shapes; octagons skip the union object. */
  private static double unionArea(RegularTileShape first, RegularTileShape second) {
    if (first instanceof IntOctagon firstOctagon && second instanceof IntOctagon secondOctagon) {
      return IntOctagon.unionArea(firstOctagon, secondOctagon);
    }
    return first.union(second).area();
  }

  /** Removes an entry from this tree. */
  @Override
  public void removeLeaf(Leaf leaf) {
    Lock lock = writeLock();
    lock.lock();
    try {
      removeLeafUnlocked(leaf);
    } finally {
      lock.unlock();
    }
  }

  /** Removes a leaf while the write lock is already held. */
  private void removeLeafUnlocked(Leaf leaf) {
    if (leaf == null) {
      return;
    }
    if (leaf.parent == null && root != leaf) {
      return;
    }
    // remove the leaf node
    InnerNode parent = leaf.parent;
    // Keep payload fields valid for callers that retained this Leaf from an earlier query; only
    // structural ownership is cleared by detaching the leaf.
    leaf.parent = null;
    --this.leafCount;
    if (parent == null) {
      // tree gets empty
      root = null;
      return;
    }
    // find the other leaf of the parent
    TreeNode otherLeaf;
    if (parent.secondChild == leaf) {
      otherLeaf = parent.firstChild;
    } else if (parent.firstChild == leaf) {
      otherLeaf = parent.secondChild;
    } else {
      FRLogger.warn("MinAreaTree.remove_leaf: parent inconsistent");
      otherLeaf = null;
    }
    // link the other leaf to the grandParent and remove the parent node
    InnerNode grandParent = parent.parent;
    otherLeaf.parent = grandParent;
    if (grandParent == null) {
      // only one leaf left in the tree
      root = otherLeaf;
    } else {
      if (grandParent.secondChild == parent) {
        grandParent.secondChild = otherLeaf;
      } else if (grandParent.firstChild == parent) {
        grandParent.firstChild = otherLeaf;
      } else {
        FRLogger.warn("MinAreaTree.remove_leaf: grandParent inconsistent");
      }
    }
    parent.parent = null;
    parent.firstChild = null;
    parent.secondChild = null;
    parent.boundingShape = null;
    refitUpwards(grandParent);
  }

  /**
   * Recomputes bounds, layer masks and heights from {@code node} up to the root, and keeps every
   * node height-balanced (children differ in height by at most one, as in an AVL tree), so the
   * depth stays logarithmic for any input, also for many equal or nested shapes.
   */
  private void refitUpwards(InnerNode node) {
    for (InnerNode current = node; current != null; current = current.parent) {
      int balance = height(current.firstChild) - height(current.secondChild);
      if (balance > 1) {
        InnerNode first = (InnerNode) current.firstChild;
        if (height(first.secondChild) > height(first.firstChild)) {
          rotateLeft(first);
        }
        rotateRight(current);
      } else if (balance < -1) {
        InnerNode second = (InnerNode) current.secondChild;
        if (height(second.firstChild) > height(second.secondChild)) {
          rotateRight(second);
        }
        rotateLeft(current);
      } else {
        refit(current);
      }
    }
  }

  /**
   * Turns {@code node = (a, (b, c))} into {@code node = ((a, b), c)}. Like every change of the tree
   * structure, a rotation keeps the order of the leaves, which is the order in which traversals
   * meet them, so results that depend on that order only change where leaves come and go.
   */
  private static void rotateLeft(InnerNode node) {
    InnerNode pivot = (InnerNode) node.secondChild;
    TreeNode a = node.firstChild;
    final TreeNode c = pivot.secondChild;
    pivot.secondChild = pivot.firstChild;
    pivot.firstChild = a;
    a.parent = pivot;
    node.firstChild = pivot;
    node.secondChild = c;
    c.parent = node;
    refit(pivot);
    refit(node);
  }

  /** Turns {@code node = ((a, b), c)} into {@code node = (a, (b, c))}, keeping the leaf order. */
  private static void rotateRight(InnerNode node) {
    InnerNode pivot = (InnerNode) node.firstChild;
    final TreeNode a = pivot.firstChild;
    TreeNode c = node.secondChild;
    pivot.firstChild = pivot.secondChild;
    pivot.secondChild = c;
    c.parent = pivot;
    node.secondChild = pivot;
    node.firstChild = a;
    a.parent = node;
    refit(pivot);
    refit(node);
  }

  /** Recomputes the bounds, layer mask and height of node from its children. */
  private static void refit(InnerNode node) {
    TreeNode first = node.firstChild;
    TreeNode second = node.secondChild;
    node.boundingShape = first.boundingShape.union(second.boundingShape);
    node.layerMask = first.layerMask | second.layerMask;
    node.height = 1 + Math.max(height(first), height(second));
  }

  private static int height(TreeNode node) {
    return node instanceof InnerNode inner ? inner.height : 0;
  }
}
