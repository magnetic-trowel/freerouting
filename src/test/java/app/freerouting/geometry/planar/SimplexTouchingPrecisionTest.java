package app.freerouting.geometry.planar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Removing redundant lines while intersecting simplices decides on which side of a line the
 * intersection of two other lines lies. That decision was taken from a floating point corner with a
 * fixed tolerance; far from the origin and for long edges the corner's rounding error exceeds the
 * tolerance, and two shapes sharing a corner were found not to intersect although both contain the
 * shared point.
 */
class SimplexTouchingPrecisionTest {

  private static TileShape shape(int... coordinates) {
    Point[] corners = new Point[coordinates.length / 2];
    for (int i = 0; i < corners.length; i++) {
      corners[i] = new IntPoint(coordinates[2 * i], coordinates[2 * i + 1]);
    }
    return TileShape.getInstance(corners);
  }

  @Test
  void shapesSharingOneCornerFarFromTheOriginIntersect() {
    TileShape octagon =
        shape(
            -3904198, -530944, -2385251, -3136119, 530944, -3904198, 3136119, -2385251, 3904198,
            530944, 2385251, 3136119, -530944, 3904198, -3136119, 2385251);
    TileShape triangle = shape(-7588472, -2679060, -3904198, -530944, -6052314, 3153330);
    IntPoint shared = new IntPoint(-3904198, -530944);
    assertTrue(octagon.contains(shared));
    assertTrue(triangle.contains(shared));

    assertTrue(octagon.intersects(triangle));
    assertTrue(triangle.intersects(octagon));
    Simplex intersection = octagon.toSimplex().intersection(triangle.toSimplex());
    assertFalse(intersection.isEmpty());
    assertEquals(0, intersection.dimension());
    assertEquals(0, triangle.toSimplex().intersection(octagon.toSimplex()).dimension());
  }

  @Test
  void separatedShapesStayDisjoint() {
    TileShape octagon =
        shape(
            -3904198, -530944, -2385251, -3136119, 530944, -3904198, 3136119, -2385251, 3904198,
            530944, 2385251, 3136119, -530944, 3904198, -3136119, 2385251);
    // the same triangle moved one unit away from the shared corner
    TileShape triangle = shape(-7588473, -2679060, -3904199, -530944, -6052315, 3153330);
    assertFalse(octagon.intersects(triangle));
    assertFalse(triangle.intersects(octagon));
  }
}
