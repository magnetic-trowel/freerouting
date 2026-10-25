package app.freerouting.datastructures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Limits;
import app.freerouting.geometry.planar.Point;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * The triangulation of the incomplete count must take in every corner of the board coordinate range
 * |x|, |y| <= CRIT_INT. Its bounding triangle used to reach only x + y <= CRIT_INT, so items far
 * from the origin were left out (their nets counted as connected) or not found in the search graph
 * (NullPointerException in split, which ended the routing job).
 */
class PlanarDelaunayTriangulationTest {

  private static final int C = Limits.CRIT_INT;

  @Test
  void connectsPointsAnywhereInTheCoordinateRange() {
    int[][] origins = {
      {0, 0}, {C * 6 / 10, C * 6 / 10}, {-C * 7 / 10, 0}, {C * 3 / 10, -C * 9 / 10}
    };
    for (int[] origin : origins) {
      List<StorablePoint> points = new ArrayList<>();
      for (int i = 0; i < 50; i++) {
        points.add(new StorablePoint(origin[0] + (i % 10) * 1000, origin[1] + (i / 10) * 700));
      }
      assertEquals(1, componentCount(points), "origin " + origin[0] + ", " + origin[1]);
    }
  }

  @Test
  void randomPointsOverWholeRangeGiveConnectedPlanarTriangulation() {
    Random random = new Random(7);
    for (int run = 0; run < 300; run++) {
      List<StorablePoint> points = new ArrayList<>();
      int count = 3 + random.nextInt(40);
      for (int i = 0; i < count; i++) {
        points.add(
            new StorablePoint(
                (int) ((random.nextDouble() * 2 - 1) * C),
                (int) ((random.nextDouble() * 2 - 1) * C)));
      }
      PlanarDelaunayTriangulation triangulation =
          new PlanarDelaunayTriangulation(toStorables(points));
      List<PlanarDelaunayTriangulation.ResultEdge> edges =
          new ArrayList<>(triangulation.getEdgeLines());
      assertEquals(1, componentCount(points), "run " + run);
      for (int i = 0; i < edges.size(); i++) {
        for (int j = i + 1; j < edges.size(); j++) {
          assertFalse(crossProperly(edges.get(i), edges.get(j)), "run " + run);
        }
      }
    }
  }

  private static List<PlanarDelaunayTriangulation.Storable> toStorables(
      List<StorablePoint> points) {
    return new ArrayList<>(points);
  }

  private static int componentCount(List<StorablePoint> points) {
    PlanarDelaunayTriangulation triangulation =
        new PlanarDelaunayTriangulation(toStorables(points));
    Map<Object, Integer> index = new IdentityHashMap<>();
    for (int i = 0; i < points.size(); i++) {
      index.put(points.get(i), i);
    }
    int[] parent = new int[points.size()];
    for (int i = 0; i < parent.length; i++) {
      parent[i] = i;
    }
    for (PlanarDelaunayTriangulation.ResultEdge edge : triangulation.getEdgeLines()) {
      parent[root(parent, index.get(edge.startObject))] = root(parent, index.get(edge.endObject));
    }
    int components = 0;
    for (int i = 0; i < parent.length; i++) {
      if (root(parent, i) == i) {
        ++components;
      }
    }
    return components;
  }

  private static int root(int[] parent, int i) {
    while (parent[i] != i) {
      i = parent[i];
    }
    return i;
  }

  private static boolean crossProperly(
      PlanarDelaunayTriangulation.ResultEdge first, PlanarDelaunayTriangulation.ResultEdge second) {
    IntPoint a = (IntPoint) first.startPoint;
    IntPoint b = (IntPoint) first.endPoint;
    IntPoint c = (IntPoint) second.startPoint;
    IntPoint d = (IntPoint) second.endPoint;
    return Long.signum(cross(a, b, c)) * Long.signum(cross(a, b, d)) < 0
        && Long.signum(cross(c, d, a)) * Long.signum(cross(c, d, b)) < 0;
  }

  private static long cross(IntPoint a, IntPoint b, IntPoint p) {
    return ((long) b.x - a.x) * ((long) p.y - a.y) - ((long) b.y - a.y) * ((long) p.x - a.x);
  }

  private static final class StorablePoint implements PlanarDelaunayTriangulation.Storable {
    private final Point point;

    StorablePoint(int x, int y) {
      this.point = new IntPoint(x, y);
    }

    @Override
    public Point[] getTriangulationCorners() {
      return new Point[] {point};
    }
  }
}
