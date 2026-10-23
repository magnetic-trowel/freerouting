package app.freerouting.autoroute.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.autoroute.maze.AutorouteControl;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.items.Trace;
import app.freerouting.drc.DesignRulesChecker;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.settings.RouterSettings;
import app.freerouting.settings.SettingsMerger;
import app.freerouting.settings.sources.DefaultSettings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Automatic neckdown narrows a trace that does not fit at its class width: into a pad narrower than
 * the trace, and through the micro-neckdown retry at 3/4, 3/5 and 1/2 of the class width. It stays
 * off while automatic neckdown is off, also in the fanout pre-pass (issue #973), never goes below
 * {@code min_trace_width_um} or {@code neck_width_um}, and is unchanged when neither is set.
 *
 * <p>Synthetic boards, one layer, 0.2 mm clearance, net SIG at 0.5 mm. In {@link #GAP_DSN} SIG runs
 * straight from pad A to pad B through a gap between two pads of net O that reach almost to the
 * board edge, so the trace can neither shove nor spring around them; a gap {@code g} passes a trace
 * of width {@code w} when {@code g >= w + 0.4 mm}. In {@link #PAD_ENTRY_DSN} SIG ends in a 0.2 mm
 * wide pad between two pads of net O 0.3 mm away, which only a trace up to 0.4 mm wide can enter.
 */
class MicroNeckdownMinimumWidthTest {

  private static final String GAP_DSN =
      """
      (pcb microneck (resolution um 10) (unit um)
        (structure (layer top (type signal))
          (boundary (rect pcb 0 0 20000 10000))
          (via V) (rule (width 500) (clearance 200)))
        (placement
          (component P (place A 2000 5000 front 0) (place B 18000 5000 front 0))
          (component W (place WALL 10000 5000 front 0)))
        (library (image P (pin S 1 0 0)) (image W (pin T 1 0 %1$d) (pin T 2 0 -%1$d))
          (padstack S (shape (circle top 800)))
          (padstack T (shape (rect top -500 -%2$d 500 %2$d)))
          (padstack V (shape (circle top 600)) (attach off)))
        (network (net SIG (pins A-1 B-1)) (net O (pins WALL-1 WALL-2))))
      """;

  private static final String PAD_ENTRY_DSN =
      """
      (pcb padentry (resolution um 10) (unit um)
        (structure (layer top (type signal))
          (boundary (rect pcb 0 0 20000 10000))
          (via V) (rule (width 500) (clearance 200)))
        (placement
          (component P (place A 2000 5000 front 0))
          (component Q (place B 15000 5000 front 0)
            (place N1 15000 5500 front 0) (place N2 15000 4500 front 0)))
        (library (image P (pin S 1 0 0)) (image Q (pin T 1 0 0))
          (padstack S (shape (circle top 800)))
          (padstack T (shape (rect top -500 -100 500 100)))
          (padstack V (shape (circle top 600)) (attach off)))
        (network (net SIG (pins A-1 B-1)) (net O (pins N1-1 N2-1))))
      """;

  /** Class half width of SIG in board units (0.5 mm at 10 units per um). */
  private static final int SIGNAL_HALF_WIDTH = 2500;

  /** Passes 3/4 of the SIG width (0.375 mm needs 0.775 mm) but not the full width. */
  private static final int WIDE_GAP_UM = 836;

  /** Passes 0.32 mm (needs 0.72 mm) but not 3/4 of the SIG width (0.375 mm needs 0.775 mm). */
  private static final int MID_GAP_UM = 752;

  /** Passes 1/2 of the SIG width (0.25 mm needs 0.65 mm) but not 3/5 (0.3 mm needs 0.7 mm). */
  private static final int NARROW_GAP_UM = 676;

  @Test
  void neckdownOffNarrowsNothingAlsoInFanout() {
    RoutingBoard board = gapBoard(WIDE_GAP_UM);

    assertNull(insertSignal(board, settings(false, 0)), "only a narrowed trace fits the gap");
    assertNoSignalTraceNarrowerThan(board, SIGNAL_HALF_WIDTH);
  }

  @Test
  void withoutMinimumNeckdownNarrowsAsBefore() {
    RoutingBoard wide = gapBoard(WIDE_GAP_UM);
    assertNotNull(insertSignal(wide, settings(true, 0)));
    // Full width up to the gap, 3/4 of it from there.
    assertEquals(1875, narrowestSignalHalfWidth(wide));

    RoutingBoard narrow = gapBoard(NARROW_GAP_UM);
    assertNotNull(insertSignal(narrow, settings(true, 0)));
    assertEquals(1250, narrowestSignalHalfWidth(narrow));

    RoutingBoard padEntry = padEntryBoard();
    assertNotNull(insertSignal(padEntry, settings(true, 0)));
    // Into the 0.2 mm pad at the pad's width less one unit on each side.
    assertEquals(999, narrowestSignalHalfWidth(padEntry));
  }

  @Test
  void neckdownNeverGoesBelowTheMinimumTraceWidth() {
    RoutingBoard narrow = gapBoard(NARROW_GAP_UM);
    narrow.rules.setMinAllowedTraceHalfWidth(1500);
    assertNull(insertSignal(narrow, settings(true, 0)), "1/2 width would fit, below 0.3 mm");
    assertNoSignalTraceNarrowerThan(narrow, 1500);

    RoutingBoard wide = gapBoard(WIDE_GAP_UM);
    wide.rules.setMinAllowedTraceHalfWidth(1500);
    assertNotNull(insertSignal(wide, settings(true, 0)));
    assertEquals(1875, narrowestSignalHalfWidth(wide));
    assertTrue(new DesignRulesChecker(wide, null).getAllClearanceViolations().isEmpty());

    RoutingBoard padEntry = padEntryBoard();
    padEntry.rules.setMinAllowedTraceHalfWidth(1500);
    assertNotNull(insertSignal(padEntry, settings(true, 0)));
    assertEquals(1500, narrowestSignalHalfWidth(padEntry), "pad-entry neck raised to 0.3 mm");
    assertTrue(new DesignRulesChecker(padEntry, null).getAllClearanceViolations().isEmpty());
  }

  @Test
  void minimumWidthIsTriedWhenNothingWiderFits() {
    RoutingBoard board = gapBoard(MID_GAP_UM);
    board.rules.setMinAllowedTraceHalfWidth(1600);

    assertNotNull(insertSignal(board, settings(true, 0)));
    assertEquals(1600, narrowestSignalHalfWidth(board));
    assertTrue(new DesignRulesChecker(board, null).getAllClearanceViolations().isEmpty());
  }

  @Test
  void neckWidthAlsoSetsTheMinimum() {
    RoutingBoard narrow = gapBoard(NARROW_GAP_UM);
    assertNull(insertSignal(narrow, settings(true, 300)), "1/2 width would fit, below 0.3 mm");
    assertNoSignalTraceNarrowerThan(narrow, 1500);

    RoutingBoard padEntry = padEntryBoard();
    assertNotNull(insertSignal(padEntry, settings(true, 300)));
    assertEquals(1500, narrowestSignalHalfWidth(padEntry), "pad-entry neck raised to 0.3 mm");
  }

  private static RouterSettings settings(boolean automaticNeckdown, double neckWidthUm) {
    RouterSettings settings = new SettingsMerger(new DefaultSettings()).merge();
    settings.setAutomaticNeckdown(automaticNeckdown);
    settings.setNeckWidthUm(neckWidthUm);
    return settings;
  }

  /** Reads the gap board with the given gap between the O pads in um. */
  private static RoutingBoard gapBoard(int gapUm) {
    // The O pads run from the gap to 100 um inside the board edge.
    int padHalfHeight = (4900 - gapUm / 2) / 2;
    int padOffset = gapUm / 2 + padHalfHeight;
    return readBoard(GAP_DSN.formatted(padOffset, padHalfHeight));
  }

  private static RoutingBoard padEntryBoard() {
    return readBoard(PAD_ENTRY_DSN);
  }

  private static RoutingBoard readBoard(String dsn) {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(dsn.getBytes(StandardCharsets.UTF_8)), null, null, "test");
    assertInstanceOf(BoardReadResult.Success.class, result);
    RoutingBoard board = (RoutingBoard) ((BoardReadResult.Success) result).board();
    assertEquals(SIGNAL_HALF_WIDTH, board.rules.getTraceHalfWidth(netNumber(board, "SIG"), 0));
    return board;
  }

  /** Inserts SIG straight from A to B the way the fanout pre-pass inserts a found connection. */
  private static FoundConnectionInserter insertSignal(RoutingBoard board, RouterSettings settings) {
    settings.setLayerCount(board.getLayerCount());
    settings.applyBoardSpecificOptimizations(board);
    AutorouteControl ctrl = new AutorouteControl(board, netNumber(board, "SIG"), settings);
    ctrl.isFanout = true;
    Pin a = pin(board, "A");
    Pin b = pin(board, "B");
    PlannedConnection.Segment segment =
        new PlannedConnection.Segment(
            new IntPoint[] {(IntPoint) a.getCenter(), (IntPoint) b.getCenter()}, 0);
    return FoundConnectionInserter.insertSegments(
        board, ctrl, b, a, 0, 0, new PlannedConnection.Segment[] {segment});
  }

  private static int narrowestSignalHalfWidth(RoutingBoard board) {
    return signalTraces(board).stream().mapToInt(Trace::getHalfWidth).min().orElse(0);
  }

  private static void assertNoSignalTraceNarrowerThan(RoutingBoard board, int halfWidth) {
    List<Integer> narrower =
        signalTraces(board).stream().map(Trace::getHalfWidth).filter(w -> w < halfWidth).toList();
    assertTrue(narrower.isEmpty(), () -> "SIG traces narrower than " + halfWidth + ": " + narrower);
  }

  private static List<Trace> signalTraces(RoutingBoard board) {
    int signal = netNumber(board, "SIG");
    return board.getTraces().stream().filter(t -> t.containsNet(signal)).toList();
  }

  private static int netNumber(RoutingBoard board, String name) {
    return board.rules.nets.get(name).iterator().next().netNumber;
  }

  private static Pin pin(RoutingBoard board, String component) {
    return board.getPins().stream()
        .filter(p -> component.equals(board.components.get(p.getComponentId()).name))
        .findFirst()
        .orElseThrow();
  }
}
