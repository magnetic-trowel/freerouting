package app.freerouting.management;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.core.RoutingJob;
import app.freerouting.fixtures.RoutingFixtureTest;
import app.freerouting.settings.GlobalSettings;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompositeBoardInputTest extends RoutingFixtureTest {

  @BeforeEach
  protected void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  @Test
  void testAssembleBoardFromDsn() throws Exception {
    RoutingJob sampleJob = getRoutingJob("Issue508-DAC2020_bm01.dsn");
    assertNotNull(sampleJob);
    byte[] dsnBytes = sampleJob.input.getData().readAllBytes();

    CompositeBoardInput input = new CompositeBoardInput();
    input.setDesign(dsnBytes, "test.dsn");

    RoutingJob job = new RoutingJob(UUID.randomUUID());
    input.assembleBoard(job);

    assertNotNull(job.board, "Routing board should be assembled");
    assertNotNull(job.routerSettings, "Router settings should be merged");
    assertNotNull(job.board.getStatistics());
  }

  @Test
  void testAssembleBoardFromDsnAndRules() throws Exception {
    RoutingJob sampleJob = getRoutingJob("Issue508-DAC2020_bm01.dsn");
    assertNotNull(sampleJob);
    byte[] dsnBytes = sampleJob.input.getData().readAllBytes();

    String rulesContent = "(rules (autoroute (fanout on) (pass 5)))";
    CompositeBoardInput input = new CompositeBoardInput();
    input.setDesign(dsnBytes, "test.dsn");
    input.setRules(rulesContent.getBytes(StandardCharsets.UTF_8), "test.rules");

    RoutingJob job = new RoutingJob(UUID.randomUUID());
    input.assembleBoard(job);

    assertNotNull(job.board);
    assertNotNull(job.rules);
    assertEquals("test.rules", job.rules.getFilename());
  }

  /**
   * The board load used to fill board defaults (every layer routable, alternating directions) into
   * the job's settings, which were then merged at API priority over the DSN's layer rules.
   */
  @Test
  void dsnLayerRulesSurviveTheApiSettingsSource() throws Exception {
    String dsn = Files.readString(Path.of("fixtures/Issue187-processor.Z80.dsn"));
    String original =
        "(layer_rule F.Cu\n        (active on)\n        (preferred_direction horizontal)";
    assertTrue(dsn.contains(original), "fixture layout changed");
    dsn =
        dsn.replace(
            original,
            "(layer_rule F.Cu\n        (active off)\n        (preferred_direction vertical)");

    CompositeBoardInput input = new CompositeBoardInput();
    input.setDesign(dsn.getBytes(StandardCharsets.UTF_8), "layer_rules.dsn");
    RoutingJob job = new RoutingJob(UUID.randomUUID());
    input.assembleBoard(job);

    assertFalse(job.routerSettings.getLayerActive(0), "F.Cu is inactive in the DSN");
    assertFalse(
        job.routerSettings.getPreferredDirectionIsHorizontal(0),
        "F.Cu prefers vertical in the DSN");
    assertTrue(job.routerSettings.getLayerActive(1), "B.Cu stays active");
  }
}
