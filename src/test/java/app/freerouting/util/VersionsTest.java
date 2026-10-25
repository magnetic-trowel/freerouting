package app.freerouting.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VersionsTest {

  @Test
  void releaseNumbersCompareNumerically() {
    assertEquals(0, Versions.compare("2.2.0", "2.2.0"));
    assertEquals(0, Versions.compare("v2.5", "2.5.0"));
    assertTrue(Versions.compare("2.1.0", "2.2.0") < 0);
    assertTrue(Versions.compare("1.9.9", "2.0.0") < 0);
    assertTrue(Versions.compare("2.2.1", "2.2.0") > 0);
    assertTrue(Versions.compare("10.0.0", "9.99.99") > 0);
  }

  @Test
  void preReleaseIsBetweenThePreviousAndItsOwnRelease() {
    assertTrue(Versions.compare("2.6.0-SNAPSHOT", "v2.5.0") > 0);
    assertTrue(Versions.compare("2.6.0-SNAPSHOT", "2.6.0") < 0);
    assertTrue(Versions.compare("2.6.0", "2.6.0-rc.1") > 0);
    assertTrue(Versions.compare("2.6.0-rc.2", "2.6.0-rc.10") < 0);
    assertTrue(Versions.compare("2.6.0-alpha", "2.6.0-alpha.1") < 0);
    assertEquals(0, Versions.compare("2.6.0+build.7", "2.6.0"));
  }

  @Test
  void nullIsOlderThanAnyVersion() {
    assertTrue(Versions.compare(null, "1.0.0") < 0);
    assertTrue(Versions.compare("1.0.0", null) > 0);
    assertEquals(0, Versions.compare(null, null));
  }
}
