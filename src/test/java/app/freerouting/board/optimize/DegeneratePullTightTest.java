package app.freerouting.board.optimize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.structure.AngleRestriction;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.board.model.structure.Layer;
import app.freerouting.board.model.structure.LayerStructure;
import app.freerouting.board.state.ChangedArea;
import app.freerouting.board.state.Communication;
import app.freerouting.board.trace.PolylineTrace;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Point;
import app.freerouting.geometry.planar.Polyline;
import app.freerouting.geometry.planar.PolylineShape;
import app.freerouting.rules.BoardRules;
import app.freerouting.rules.ClearanceMatrix;
import app.freerouting.settings.GlobalSettings;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A pull-tight result whose corners collapse into one point has no lines. It must not count as a
 * change: before the fix, PolylineTrace.pullTight reported it as one while change() left the trace
 * as it was, so TraceTightener.optChangedArea pulled the same trace tight without end (one board
 * logged 5.9 million "Polyline: must contain at least 2 different points" warnings and spent its
 * whole job timeout there).
 */
class DegeneratePullTightTest {
  private RoutingBoard board;

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
    LayerStructure layers = new LayerStructure(new Layer[] {new Layer("Top", true)});
    BoardRules rules = new BoardRules(layers, ClearanceMatrix.getDefaultInstance(layers, 10));
    rules.createDefaultNetClass();
    board =
        new RoutingBoard(
            new IntBox(0, 0, 100000, 100000),
            layers,
            new PolylineShape[0],
            0,
            rules,
            new Communication());
    board.rules.nets.add("Net1", 1, false);
    board.rules.setTraceAngleRestriction(AngleRestriction.FORTYFIVE_DEGREE);
  }

  /**
   * A tightener that collapses every polyline into a single point and, like
   * TraceTightener45.reduceCorners, marks the removed corners' area as changed.
   */
  private TraceTightener collapsing() {
    return new TraceTightener(board, new int[0], null, -1, null, -1) {
      @Override
      public Polyline pullTight(Polyline polyline) {
        if (board.changedArea != null) {
          board.changedArea.join(polyline.cornerApprox(1), 0);
        }
        return new Polyline(new Point[] {polyline.firstCorner()});
      }

      @Override
      Polyline smoothenStartCornerAtTrace(PolylineTrace trace) {
        return null;
      }

      @Override
      Polyline smoothenEndCornerAtTrace(PolylineTrace trace) {
        return null;
      }
    };
  }

  private PolylineTrace insertTrace(Point... points) {
    return board.insertTraceWithoutCleaning(
        new Polyline(points), 0, 100, new int[] {1}, 1, FixedState.UNFIXED);
  }

  @Test
  void collapsedResultIsNoChange() {
    PolylineTrace trace =
        insertTrace(
            new IntPoint(10000, 10000), new IntPoint(40000, 10000), new IntPoint(40000, 40000));
    Polyline before = trace.polyline();
    assertFalse(trace.pullTight(collapsing()));
    assertEquals(before, trace.polyline());
  }

  @Test
  void changedAreaOptimizationEndsOnACollapsedResult() {
    PolylineTrace trace =
        insertTrace(
            new IntPoint(10000, 10000), new IntPoint(40000, 10000), new IntPoint(40000, 40000));
    board.changedArea = new ChangedArea(board.getLayerCount());
    board.changedArea.join(new IntPoint(10000, 10000).toFloat(), 0);
    board.changedArea.join(new IntPoint(40000, 40000).toFloat(), 0);
    TraceTightener tightener = collapsing();
    assertTimeoutPreemptively(Duration.ofSeconds(20), () -> tightener.optChangedArea(null));
    assertTrue(trace.polyline().lines.length >= 3);
  }

  @Test
  void loopTraceIsNotReducedToAPoint() {
    // first and last corner equal: shortcuts across the loop would leave one point
    Polyline loop =
        new Polyline(
            new Point[] {
              new IntPoint(10000, 10000),
              new IntPoint(30000, 10000),
              new IntPoint(30000, 30000),
              new IntPoint(10000, 30000),
              new IntPoint(10000, 10000)
            });
    assertTrue(loop.lines.length > 4);
    TraceTightener tightener =
        TraceTightener.getInstance(board, new int[0], null, 100, null, -1, null, -1);
    Polyline result = tightener.pullTight(loop, 0, 100, new int[] {1}, 1, Set.of());
    assertTrue(
        result.lines.length >= 3,
        "pull-tight collapsed the loop to " + result.lines.length + " lines");
  }
}
