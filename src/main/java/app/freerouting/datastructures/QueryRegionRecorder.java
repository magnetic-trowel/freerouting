package app.freerouting.datastructures;

import app.freerouting.geometry.planar.IntBox;
import app.freerouting.geometry.planar.IntOctagon;
import app.freerouting.geometry.planar.RegularTileShape;

/**
 * Collects, per thread, the region read by the search-tree queries made while a frame is open. A
 * cache can keep a result computed inside a frame for as long as nothing changes in that region.
 * Frames nest: closing a frame adds its region to the enclosing one.
 */
public final class QueryRegionRecorder {

  private static final ThreadLocal<QueryRegionRecorder> CURRENT =
      ThreadLocal.withInitial(QueryRegionRecorder::new);

  private int depth;
  private int[] minX = new int[8];
  private int[] minY = new int[8];
  private int[] maxX = new int[8];
  private int[] maxY = new int[8];
  private long[] layers = new long[8];

  private QueryRegionRecorder() {}

  /** Returns this thread's recorder. */
  public static QueryRegionRecorder forThread() {
    return CURRENT.get();
  }

  /** Records a query of {@code shape} on the layers in {@code layerBits} (-1 for all layers). */
  public static void recordQuery(RegularTileShape shape, long layerBits) {
    QueryRegionRecorder recorder = CURRENT.get();
    if (recorder.depth == 0) {
      return;
    }
    if (shape instanceof IntOctagon octagon) {
      recorder.add(octagon.leftX, octagon.bottomY, octagon.rightX, octagon.topY, layerBits);
    } else {
      IntBox box = shape.boundingBox();
      recorder.add(box.ll.x, box.ll.y, box.ur.x, box.ur.y, layerBits);
    }
  }

  /** Records a read whose result can depend on anything on the board. */
  public static void recordEverything() {
    QueryRegionRecorder recorder = CURRENT.get();
    if (recorder.depth > 0) {
      recorder.add(Region.EVERYTHING);
    }
  }

  /** Opens a frame. */
  public void begin() {
    if (depth == minX.length) {
      int length = 2 * depth;
      minX = java.util.Arrays.copyOf(minX, length);
      minY = java.util.Arrays.copyOf(minY, length);
      maxX = java.util.Arrays.copyOf(maxX, length);
      maxY = java.util.Arrays.copyOf(maxY, length);
      layers = java.util.Arrays.copyOf(layers, length);
    }
    minX[depth] = Integer.MAX_VALUE;
    minY[depth] = Integer.MAX_VALUE;
    maxX[depth] = Integer.MIN_VALUE;
    maxY[depth] = Integer.MIN_VALUE;
    layers[depth] = 0;
    ++depth;
  }

  /** Closes the innermost frame, adds its region to the enclosing frame and returns it. */
  public Region end() {
    int i = --depth;
    if (layers[i] == 0) {
      return Region.EMPTY;
    }
    Region region = new Region(minX[i], minY[i], maxX[i], maxY[i], layers[i]);
    if (depth > 0) {
      add(region.minX, region.minY, region.maxX, region.maxY, region.layerBits);
    }
    return region;
  }

  /** Adds a region read earlier, for a result taken from a cache, to the open frame. */
  public void add(Region region) {
    if (depth > 0 && !region.isEmpty()) {
      add(region.minX, region.minY, region.maxX, region.maxY, region.layerBits);
    }
  }

  private void add(int x0, int y0, int x1, int y1, long layerBits) {
    int i = depth - 1;
    if (x0 < minX[i]) {
      minX[i] = x0;
    }
    if (y0 < minY[i]) {
      minY[i] = y0;
    }
    if (x1 > maxX[i]) {
      maxX[i] = x1;
    }
    if (y1 > maxY[i]) {
      maxY[i] = y1;
    }
    layers[i] |= layerBits;
  }

  /**
   * The bounding box of the queried shapes and the union of their layer bits ({@link
   * ShapeTree#layerBit}); empty when nothing was queried.
   */
  public record Region(int minX, int minY, int maxX, int maxY, long layerBits) {

    /** No query was made. */
    public static final Region EMPTY = new Region(0, 0, -1, -1, 0L);

    /** The whole board on all layers. */
    public static final Region EVERYTHING =
        new Region(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, -1L);

    public boolean isEmpty() {
      return layerBits == 0;
    }
  }
}
