package app.freerouting.io.specctra.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class PolygonPathBoundingBoxTest {

  /** The right edge used to add half the path width once per vertex instead of once. */
  @Test
  void boundingBoxGrowsByHalfTheWidthOnEverySide() {
    double[] square = {0, 0, 10, 0, 10, 10, 0, 10, 0, 0};
    PolygonPath path = new PolygonPath(Layer.PCB, 2, square);
    assertArrayEquals(new double[] {-1, -1, 11, 11}, path.boundingBox().coor, 1e-12);
  }
}
