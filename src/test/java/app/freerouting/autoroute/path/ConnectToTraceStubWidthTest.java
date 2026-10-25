package app.freerouting.autoroute.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.freerouting.autoroute.maze.AutorouteControl;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.items.Trace;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.board.trace.PolylineTrace;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Point;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.settings.RouterSettings;
import app.freerouting.settings.SettingsMerger;
import app.freerouting.settings.sources.DefaultSettings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * A found connection that ends on a trace is joined to it by a short stub on the trace's layer. The
 * stub must have that layer's width, also when the connection started on another layer.
 */
class ConnectToTraceStubWidthTest {

  private static final String DSN =
      """
      (pcb stub (resolution um 10) (unit um)
        (structure (layer top (type signal)) (layer bottom (type signal))
          (boundary (rect pcb 0 0 20000 10000))
          (via V) (rule (width 500) (clearance 200)))
        (placement (component P (place A 2000 5000 front 0)))
        (library (image P (pin S 1 0 0))
          (padstack S (shape (circle top 800)) (shape (circle bottom 800)))
          (padstack V (shape (circle top 600)) (shape (circle bottom 600)) (attach off)))
        (network (net SIG (pins A-1))))
      """;

  private static final int TOP_HALF_WIDTH = 2500;
  private static final int BOTTOM_HALF_WIDTH = 1500;

  @Test
  void stubToATraceOnAnotherLayerThanTheStartHasTheTraceLayersWidth() {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(DSN.getBytes(StandardCharsets.UTF_8)), null, null, "stub");
    assertInstanceOf(BoardReadResult.Success.class, result);
    RoutingBoard board = (RoutingBoard) ((BoardReadResult.Success) result).board();
    int signal = board.rules.nets.get("SIG").iterator().next().netNumber;
    Pin pin = board.getPins().stream().filter(p -> p.containsNet(signal)).findFirst().orElseThrow();
    // The target: a vertical SIG trace on the bottom layer.
    board.insertTrace(
        new Point[] {new IntPoint(100000, 20000), new IntPoint(100000, 80000)},
        1,
        BOTTOM_HALF_WIDTH,
        new int[] {signal},
        board.rules.nets.get(signal).getNetClass().getTraceClearanceClass(),
        FixedState.UNFIXED);
    PolylineTrace target =
        (PolylineTrace)
            board.getTraces().stream().filter(t -> t.containsNet(signal)).findFirst().get();

    RouterSettings settings = new SettingsMerger(new DefaultSettings()).merge();
    settings.setLayerCount(board.getLayerCount());
    settings.applyBoardSpecificOptimizations(board);
    AutorouteControl ctrl = new AutorouteControl(board, signal, settings);
    ctrl.traceHalfWidth[0] = TOP_HALF_WIDTH;
    ctrl.traceHalfWidth[1] = BOTTOM_HALF_WIDTH;
    ctrl.compensatedTraceHalfWidth[0] = TOP_HALF_WIDTH;
    ctrl.compensatedTraceHalfWidth[1] = BOTTOM_HALF_WIDTH;
    // From near the target trace along the bottom, a via, then along the top to the pin.
    PlannedConnection.Segment[] segments = {
      new PlannedConnection.Segment(
          new IntPoint[] {new IntPoint(90000, 50000), new IntPoint(40000, 50000)}, 1),
      new PlannedConnection.Segment(
          new IntPoint[] {new IntPoint(40000, 50000), (IntPoint) pin.getCenter()}, 0)
    };

    assertNotNull(FoundConnectionInserter.insertSegments(board, ctrl, pin, target, 0, 1, segments));

    for (Trace trace : board.getTraces()) {
      if (trace.containsNet(signal)) {
        int expected = trace.getLayer() == 1 ? BOTTOM_HALF_WIDTH : TOP_HALF_WIDTH;
        assertEquals(expected, trace.getHalfWidth(), "trace on layer " + trace.getLayer());
      }
    }
  }
}
