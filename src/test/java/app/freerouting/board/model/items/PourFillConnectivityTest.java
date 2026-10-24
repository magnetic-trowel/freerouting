package app.freerouting.board.model.items;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.board.model.structure.Layer;
import app.freerouting.board.model.structure.LayerStructure;
import app.freerouting.board.state.Communication;
import app.freerouting.core.library.Padstack;
import app.freerouting.core.library.Padstacks;
import app.freerouting.geometry.planar.ConvexShape;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Point;
import app.freerouting.geometry.planar.PolylineShape;
import app.freerouting.geometry.planar.TileShape;
import app.freerouting.rules.BoardRules;
import app.freerouting.rules.ClearanceMatrix;
import app.freerouting.rules.Net;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests that a pour connects its contacts through its fill islands, not its whole polygon. */
class PourFillConnectivityTest {

  private static final IntPoint LEFT = new IntPoint(500000, 2000000);
  private static final IntPoint RIGHT = new IntPoint(3500000, 2000000);

  private RoutingBoard board;
  private Net gndNet;
  private Net sigNet;
  private ConductionArea pour;

  @BeforeEach
  void setUp() {
    LayerStructure layerStructure =
        new LayerStructure(new Layer[] {new Layer("Top", true), new Layer("Bottom", true)});
    BoardRules boardRules =
        new BoardRules(layerStructure, ClearanceMatrix.getDefaultInstance(layerStructure, 10));
    boardRules.createDefaultNetClass();
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
    board.library.padstacks = new Padstacks(layerStructure);
    ConvexShape square = TileShape.getInstance(-100000, -100000, 100000, 100000);
    Padstack viaPadstack =
        board.library.padstacks.add("TestVia", new ConvexShape[] {square, square}, true, false);
    pour =
        board.insertConductionArea(
            TileShape.getInstance(0, 0, 4000000, 4000000),
            0,
            new int[] {gndNet.netNumber},
            0,
            false,
            FixedState.UNFIXED);
    for (IntPoint location : new IntPoint[] {LEFT, RIGHT}) {
      board.insertVia(
          viaPadstack, location, new int[] {gndNet.netNumber}, 0, FixedState.UNFIXED, true);
    }
  }

  /** A GND trace on the bottom layer from the left via to the right one. */
  private Trace bottomTrace() {
    board.insertTrace(
        new Point[] {LEFT, RIGHT}, 1, 50000, new int[] {gndNet.netNumber}, 0, FixedState.UNFIXED);
    Trace trace =
        board.getTraces().stream().filter(t -> t.containsNet(gndNet.netNumber)).findFirst().get();
    assertNotNull(trace);
    return trace;
  }

  /** A SIG trace on the pour layer across the whole pour, between the two vias. */
  private void cutThePour() {
    board.insertTrace(
        new Point[] {new IntPoint(2000000, 0), new IntPoint(2000000, 4000000)},
        0,
        50000,
        new int[] {sigNet.netNumber},
        0,
        FixedState.UNFIXED);
  }

  @Test
  void continuousFillJoinsBothVias() {
    assertTrue(pour.joins(LEFT, RIGHT));
    assertTrue(bottomTrace().isCycle());
  }

  @Test
  void foreignTrackSplitsTheFill() {
    assertTrue(pour.joins(LEFT, RIGHT));

    cutThePour();

    // The islands of the earlier board revision are not reused.
    assertFalse(pour.joins(LEFT, RIGHT));
    assertTrue(pour.joins(LEFT, new IntPoint(1000000, 3000000)));
    assertTrue(pour.fillIslandsMetBy(board.getVias().iterator().next()).cardinality() == 1);
  }

  @Test
  void traceAcrossTheCutIsNoCycle() {
    cutThePour();

    Trace trace = bottomTrace();

    assertFalse(trace.isCycle());
    board.removeIfCycle(trace);
    assertTrue(trace.isOnTheBoard());
  }

  /**
   * A GND trace on the pour layer that leaves the pour and comes back: the normalization of the
   * inserted trace removes it as redundant only if the fill joins its two ends.
   */
  private boolean detourOnThePourLayerStays() {
    board.insertTrace(
        new Point[] {LEFT, new IntPoint(500000, 5000000), new IntPoint(3500000, 5000000), RIGHT},
        0,
        50000,
        new int[] {gndNet.netNumber},
        0,
        FixedState.UNFIXED);
    return board.getTraces().stream().anyMatch(t -> t.containsNet(gndNet.netNumber));
  }

  @Test
  void detourOnThePourLayerIsRemovedWhereTheFillJoinsItsEnds() {
    assertFalse(detourOnThePourLayerStays());
  }

  @Test
  void detourOnThePourLayerStaysWhereItJoinsTwoIslands() {
    board.insertTrace(
        new Point[] {new IntPoint(2000000, 0), new IntPoint(2000000, 4500000)},
        0,
        50000,
        new int[] {sigNet.netNumber},
        0,
        FixedState.UNFIXED);

    assertTrue(detourOnThePourLayerStays());
  }
}
