package app.freerouting.management;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.core.BoardFileDetails;
import app.freerouting.core.RoutingJob;
import app.freerouting.fixtures.RoutingFixtureTest;
import app.freerouting.settings.GlobalSettings;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

  /**
   * The deferred post-load scan used to compute the optimizer's score bounds while the rules file
   * was still being applied, and cached them on the board for good. The optimizer then compared the
   * live board (old rules) with candidate copies (new rules) on different scales.
   */
  @Test
  void scoreBoundsReflectTheRulesFile() throws Exception {
    RoutingJob sampleJob = getRoutingJob("Issue508-DAC2020_bm08.dsn");
    byte[] dsnBytes = sampleJob.input.getData().readAllBytes();
    String rules = "(rules PCB bm08\n  (rule\n    (width 150.0)\n    (clear 150.0)\n  )\n)\n";

    CompositeBoardInput input = new CompositeBoardInput();
    input.setDesign(dsnBytes, "bm08.dsn");
    input.setRules(rules.getBytes(StandardCharsets.UTF_8), "bm08.rules");
    RoutingJob job = new RoutingJob(UUID.randomUUID());
    input.assembleBoard(job);
    job.board.awaitPostLoad();

    var live = new app.freerouting.core.scoring.BoardStatistics(job.board).bounds;
    var fresh = new app.freerouting.core.scoring.BoardStatistics(job.board.deepCopy()).bounds;
    assertEquals(fresh.viaEquivalentLengthMm, live.viaEquivalentLengthMm, 1e-6);
    assertEquals(fresh.bendEquivalentLengthMm, live.bendEquivalentLengthMm, 1e-6);
  }

  /** The scheduler reloads the job's input; the session used to be dropped on the way. */
  @Test
  void sessionIsHandedToTheScheduler() throws Exception {
    byte[] dsnBytes = Files.readAllBytes(Path.of("fixtures/Issue508-DAC2020_bm05.dsn"));
    byte[] sesBytes = Files.readAllBytes(Path.of("fixtures/Issue508-DAC2020_bm05.ses"));

    CompositeBoardInput input = new CompositeBoardInput();
    input.setDesign(dsnBytes, "bm05.dsn");
    input.setSession(sesBytes, "bm05.ses");
    RoutingJob job = new RoutingJob(UUID.randomUUID());
    input.assembleBoard(job);

    assertNotNull(job.initialSession, "session must reach the scheduler");
    assertEquals(sesBytes.length, job.initialSession.getData().readAllBytes().length);
  }

  /**
   * The autoroute settings of a RULES file have RULES-file priority (40): a value the API caller
   * set (70) wins, and the RULES file only fills in what the caller left unset. They used to be
   * applied on top of the merged settings, so a rules file written by Freerouting, which always
   * says {@code (postroute on)} and {@code (vias on)}, re-enabled the optimizer and vias.
   */
  @Test
  void callerSettingsOutrankRulesFileAutorouteSettings() throws Exception {
    RoutingJob job = new RoutingJob(UUID.randomUUID());
    job.routerSettings.optimizer.enabled = false;
    job.routerSettings.viasAllowed = false;
    job.routerSettings.scoring.startRipupCosts = 7;

    byte[] dsnBytes = getRoutingJob("Issue508-DAC2020_bm01.dsn").input.getData().readAllBytes();
    String rulesContent =
        "(rules PCB test (autoroute_settings (autoroute on) (postroute on) (vias on)"
            + " (via_costs 51) (start_ripup_costs 103)))";
    new CompositeBoardInput()
        .setDesign(dsnBytes, "test.dsn")
        .setRules(rulesContent.getBytes(StandardCharsets.UTF_8), "test.rules")
        .assembleBoard(job);

    assertFalse(job.routerSettings.optimizer.enabled, "caller disabled the optimizer");
    assertFalse(job.routerSettings.viasAllowed, "caller disallowed vias");
    assertEquals(7, job.routerSettings.scoring.startRipupCosts, "caller set the ripup costs");
    assertEquals(51, job.routerSettings.scoring.viaCosts, "the rules file sets the via costs");
  }

  @Test
  void adjacentRulesFileIsTheRulesFileNamedAfterTheDsn(@TempDir Path directory) throws Exception {
    byte[] dsnBytes = getRoutingJob("Issue508-DAC2020_bm01.dsn").input.getData().readAllBytes();
    Path dsn = directory.resolve("board.dsn");
    Files.write(dsn, dsnBytes);
    BoardFileDetails input = new BoardFileDetails(dsn.toFile());
    assertNull(CompositeBoardInput.adjacentRulesFile(input), "no rules file yet");

    Path rules = directory.resolve("board.rules");
    Files.writeString(rules, "(rules PCB board)");
    assertEquals(
        rules.toFile().getAbsoluteFile(),
        CompositeBoardInput.adjacentRulesFile(input).getAbsoluteFile());
  }
}
