package app.freerouting.geometry.planar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class RationalPointProjectionTest {

  private static RationalPoint rational(long x, long y, long z) {
    return new RationalPoint(BigInteger.valueOf(x), BigInteger.valueOf(y), BigInteger.valueOf(z));
  }

  private static void assertSamePoint(FloatPoint expected, Point actual) {
    FloatPoint actualFloat = actual.toFloat();
    assertEquals(expected.x, actualFloat.x, 1e-9, "x of " + actual);
    assertEquals(expected.y, actualFloat.y, 1e-9, "y of " + actual);
  }

  @Test
  void projectsHalfIntegerPointOntoHorizontalLine() {
    Line horizontalAxis = new Line(new IntPoint(0, 0), new IntPoint(10, 0));
    // (0.5, 0.5) projects to (0.5, 0).
    assertSamePoint(
        new FloatPoint(0.5, 0.0), rational(1, 1, 2).perpendicularProjection(horizontalAxis));
  }

  @Test
  void projectsOntoOffsetHorizontalLine() {
    Line line = new Line(new IntPoint(0, 5), new IntPoint(10, 5));
    // (100.5, 100.5) projects to (100.5, 5).
    assertSamePoint(
        new FloatPoint(100.5, 5.0), rational(201, 201, 2).perpendicularProjection(line));
  }

  @Test
  void matchesIntPointProjectionForEquivalentPoints() {
    Line[] lines = {
      new Line(new IntPoint(0, 0), new IntPoint(7, 3)),
      new Line(new IntPoint(-40, 25), new IntPoint(13, -9)),
      new Line(new IntPoint(3, 3), new IntPoint(3, 50)),
    };
    int[][] points = {{5, 9}, {-17, 4}, {100, -33}, {0, 0}};
    for (Line line : lines) {
      for (int[] p : points) {
        IntPoint intPoint = new IntPoint(p[0], p[1]);
        for (int z : new int[] {2, 3, 7}) {
          RationalPoint scaled = rational((long) p[0] * z, (long) p[1] * z, z);
          assertSamePoint(
              intPoint.perpendicularProjection(line).toFloat(),
              scaled.perpendicularProjection(line));
        }
      }
    }
  }
}
