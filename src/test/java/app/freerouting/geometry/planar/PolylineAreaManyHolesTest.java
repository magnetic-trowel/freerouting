package app.freerouting.geometry.planar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * A plane with many small windows is split into convex pieces by cutting each window out of the
 * pieces it overlaps. Cutting it out of every piece made the split quadratic in the number of
 * windows: 1,600 windows of 32 corners took 87 s.
 */
class PolylineAreaManyHolesTest {

  private static final int SIZE = 1_000_000;

  @Test
  void splitsPlaneWithManyWindowsQuicklyWithoutLosingArea() {
    int grid = 30;
    double pitch = (double) SIZE / grid;
    PolylineShape[] holes = new PolylineShape[grid * grid];
    double holeArea = 0;
    for (int i = 0; i < holes.length; i++) {
      holes[i] = polygon(pitch * (i % grid + 0.5), pitch * (i / grid + 0.5), 4000, 32);
      holeArea += holes[i].area();
    }
    PolylineShape border =
        new PolygonShape(
            new Point[] {
              new IntPoint(0, 0),
              new IntPoint(SIZE, 0),
              new IntPoint(SIZE, SIZE),
              new IntPoint(0, SIZE)
            });
    PolylineArea plane = new PolylineArea(border, holes);

    TileShape[] pieces =
        assertTimeoutPreemptively(Duration.ofSeconds(20), () -> plane.splitToConvex());

    double pieceArea = 0;
    for (TileShape piece : pieces) {
      pieceArea += piece.area();
    }
    double expected = border.area() - holeArea;
    assertEquals(expected, pieceArea, 1e-6 * expected);
  }

  private static PolylineShape polygon(double centerX, double centerY, double radius, int corners) {
    Point[] points = new Point[corners];
    for (int i = 0; i < corners; i++) {
      double angle = 2 * Math.PI * i / corners;
      points[i] =
          new IntPoint(
              (int) Math.round(centerX + radius * Math.cos(angle)),
              (int) Math.round(centerY + radius * Math.sin(angle)));
    }
    return new PolygonShape(points);
  }
}
