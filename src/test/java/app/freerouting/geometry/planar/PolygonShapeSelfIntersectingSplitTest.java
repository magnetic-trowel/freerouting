package app.freerouting.geometry.planar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import org.junit.jupiter.api.Test;

/**
 * The division-line split of a polygon only works on a simple border and returned null for a border
 * that crosses itself. BoardOutline.contains then threw a NullPointerException while a board
 * loaded, and an obstacle area with such a border got no search-tree shapes at all.
 */
class PolygonShapeSelfIntersectingSplitTest {

  /** A border that crosses itself; the division-line split fails on it. */
  private static PolygonShape crossingBorder() {
    return new PolygonShape(
        new Point[] {
          new IntPoint(1000, 200),
          new IntPoint(600, 400),
          new IntPoint(500, 700),
          new IntPoint(1000, 1000),
          new IntPoint(200, 1000)
        });
  }

  private static PolygonShape polygon(int... coordinates) {
    Point[] corners = new Point[coordinates.length / 2];
    for (int i = 0; i < corners.length; i++) {
      corners[i] = new IntPoint(coordinates[2 * i], coordinates[2 * i + 1]);
    }
    return new PolygonShape(corners);
  }

  @Test
  void crossingBorderIsSplitAsTheRegionItEncloses() {
    assertCoversItsRegion(crossingBorder());
  }

  /**
   * The division-line split does not fail on these borders, but its pieces do not cover their
   * region: the two pieces of a bowtie overlap and cover twice its area, the pieces of the second
   * border cover a third of it.
   */
  @Test
  void bordersTheDivisionSplitAcceptsAreSplitAsTheirRegion() {
    assertCoversItsRegion(polygon(0, 0, 100000, 100000, 100000, 0, 0, 100000));
    assertCoversItsRegion(
        polygon(
            3500000, 500000, 8000000, 3500000, 7500000, 8000000, 7000000, 9000000, 5500000, 8000000,
            7000000, 8000000, 4000000, 6000000, 7500000, 2000000, 6500000, 8000000));
  }

  /**
   * The region of these borders has loops that touch themselves: in a corner that lies on another
   * border line, or in a crossing point that rounding moves across a border line. Such a loop is
   * split where it touches; split as one loop, the region was left out or covered wrongly.
   */
  @Test
  void regionsWithTouchingLoopsAreCovered() {
    assertCoversItsRegion(
        polygon(
            10000, 5000, 50000, 20000, 90000, 80000, 45000, 75000, 65000, 65000, 40000, 75000,
            80000, 5000, 40000, 85000, 15000, 75000, 40000, 40000));
    assertCoversItsRegion(
        polygon(
            100000, 50000, 650000, 450000, 750000, 600000, 250000, 700000, 800000, 500000, 650000,
            500000, 400000, 150000, 600000, 400000, 300000, 350000, 900000, 700000, 900000, 800000,
            550000, 800000, 250000, 450000));
  }

  @Test
  void simpleBorderIsRecognised() {
    assertTrue(polygon(0, 0, 100, 0, 100, 100, 0, 100).borderIsSimple());
    assertTrue(polygon(0, 0, 100, 0, 50, 10, 100, 100, 0, 100).borderIsSimple());
    // crossing
    assertFalse(polygon(0, 0, 100, 100, 100, 0, 0, 100).borderIsSimple());
    // two squares touching in a corner
    assertFalse(
        polygon(0, 0, 10, 0, 10, 10, 20, 10, 20, 20, 10, 20, 10, 10, 0, 10).borderIsSimple());
    // a corner on another border line
    assertFalse(polygon(0, 0, 20, 0, 20, 10, 10, 0, 0, 10).borderIsSimple());
    // collinear border lines that overlap
    assertFalse(polygon(0, 0, 30, 0, 30, 10, 20, 0, 10, 0, 0, 10).borderIsSimple());
  }

