package app.freerouting.geometry.planar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/** Splitting a non-convex polygon must give the same pieces when other threads split, too. */
class PolygonShapeSplitDeterminismTest {

  /** A comb: many concave corners, so the split draws many random start corners. */
  private static PolygonShape comb() {
    List<Point> corners = new ArrayList<>();
    for (int tooth = 0; tooth < 20; ++tooth) {
      corners.add(new IntPoint(tooth * 100, 0));
      corners.add(new IntPoint(tooth * 100 + 50, 0));
      corners.add(new IntPoint(tooth * 100 + 50, 500));
      corners.add(new IntPoint(tooth * 100 + 100, 500));
    }
    corners.add(new IntPoint(2000, 1000));
    corners.add(new IntPoint(0, 1000));
    return new PolygonShape(corners.toArray(new Point[0]));
  }

  private static String split() {
    StringBuilder pieces = new StringBuilder();
    for (TileShape piece : comb().splitToConvex()) {
      for (int i = 0; i < piece.borderLineCount(); ++i) {
        pieces.append(piece.corner(i)).append(' ');
      }
      pieces.append('|');
    }
    return pieces.toString();
  }

  @Test
  void concurrentSplitsMatchSingleThreadedSplit() throws Exception {
    String expected = split();
    ExecutorService pool = Executors.newFixedThreadPool(8);
    try {
      List<Future<String>> results = new ArrayList<>();
      for (int i = 0; i < 400; ++i) {
        results.add(pool.submit(PolygonShapeSplitDeterminismTest::split));
      }
      for (Future<String> result : results) {
        assertEquals(expected, result.get());
      }
    } finally {
      pool.shutdownNow();
    }
  }
}
