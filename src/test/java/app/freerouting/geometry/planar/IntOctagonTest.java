package app.freerouting.geometry.planar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class IntOctagonTest {

  @Test
  void normalizeReturnsSameInstanceWhenBoundsAreAlreadyTight() {
    IntOctagon octagon = new IntOctagon(0, 0, 10, 10, -10, 10, 0, 20);

    assertSame(octagon, octagon.normalize());
  }

  @Test
  void normalizeStillAllocatesWhenBoundsNeedTightening() {
    IntOctagon octagon = new IntOctagon(0, 0, 10, 20, -10, 10, 0, 20);

    IntOctagon normalized = octagon.normalize();

    assertNotSame(octagon, normalized);
    assertEquals(15, normalized.topY);
  }

  /**
   * When the vertical upper right divide line of a box minus an octagon was switched to a
   * horizontal one, the upper box was widened but the upper right octagon kept its full height, so
   * the two pieces overlapped.
   */
  @Test
  void piecesOfBoxMinusOctagonDoNotOverlap() {
    IntBox box = new IntBox(245, 1110, 1189, 2488);
    IntOctagon cut = new IntOctagon(96, 1937, 860, 2051, -1946, -1132, 2075, 2882);

    TileShape[] pieces = box.cutout(cut);

    double pieceArea = 0;
    for (TileShape piece : pieces) {
      if (piece.dimension() == 2) {
        pieceArea += piece.area();
      }
    }
    assertEquals(box.area() - cut.intersection(box).area(), pieceArea, 1e-6);
  }
}
