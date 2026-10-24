package app.freerouting.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Via;
import app.freerouting.core.RoutingJob;
import app.freerouting.io.specctra.DsnTestFixtures;
import app.freerouting.io.specctra.SesWriter;
import app.freerouting.settings.sources.TestingSettings;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
}
