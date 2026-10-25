package app.freerouting.board.optimize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Via;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.board.model.structure.Layer;
import app.freerouting.board.model.structure.LayerStructure;
import app.freerouting.board.state.Communication;
import app.freerouting.core.library.Padstack;
import app.freerouting.core.library.Padstacks;
import app.freerouting.geometry.planar.ConvexShape;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Polyline;
import app.freerouting.geometry.planar.PolylineShape;
import app.freerouting.geometry.planar.TileShape;
import app.freerouting.rules.BoardRules;
import app.freerouting.rules.ClearanceMatrix;
import app.freerouting.rules.ViaRule;
import app.freerouting.settings.GlobalSettings;
import org.junit.jupiter.api.Test;

/**
 * A fanout via whose trace is free for less than half a board unit towards its next corner cannot
 * move: the step rounds back to the via centre. Reporting such a zero move as a change made
 * TraceTightener.optChangedArea repeat it forever (tomu-fpga8 hung in a fanout pass).
 */
class ViaOptimizerRoundedStepTest {

  @Test
  void viaDoesNotReportAMoveThatRoundsToItsCentre() {
    Freerouting.globalSettings = new GlobalSettings();
    LayerStructure layers =
        new LayerStructure(new Layer[] {new Layer("Top", true), new Layer("Bottom", true)});
    BoardRules rules = new BoardRules(layers, ClearanceMatrix.getDefaultInstance(layers, 10));
    rules.createDefaultNetClass();
    RoutingBoard board =
        new RoutingBoard(
            new IntBox(0, 0, 200000, 200000),
            layers,
            new PolylineShape[] {TileShape.getInstance(0, 0, 200000, 200000)},
            0,
            rules,
            new Communication());
    board.rules.nets.add("Net1", 1, false);
    board.library.padstacks = new Padstacks(layers);
    ConvexShape[] shapes =
        new ConvexShape[] {
          TileShape.getInstance(-50, -50, 50, 50), TileShape.getInstance(-50, -50, 50, 50)
        };
    Padstack padstack = board.library.padstacks.add("Via", shapes, true, false);
    board.library.addViaPadstack(padstack);
    ViaRule viaRule = new ViaRule("Default");
    rules.viaRules.add(viaRule);
    rules.getDefaultNetClass().setViaRule(viaRule);

    IntPoint center = new IntPoint(100000, 100000);
    IntPoint traceEnd = new IntPoint(110000, 110000);
    Via via = board.insertVia(padstack, center, new int[] {1}, 1, FixedState.UNFIXED, false);
    board.insertTraceWithoutCleaning(
        new Polyline(new IntPoint[] {center, traceEnd}),
        0,
        100,
        new int[] {1},
        1,
        FixedState.UNFIXED);
    board.insertObstacle(new IntBox(100179, 99000, 101179, 100079), 0, 1, FixedState.UNFIXED);
    double freeLength = board.checkTraceSegment(center, traceEnd, 0, new int[] {1}, 100, 1, false);
    assertTrue(
        freeLength > 0 && freeLength < 0.5, "setup: free length must round away: " + freeLength);

    assertFalse(ViaOptimizer.optViaLocation(board, via, null, 100, 10));
    assertEquals(center, via.getCenter());
  }
}
