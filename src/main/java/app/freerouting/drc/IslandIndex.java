package app.freerouting.drc;

import java.util.List;

/**
 * Finds the rectangles that may contain a point: a uniform grid over their joint bounds, each cell
 * holding, in ascending order, the rectangles whose closed bounds meet it. A point's cell holds
 * every rectangle that contains it, so a scan of the candidates in order finds what a scan of all
 * rectangles finds. Fill island lookups (ConductionArea.setFillIslandAt) scanned every island of a
 * fill for every pad, via and trace end; on fills with thousands of islands that was most of the
 * time of the pour cut-off repair after routing.
 *
 * <p>Each rectangle is given as {@code {minX, minY, width, height}}, the form of {@link
 * app.freerouting.geometry.planar.PlanarArea#getBoundingBox()}.
 */
public final class IslandIndex {

  private static final int[] NONE = new int[0];

  private final double minX;
  private final double minY;
  private final double maxX;
  private final double maxY;
  private final double cellWidth;
  private final double cellHeight;
  private final int cells;
  private final int[][] members;

  public IslandIndex(List<double[]> bounds) {
    int count = bounds.size();
    double x0 = Double.POSITIVE_INFINITY;
    double y0 = Double.POSITIVE_INFINITY;
    double x1 = Double.NEGATIVE_INFINITY;
    double y1 = Double.NEGATIVE_INFINITY;
    for (double[] b : bounds) {
      x0 = Math.min(x0, b[0]);
      y0 = Math.min(y0, b[1]);
      x1 = Math.max(x1, b[0] + b[2]);
      y1 = Math.max(y1, b[1] + b[3]);
    }
    this.minX = x0;
    this.minY = y0;
    this.maxX = x1;
    this.maxY = y1;
    this.cells = count == 0 ? 1 : (int) Math.max(1, Math.min(256, Math.ceil(Math.sqrt(count))));
    this.cellWidth = x1 > x0 ? (x1 - x0) / cells : 1.0;
    this.cellHeight = y1 > y0 ? (y1 - y0) / cells : 1.0;
    int[] size = new int[cells * cells];
    for (double[] b : bounds) {
      for (int y = cellY(b[1]); y <= cellY(b[1] + b[3]); y++) {
        for (int x = cellX(b[0]); x <= cellX(b[0] + b[2]); x++) {
          size[y * cells + x]++;
        }
      }
    }
    this.members = new int[cells * cells][];
    for (int c = 0; c < members.length; c++) {
      members[c] = new int[size[c]];
      size[c] = 0;
    }
    for (int i = 0; i < count; i++) {
      double[] b = bounds.get(i);
      for (int y = cellY(b[1]); y <= cellY(b[1] + b[3]); y++) {
        for (int x = cellX(b[0]); x <= cellX(b[0] + b[2]); x++) {
          int c = y * cells + x;
          members[c][size[c]++] = i;
        }
      }
    }
  }

  /** The rectangles whose cell holds the point, in ascending order; none outside all of them. */
  public int[] candidates(double x, double y) {
    if (members.length == 0 || !(x >= minX && x <= maxX && y >= minY && y <= maxY)) {
      return NONE;
    }
    return members[cellY(y) * cells + cellX(x)];
  }

  /** Whether the closed rectangle {@code {minX, minY, width, height}} contains the point. */
  public static boolean boundsContain(double[] bounds, double x, double y) {
    return x >= bounds[0]
        && x <= bounds[0] + bounds[2]
        && y >= bounds[1]
        && y <= bounds[1] + bounds[3];
  }

  private int cellX(double x) {
    return Math.max(0, Math.min(cells - 1, (int) Math.floor((x - minX) / cellWidth)));
  }

  private int cellY(double y) {
    return Math.max(0, Math.min(cells - 1, (int) Math.floor((y - minY) / cellHeight)));
  }
}
