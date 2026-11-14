package app.freerouting.autoroute.pipeline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.settings.RouterSettings;
import org.junit.jupiter.api.Test;

/**
 * The dangling-copper cleanup runs after every routing run. {@code max_passes} 0 is the default and
 * means no pass limit, so it must count as a run: before this, the cleanup was skipped for every
 * GUI and KiCad plugin user.
 */
class RoutingPipelineRouterRanTest {

  @Test
  void theDefaultUnlimitedPassCountCountsAsARun() {
    RouterSettings settings = new RouterSettings();
    settings.setRunRouter(true);
    settings.setMaxPasses(0);
    assertTrue(RoutingPipeline.routerRan(settings));
  }

  @Test
  void aPassLimitCountsAsARun() {
    RouterSettings settings = new RouterSettings();
    settings.setRunRouter(true);
    settings.setMaxPasses(100);
    assertTrue(RoutingPipeline.routerRan(settings));
  }

  @Test
  void noRouterRunWhenTheRouterIsOff() {
    RouterSettings settings = new RouterSettings();
    settings.setRunRouter(false);
    settings.setMaxPasses(0);
    assertFalse(RoutingPipeline.routerRan(settings));
    assertFalse(RoutingPipeline.routerRan(null));
  }
}
