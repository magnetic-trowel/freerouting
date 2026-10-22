package app.freerouting.geometry.planar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IntBoxTest {

  @Test
  void offsetMovesEveryEdge() {
    IntBox box = new IntBox(-10, -20, 30, 40);

    assertCorners(box.offset(1000), -1010, -1020, 1030, 1040);
    assertCorners(box.offset(-5), -5, -15, 25, 35);
  }

  @Test
  void offsetSaturatesInsteadOfOverflowing() {
    IntBox box = new IntBox(Integer.MIN_VALUE + 10, 0, Integer.MAX_VALUE - 10, 100);

    assertCorners(box.offset(1000), Integer.MIN_VALUE, -1000, Integer.MAX_VALUE, 1100);
  }

  private static void assertCorners(IntBox box, int llx, int lly, int urx, int ury) {
    assertEquals(llx, box.ll.x);
    assertEquals(lly, box.ll.y);
    assertEquals(urx, box.ur.x);
    assertEquals(ury, box.ur.y);
  }
}
