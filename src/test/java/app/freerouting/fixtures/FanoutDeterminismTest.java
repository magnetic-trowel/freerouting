package app.freerouting.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.freerouting.core.RoutingJob;
import app.freerouting.settings.sources.TestingSettings;
import org.junit.jupiter.api.Test;

/** A per-pin fanout budget small enough to bind must still give the same board on every run. */
class FanoutDeterminismTest extends RoutingFixtureTest {

  private String fanoutWithTinyPinBudget() {
    TestingSettings settings = new TestingSettings();
    settings.setEnabled(false);
    settings.setOptimizerEnabled(false);
    settings.setFanoutMaxPasses(3);
    RoutingJob job = getRoutingJob("Issue508-DAC2020_bm06.dsn", settings);
    job.routerSettings.fanout.maxMillisecondsPerPin = 1L;
    return runRoutingJob(job).board.getHash();
  }

  @Test
  void bindingPinBudgetGivesTheSameBoardOnEveryRun() {
    assertEquals(fanoutWithTinyPinBudget(), fanoutWithTinyPinBudget());
  }
}
