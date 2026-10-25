package app.freerouting.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.TestFixtures;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.items.Trace;
import app.freerouting.board.model.items.Via;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.scoring.BoardStatistics;
import app.freerouting.io.specctra.DsnTestFixtures;
import app.freerouting.io.specctra.SesWriter;
import app.freerouting.settings.sources.TestingSettings;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Routing a design together with its session ({@code -de board.dsn+board.ses}) must not double the
 * design's own wiring: the session carries that wiring too.
 */
class SessionInputRoutingTest extends RoutingFixtureTest {

  private static final String BOARD = "Issue153-wavefolder.dsn";

  @Test
  void theSessionReplacesTheDesignWiringInsteadOfAddingToIt() throws Exception {
    RoutingBoard design = DsnTestFixtures.loadBoard(BOARD);
    long designVias = design.getVias().size();
    assertTrue(designVias > 0, "the fixture must have vias in its wiring");
    ByteArrayOutputStream session = new ByteArrayOutputStream();
    SesWriter.write(design, session, BOARD);

    TestingSettings settings = new TestingSettings();
    settings.setMaxPasses(1);
    RoutingJob job = getRoutingJob(BOARD, settings);
    job.setInitialSession(session.toByteArray(), "Issue153-wavefolder.ses");
    runRoutingJob(job);

    assertNotNull(job.board);
    Map<String, Long> viasPerPosition =
        job.board.getVias().stream()
            .map(Via::getCenter)
            .map(Object::toString)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    assertEquals(
        Map.of(),
        viasPerPosition.entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
        "vias at the same position");
  }

  /** The optimizer may change the routing of a session only if the settings allow it. */
  @Test
  void theOptimizerReworksTheSessionRoutingOnlyWhenAllowed() throws Exception {
    byte[] session = Files.readAllBytes(TestFixtures.resolvePath("Issue508-DAC2020_bm07-v190.ses"));
    RoutingJob kept = optimizeWithSession(session, false);
    RoutingJob reworked = optimizeWithSession(session, true);

    assertTrue(
        routing(kept.board).allMatch(item -> item.getFixedState() == FixedState.USER_FIXED),
        "the session routing is protected by default");
    assertTrue(
        routing(reworked.board).noneMatch(item -> item.getFixedState() == FixedState.USER_FIXED),
        "the session routing is ordinary routing when it may be reworked");
    BoardStatistics before = new BoardStatistics(kept.board);
    BoardStatistics after = new BoardStatistics(reworked.board);
    assertEquals(0, after.connections.incompleteCount);
    assertTrue(
        after.clearanceViolations.totalCount <= before.clearanceViolations.totalCount,
        "no new clearance violations");
    // Scores are not comparable here: the bounds count fixed vias as mandatory.
    assertTrue(
        after.vias.totalCount < before.vias.totalCount
            || (after.vias.totalCount.equals(before.vias.totalCount)
                && after.traces.totalLengthMm < before.traces.totalLengthMm),
        "the optimizer improved the reworkable session routing");
  }

  private RoutingJob optimizeWithSession(byte[] session, boolean reworkImportedRouting) {
    RoutingJob job = getRoutingJob("Issue508-DAC2020_bm07.dsn", new TestingSettings());
    job.routerSettings.optimizer.maxPasses = 2;
    job.routerSettings.optimizer.maxThreads = 1;
    job.routerSettings.optimizer.reworkImportedRouting = reworkImportedRouting;
    job.setInitialSession(session, "Issue508-DAC2020_bm07-v190.ses");
    return runRoutingJob(job);
  }

  private static Stream<Item> routing(RoutingBoard board) {
    return board.getItems().stream().filter(item -> item instanceof Trace || item instanceof Via);
  }
}
