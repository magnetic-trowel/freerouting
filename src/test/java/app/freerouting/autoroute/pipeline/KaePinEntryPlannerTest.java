package app.freerouting.autoroute.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.autoroute.AutorouteAttemptState;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.kae.KaePinEntryTails;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.items.Trace;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.board.trace.PolylineTrace;
import app.freerouting.core.StoppableThread;
import app.freerouting.drc.DesignRulesChecker;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.rules.KaePinEntries;
import app.freerouting.settings.RouterSettings;
import app.freerouting.settings.SettingsMerger;
import app.freerouting.settings.sources.DefaultSettings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Phase 2 of the kae pin entries (table mode "plan"): the tail is planned before the search, the
 * search ends at its approach point, and the tail is ordinary routing.
 *
 * <p>Net SIG from a round pad A to a 1.0 x 0.6 mm pad B (long axis along x) whose table lists a
 * long-edge entry from +y and an axial entry from -x. In {@link #PAIR} two such pads face each
 * other 0.4 mm apart: no tail fits between them, and the straight stub stays.
 */
class KaePinEntryPlannerTest {

  private static final String DSN =
      """
      (pcb kaeplan (resolution um 10) (unit um)
        (structure (layer top (type signal))
          (boundary (rect pcb 0 0 30000 20000))
          (via V) (rule (width 250) (clearance 200)))
        (placement
          (component P (place A 17000 16000 front 0))
          (component Q (place B 15000 10000 front 0)))
        (library (image P (pin S 1 0 0)) (image Q (pin T 1 0 0))
          (padstack S (shape (circle top 800)))
          (padstack T (shape (rect top -500 -300 500 300)))
          (padstack V (shape (circle top 600)) (attach off)))
        (network (net SIG (pins A-1 B-1)) (class kc SIG (rule (width 250))))
        (kae_pin_entries (version 1) (mode plan)
          (pin "B" "1" (layer top) (centre 15000 10000) (frame 1 0 1000 600 15000 10000)
            (entry long (width 250) (approach 15000 10550) (end 15000 10000))
            (entry axial (width 250) (approach 14250 10000) (end 14800 10000)))))
      """;

  private static final String PAIR =
      """
      (pcb kaepair (resolution um 10) (unit um)
        (structure (layer top (type signal))
          (boundary (rect pcb 0 0 30000 20000))
          (via V) (rule (width 250) (clearance 200)))
        (placement (component Q (place B 15000 10000 front 0) (place C 16400 10000 front 0)))
        (library (image Q (pin T 1 0 0))
          (padstack T (shape (rect top -500 -300 500 300)))
          (padstack V (shape (circle top 600)) (attach off)))
        (network (net SIG (pins B-1 C-1)) (class kc SIG (rule (width 250))))
        (kae_pin_entries (version 1) (mode plan)
          (pin "B" "1" (layer top) (centre 15000 10000) (frame 1 0 1000 600 15000 10000)
            (entry axial (width 250) (approach 15750 10000) (end 15200 10000)))
          (pin "C" "1" (layer top) (centre 16400 10000) (frame 1 0 1000 600 16400 10000)
            (entry axial (width 250) (approach 15650 10000) (end 16200 10000)))))
      """;

  @Test
  void tailIsPlannedAndTheSearchEndsAtItsApproachPoint() {
    RoutingBoard board = readBoard(DSN);
    board.rules.kaePinEntries.setEnabled(true);
    assertEquals(
        AutorouteAttemptState.ROUTED, routeFrom(board, "A").state, "the connection is routed");
    Pin a = pin(board, "A");
    Pin b = pin(board, "B");
    assertTrue(a.getConnectedSet(net(board), true).contains(b));
    KaePinEntries.PinLayer pl = board.rules.kaePinEntries.pinLayers().get(0);
    KaePinEntries.Entry e = pl.entries.get(0);
    List<PolylineTrace> atB = tracesAt(board, b);
    assertEquals(1, atB.size(), "only the tail reaches the pad");
    PolylineTrace tail = atB.get(0);
    assertTrue(KaePinEntries.isTail(board.rules, tail.getId()));
    assertEquals(FixedState.UNFIXED, tail.getFixedState(), "the tail is routing");
    assertTrue(tail.firstCorner().equals(e.approach) || tail.lastCorner().equals(e.approach));
    assertTrue(new DesignRulesChecker(board, null).getAllClearanceViolations().isEmpty());
    String s = KaePinEntryTails.summary(board);
    assertTrue(s.contains("tails=1") && s.contains("raw=0"), s);
  }

  @Test
  void closePairKeepsTheStraightStub() {
    RoutingBoard board = readBoard(PAIR);
    board.rules.kaePinEntries.setEnabled(true);
    assertEquals(AutorouteAttemptState.ROUTED, routeFrom(board, "B").state);
    for (Trace t : board.getTraces()) {
      assertTrue(!KaePinEntries.isTail(board.rules, t.getId()), "no tail between close pads");
    }
    String s = KaePinEntryTails.summary(board);
    assertTrue(s.contains("direct=2") && s.contains("raw=0"), s);
    assertTrue(new DesignRulesChecker(board, null).getAllClearanceViolations().isEmpty());
  }

  @Test
  void sameInputSameBoard() {
    assertEquals(routed(DSN), routed(DSN));
  }

  private static String routed(String dsn) {
    RoutingBoard board = readBoard(dsn);
    board.rules.kaePinEntries.setEnabled(true);
    routeFrom(board, "A");
    StringBuilder sb = new StringBuilder();
    board.getTraces().stream()
        .sorted(java.util.Comparator.comparingInt(Trace::getId))
        .forEach(
            t -> {
              PolylineTrace p = (PolylineTrace) t;
              for (int i = 0; i < p.polyline().cornerCount(); i++) {
                sb.append(p.polyline().corner(i)).append(' ');
              }
              sb.append('\n');
            });
    return sb.toString();
  }

  private static app.freerouting.autoroute.AutorouteAttemptResult routeFrom(
      RoutingBoard board, String component) {
    RouterSettings settings = new SettingsMerger(new DefaultSettings()).merge();
    settings.setLayerCount(board.getLayerCount());
    settings.applyBoardSpecificOptimizations(board);
    BatchAutorouter router =
        new BatchAutorouter(
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
    return new AutorouteConnectionRouter(router)
        .route(pin(board, component), net(board), new TreeSet<>(), new HashMap<>(), 1);
  }

  private static List<PolylineTrace> tracesAt(RoutingBoard board, Pin pin) {
    return pin.getNormalContacts().stream()
        .filter(PolylineTrace.class::isInstance)
        .map(PolylineTrace.class::cast)
        .toList();
  }

  private static RoutingBoard readBoard(String dsn) {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(dsn.getBytes(StandardCharsets.UTF_8)), null, null, "test");
    assertInstanceOf(BoardReadResult.Success.class, result);
    return (RoutingBoard) ((BoardReadResult.Success) result).board();
  }

  private static int net(RoutingBoard board) {
    return board.rules.nets.get("SIG").iterator().next().netNumber;
  }

  private static Pin pin(RoutingBoard board, String component) {
    return board.getPins().stream()
        .filter(p -> component.equals(board.components.get(p.getComponentId()).name))
        .findFirst()
        .orElseThrow();
  }
}
