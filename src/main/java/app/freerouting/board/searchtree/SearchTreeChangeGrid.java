package app.freerouting.board.searchtree;

import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.model.items.Item;
import app.freerouting.datastructures.QueryRegionRecorder.Region;
import app.freerouting.datastructures.ShapeTree;
import app.freerouting.datastructures.UndoableObjects;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.geometry.planar.IntOctagon;
import app.freerouting.geometry.planar.RegularTileShape;
import app.freerouting.rules.ClearanceMatrix;
import java.util.Arrays;

/**
 * Where the items of a board changed, on a coarse grid per layer. Every change stamps the cells it
 * covers with a new sequence number: all default-tree entries of an item before and after the
 * default tree changes any of them, and the bounding box (plus the largest clearance) of an item
 * whose other state changes. A result computed from default-tree queries inside a region stays
 * valid while no cell of that region carries a newer stamp. Changes that cannot be located (undo,
 * redo, net or clearance-class reassignments) start a new generation, which invalidates everything.
 */
public final class SearchTreeChangeGrid implements UndoableObjects.ContentListener {

  private static final int CELLS_PER_AXIS = 64;

  /** Slack added to the largest clearance when stamping by bounding box. */
  private static final int MARGIN_SLACK = 2 * ClearanceMatrix.clearance_safety_margin;

  private final BasicBoard board;
  private final int layerCount;
  private final long allLayers;
  private final int originX;
  private final int originY;
  private final int cellWidth;
  private final int cellHeight;
  private final int[] stamps;
  private int sequence;
  private int generation;
  private int margin;
  private ClearanceMatrix marginMatrix;
  private int marginModifications;

  /** Creates the grid of {@code board}; {@link BasicBoard#changeGrid} attaches it. */
  public SearchTreeChangeGrid(BasicBoard board) {
    this.board = board;
    this.layerCount = Math.max(1, board.getLayerCount());
    this.allLayers = layerCount >= 64 ? -1L : (1L << layerCount) - 1;
    IntBox box = board.getBoundingBox();
    this.originX = box.ll.x;
    this.originY = box.ll.y;
    long width = (long) box.ur.x - box.ll.x + 1;
    long height = (long) box.ur.y - box.ll.y + 1;
    this.cellWidth = (int) Math.max(1, (width + CELLS_PER_AXIS - 1) / CELLS_PER_AXIS);
    this.cellHeight = (int) Math.max(1, (height + CELLS_PER_AXIS - 1) / CELLS_PER_AXIS);
    this.stamps = new int[layerCount * CELLS_PER_AXIS * CELLS_PER_AXIS];
  }

  /** The stamp of the latest change. */
  public int sequence() {
    return sequence;
  }

  /** Changes whenever everything is invalidated. */
  public int generation() {
    return generation;
  }

  /** True if no change after {@code since} touched {@code region}. */
  public boolean unchangedSince(Region region, int since) {
    if (since == sequence || region.isEmpty()) {
      return true;
    }
    int x0 = cellX(region.minX());
    int x1 = cellX(region.maxX());
    int y0 = cellY(region.minY());
    int y1 = cellY(region.maxY());
    long bits = region.layerBits() & allLayers;
    if (region.layerBits() == -1L) {
      bits = allLayers;
    }
    while (bits != 0) {
      int layer = Long.numberOfTrailingZeros(bits);
      bits &= bits - 1;
      int layerBase = layer * CELLS_PER_AXIS * CELLS_PER_AXIS;
      for (int y = y0; y <= y1; y++) {
        int rowBase = layerBase + y * CELLS_PER_AXIS;
        for (int x = x0; x <= x1; x++) {
          if (stamps[rowBase + x] > since) {
            return false;
          }
        }
      }
    }
    return true;
  }

  /** Stamps the given default-tree entries of an item. */
  void entriesChanged(ShapeTree.Leaf[] entries) {
    if (entries == null) {
      return;
    }
    int stamp = nextSequence();
    for (ShapeTree.Leaf leaf : entries) {
      if (leaf != null && leaf.object instanceof Item) {
        stamp(leaf.boundingShape, leaf.layerMask, 0, stamp);
      }
    }
  }

  @Override
  public void objectChanged(UndoableObjects.Storable object) {
    if (!(object instanceof Item item)) {
      allChanged();
      return;
    }
    if (!item.isOnTheBoard() || item.board != board) {
      return;
    }
    IntBox box = item.boundingBox();
    if (box == null || box.isEmpty()) {
      allChanged();
      return;
    }
    int expansion = maxClearance();
    int first = item.firstLayer();
    int last = item.lastLayer();
    long layerBits;
    if (first < 0 || last < first || last >= 64) {
      layerBits = -1L;
    } else {
      layerBits = (last == 63 ? -1L : (1L << (last + 1)) - 1) & ~((1L << first) - 1);
    }
    stamp(
        (long) box.ll.x - expansion,
        (long) box.ll.y - expansion,
        (long) box.ur.x + expansion,
        (long) box.ur.y + expansion,
        layerBits,
        nextSequence());
  }

  @Override
  public void allChanged() {
    ++generation;
  }

  private int maxClearance() {
    ClearanceMatrix matrix = board.rules.clearanceMatrix;
    if (matrix != marginMatrix || matrix.getModificationCount() != marginModifications) {
      int result = 0;
      for (int layer = 0; layer < matrix.getLayerCount(); layer++) {
        result = Math.max(result, matrix.maxValue(layer));
      }
      margin = result + MARGIN_SLACK;
      marginMatrix = matrix;
      marginModifications = matrix.getModificationCount();
    }
    return margin;
  }

  private int nextSequence() {
    if (sequence == Integer.MAX_VALUE) {
      Arrays.fill(stamps, 0);
      sequence = 0;
      ++generation;
    }
    return ++sequence;
  }

  private void stamp(RegularTileShape shape, long layerBits, int expansion, int stamp) {
    if (shape instanceof IntOctagon octagon) {
      stamp(
          (long) octagon.leftX - expansion,
          (long) octagon.bottomY - expansion,
          (long) octagon.rightX + expansion,
          (long) octagon.topY + expansion,
          layerBits,
          stamp);
    } else {
      IntBox box = shape.boundingBox();
      stamp(
          (long) box.ll.x - expansion,
          (long) box.ll.y - expansion,
          (long) box.ur.x + expansion,
          (long) box.ur.y + expansion,
          layerBits,
          stamp);
    }
  }

  private void stamp(long minX, long minY, long maxX, long maxY, long layerBits, int stamp) {
    int x0 = cellX(minX);
    int x1 = cellX(maxX);
    int y0 = cellY(minY);
    int y1 = cellY(maxY);
    long bits = layerBits == -1L ? allLayers : layerBits & allLayers;
    while (bits != 0) {
      int layer = Long.numberOfTrailingZeros(bits);
      bits &= bits - 1;
      int layerBase = layer * CELLS_PER_AXIS * CELLS_PER_AXIS;
      for (int y = y0; y <= y1; y++) {
        int rowBase = layerBase + y * CELLS_PER_AXIS;
        Arrays.fill(stamps, rowBase + x0, rowBase + x1 + 1, stamp);
      }
    }
  }

  private int cellX(long x) {
    return clampCell(Math.floorDiv(x - originX, (long) cellWidth));
  }

  private int cellY(long y) {
    return clampCell(Math.floorDiv(y - originY, (long) cellHeight));
  }

  private static int clampCell(long cell) {
    return (int) Math.max(0, Math.min(CELLS_PER_AXIS - 1, cell));
  }
}
