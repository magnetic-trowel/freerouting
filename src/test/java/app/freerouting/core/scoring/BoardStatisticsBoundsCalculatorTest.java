package app.freerouting.core.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.structure.AngleRestriction;
import app.freerouting.core.scoring.BoardStatisticsBoundsCalculator.Terminal;
import app.freerouting.core.scoring.BoardStatisticsBoundsCalculator.TerminalKind;
import app.freerouting.core.scoring.BoardStatisticsBoundsCalculator.TreeBound;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.io.specctra.DsnTestFixtures;
import app.freerouting.settings.GlobalSettings;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BoardStatisticsBoundsCalculatorTest {

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  @Test
  void emptyNetAndSinglePinNetContributeZeroBounds() {
    BoardStatistics stats = statisticsFromFixture("scoring-empty-net.dsn");
    assertEquals(0.0f, stats.bounds.minTraceLengthMm, 0.001f);
    assertEquals(0, stats.bounds.minViaCount);
    assertEquals(0, stats.bounds.minBendCount);
  }

  @Test
  void coincidentPinsHaveZeroLengthAndNoBendBound() {
    BoardStatistics stats = statisticsFromFixture("scoring-zero-length.dsn");
    assertEquals(0.0f, stats.bounds.minTraceLengthMm, 0.001f);
    assertEquals(0, stats.bounds.minViaCount);
    assertEquals(0, stats.bounds.minBendCount);
  }

  @Test
  void mixedLayerPinsRequireOneThroughViaAndNoBendOnTheirDiagonal() {
    BoardStatistics stats = statisticsFromFixture("scoring-mixed-layer.dsn");
    assertEquals(1, stats.bounds.minViaCount);
    assertTrue(stats.bounds.minTraceLengthMm > 0.0f, "offset pads must produce a positive L_min");
    // The pads are offset diagonally, which a straight 45 degree trace joins.
    assertEquals(0, stats.bounds.minBendCount);
    assertEquals(stats.difficulty.complexityC.intValue(), stats.difficulty.difficultyD.intValue());
  }

  @Test
  void emptyBoardHasZeroBoundsAndDefinedDifficulty() {
    BoardStatistics stats = statisticsFromFixture("empty_board.dsn");
    assertEquals(0.0f, stats.bounds.minTraceLengthMm, 0.001f);
    assertEquals(0, stats.bounds.minViaCount);
    assertEquals(0, stats.bounds.minBendCount);
    assertEquals(1, stats.difficulty.complexityC);
    assertEquals(1.0f, stats.difficulty.difficultyD, 0.001f);
  }

  @Test
  void twoPinsGetTheirExactDistanceUnderEachAngleRestriction() {
    List<Terminal> pins = List.of(pin(0, 0), pin(30, 40));
    assertTree(70.0, 1, pins, AngleRestriction.NINETY_DEGREE);
    assertTree(40.0 + (Math.sqrt(2.0) - 1.0) * 30.0, 1, pins, AngleRestriction.FORTYFIVE_DEGREE);
    assertTree(50.0, 0, pins, AngleRestriction.NONE);
    // A diagonal needs no bend at 45 degrees.
    assertTree(
        Math.sqrt(2.0) * 30.0,
        0,
        List.of(pin(0, 0), pin(30, 30)),
        AngleRestriction.FORTYFIVE_DEGREE);
  }

  /** The spanning tree of a cross is longer than its Steiner tree, so it is no lower bound. */
  @Test
  void crossIsBoundedByItsSteinerTree() {
    List<Terminal> cross = List.of(pin(-10, 0), pin(10, 0), pin(0, -10), pin(0, 10));
    double rectilinearSteiner = 40.0; // both diagonals through the centre
    double euclideanSteiner = (1.0 + Math.sqrt(3.0)) * 10.0 * Math.sqrt(2.0);
    for (AngleRestriction restriction : AngleRestriction.values()) {
      double bound =
          BoardStatisticsBoundsCalculator.calculateTreeBound(cross, restriction).length();
      double steiner =
          restriction == AngleRestriction.NINETY_DEGREE ? rectilinearSteiner : euclideanSteiner;
      assertTrue(bound <= steiner + 1e-9, restriction + ": " + bound + " > " + steiner);
      assertTrue(bound >= 20.0 - 1e-9, restriction + ": below the largest pin distance");
    }
  }

  private static void assertTree(
      double length, int bends, List<Terminal> pins, AngleRestriction restriction) {
    TreeBound tree = BoardStatisticsBoundsCalculator.calculateTreeBound(pins, restriction);
    assertEquals(length, tree.length(), 1e-9, restriction.toString());
    assertEquals(bends, tree.bendCount(), restriction.toString());
  }

  private static Terminal pin(double x, double y) {
    return new Terminal(x, y, Set.of(0), TerminalKind.PIN);
  }

  private static BoardStatistics statisticsFromFixture(String filename) {
    BoardReadResult result =
        DsnReader.readBoard(DsnTestFixtures.openResource(filename), null, null);
    assertInstanceOf(BoardReadResult.Success.class, result, filename + " must parse");
    RoutingBoard board = (RoutingBoard) ((BoardReadResult.Success) result).board();
    assertNotNull(board);
    return new BoardStatistics(board);
  }
}
