package app.freerouting.util.gson;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.freerouting.settings.RouterSettings;
import org.junit.jupiter.api.Test;

/** Numbers written by the shared Gson read back as the same value. */
class GsonProviderNumberTest {

  @Test
  void doublesAndFloatsKeepEveryDigit() {
    assertEquals("0.004", GsonProvider.GSON.toJson(0.004));
    assertEquals("0.125", GsonProvider.GSON.toJson(0.125));
    assertEquals("1.2345", GsonProvider.GSON.toJson(1.2345));
    assertEquals("0.1", GsonProvider.GSON.toJson(0.1f));
    assertEquals("250.0", GsonProvider.GSON.toJson(250.0));
    for (double value : new double[] {0.004, 1.0 / 3, -827612.6504826803, 1e-7, 1e300}) {
      assertEquals(
          value, GsonProvider.GSON.fromJson(GsonProvider.GSON.toJson(value), Double.class));
    }
  }

  @Test
  void nonFiniteNumbersAreWrittenAsNull() {
    assertEquals("null", GsonProvider.GSON.toJson(Double.NaN));
    assertEquals("null", GsonProvider.GSON.toJson(Float.POSITIVE_INFINITY));
  }

  @Test
  void settingsSurviveSaveAndLoad() {
    RouterSettings settings = new RouterSettings();
    settings.clearanceToleranceUm = 0.004;
    RouterSettings loaded =
        GsonProvider.GSON.fromJson(GsonProvider.GSON.toJson(settings), RouterSettings.class);
    assertEquals(0.004, loaded.clearanceToleranceUm);
  }
}
