package app.freerouting.board.kae;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.autoroute.maze.AutorouteControl;
import app.freerouting.autoroute.path.FoundConnectionInserter;
import app.freerouting.autoroute.path.PlannedConnection;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.items.Trace;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.board.trace.PolylineTrace;
import app.freerouting.drc.DesignRulesChecker;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Point;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.rules.KaePinEntries;
import app.freerouting.settings.RouterSettings;
import app.freerouting.settings.SettingsMerger;
import app.freerouting.settings.sources.DefaultSettings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The DSN's {@code kae_pin_entries} table and the rule D tails made from it.
 *
 * <p>Synthetic board, one layer, 0.2 mm clearance, net SIG at 0.25 mm from a round pad A to a 1.0 x
 * 0.6 mm pad B (long axis along x, rotated by the placement in some tests). The table gives B a
 * long-edge entry from +y (approach 0.25 mm outside the edge, end at the centre) and an axial entry
 * from -x (approach 0.25 mm outside the short edge, end on the centre line 0.2 mm from the centre).
 * A is oblique to both, so the inserted trace B-A gets a tail.
 */
class KaePinEntryTailsTest {

  private static final String DSN =
      """
      (pcb kaeentry (resolution um 10) (unit um)
        (structure (layer top (type signal))
          (boundary (rect pcb 0 0 30000 20000))
          (via V) (rule (width 250) (clearance 200)))
        (placement
          (component P (place A %1$s front 0))
          (component Q (place B 15000 10000 front %2$s)))
        (library (image P (pin S 1 0 0)) (image Q (pin T 1 0 0))
          (padstack S (shape (circle top 800)))
          (padstack T (shape (rect top -500 -300 500 300)))
          (padstack V (shape (circle top 600)) (attach off)))
        (network (net SIG (pins A-1 B-1)) (class kc SIG (rule (width 250))))
        (kae_pin_entries (version 1)
          (pin "B" "1" (layer top) (centre 15000 10000) (frame %3$s 1000 600 15000 10000)
            (entry long (width 250) (approach %4$s) (end 15000 10000))
            (entry axial (width 250) (approach %5$s) (end %6$s)))
          (class_floor kc (width 300))
          (pin "NOPE" "1" (layer top) (centre 0 0) (frame 1 0 1 1 0 0)
            (entry long (width 250) (approach 1 1) (end 0 0)))))
      """;

  private static String dsn(double rotDeg, String placeA) {
    double r = Math.toRadians(rotDeg);
    double ux = Math.cos(r);
    double uy = Math.sin(r);
    double vx = -uy;
    double vy = ux;
    String approachLong = xy(15000 + 550 * vx, 10000 + 550 * vy);
    String approachAxial = xy(15000 - 750 * ux, 10000 - 750 * uy);
    String endAxial = xy(15000 - 200 * ux, 10000 - 200 * uy);
    return DSN.formatted(
        placeA, fmt(rotDeg), fmt(ux) + " " + fmt(uy), approachLong, approachAxial, endAxial);
  }

  private static String xy(double x, double y) {
    return fmt(x) + " " + fmt(y);
  }

  private static String fmt(double v) {
    return String.format(java.util.Locale.ROOT, "%.4f", v);
  }

