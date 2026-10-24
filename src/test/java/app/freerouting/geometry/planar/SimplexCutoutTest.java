package app.freerouting.geometry.planar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Cutting a convex shape out of another must leave exactly the difference: keepouts with windows
 * and planes with cutouts are split into convex pieces this way, and a lost piece is area that
 * neither routing nor DRC treats as covered.
 */
class SimplexCutoutTest {

  private static Simplex simplex(int... coordinates) {
    Point[] corners = new Point[coordinates.length / 2];
    for (int i = 0; i < corners.length; i++) {
      corners[i] = new IntPoint(coordinates[2 * i], coordinates[2 * i + 1]);
    }
    return TileShape.getInstance(corners).toSimplex();
  }

  private static void assertPartitionsDifference(Simplex outer, Simplex cut) {
    Simplex[] pieces = cut.cutoutFrom(outer);
    Simplex inner = cut.intersection(outer);
    double expected = outer.area() - (inner.dimension() == 2 ? inner.area() : 0);
    double tolerance = 1e-6 * outer.area() + 1e-3;
    double sum = 0;
    for (Simplex piece : pieces) {
      if (piece.dimension() < 2) {
        continue;
      }
      sum += piece.area();
      Simplex overlap = piece.intersection(cut);
      assertTrue(
          overlap.dimension() < 2 || overlap.area() <= tolerance,
          "a piece overlaps the cut shape by " + overlap.area());
      Simplex outside = piece.intersection(outer);
      assertEquals(piece.area(), outside.area(), tolerance, "a piece reaches outside the shape");
    }
    assertEquals(expected, sum, tolerance, "the pieces must cover the difference");
  }

  @Test
  void cutoutKeepsTheWholeDifferenceForAPairTheDivisionGotWrong() {
    // The division along minimal distance lines lost 17% of this difference.
    Simplex outer = simplex(99365, 100600, 99597, 99465, 100727, 100601, 100609, 100884);
    Simplex cut = simplex(99886, 100687, 100387, 99369, 100993, 100603);

    assertPartitionsDifference(outer, cut);
  }

  @Test
  void cutoutPartitionsTheDifferenceForRandomConvexPairs() {
    Random random = new Random(20261023);
    for (int i = 0; i < 2000; i++) {
      Simplex outer = randomConvex(random, 100_000, 100_000, 2000);
      Simplex cut =
          randomConvex(
              random, 100_000 + random.nextInt(1000), 100_000 + random.nextInt(1000), 2000);
      if (outer.dimension() == 2 && cut.dimension() == 2) {
        assertPartitionsDifference(outer, cut);
      }
    }
  }

  @Test
  void sweepCutoutPartitionsTheDifference() {
    Simplex outer = simplex(0, 0, 1000, 0, 1000, 1000, 0, 1000);
    Simplex inner = simplex(300, 200, 700, 400, 500, 800);

    Simplex[] pieces = Simplex.sweepCutout(outer, inner);

    assertTrue(Simplex.partitionsDifference(pieces, outer, inner));
    assertEquals(3, pieces.length, "one piece per border line of the cut shape");
  }

  /** The convex hull of 3 to 8 random points around (centerX, centerY). */
  private static Simplex randomConvex(Random random, int centerX, int centerY, int size) {
    int count = 3 + random.nextInt(6);
    List<long[]> points = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      points.add(
          new long[] {
            centerX + random.nextInt(size) - size / 2, centerY + random.nextInt(size) - size / 2
          });
    }
    points.sort((a, b) -> a[0] != b[0] ? Long.compare(a[0], b[0]) : Long.compare(a[1], b[1]));
    List<long[]> hull = new ArrayList<>();
    for (int pass = 0; pass < 2; pass++) {
      int start = hull.size();
      for (long[] p : points) {
        while (hull.size() >= start + 2
            && cross(hull.get(hull.size() - 2), hull.get(hull.size() - 1), p) <= 0) {
          hull.remove(hull.size() - 1);
        }
        hull.add(p);
      }
      hull.remove(hull.size() - 1);
      java.util.Collections.reverse(points);
    }
    if (hull.size() < 3) {
      return Simplex.EMPTY;
    }
    Point[] corners = new Point[hull.size()];
    for (int i = 0; i < corners.length; i++) {
      corners[i] = new IntPoint((int) hull.get(i)[0], (int) hull.get(i)[1]);
    }
    return TileShape.getInstance(corners).toSimplex();
  }

  private static long cross(long[] o, long[] a, long[] b) {
    return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
  }
}
