package app.freerouting.io.specctra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Via;
import app.freerouting.drc.DesignRulesChecker;
import app.freerouting.settings.GlobalSettings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A session carries all routing, including what the design's own wiring already had, so reading it
 * onto a freshly loaded design must replace that wiring rather than add a second copy (each doubled
 * via counted as one clearance violation per layer in the DSN + SES DRC).
 */
class SesReloadOntoDesignTest {

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  @Test
  void readingTheSessionOfADesignWithWiringDoesNotDoubleIt() throws Exception {
    RoutingBoard design = DsnTestFixtures.loadBoard("Issue153-wavefolder.dsn");
    long viaCount = viaCount(design);
    int violationsBefore = new DesignRulesChecker(design, null).getAllClearanceViolations().size();
    assertTrue(viaCount > 0, "the fixture must have vias in its wiring");
    ByteArrayOutputStream session = new ByteArrayOutputStream();
    SesWriter.write(design, session, "Issue153-wavefolder.dsn");

    RoutingBoard reloaded = DsnTestFixtures.loadBoard("Issue153-wavefolder.dsn");
    SesReader.read(new ByteArrayInputStream(session.toByteArray()), reloaded);

    assertEquals(viaCount, viaCount(reloaded));
    assertEquals(
        violationsBefore,
        new DesignRulesChecker(reloaded, null).getAllClearanceViolations().size());
  }

  private static long viaCount(RoutingBoard board) {
    return board.getItems().stream().filter(Via.class::isInstance).count();
  }
}
