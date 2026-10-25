package app.freerouting.geometry.planar;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * The convex pieces of a simple polygon cover it up to the rounding of the division points, which
 * moves a piece border by less than a unit. A division point rounded to the nearest integer point
 * could cross the border at a spike narrower than a unit; the piece then ran back over itself, had
 * no concave corner, was kept as convex and lost most of its area (a third of the polygon below).
 */
class PolygonShapeSplitAreaTest {

  @Test
  void piecesOfSpikyPolygonCoverIt() {
    int[] coordinates = {
      1778, 1640, 1655, 2008, 1574, 2050, 1380, 1971, 822, 1462, 724, 1843, 553, 2198, 511, 2267,
      667, 1776, 773, 1451, 535, 2119, 279, 1871, 28, 1907, 152, 1385, 320, 1080, 150, 1018, 180,
      490, 801, 1054, 848, 695, 1192, 586, 1385, 295, 1084, 944, 1091, 988, 1156, 1169
    };
    Point[] corners = new Point[coordinates.length / 2];
    for (int i = 0; i < corners.length; i++) {
      corners[i] = new IntPoint(coordinates[2 * i], coordinates[2 * i + 1]);
    }
    assertCovered(new PolygonShape(corners), corners);
  }

  @Test
  void piecesOfRandomStarPolygonsCoverThem() {
    Random random = new Random(5);
    for (int run = 0; run < 3000; run++) {
      int count = 4 + random.nextInt(30);
      double radius = Math.pow(10, 2 + random.nextInt(4));
      double[] angles = new double[count];
      for (int i = 0; i < count; i++) {
        angles[i] = random.nextDouble() * 2 * Math.PI;
      }
      Arrays.sort(angles);
      Point[] corners = new Point[count];
      for (int i = 0; i < count; i++) {
        double distance = radius * (0.2 + random.nextDouble());
        corners[i] =
            new IntPoint(
                (int) Math.round(distance * Math.cos(angles[i])),
                (int) Math.round(distance * Math.sin(angles[i])));
      }
      PolygonShape polygon = new PolygonShape(corners);
      if (polygon.borderIsSimple()) {
        assertCovered(polygon, corners);
      }
    }
  }

  /** The pieces may miss or add at most a strip of half a unit along the border. */
  private static void assertCovered(PolygonShape polygon, Point[] corners) {
    double perimeter = 0;
    for (int i = 0; i < corners.length; i++) {
      perimeter += corners[i].toFloat().distance(corners[(i + 1) % corners.length].toFloat());
    }
    double pieceArea = 0;
    for (TileShape piece : polygon.splitToConvex()) {
      pieceArea += piece.area();
    }
    double difference = Math.abs(pieceArea - polygon.area());
    assertTrue(
        difference <= 0.5 * perimeter,
        "pieces cover " + pieceArea + " of " + polygon.area() + ", perimeter " + perimeter);
  }
}
