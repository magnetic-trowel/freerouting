package app.freerouting.io.specctra;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SesCoordinateFormatTest {

  @Test
  void keepsTheUsualFormatting() {
    assertEquals("2540000.0", SesWriter.formatCoordinate(2540000.0));
    assertEquals("-1234.5", SesWriter.formatCoordinate(-1234.5));
    assertEquals("0.0", SesWriter.formatCoordinate(0.0));
  }

  /**
   * High-resolution boards (LibrePCB, resolution mm 1000000) used to get place lines like 1.7145E7.
   */
  @Test
  void neverWritesScientificNotation() {
    assertEquals("17145000", SesWriter.formatCoordinate(1.7145e7));
    assertEquals("19684999.999999996", SesWriter.formatCoordinate(1.9684999999999996e7));
    assertEquals("-12000000", SesWriter.formatCoordinate(-1.2e7));
    assertEquals("0.00012", SesWriter.formatCoordinate(1.2e-4));
  }
}