  @Test
  void tableIsReadInBoardUnitsAndOffUntilTheSettingIsOn() {
    RoutingBoard board = readBoard(dsn(0, "12000 16000"));
    KaePinEntries t = board.rules.kaePinEntries;
    assertNotNull(t);
    assertFalse(t.isEnabled());
    assertNull(KaePinEntries.active(board.rules));
    assertEquals(1, t.pinLayers().size(), "the unknown pin is skipped");
    KaePinEntries.PinLayer pl = t.pinLayers().get(0);
    assertEquals(pin(board, "B").getId(), pl.pinId);
    assertEquals(new IntPoint(150000, 100000), pl.centre);
    assertEquals(10000, pl.frameLong, 1e-6);
    assertEquals(6000, pl.frameShort, 1e-6);
    assertEquals(2, pl.entries.size());
    assertEquals("long", pl.entries.get(0).kind);
    assertEquals(new IntPoint(150000, 105500), pl.entries.get(0).approach);
    assertEquals(1250, pl.entries.get(0).halfWidth);
    assertEquals(new IntPoint(148000, 100000), pl.entries.get(1).end);
    assertEquals(1500, t.classFloor("kc", 0));
    // off: no floor, upstream exit restrictions unchanged
    assertEquals(0, KaePinEntries.floorHalfWidth(board.rules, netNumber(board), 0));
    t.setEnabled(true);
    assertEquals(1500, KaePinEntries.floorHalfWidth(board.rules, netNumber(board), 0));
    assertTrue(pin(board, "B").getTraceExitRestrictions(0).isEmpty());
    AutorouteControl ctrl = new AutorouteControl(board, netNumber(board), settings(board));
    assertEquals(1500, ctrl.minNeckdownHalfWidth(0), "the class floor bounds every neck-down");
  }

  @Test
  void settingOffLeavesTheInsertedTraceAsBefore() {
    RoutingBoard board = readBoard(dsn(0, "12000 16000"));
    insertSignal(board);
    assertTrue(fixedTails(board).isEmpty());
    assertTrue(signalTraces(board).stream().anyMatch(t -> endsAt(t, pin(board, "B").getCenter())));
  }

  @Test
  void obliqueEndGetsFixedTailThatPullTightKeeps() {
    for (double rot : new double[] {0, 30}) {
      RoutingBoard board = readBoard(dsn(rot, "17000 16000"));
      board.rules.kaePinEntries.setEnabled(true);
      KaePinEntries.PinLayer pl = board.rules.kaePinEntries.pinLayers().get(0);
      insertSignal(board);
      List<PolylineTrace> tails = fixedTails(board);
      assertEquals(1, tails.size(), "one tail at B (rotation " + rot + ")");
      PolylineTrace tail = tails.get(0);
      Point centre = pin(board, "B").getCenter();
      KaePinEntries.Entry e = pl.entries.get(0);
      assertTrue(endsAt(tail, centre) && endsAt(tail, e.approach), "tail approach -> centre");
      // the routed trace now ends at the approach point, not at the pin
      List<Trace> free =
          signalTraces(board).stream()
              .filter(t -> t.getFixedState() == FixedState.UNFIXED)
              .toList();
      assertTrue(free.stream().noneMatch(t -> endsAt(t, centre)));
      assertTrue(free.stream().anyMatch(t -> endsAt(t, e.approach)));
      assertTrue(new DesignRulesChecker(board, null).getAllClearanceViolations().isEmpty());
      // pull-tight (45 degree mode, the DSN default) tightens up to the approach point and leaves
      // the off-grid tail of the rotated pad as it is
      String before = corners(tail);
      for (int pass = 0; pass < 3; pass++) {
        for (Trace t : List.copyOf(signalTraces(board))) {
          if (t instanceof PolylineTrace p && p.isOnTheBoard()) {
            p.pullTight(true, 500, null);
          }
        }
      }
      List<PolylineTrace> after = fixedTails(board);
      assertEquals(1, after.size());
      assertEquals(before, corners(after.get(0)));
      assertTrue(
          signalTraces(board).stream()
              .filter(t -> t.getFixedState() == FixedState.UNFIXED)
              .anyMatch(t -> endsAt(t, e.approach)));
      assertTrue(new DesignRulesChecker(board, null).getAllClearanceViolations().isEmpty());
      String summary = KaePinEntryTails.summary(board);
      assertTrue(summary.contains("tails=1") && summary.contains("raw=0"), summary);
    }
  }

  @Test
  void directEntryAlongThePlanIsKept() {
    // A straight above B: the trace already arrives along the long-edge entry
    RoutingBoard board = readBoard(dsn(0, "15000 16000"));
    board.rules.kaePinEntries.setEnabled(true);
    insertSignal(board);
    assertTrue(fixedTails(board).isEmpty());
    String summary = KaePinEntryTails.summary(board);
    assertTrue(summary.contains("direct=1") && summary.contains("raw=0"), summary);
  }

