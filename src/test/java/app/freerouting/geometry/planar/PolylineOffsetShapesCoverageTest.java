package app.freerouting.geometry.planar;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * The offset shapes of a trace polyline are the trace's search-tree shapes: routing and the
 * clearance check see the trace's copper only through them, so together they must cover every point
 * within half the trace width of the centre line. Cutting off the "dog ears" of short segments with
 * the border lines of later segments removed copper of the segment itself where the polyline winds
 * back past it or turns the other way in between.
 */
class PolylineOffsetShapesCoverageTest {

  private static Polyline polyline(int... coordinates) {
    Point[] corners = new Point[coordinates.length / 2];
    for (int i = 0; i < corners.length; i++) {
      corners[i] = new IntPoint(coordinates[2 * i], coordinates[2 * i + 1]);
    }
    return new Polyline(corners);
  }

  private static boolean covered(TileShape[] shapes, FloatPoint point) {
    for (TileShape shape : shapes) {
      if (shape != null && shape.contains(point)) {
        return true;
      }
    }
    return false;
  }

  /** Returns a point of the polyline's copper that no offset shape contains, or null. */
  private static FloatPoint uncoveredCopper(Polyline polyline, int halfWidth, Random random) {
    TileShape[] shapes = polyline.offsetShapes(halfWidth);
    FloatPoint[] corners = polyline.cornerApproxArr();
    double margin = halfWidth - 1.5;
    for (int i = 0; i + 1 < corners.length; i++) {
      FloatPoint from = corners[i];
      FloatPoint to = corners[i + 1];
      double length = from.distance(to);
      if (length == 0) {
        continue;
      }
      double ux = (to.x - from.x) / length;
      double uy = (to.y - from.y) / length;
      for (int j = 0; j < 60; j++) {
        double along = random.nextDouble() * length;
        double across = (2 * random.nextDouble() - 1) * margin;
        FloatPoint point =
            new FloatPoint(from.x + ux * along - uy * across, from.y + uy * along + ux * across);
        if (!covered(shapes, point)) {
          return point;
        }
      }
    }
    return null;
  }

  @Test
  void anyAnglePolylineTurningBackKeepsItsCopper() {
    Polyline polyline = polyline(519, 389, 458, 425, 357, 414, 459, 526, 577, 448);
    TileShape[] shapes = polyline.offsetShapes(96);
    // 83 units from the first segment, inside its copper, and cut off by the border of the last
    // segment before the fix
    assertTrue(covered(shapes, new FloatPoint(460.0408, 327.0001)));
  }

  @Test
  void fortyFiveDegreeSpiralKeepsTheCopperOfItsFirstSegment() {
    Polyline polyline = polyline(856, 599, 733, 476, 733, 543, 750, 560, 750, 527);
    TileShape[] shapes = polyline.offsetShapes(119);
    // 52 units from the first segment; the vertical last segment's border x = 869 cut it off
    assertTrue(covered(shapes, new FloatPoint(871.6958, 540.8559)));
  }

  @Test
  void randomPolylinesKeepAllCopper() {
    Random random = new Random(7);
    int[][] steps = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};
    for (int iteration = 0; iteration < 4000; iteration++) {
      boolean fortyFive = iteration % 2 == 0;
      int cornerCount = 3 + random.nextInt(4);
      int[] coordinates = new int[2 * cornerCount];
      int x = random.nextInt(1000);
      int y = random.nextInt(1000);
      for (int i = 0; i < cornerCount; i++) {
        coordinates[2 * i] = x;
        coordinates[2 * i + 1] = y;
        if (fortyFive) {
          int[] step = steps[random.nextInt(steps.length)];
          int length = 1 + random.nextInt(250);
          x += step[0] * length;
          y += step[1] * length;
        } else {
          x += random.nextInt(501) - 250;
          y += random.nextInt(501) - 250;
        }
      }
      Polyline polyline = polyline(coordinates);
      if (polyline.lines.length < 3) {
        continue;
      }
      int halfWidth = 2 + random.nextInt(125);
      FloatPoint uncovered = uncoveredCopper(polyline, halfWidth, random);
      assertTrue(
          uncovered == null,
          "copper at "
              + uncovered
              + " of "
              + Arrays.toString(coordinates)
              + " hw "
              + halfWidth
              + " is not covered");
    }
  }
}
