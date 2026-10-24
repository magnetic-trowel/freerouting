package app.freerouting.drc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Via;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.board.model.structure.Layer;
import app.freerouting.board.model.structure.LayerStructure;
import app.freerouting.board.state.Communication;
import app.freerouting.core.library.Padstack;
import app.freerouting.geometry.planar.ConvexShape;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Point;
import app.freerouting.geometry.planar.PolylineShape;
import app.freerouting.geometry.planar.TileShape;
import app.freerouting.rules.BoardRules;
import app.freerouting.rules.ClearanceMatrix;
import app.freerouting.rules.Net;
import app.freerouting.rules.ViaRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests connectivity through the actual fill of a pour. */
class PlaneFillConnectivityTest {

  private RoutingBoard board;
  private Net gndNet;
  private Net sigNet;
  private Padstack viaPadstack;

  @BeforeEach
  void setUp() {
    LayerStructure layerStructure =
        new LayerStructure(new Layer[] {new Layer("Top", true), new Layer("Bottom", true)});
    BoardRules boardRules =
        new BoardRules(layerStructure, ClearanceMatrix.getDefaultInstance(layerStructure, 10));
    boardRules.createDefaultNetClass();
    ViaRule viaRule = new ViaRule("defaultViaRule");
    boardRules.viaRules.add(viaRule);
    boardRules.getDefaultNetClass().setViaRule(viaRule);
    board =
        new RoutingBoard(
            new IntBox(0, 0, 10000000, 10000000),
            layerStructure,
            new PolylineShape[] {TileShape.getInstance(0, 0, 10000000, 10000000)},
            0,
            boardRules,
            new Communication());
    gndNet = board.rules.nets.add("GND", 1, true);
    sigNet = board.rules.nets.add("SIG", 1, false);
    board.library.padstacks = new app.freerouting.core.library.Padstacks(layerStructure);
    ConvexShape square = TileShape.getInstance(-100000, -100000, 100000, 100000);
    viaPadstack =
        board.library.padstacks.add("TestVia", new ConvexShape[] {square, square}, true, false);
    board.insertConductionArea(
        TileShape.getInstance(0, 0, 4000000, 4000000),
        0,
        new int[] {gndNet.netNumber},
        0,
        false,
        FixedState.UNFIXED);
  }

  private Via gndVia(int x, int y) {
    return board.insertVia(
        viaPadstack, new IntPoint(x, y), new int[] {gndNet.netNumber}, 0, FixedState.UNFIXED, true);
  }

  private void trace(int layer, int netNumber, int x1, int y1, int x2, int y2) {
    board.insertTrace(
        new Point[] {new IntPoint(x1, y1), new IntPoint(x2, y2)},
        layer,
        50000,
        new int[] {netNumber},
        0,
        FixedState.UNFIXED);
  }

  @Test
  void continuousPourCutsNothingOff() {
    gndVia(500000, 2000000);
    gndVia(3500000, 2000000);

    PlaneFillConnectivity.Result result = PlaneFillConnectivity.analyze(board, gndNet.netNumber);

    assertTrue(result.cutOffGroups.isEmpty());
  }

  @Test
  void foreignTrackAcrossThePourCutsTheFartherViaOff() {
    Via left = gndVia(500000, 2000000);
    Via right = gndVia(3500000, 2000000);
    trace(0, sigNet.netNumber, 2000000, 0, 2000000, 4000000);
    // The board still counts both vias as connected through the pour polygon.
    assertTrue(left.getConnectedSet(gndNet.netNumber).contains(right));

    PlaneFillConnectivity.Result result = PlaneFillConnectivity.analyze(board, gndNet.netNumber);

    assertEquals(1, result.cutOffGroups.size());
    assertTrue(result.mainGroup.contains(left), "ties go to the group with the lowest item id");
    assertTrue(result.cutOffGroups.get(0).contains(right));
  }

  @Test
  void copperOnTheOtherLayerKeepsTheGroupsJoined() {
    gndVia(500000, 2000000);
    gndVia(3500000, 2000000);
    trace(0, sigNet.netNumber, 2000000, 0, 2000000, 4000000);
    trace(1, gndNet.netNumber, 500000, 2000000, 3500000, 2000000);

    PlaneFillConnectivity.Result result = PlaneFillConnectivity.analyze(board, gndNet.netNumber);

    assertTrue(result.cutOffGroups.isEmpty());
  }

  @Test
  void anotherNetsPourDoesNotCutTheFill() {
    gndVia(500000, 2000000);
    gndVia(3500000, 2000000);
    // A pour of another net across the middle: its priority is unknown, so it clears nothing.
    board.insertConductionArea(
        TileShape.getInstance(1800000, 0, 2200000, 4000000),
        0,
        new int[] {sigNet.netNumber},
        0,
        false,
        FixedState.UNFIXED);

    PlaneFillConnectivity.Result result = PlaneFillConnectivity.analyze(board, gndNet.netNumber);

    assertTrue(result.cutOffGroups.isEmpty());
  }

  @Test
  void itemOutsideThePourIsAnOrdinaryIncomplete() {
    gndVia(500000, 2000000);
    gndVia(3500000, 2000000);
    gndVia(8000000, 8000000);
    trace(0, sigNet.netNumber, 2000000, 0, 2000000, 4000000);

    PlaneFillConnectivity.Result result = PlaneFillConnectivity.analyze(board, gndNet.netNumber);

    assertEquals(1, result.cutOffGroups.size());
    assertEquals(1, result.cutOffItemCount());
  }
}
