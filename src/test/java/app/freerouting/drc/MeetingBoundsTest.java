package app.freerouting.drc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * The grid in DesignRulesChecker.meetingBounds must find exactly the pairs a comparison of every
 * pair finds, including rectangles that only share an edge or a corner, so island decomposition
 * gives the same result as before.
 */
class MeetingBoundsTest {

  private static int[][] allPairs(Rectangle2D[] b) {
    int[][] result = new int[b.length][];
    for (int i = 0; i < b.length; i++) {
      List<Integer> found = new ArrayList<>();
      for (int j = 0; j < b.length; j++) {
        if (j != i
            && b[i].getMinX() <= b[j].getMaxX()
            && b[j].getMinX() <= b[i].getMaxX()
            && b[i].getMinY() <= b[j].getMaxY()
            && b[j].getMinY() <= b[i].getMaxY()) {
          found.add(j);
        }
      }
      result[i] = found.stream().mapToInt(Integer::intValue).toArray();
    }
    return result;
  }

  @Test
  void sameAsComparingEveryPair() {
    Random random = new Random(2611);
    for (int round = 0; round < 50; round++) {
      int n = 1 + random.nextInt(400);
      Rectangle2D[] b = new Rectangle2D[n];
      for (int i = 0; i < n; i++) {
        // integer coordinates on a small range: many rectangles share edges and corners
        int x = random.nextInt(60);
        int y = random.nextInt(60);
        int w = random.nextInt(4) == 0 ? random.nextInt(60) : random.nextInt(5);
        int h = random.nextInt(4) == 0 ? random.nextInt(60) : random.nextInt(5);
        b[i] = new Rectangle2D.Double(x, y, w, h);
      }
      int[][] expected = allPairs(b);
      int[][] actual = DesignRulesChecker.meetingBounds(b);
      assertEquals(expected.length, actual.length);
      for (int i = 0; i < n; i++) {
        assertArrayEquals(expected[i], actual[i], "round " + round + ", rectangle " + i);
      }
    }
  }

  @Test
  void degenerateInputs() {
    assertEquals(0, DesignRulesChecker.meetingBounds(new Rectangle2D[0]).length);
    Rectangle2D[] same = {
      new Rectangle2D.Double(5, 5, 0, 0), new Rectangle2D.Double(5, 5, 0, 0)
    };
    assertArrayEquals(new int[] {1}, DesignRulesChecker.meetingBounds(same)[0]);
    assertArrayEquals(new int[] {0}, DesignRulesChecker.meetingBounds(same)[1]);
  }
}
