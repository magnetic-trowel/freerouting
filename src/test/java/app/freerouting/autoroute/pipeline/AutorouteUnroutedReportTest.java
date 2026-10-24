package app.freerouting.autoroute.pipeline;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

/** Tests the report of vias and pins cut off from the fill of their pour. */
class AutorouteUnroutedReportTest {

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
    viaPadstack =
        board.library.padstacks.add("TestVia", new ConvexShape[] {square, square}, true, false);
    board.insertConductionArea(
        TileShape.getInstance(0, 0, 4000000, 4000000),
        0,
        new int[] {gndNet.netNumber},
        0,
        false,
        FixedState.UNFIXED);
    for (int x : new int[] {500000, 3500000}) {
      board.insertVia(
          viaPadstack,
          new IntPoint(x, 2000000),
          new int[] {gndNet.netNumber},
          0,
          FixedState.UNFIXED,
          true);
    }
  }

  @Test
  void continuousPourReportsNothing() {
    assertNull(AutorouteUnroutedReport.planeFillCutOffs(board));
  }

  @Test
  void foreignTrackAcrossThePourIsReported() {
    board.insertTrace(
        new Point[] {new IntPoint(2000000, 0), new IntPoint(2000000, 4000000)},
        0,
        50000,
        new int[] {sigNet.netNumber},
        0,
        FixedState.UNFIXED);

    String report = AutorouteUnroutedReport.planeFillCutOffs(board);

    assertNotNull(report);
    assertTrue(report.startsWith("1 pin(s) or via(s)"), report);
    assertTrue(report.contains("Net 'GND'"), report);
  }
}
