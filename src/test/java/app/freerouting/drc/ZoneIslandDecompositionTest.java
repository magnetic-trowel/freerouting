package app.freerouting.drc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Splitting a zone fill into its separate islands, as the zone-island DRC does. */
class ZoneIslandDecompositionTest {

  private static Area rectangle(double x, double y, double width, double height) {
    return new Area(new Rectangle2D.Double(x, y, width, height));
  }

  @Test
  void anIslandInsideAHoleOfAnotherIslandIsSeparate() {
    Area fill = rectangle(0, 0, 100, 100);
    fill.subtract(rectangle(20, 20, 60, 60));
    fill.add(rectangle(40, 40, 20, 20));
    fill.add(rectangle(200, 0, 10, 10));

    List<Area> islands = DesignRulesChecker.decomposeAreaIntoIslands(fill);

    assertEquals(3, islands.size());
    Area frame = islands.stream().filter(a -> a.contains(5, 5)).findFirst().orElseThrow();
    assertFalse(frame.contains(30, 30), "the hole is not part of the surrounding island");
    assertFalse(frame.contains(50, 50), "the island in the hole is not part of the frame");
    assertTrue(islands.stream().anyMatch(a -> a.contains(50, 50) && !a.contains(5, 5)));
    assertTrue(islands.stream().anyMatch(a -> a.contains(205, 5)));
  }

  @Test
  void aFillWithThousandsOfHolesIsOneIslandAndSplitsQuickly() {
    Area fill = rectangle(0, 0, 1000, 1000);
    for (int x = 0; x < 50; x++) {
      for (int y = 0; y < 40; y++) {
        fill.subtract(rectangle(5 + 20 * x, 5 + 25 * y, 8, 8));
      }
    }

    List<Area> islands =
        assertTimeoutPreemptively(
            Duration.ofSeconds(20), () -> DesignRulesChecker.decomposeAreaIntoIslands(fill));

    assertEquals(1, islands.size());
    assertFalse(islands.get(0).contains(9, 9), "a hole is not copper");
    assertTrue(islands.get(0).contains(2, 2));
  }
}
