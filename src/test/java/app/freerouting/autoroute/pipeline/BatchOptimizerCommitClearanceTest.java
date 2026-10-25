package app.freerouting.autoroute.pipeline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.board.trace.PolylineTrace;
import app.freerouting.drc.ClearanceBaseline;
import app.freerouting.drc.DesignRulesChecker;
import app.freerouting.io.specctra.DsnTestFixtures;
import app.freerouting.settings.GlobalSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** An optimizer commit is vetoed for a clearance violation only if the violation is new. */
class BatchOptimizerCommitClearanceTest {

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  @Test
  void aViolationIsNewOnlyIfTheStartBoardDidNotHaveIt() throws Exception {
    RoutingBoard board = DsnTestFixtures.loadBoard("Issue103-Board-Routed.dsn");
    ClearanceBaseline cleanStart =
        new ClearanceBaseline(new DesignRulesChecker(board, null).getAllClearanceViolations());
    PolylineTrace trace =
        board.getItems().stream()
            .filter(PolylineTrace.class::isInstance)
            .map(PolylineTrace.class::cast)
            .findFirst()
            .orElseThrow();
    int otherNet = trace.getNetNumber(0) == 1 ? 2 : 1;
    // A trace of another net on top of it: a violation the next start board already has.
    PolylineTrace overlap =
        board.insertTraceWithoutCleaning(
            trace.polyline(),
            trace.getLayer(),
            trace.getHalfWidth(),
            new int[] {otherNet},
            trace.clearanceClassIndex(),
            FixedState.UNFIXED);
    ClearanceBaseline startWithOverlap =
        new ClearanceBaseline(new DesignRulesChecker(board, null).getAllClearanceViolations());

    // Reroute the overlapping trace exactly as it was.
    board.generateSnapshot();
    Item copy = overlap.copy(overlap.getId());
    board.removeItem(overlap);
    board.insertItem(copy);
    BatchOptimizer.ChangeSet changes = BatchOptimizer.ChangeSet.since(board);

    assertFalse(changes.addsClearanceViolation(board, startWithOverlap));
    assertTrue(changes.addsClearanceViolation(board, cleanStart));
  }
}