  @Test
  void axialTailRunsThroughThePlannedEnd() {
    // A far to the left: the axial entry, whose end lies 0.2 mm off the centre on the centre line
    RoutingBoard board = readBoard(dsn(0, "3000 12500"));
    board.rules.kaePinEntries.setEnabled(true);
    insertSignal(board);
    List<PolylineTrace> tails = fixedTails(board);
    assertEquals(1, tails.size());
    KaePinEntries.Entry axial = board.rules.kaePinEntries.pinLayers().get(0).entries.get(1);
    assertTrue(endsAt(tails.get(0), axial.approach));
    assertTrue(new DesignRulesChecker(board, null).getAllClearanceViolations().isEmpty());
  }

  @Test
  void sameInputSameBoard() {
    String a = routedTraces(dsn(30, "17000 16000"));
    String b = routedTraces(dsn(30, "17000 16000"));
    assertEquals(a, b);
  }

  private static String routedTraces(String dsn) {
    RoutingBoard board = readBoard(dsn);
    board.rules.kaePinEntries.setEnabled(true);
    insertSignal(board);
    StringBuilder sb = new StringBuilder();
    signalTraces(board).stream()
        .sorted(java.util.Comparator.comparingInt(Trace::getId))
        .forEach(t -> sb.append(t.getFixedState()).append(corners((PolylineTrace) t)).append('\n'));
    return sb.toString();
  }

  private static boolean endsAt(Trace t, Point p) {
    PolylineTrace pt = (PolylineTrace) t;
    return pt.firstCorner().equals(p) || pt.lastCorner().equals(p);
  }

  private static String corners(PolylineTrace t) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < t.polyline().cornerCount(); i++) {
      sb.append(t.polyline().corner(i)).append(' ');
    }
    return sb.toString();
  }

  private static RouterSettings settings(RoutingBoard board) {
    RouterSettings settings = new SettingsMerger(new DefaultSettings()).merge();
    settings.setAutomaticNeckdown(false);
    settings.setLayerCount(board.getLayerCount());
    settings.applyBoardSpecificOptimizations(board);
    return settings;
  }

  private static void insertSignal(RoutingBoard board) {
    AutorouteControl ctrl = new AutorouteControl(board, netNumber(board), settings(board));
    Pin a = pin(board, "A");
    Pin b = pin(board, "B");
    PlannedConnection.Segment segment =
        new PlannedConnection.Segment(
            new IntPoint[] {(IntPoint) b.getCenter(), (IntPoint) a.getCenter()}, 0);
    assertNotNull(
        FoundConnectionInserter.insertSegments(
            board, ctrl, a, b, 0, 0, new PlannedConnection.Segment[] {segment}));
  }

  private static List<PolylineTrace> fixedTails(RoutingBoard board) {
    return signalTraces(board).stream()
        .filter(t -> t.getFixedState() == FixedState.SHOVE_FIXED)
        .map(t -> (PolylineTrace) t)
        .toList();
  }

  private static RoutingBoard readBoard(String dsn) {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(dsn.getBytes(StandardCharsets.UTF_8)), null, null, "test");
    assertInstanceOf(BoardReadResult.Success.class, result);
    return (RoutingBoard) ((BoardReadResult.Success) result).board();
  }

  private static List<Trace> signalTraces(RoutingBoard board) {
    int signal = netNumber(board);
    return board.getTraces().stream().filter(t -> t.containsNet(signal)).toList();
  }

  private static int netNumber(RoutingBoard board) {
    return board.rules.nets.get("SIG").iterator().next().netNumber;
  }

  private static Pin pin(RoutingBoard board, String component) {
    return board.getPins().stream()
        .filter(p -> component.equals(board.components.get(p.getComponentId()).name))
        .findFirst()
        .orElseThrow();
  }
}
