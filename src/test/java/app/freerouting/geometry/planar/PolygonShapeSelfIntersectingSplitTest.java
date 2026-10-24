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

  @Test
  void crossingBorderIsSplitAsTheRegionItEncloses() {
    PolygonShape shape = crossingBorder();

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
