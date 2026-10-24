package app.freerouting.geometry.planar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Obstacle areas are cut into tree-shape sections over their whole extent. A keepout far larger
 * than the board (Issue208) became hundreds of thousands of sections; the sections that cannot meet
 * the board are now left out, and the others stay exactly as before.
 */
class TileShapeSectionKeepRegionTest {

  @Test
  void keptSectionsAreTheUnclippedSectionsThatMeetTheKeepRegion() {
    TileShape obstacle = new IntBox(-100_000, -100_000, 100_000, 100_000).toIntOctagon();
    IntBox keepRegion = new IntBox(-5_000, -5_000, 15_000, 5_000);

    TileShape[] all = obstacle.divideIntoSections(5_000);
    TileShape[] kept = obstacle.divideIntoSections(5_000, keepRegion);

    List<String> expected = new ArrayList<>();
    for (TileShape section : all) {
      if (section.boundingBox().intersects(keepRegion)) {
        expected.add(section.toString());
      }
    }
    List<String> actual = new ArrayList<>();
    for (TileShape section : kept) {
      actual.add(section.toString());
    }
    assertEquals(expected, actual);
    assertTrue(kept.length < all.length / 10);
  }

  @Test
  void noKeepRegionKeepsEverySection() {
    TileShape obstacle = new IntBox(0, 0, 40_000, 40_000).toIntOctagon();

    assertEquals(
        obstacle.divideIntoSections(5_000).length, obstacle.divideIntoSections(5_000, null).length);
  }
}
