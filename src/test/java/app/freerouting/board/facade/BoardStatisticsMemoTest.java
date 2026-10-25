package app.freerouting.board.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.core.scoring.BoardStatistics;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Point;
import app.freerouting.geometry.planar.Polyline;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.util.gson.GsonProvider;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The board keeps its last statistics until something they read changes, and hands every caller its
 * own copy: the kept statistics always equal a fresh measurement of the board.
 */
class BoardStatisticsMemoTest {

  private static final String DSN =
      """
      (pcb memo (resolution um 1)
        (structure (layer top (type signal))
          (boundary (rect pcb 0 0 20000 10000))
          (via V) (rule (width 400) (clearance 200)))
        (placement (component C (place A 3000 3000 front 0) (place B 17000 3000 front 0)))
        (library (image C (pin S 1 0 0) (pin S 2 0 2000)) (padstack S (shape (circle top 800)))
          (padstack V (shape (circle top 600)) (attach off)))
        (network (net X (pins A-1 B-1)) (net Y (pins A-2 B-2))))
      """;

  private RoutingBoard board;

  @BeforeEach
  void loadBoard() {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(DSN.getBytes(StandardCharsets.UTF_8)), null, null, "memo");
    assertInstanceOf(BoardReadResult.Success.class, result);
    board = (RoutingBoard) ((BoardReadResult.Success) result).board();
  }

  @Test
  void callersGetEqualCopiesThatTheyCannotChangeForOthers() {
    BoardStatistics first = board.getStatistics();
    BoardStatistics second = board.getStatistics();
    assertNotSame(first, second);
    assertNotSame(first.connections, second.connections);
    assertNotSame(first.bounds, second.bounds);
    assertEquals(json(first), json(second));
    assertEquals(2, first.connections.incompleteCount);

    first.connections.incompleteCount = 99;
    first.clearanceViolations.totalCount = 99;
    first.bounds.minViaCount = 99;
    first.board.boundingBox.width = 99;

    assertEquals(2, board.getStatistics().connections.incompleteCount);
    assertEqualsFresh();
  }

  @Test
  void itemEditsAreSeen() {
    board.getStatistics();
    insertTrace("X", 3000);
    BoardStatistics after = assertEqualsFresh();
    assertEquals(1, after.connections.incompleteCount);
    assertEquals(1, after.traces.totalCount);
  }

  @Test
  void clearanceRuleEditsAreSeen() {
    insertTrace("X", 3000);
    insertTrace("Y", 5000);
    assertEquals(0, board.getStatistics().clearanceViolations.totalCount);

    // As the clearance matrix window does it: change the value, then update the search trees.
    board.rules.clearanceMatrix.setValue(1, 1, 4000);
    board.searchTreeManager.clearanceValueChanged();

    BoardStatistics after = assertEqualsFresh();
    assertTrue(after.clearanceViolations.totalCount > 0, json(after));
  }

  @Test
  void netChangesAreSeen() {
    assertEquals(2, board.getStatistics().nets.totalCount);
    int newNet = board.rules.nets.add("Z", 1, false).netNumber;
    assertEquals(3, assertEqualsFresh().nets.totalCount);

    Pin pinOfX =
        board.getPins().stream()
            .filter(p -> p.containsNet(netNumber("X")))
            .findFirst()
            .orElseThrow();
    pinOfX.assignNetNo(newNet);
    assertEquals(1, assertEqualsFresh().connections.incompleteCount);
  }

  @Test
  void statisticsWithoutClearanceViolationsAreKeptApart() {
    insertTrace("X", 3000);
    insertTrace("Y", 5000);
    board.rules.clearanceMatrix.setValue(1, 1, 4000);
    board.searchTreeManager.clearanceValueChanged();
    assertTrue(board.getStatistics().clearanceViolations.totalCount > 0);

    BoardStatistics withoutViolations = board.getStatistics(false);
    assertEquals(0, withoutViolations.clearanceViolations.totalCount);
    assertEquals(json(new BoardStatistics(board, null, false)), json(withoutViolations));
    assertEqualsFresh();
  }

  @Test
  void forgettingTheStatisticsAfterOtherRuleEditsMeasuresAgain() {
    BoardStatistics before = board.getStatistics();
    board.rules.setDefaultTraceHalfWidths(1000);
    board.forgetStatistics();
    BoardStatistics after = assertEqualsFresh();
    // A trace pitch of 2 * 1000 + 200 instead of 2 * 200 + 200 board units.
    assertTrue(
        after.bounds.bendEquivalentLengthMm > before.bounds.bendEquivalentLengthMm,
        before.bounds.bendEquivalentLengthMm + " -> " + after.bounds.bendEquivalentLengthMm);
  }

  private BoardStatistics assertEqualsFresh() {
    BoardStatistics kept = board.getStatistics();
    assertEquals(json(new BoardStatistics(board)), json(kept));
    return kept;
  }

  private void insertTrace(String netName, int y) {
    Point[] ends =
        board.getPins().stream()
            .filter(p -> p.containsNet(netNumber(netName)))
            .map(Pin::getCenter)
            .toArray(Point[]::new);
    assertEquals(2, ends.length);
    assertNotNull(
        board.insertTraceWithoutCleaning(
            new Polyline(new Point[] {ends[0], new IntPoint(10000, y), ends[1]}),
            0,
            200,
            new int[] {netNumber(netName)},
            1,
            FixedState.UNFIXED));
  }

  private int netNumber(String name) {
    return board.rules.nets.get(name).iterator().next().netNumber;
  }

  private static String json(BoardStatistics statistics) {
    return GsonProvider.GSON.toJson(statistics);
  }
}
