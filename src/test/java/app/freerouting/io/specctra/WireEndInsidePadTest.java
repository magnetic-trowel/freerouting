package app.freerouting.io.specctra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.items.Trace;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.board.trace.PolylineTrace;
import app.freerouting.drc.DesignRulesChecker;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Point;
import app.freerouting.io.BoardReadResult;
import app.freerouting.settings.GlobalSettings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The host tool connects a track to a pad wherever the track end lies inside the pad. The DSN
 * import joins such an end to the pad centre, where Freerouting connects items, so a fixed track
 * that ends off the centre (or beside it after KiCad's DSN rounding) is not imported as an open.
 */
class WireEndInsidePadTest {

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  /**
   * Pins R1-1 (index 0) at (0,0) and R1-2 at (secondPinX,0) of net A, R1-3 at (5000,3000) of net B.
   */
  private static String dsn(String secondPinX, String wirePath) {
    return dsn(secondPinX, "5000 3000", wire(wirePath));
  }

  private static String dsn(String secondPinX, String thirdPin, String wiring) {
    return """
        (pcb ends (resolution um 10) (unit um)
          (structure (layer F.Cu (type signal)) (layer B.Cu (type signal))
            (boundary (rect pcb -5000 -5000 20000 5000)) (via V) (rule (width 250) (clearance 200)))
          (placement (component R (place R1 0 0 front 0)))
          (library
            (image R (pin P 1 0 0) (pin P 2 %s 0) (pin P 3 %s))
            (padstack P (shape (rect F.Cu -500 -500 500 500)) (attach off))
            (padstack V (shape (circle F.Cu 600)) (shape (circle B.Cu 600)) (attach off)))
          (network (net A (pins R1-1 R1-2)) (net B (pins R1-3)))
          (wiring %s))
        """
        .formatted(secondPinX, thirdPin, wiring);
  }

  private static String wire(String path) {
    return "(wire (path F.Cu 250  " + path + ")(net A)(type route))";
  }

  private static RoutingBoard load(String text) {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), null, null, "ends");
    assertInstanceOf(BoardReadResult.Success.class, result);
    return (RoutingBoard) ((BoardReadResult.Success) result).board();
  }

  private static Pin pin(RoutingBoard board, int pinIndex) {
    return board.getPins().stream()
        .filter(p -> p.getPinIndex() == pinIndex)
        .findFirst()
        .orElseThrow();
  }

  private static int incompletes(RoutingBoard board) {
    DesignRulesChecker drc = new DesignRulesChecker(board, null);
    drc.calculateAllIncompletes();
    return drc.getIncompleteCount();
  }

  private static List<Point> corners(RoutingBoard board) {
    List<Point> result = new ArrayList<>();
    for (Trace trace : board.getTraces()) {
      result.addAll(List.of(((PolylineTrace) trace).polyline().corners()));
    }
    return result;
  }

  @Test
  void wireEndHalfMicronOffThePinCentreIsJoinedToIt() {
    // The last segment runs along x = 5001, so it does not pass through the centre at x = 5000.5.
    RoutingBoard board = load(dsn("5000.5", "0 0  0 -2000  5001 -2000  5001 0"));
    Pin second = pin(board, 1);

    assertEquals(0, incompletes(board));
    assertTrue(second.getNormalContacts().stream().anyMatch(Trace.class::isInstance));
    // The wiring keeps its own end; the join is a separate fixed trace.
    assertTrue(corners(board).contains(new IntPoint(50010, 0)));
    for (Trace trace : board.getTraces()) {
      assertEquals(FixedState.USER_FIXED, trace.getFixedState());
    }
  }

  @Test
  void wireEndingAtThePadEdgeIsJoinedToTheCentre() {
    // Ends 400 um left of the centre of R1-2, inside its 1000 um pad.
    RoutingBoard board = load(dsn("5000", "0 0  0 -2000  4600 -2000  4600 0"));

    assertEquals(0, incompletes(board));
    assertTrue(corners(board).contains(pin(board, 1).getCenter()));
  }

  @Test
  void wireAlreadyTooCloseToNeighbourIsStillJoined() {
    // R1-3 (net B) sits 300 um above the pad of R1-2. The wire ends 185 um from it, inside the
    // 200 um clearance, as a host with looser rules may route it; the join runs away from R1-3.
    RoutingBoard board = load(dsn("5000", "5000 1300", wire("0 0  0 -2000  5100 -2000  5100 490")));

    assertEquals(0, incompletes(board));
  }

  @Test
  void wiresMeetingBesideThePinCentreAreJoinedToItOnce() {
    // Both wires end 0.5 um beside the centre of R1-2 and meet there, as KiCad's rounding leaves
    // a chain of tracks that ends on a pad.
    String wiring = wire("0 0  0 -2000  5001 -2000  5001 0") + wire("5001 0  5001 1500  9000 1500");
    RoutingBoard board = load(dsn("5000.5", "5000 3000", wiring));

    assertEquals(0, incompletes(board));
    assertEquals(1, pin(board, 1).getNormalContacts().size());
  }

  @Test
  void joinThatWouldAddViolationIsNotMade() {
    // A net B track crosses the pad of R1-2 at x = 4700: the wire end at x = 5400 keeps 450 um
    // from it, but a join to the centre would come within 50 um, so the end stays open.
    String wiring =
        wire("9000 -2000  5400 -2000  5400 0")
            + "(wire (path F.Cu 250  4700 -3000  4700 2000)(net B)(type route))";
    RoutingBoard board = load(dsn("5000", "5000 3000", wiring));

    assertEquals(2, board.getTraces().size());
    assertTrue(pin(board, 1).getNormalContacts().isEmpty());
  }

  @Test
  void wireEndOutsideThePadStaysOpen() {
    RoutingBoard board = load(dsn("5000", "0 0  0 -2000  5600 -2000  5600 0"));

    assertEquals(1, board.getTraces().size());
    assertEquals(1, incompletes(board));
  }

  @Test
  void wireEndInsideForeignPadIsNotJoined() {
    // Net A ends inside R1-3, which belongs to net B.
    RoutingBoard board = load(dsn("5000", "0 0  0 2000  5200 2000  5200 3200"));

    assertEquals(1, board.getTraces().size());
    assertTrue(pin(board, 2).getNormalContacts().isEmpty());
  }

  @Test
  void wireThatAlreadyReachesThePadIsNotJoinedAgain() {
    // The wire crosses the centre of R1-2 and ends beyond it, still inside the pad.
    RoutingBoard board = load(dsn("5000", "0 0  5300 0"));

    assertEquals(1, board.getTraces().size());
    assertEquals(0, incompletes(board));
  }
}
