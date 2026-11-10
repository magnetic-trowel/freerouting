package app.freerouting.drc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** IslandIndex must offer every rectangle that contains a point, in ascending order. */
class IslandIndexTest {

  @Test
  void firstContainingRectangleIsTheSameAsAFullScan() {
    Random random = new Random(2611);
    for (int round = 0; round < 50; round++) {
      List<Rectangle2D> bounds = new ArrayList<>();
      int n = random.nextInt(300);
      for (int i = 0; i < n; i++) {
        bounds.add(
            new Rectangle2D.Double(
                random.nextInt(80), random.nextInt(80), random.nextInt(6), random.nextInt(6)));
      }
      IslandIndex index = new IslandIndex(bounds);
      for (int k = 0; k < 500; k++) {
        // points on the grid of corners too: closed bounds are what counts
        double x = random.nextBoolean() ? random.nextInt(90) - 5 : random.nextDouble() * 90 - 5;
        double y = random.nextBoolean() ? random.nextInt(90) - 5 : random.nextDouble() * 90 - 5;
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < n; i++) {
          if (bounds.get(i).contains(x, y)) {
            all.add(i);
          }
        }
        List<Integer> viaIndex = new ArrayList<>();
        for (int i : index.candidates(x, y)) {
          if (bounds.get(i).contains(x, y)) {
            viaIndex.add(i);
          }
        }
        assertEquals(all, viaIndex, "round " + round + " point " + x + "," + y);
      }
    }
  }
}
