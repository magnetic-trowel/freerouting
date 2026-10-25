package app.freerouting.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.freerouting.core.RoutingJob;
import app.freerouting.settings.sources.TestingSettings;
import org.junit.jupiter.api.Test;

/** Fanout gives the same board on every run. */
class FanoutDeterminismTest extends RoutingFixtureTest {

  private String fanoutBoardHash() {
    TestingSettings settings = new TestingSettings();
    settings.setEnabled(false);
    settings.setOptimizerEnabled(false);
    settings.setFanoutMaxPasses(3);
    RoutingJob job = getRoutingJob("Issue508-DAC2020_bm06.dsn", settings);
    return runRoutingJob(job).board.getHash();
  }

  @Test
  void fanoutGivesTheSameBoardOnEveryRun() {
    assertEquals(fanoutBoardHash(), fanoutBoardHash());
  }
}
