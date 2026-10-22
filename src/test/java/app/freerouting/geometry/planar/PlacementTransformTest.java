package app.freerouting.geometry.planar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlacementTransformTest {
  @Test
  void padRotationAndBothMirrorConventionsComposeBeforeRounding() {
    for (boolean rotateFirst : new boolean[] {false, true}) {
      for (boolean mirrored : new boolean[] {false, true}) {
        var transform =
            new PlacementTransform(new FloatPoint(-2000.49, 3000.49), 90, mirrored, rotateFirst);
        var offset = new FloatPoint(10.49, 20.49);
        var local = new Circle(new IntPoint(100, 200), 30);
        var shape = (Circle) transform.shape(local, offset, 90);
        double x = -200 + offset.x;
        double y = 100 + offset.y;
        if (mirrored && !rotateFirst) {
          x = -x;
        }
        double worldX = -y;
        double worldY = x;
        if (mirrored && rotateFirst) {
          worldX = -worldX;
        }
        assertEquals(new FloatPoint(-2000.49 + worldX, 3000.49 + worldY).round(), shape.center);
        assertEquals(30, shape.radius);
        assertFalse(
            shape.center.equals(transform.point(offset).round()),
            "Off-center pads must not be recentered");
      }
    }
  }

  @Test
  void holesAndRedundantOctagonCornersStayBounded() {
    var transform = new PlacementTransform(new FloatPoint(2000.49, -3000.49), 37, true, false);
    var octagon = new IntBox(-100, -50, 100, 50).boundingOctagon();
    var shape = transform.shape(octagon, FloatPoint.ZERO, 0);
    assertTrue(shape.isBounded());
    assertTrue(shape.contains(transform.point(FloatPoint.ZERO).round()));
    var area =
        new PolylineArea(
            new IntBox(-100, -100, 100, 100), new PolylineShape[] {new IntBox(-20, -20, 20, 20)});
    var placed = transform.area(area);
    assertEquals(1, placed.getHoles().length);
    assertFalse(placed.contains(transform.point(FloatPoint.ZERO).round()));
    assertTrue(placed.contains(transform.point(new FloatPoint(50, 0)).round()));
  }

  @Test
  void emptyAndDegenerateShapesDoNotBecomeUnbounded() {
    var transform = new PlacementTransform(new FloatPoint(100.5, -200.5), 90, true, false);
    assertTrue(transform.shape(IntBox.EMPTY, FloatPoint.ZERO, 0).isEmpty());
    var point = transform.shape(new IntBox(0, 0, 0, 0), FloatPoint.ZERO, 0);
    assertTrue(point.isBounded());
    assertEquals(0, point.dimension());
    var line = transform.shape(new IntBox(0, 0, 100, 0), FloatPoint.ZERO, 0);
    assertTrue(line.isBounded());
    assertEquals(1, line.dimension());
  }

  @Test
  void quarterTurnsPreserveExactHalfCoordinates() {
    var point = new FloatPoint(0.5, -0.5);
    for (int i = 0; i < 4; i++) {
      point = PlacementTransform.rotate(point, 90);
    }
    assertEquals(0.5, point.x);
    assertEquals(-0.5, point.y);
  }

  @Test
  void roundedCornersThatTurnBackStillGiveTheBoundedHull() {
    // A circular courtyard of 72 vertices. Rounding the placed corners of its octagon-clipped
    // version can leave one-unit steps that turn against the outline, here at the two left
    // diagonals. Such a corner sequence is not convex; built directly into a tile it became an
    // unbounded or clipped shape, which overflowed the board bounding box during fanout.
    int radius = 50000;
    List<Point> corners = new ArrayList<>();
    for (int i = 0; i < 72; i++) {
      double angle = Math.toRadians(5 * i + 2.5);
      IntPoint corner =
          (IntPoint) new FloatPoint(radius * Math.cos(angle), radius * Math.sin(angle)).round();
      corners.add(corner);
      if (i == 27 || i == 45) {
        corners.add(new IntPoint(corner.x, corner.y - 1));
      }
    }
    Shape tile = PlacementTransform.tileFromRoundedCorners(corners.toArray(new Point[0]));
    assertTrue(tile.isBounded());
    IntBox box = tile.boundingBox();
    assertTrue(box.ll.x <= -radius + 200 && box.ur.x >= radius - 200, box.toString());
    assertTrue(box.ll.y <= -radius + 200 && box.ur.y >= radius - 200, box.toString());
    for (Point corner : corners) {
      assertTrue(tile.contains(corner), corner.toString());
    }
  }
}