  @Test
  void pointsOnTheBorderAreFound() {
    PolygonShape square = polygon(0, 0, 100, 0, 100, 100, 0, 100);
    assertTrue(square.containsOnBorder(new IntPoint(50, 0)));
    assertTrue(square.containsOnBorder(new IntPoint(100, 100)));
    assertFalse(square.containsOnBorder(new IntPoint(50, 50)));
    assertFalse(square.containsOnBorder(new IntPoint(150, 0)));
    assertFalse(square.containsInside(new IntPoint(0, 50)));
    assertTrue(square.containsInside(new IntPoint(1, 50)));
  }

  private static void assertCoversItsRegion(PolygonShape shape) {
    TileShape[] pieces = shape.splitToConvex();

    assertNotNull(pieces);
    assertTrue(pieces.length > 0);
    java.awt.geom.Area expected = region(shape.corners);
    java.awt.geom.Area covered = new java.awt.geom.Area();
    for (TileShape piece : pieces) {
      Point[] corners = new Point[piece.borderLineCount()];
      for (int i = 0; i < corners.length; i++) {
        corners[i] = piece.corner(i);
      }
      covered.add(region(corners));
    }
    java.awt.geom.Area difference = (java.awt.geom.Area) expected.clone();
    difference.exclusiveOr(covered);
    assertTrue(
        area(difference) <= 1e-3 * area(expected),
        "pieces cover " + area(covered) + " of the enclosed area " + area(expected));
    double pieceAreaSum = 0;
    for (TileShape piece : pieces) {
      pieceAreaSum += piece.area();
    }
    assertEquals(area(expected), pieceAreaSum, 1e-3 * area(expected), "pieces overlap");
  }

  @Test
  void containmentWorksOnACrossingBorder() {
    PolygonShape shape = crossingBorder();

    assertTrue(shape.contains(new IntPoint(800, 950)));
    assertFalse(shape.isOutside(new IntPoint(800, 950)));
    assertTrue(shape.contains(new FloatPoint(800, 950)));
    assertTrue(shape.isOutside(new IntPoint(100, 100)));
    assertFalse(shape.contains(new FloatPoint(100, 100)));
  }

  @Test
  void splitOfACrossingBorderIsReproducible() {
    assertEquals(
        describe(crossingBorder().splitToConvex()), describe(crossingBorder().splitToConvex()));
  }

  private static String describe(TileShape[] pieces) {
    StringBuilder result = new StringBuilder();
    for (TileShape piece : pieces) {
      for (int i = 0; i < piece.borderLineCount(); i++) {
        result.append(piece.corner(i)).append(' ');
      }
      result.append('|');
    }
    return result.toString();
  }

  private static java.awt.geom.Area region(Point[] corners) {
    Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
    for (int i = 0; i < corners.length; i++) {
      FloatPoint corner = corners[i].toFloat();
      if (i == 0) {
        path.moveTo(corner.x, corner.y);
      } else {
        path.lineTo(corner.x, corner.y);
      }
    }
    path.closePath();
    return new java.awt.geom.Area(path);
  }

  private static double area(java.awt.geom.Area region) {
    double sum = 0;
    double[] coords = new double[6];
    double firstX = 0;
    double firstY = 0;
    double lastX = 0;
    double lastY = 0;
    for (PathIterator it = region.getPathIterator(null); !it.isDone(); it.next()) {
      int type = it.currentSegment(coords);
      if (type == PathIterator.SEG_MOVETO) {
        firstX = lastX = coords[0];
        firstY = lastY = coords[1];
      } else if (type == PathIterator.SEG_LINETO) {
        sum += lastX * coords[1] - coords[0] * lastY;
        lastX = coords[0];
        lastY = coords[1];
      } else if (type == PathIterator.SEG_CLOSE) {
        sum += lastX * firstY - firstX * lastY;
        lastX = firstX;
        lastY = firstY;
      }
    }
    return Math.abs(sum) / 2;
  }
}
