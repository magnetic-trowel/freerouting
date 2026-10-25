package app.freerouting.drc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.items.Trace;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.geometry.planar.Point;
import app.freerouting.geometry.planar.Polyline;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.io.specctra.DsnTestFixtures;
import app.freerouting.rules.NetClass;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NetRoutingLedgerTest {

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new app.freerouting.settings.GlobalSettings();
  }

  @Test
  void incompleteCountMatchesFullScanAcrossInsertAndRemove() {
    BoardReadResult result =
        DsnReader.readBoard(
            DsnTestFixtures.openResource("scoring-perfect-two-pin.dsn"), null, null);
    assertInstanceOf(BoardReadResult.Success.class, result);
    RoutingBoard board = (RoutingBoard) ((BoardReadResult.Success) result).board();

    assertMatchesFullScan(board);

    List<Pin> pins = new ArrayList<>(board.getPins());
    Point first = pins.get(0).getCenter();
    Point second = pins.get(1).getCenter();
    int netNumber = pins.get(0).getNetNumber(0);
    NetClass netClass = board.rules.nets.get(netNumber).getNetClass();
    board.insertTraceWithoutCleaning(
        new Polyline(first, second),
        0,
        netClass.getTraceHalfWidth(0),
        new int[] {netNumber},
        netClass.getTraceClearanceClass(),
        FixedState.UNFIXED);

    assertMatchesFullScan(board);
    assertEquals(0, board.routingLedger().incompleteCount());

    Trace trace = board.getTraces().iterator().next();
    board.removeItem(trace);

    assertMatchesFullScan(board);
    assertEquals(1, board.routingLedger().incompleteCount());
  }

  @Test
  void incompleteCountMatchesFullScanAcrossUndoAndRedo() {
    BoardReadResult result =
        DsnReader.readBoard(
            DsnTestFixtures.openResource("scoring-perfect-two-pin.dsn"), null, null);
    assertInstanceOf(BoardReadResult.Success.class, result);
    RoutingBoard board = (RoutingBoard) ((BoardReadResult.Success) result).board();

    List<Pin> pins = new ArrayList<>(board.getPins());
    int netNumber = pins.get(0).getNetNumber(0);
    NetClass netClass = board.rules.nets.get(netNumber).getNetClass();
    board.insertTraceWithoutCleaning(
        new Polyline(pins.get(0).getCenter(), pins.get(1).getCenter()),
        0,
        netClass.getTraceHalfWidth(0),
        new int[] {netNumber},
        netClass.getTraceClearanceClass(),
        FixedState.UNFIXED);
    assertEquals(0, board.routingLedger().incompleteCount());

    board.generateSnapshot();
    board.removeItem(board.getTraces().iterator().next());
    assertEquals(1, board.routingLedger().incompleteCount());

    board.undo(null);
    assertMatchesFullScan(board);
    assertEquals(0, board.routingLedger().incompleteCount());

    board.redo(null);
    assertMatchesFullScan(board);
    assertEquals(1, board.routingLedger().incompleteCount());
  }

  @Test
  void incompleteCountFollowsInPlaceNetReassignment() {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(TWO_NET_BOARD.getBytes(StandardCharsets.UTF_8)), null, null);
    assertInstanceOf(BoardReadResult.Success.class, result);
    RoutingBoard board = (RoutingBoard) ((BoardReadResult.Success) result).board();
    List<Pin> pins = new ArrayList<>(board.getPins());
    pins.sort(Comparator.comparing(Pin::componentName));
    Pin u1 = pins.get(0);
    Pin u2 = pins.get(1);
    int netA = u1.getNetNumber(0);
    NetClass netClass = board.rules.nets.get(netA).getNetClass();
    board.insertTraceWithoutCleaning(
        new Polyline(u1.getCenter(), u2.getCenter()),
        0,
        netClass.getTraceHalfWidth(0),
        new int[] {netA},
        netClass.getTraceClearanceClass(),
        FixedState.UNFIXED);
    // Net A is routed, net B (U3-U4) is open.
    assertMatchesFullScan(board);
    assertEquals(1, board.routingLedger().incompleteCount());

    // Swapping the nets of U2 and U4 (as the interactive pin swap does) opens net A as well.
    u2.swap(pins.get(3));

    assertMatchesFullScan(board);
    assertEquals(2, board.routingLedger().incompleteCount());
  }

  private static final String TWO_NET_BOARD =
      """
      (pcb ledger-two-nets.dsn
        (parser (string_quote ") (space_in_quoted_tokens on))
        (resolution um 10)
        (unit um)
        (structure
          (layer F.Cu (type signal) (property (index 0)))
          (boundary (path pcb 0  0 0  20000 0  20000 10000  0 10000  0 0))
          (rule (width 250) (clearance 200))
        )
        (placement
          (component PAD
            (place U1 2000 3000 front 0)
            (place U2 8000 3000 front 0)
            (place U3 2000 7000 front 0)
            (place U4 8000 7000 front 0)
          )
        )
        (library
          (image PAD (pin CirclePad_F_800_um 1 0 0))
          (padstack CirclePad_F_800_um (shape (circle F.Cu 800)) (attach off))
        )
        (network
          (net A (pins U1-1 U2-1))
          (net B (pins U3-1 U4-1))
          (class kicad_default A B)
        )
      )
      """;

  private static void assertMatchesFullScan(RoutingBoard board) {
    DesignRulesChecker checker = new DesignRulesChecker(board, null);
    checker.calculateAllIncompletes();
    assertEquals(checker.getIncompleteCount(), board.routingLedger().incompleteCount());
    assertEquals(checker.maxConnections, board.routingLedger().maximumConnections());
    assertEquals(checker.incompleteNetNumbers(), board.routingLedger().incompleteNetNumbers());
  }
}
