package app.freerouting.geometry.planar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * A clockwise polygon whose first vertex lies on a diagonal edge used to get winding number 0: the
 * cosine of the two parallel edge vectors rounded above 1 and acos returned NaN. The polygon then
 * kept its clockwise corner order and could not be split into convex pieces, so keepouts and planes
 * with such outlines vanished from the search trees and from DRC.
 */
class PolygonWindingCollinearTest {

  private static IntPoint[] points(int... coordinates) {
    IntPoint[] result = new IntPoint[coordinates.length / 2];
    for (int i = 0; i < result.length; i++) {
      result[i] = new IntPoint(coordinates[2 * i], coordinates[2 * i + 1]);
    }
    return result;
  }

  private static final IntPoint[] CLOCKWISE_WITH_COLLINEAR_FIRST =
      points(80401, 47208, 89916, 30616, 48863, 24254, 16393, 62347, 45596, 77618, 70886, 63800);

  private static final IntPoint[] CLOCKWISE =
      points(89916, 30616, 48863, 24254, 16393, 62347, 45596, 77618, 70886, 63800);

  @Test
  void collinearVertexDoesNotChangeWindingNumber() {
    int expected = new Polygon(CLOCKWISE).windingNumberAfterClosing();
    assertEquals(-1, expected);
    assertEquals(expected, new Polygon(CLOCKWISE_WITH_COLLINEAR_FIRST).windingNumberAfterClosing());
  }

  @Test
  void clockwisePolygonWithCollinearVertexSplitsIntoConvexPieces() {
    TileShape[] pieces = new PolygonShape(CLOCKWISE_WITH_COLLINEAR_FIRST).splitToConvex();
    assertNotNull(pieces);
    assertTrue(pieces.length > 0);
  }

  @Test
  void angleOfVectorWithItselfIsZero() {
    Random random = new Random(1);
    for (int i = 0; i < 10_000; i++) {
      IntVector vector =
          new IntVector(1 + random.nextInt(100_000), random.nextInt(200_001) - 100_000);
      double angle = vector.angleApprox(vector);
      assertFalse(Double.isNaN(angle), "angle of a vector with itself is NaN");
      assertEquals(0.0, angle, 1e-7, "angle of a vector with itself");
      double reverse = Math.abs(vector.angleApprox(vector.negate()));
      assertFalse(Double.isNaN(reverse), "angle with the negated vector is NaN");
      assertEquals(Math.PI, reverse, 1e-7, "angle with the negated vector");
    }
  }
}
