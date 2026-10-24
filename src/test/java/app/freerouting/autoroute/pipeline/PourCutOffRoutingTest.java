package app.freerouting.autoroute.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.items.Trace;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.core.StoppableThread;
import app.freerouting.drc.DesignRulesChecker;
import app.freerouting.drc.PlaneFillConnectivity;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Point;
import app.freerouting.geometry.planar.Polyline;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.settings.RouterSettings;
import app.freerouting.settings.SettingsMerger;
import app.freerouting.settings.sources.DefaultSettings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Tests the routing of copper that foreign tracks cut off from the fill of its pour. */
class PourCutOffRoutingTest {

  /**
   * Two GND pins on a GND pour on the top layer, and a SIG track on the top layer from one board
   * edge to the other between them. The bottom layer is free.
   */
  private static final String DSN =
      """
      (pcb pourcut (resolution um 1)
        (structure (layer top (type signal)) (layer bottom (type signal))
          (boundary (rect pcb 0 0 20000 10000))
          (plane GND (polygon top 0 0 0 20000 0 20000 10000 0 10000))
          (via V) (rule (width 400) (clearance 200)))
        (placement (component C (place A 3000 5000 front 0) (place B 17000 5000 front 0)
          (place S1 10000 500 front 0) (place S2 10000 9500 front 0)))
        (library (image C (pin P 1 0 0))
          (padstack P (shape (circle top 800)) (shape (circle bottom 800)))
          (padstack V (shape (circle top 600)) (shape (circle bottom 600)) (attach off)))
        (network (net GND (pins A-1 B-1)) (net SIG (pins S1-1 S2-1))))
      """;

  private static RoutingBoard board() {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(DSN.getBytes(StandardCharsets.UTF_8)), null, null, "pourcut");
    assertInstanceOf(BoardReadResult.Success.class, result);
    return (RoutingBoard) ((BoardReadResult.Success) result).board();
  }

  private static int net(RoutingBoard board, String name) {
    return board.rules.nets.get(name).iterator().next().netNumber;
  }

  private static Trace cutThePour(RoutingBoard board) {
    Trace cut =
        board.insertTraceWithoutCleaning(
            new Polyline(new Point[] {new IntPoint(10000, 500), new IntPoint(10000, 9500)}),
            0,
            200,
            new int[] {net(board, "SIG")},
            1,
            FixedState.UNFIXED);
    assertNotNull(cut);
    return cut;
  }

  private static BatchAutorouter router(RoutingBoard board) {
    RouterSettings settings = new SettingsMerger(new DefaultSettings()).merge();
    settings.setLayerCount(board.getLayerCount());
    settings.applyBoardSpecificOptimizations(board);
    return new BatchAutorouter(
        new StoppableThread() {
          @Override
          protected void threadAction() {}
        },
        board,
        settings,
        true,
        true,
        settings.getStartRipupCosts(),
        500);
  }

  @Test
  void cutOffPinIsRoutedAndItsConnectionStays() {
    RoutingBoard board = board();
    int gnd = net(board, "GND");
    cutThePour(board);
    assertEquals(1, PlaneFillConnectivity.analyze(board, gnd).cutOffGroups.size());

    assertEquals(1, router(board).connectPourCutOffs());

    assertTrue(PlaneFillConnectivity.analyze(board, gnd).cutOffGroups.isEmpty());
    assertTrue(new DesignRulesChecker(board, null).getAllClearanceViolations().isEmpty());
    // The new connection closes a loop through the pour polygon, but not through its fill.
    for (Trace trace : board.getTraces()) {
      if (trace.containsNet(gnd)) {
        assertFalse(trace.isCycle(), trace.toString());
      }
    }
    Pin[] pins = board.getPins().stream().filter(p -> p.containsNet(gnd)).toArray(Pin[]::new);
    assertEquals(2, pins.length);
    assertTrue(pins[0].getConnectedSet(gnd, true).contains(pins[1]));
  }

  @Test
  void continuousPourNeedsNoConnection() {
    RoutingBoard board = board();

    assertEquals(0, router(board).connectPourCutOffs());
    assertTrue(board.getTraces().isEmpty());
  }

  @Test
  void connectionThroughAContinuousFillIsACycle() {
    RoutingBoard board = board();
    Trace cut = cutThePour(board);
    router(board).connectPourCutOffs();
    Trace gndTrace =
        board.getTraces().stream()
            .filter(t -> t.containsNet(net(board, "GND")))
            .findFirst()
            .orElseThrow();

    board.removeItem(cut);

    assertTrue(gndTrace.isCycle());
  }
}
