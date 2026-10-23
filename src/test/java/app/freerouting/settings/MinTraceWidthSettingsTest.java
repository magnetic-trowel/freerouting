package app.freerouting.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.freerouting.settings.sources.CliSettings;
import app.freerouting.settings.sources.DefaultSettings;
import org.junit.jupiter.api.Test;

/** CLI plumbing and defaults for the min_trace_width_um setting. */
class MinTraceWidthSettingsTest {

  @Test
  void cliFlagReachesRouterSettings() {
    CliSettings cli = new CliSettings(new String[] {"--router.min_trace_width_um=200"});
    assertEquals(200.0, cli.getSettings().getMinTraceWidthUm(), 1e-9);
  }

  @Test
  void defaultIsNoMinimum() {
    assertEquals(0.0, new RouterSettings().getMinTraceWidthUm(), 1e-9);
    assertEquals(0.0, new SettingsMerger(new DefaultSettings()).merge().getMinTraceWidthUm(), 1e-9);
  }

  @Test
  void cloneCarriesTheField() {
    RouterSettings settings = new RouterSettings();
    settings.minTraceWidthUm = 150.0;
    assertEquals(150.0, settings.clone().getMinTraceWidthUm(), 1e-9);
  }
}
