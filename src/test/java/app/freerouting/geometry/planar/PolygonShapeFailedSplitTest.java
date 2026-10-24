package app.freerouting.geometry.planar;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * A self-intersecting polygon cannot be split into convex pieces, so {@link
 * PolygonShape#splitToConvex()} returns null. Point and intersection queries on such a polygon must
 * still answer instead of throwing a NullPointerException. Board outlines exported from EDA tools
 * can contain such polygons, for example thin slivers left by approximated arcs whose inner and
 * outer edges cross; the board outline checks run on every pin when a design is loaded.
 */
class PolygonShapeFailedSplitTest {

  /** A loop: the edge from (1000,1000) to (500,-500) crosses the edge from (0,0) to (1000,0). */
  private static PolygonShape selfIntersecting() {
    return new PolygonShape(
        new Point[] {
          new IntPoint(0, 0),
          new IntPoint(1000, 0),
          new IntPoint(1000, 1000),
          new IntPoint(500, -500),
          new IntPoint(0, 1000)
        });
  }

  @Test
  void splitOfSelfIntersectingPolygonFails() {
    assertNull(selfIntersecting().splitToConvex());
  }

  @Test
  void failedSplitIsRememberedAndStaysNull() {
    PolygonShape shape = selfIntersecting();
    assertNull(shape.splitToConvex());
    assertNull(shape.splitToConvex());
  }

  @Test
  void containsFallsBackToWindingRule() {
    PolygonShape shape = selfIntersecting();
    assertTrue(shape.contains(new IntPoint(800, 300)));
    assertTrue(shape.contains(new IntPoint(500, -100)));
    assertFalse(shape.contains(new IntPoint(500, 900)));
    assertFalse(shape.contains(new IntPoint(5000, 5000)));
    assertTrue(shape.isOutside(new IntPoint(5000, 5000)));
    assertFalse(shape.isOutside(new IntPoint(800, 300)));
    assertTrue(shape.contains(new FloatPoint(800, 300)));
    assertFalse(shape.contains(new FloatPoint(5000, 5000)));
  }

  @Test
  void borderPointsAreContained() {
    PolygonShape shape = selfIntersecting();
    assertTrue(shape.contains(new IntPoint(0, 0)));
    assertTrue(shape.contains(new IntPoint(0, 500)));
  }

  @Test
  void containsInsideDoesNotThrow() {
    PolygonShape shape = selfIntersecting();
    assertDoesNotThrow(() -> shape.containsInside(new IntPoint(800, 300)));
  }

  @Test
  void intersectsDoesNotThrow() {
    PolygonShape shape = selfIntersecting();
    assertTrue(shape.intersects(new IntBox(700, 200, 900, 400)));
    assertFalse(shape.intersects(new IntBox(4000, 4000, 5000, 5000)));
    assertTrue(shape.intersects(new IntBox(700, 200, 900, 400).boundingOctagon()));
    assertTrue(shape.intersects(new Circle(new IntPoint(800, 300), 50)));
    assertTrue(shape.intersects((Shape) selfIntersecting()));
  }

  @Test
  void nearestPointApproxFallsBackToBorder() {
    PolygonShape shape = selfIntersecting();
    FloatPoint inside = new FloatPoint(800, 300);
    assertSame(inside, shape.nearestPointApprox(inside));
    FloatPoint nearest = shape.nearestPointApprox(new FloatPoint(-100, 500));
    assertNotNull(nearest);
    assertEquals(0, nearest.x, 1e-9);
    assertEquals(500, nearest.y, 1e-9);
  }
}
