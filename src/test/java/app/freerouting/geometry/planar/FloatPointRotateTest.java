package app.freerouting.geometry.planar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FloatPointRotateTest {

  /**
   * Rotation must produce the same bits on every CPU. At 46 degrees the x86 HotSpot intrinsic for
   * Math.sin returns 0.7193398003386511, while StrictMath (fdlibm) returns 0.7193398003386512 on
   * all platforms.
   */
  @Test
  void rotateIsPlatformIndependent() {
    FloatPoint rotated = new FloatPoint(1, 0).rotate(Math.toRadians(46), FloatPoint.ZERO);
    assertEquals(0.6946583704589973, rotated.x);
    assertEquals(0.7193398003386512, rotated.y);
  }
}
