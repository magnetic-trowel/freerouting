package app.freerouting.board.kae;

import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.model.items.DrillItem;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.board.trace.PolylineTrace;
import app.freerouting.geometry.planar.FloatPoint;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Point;
import app.freerouting.geometry.planar.Polyline;
import app.freerouting.rules.KaePinEntries;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Phase 2 of kicad-auto-everything's pin entries: tails planned before the search.
 *
 * <p>Before a connection is searched, each end pin of its airline that is listed in the table
 * (single-layer pads only) and has no copper of its own on the layer gets the planned tail of the
 * entry facing the other end: approach point, planned end, centre, as an ordinary trace. While the
 * tail is on the board the pin's trace connection shape is its approach point ({@link
 * #plannedApproach}), so the maze search ends there: the target door is a room that contains the
 * approach point, and the stub corridor is already checked (the tail is inserted only when it is
 * legal at the trace's width and clearance). The tail is routing, not a fixed obstacle: ripping the
 * connection rips it, neighbours price and rip it like any trace, and the shover may move it.
 * Pull-tight leaves it alone and it is never combined with the trace that meets it.
 *
 * <p>A pin pair whose approach points would lie in or on the other pad gets no tails: the straight
 * centre-to-centre stub stays (square when the pads face each other). When the search fails with
 * planned tails, they are removed and the caller searches once more without them, Freerouting's
 * normal way; both outcomes are counted.
 */
public final class KaePinEntryPlanner {

  private static final AtomicLong PLANNED = new AtomicLong();
  private static final AtomicLong NO_FIT = new AtomicLong();
  private static final AtomicLong PAIR_CLOSE = new AtomicLong();
  private static final AtomicLong FALLBACK = new AtomicLong();

  private KaePinEntryPlanner() {}

  /** Whether planning is on for this board. */
  public static boolean active(BasicBoard board) {
    KaePinEntries t = board == null ? null : KaePinEntries.active(board.rules);
    return t != null && t.isPlanned();
  }

  /**
   * Plans the tails of a connection from {@code startSet} to {@code destSet} of net {@code netNo}
   * at half width {@code halfWidth[layer]} and clearance class {@code clearanceClass}. Returns the
   * tails inserted (empty when none); each is added to the set of its pin.
   */
  public static List<PolylineTrace> plan(
      BasicBoard board,
      int netNo,
      Collection<Item> startSet,
      Collection<Item> destSet,
      int[] halfWidth,
      int clearanceClass) {
    List<PolylineTrace> out = new ArrayList<>();
    if (!active(board)) {
      return out;
    }
    KaePinEntries table = KaePinEntries.active(board.rules);
    DrillItem[] pair = nearestPair(startSet, destSet);
    if (pair == null) {
      return out;
    }
    Pin[] pins = new Pin[2];
    KaePinEntries.PinLayer[] pls = new KaePinEntries.PinLayer[2];
    for (int k = 0; k < 2; k++) {
      if (pair[k] instanceof Pin p && p.firstLayer() == p.lastLayer()) {
        KaePinEntries.PinLayer pl = table.get(p.getId(), p.firstLayer());
        if (pl != null && !hasCopperAt(p, p.firstLayer())) {
          pins[k] = p;
          pls[k] = pl;
        }
      }
    }
    for (int k = 0; k < 2; k++) {
      if (pins[k] == null) {
        continue;
      }
      FloatPoint aim = pair[1 - k].getCenter().toFloat();
      KaePinEntries.PinLayer other = pins[1 - k] != null ? pls[1 - k] : null;
      PolylineTrace tail =
          insertTail(board, table, pins[k], pls[k], aim, other, netNo, halfWidth, clearanceClass);
      if (tail != null) {
        out.add(tail);
        // the tail belongs to its pin's side of the connection: a start item where the pin is one
        // (else the search would take it for a destination and stop at once)
        (k == 0 ? startSet : destSet).add(tail);
      }
    }
    return out;
  }

  /** Removes the planned tails that are still dangling (the search did not use them). */
  public static void unplan(BasicBoard board, List<PolylineTrace> tails) {
    if (tails.isEmpty()) {
      return;
    }
    FALLBACK.incrementAndGet();
    for (PolylineTrace t : tails) {
      if (t.isOnTheBoard() && t.isTail()) {
        board.removeItem(t);
      }
    }
  }

  private static PolylineTrace insertTail(
      BasicBoard board,
      KaePinEntries table,
      Pin pin,
      KaePinEntries.PinLayer pl,
      FloatPoint aim,
      KaePinEntries.PinLayer other,
      int netNo,
      int[] halfWidth,
      int clearanceClass) {
    int layer = pl.layer;
    int hw = halfWidth[layer];
    FloatPoint c = pl.centre.toFloat();
    List<Integer> order = new ArrayList<>();
    for (int i = 0; i < pl.entries.size(); i++) {
      order.add(i);
    }
    order.sort(
        Comparator.comparingDouble(
                (Integer i) -> pl.entries.get(i).approach.toFloat().distance(aim))
            .thenComparingInt(i -> i));
    boolean closePair = false;
    for (int i : order) {
      KaePinEntries.Entry e = pl.entries.get(i);
      // the tail faces the other end: never an approach from behind the pad
      double vx = e.approach.x - (double) e.end.x;
      double vy = e.approach.y - (double) e.end.y;
      if ((aim.x - c.x) * vx + (aim.y - c.y) * vy <= 0) {
        continue;
      }
      if (other != null && nearFrame(other, e.approach, hw)) {
        closePair = true;
        continue;
      }
      int tailHw = e.neckHalfWidth > 0 && e.neckHalfWidth < hw ? e.neckHalfWidth : hw;
      Polyline tail = KaePinEntryTails.tailPolyline(e, pl.centre);
      if (tail == null) {
        continue;
      }
      int[] nets = new int[] {netNo};
      if (!board.checkPolylineTrace(tail, layer, tailHw, nets, clearanceClass)) {
        continue;
      }
      board.insertTrace(tail, layer, tailHw, nets, clearanceClass, FixedState.UNFIXED);
      PolylineTrace t = traceBetween(pin, layer, pl.centre, e.approach);
      if (t == null) {
        continue;
      }
      table.registerTail(pin.getId(), layer, t.getId(), e.approach);
      PLANNED.incrementAndGet();
      return t;
    }
    if (closePair) {
      PAIR_CLOSE.incrementAndGet();
    } else {
      NO_FIT.incrementAndGet();
    }
    return null;
  }

  /**
   * Whether a point lies on or within the half width of a pad's frame (the other pad of a pair).
   */
  private static boolean nearFrame(KaePinEntries.PinLayer pl, IntPoint p, int hw) {
    double dx = p.x - pl.frameX;
    double dy = p.y - pl.frameY;
    double u = Math.abs(dx * pl.ux + dy * pl.uy);
    double v = Math.abs(-dx * pl.uy + dy * pl.ux);
    return u <= pl.frameLong / 2 + 2.0 * hw && v <= pl.frameShort / 2 + 2.0 * hw;
  }

  private static DrillItem[] nearestPair(Collection<Item> from, Collection<Item> to) {
    List<DrillItem> a = drillItems(from);
    List<DrillItem> b = drillItems(to);
    DrillItem[] best = null;
    double bestD = Double.MAX_VALUE;
    for (DrillItem x : a) {
      FloatPoint px = x.getCenter().toFloat();
      for (DrillItem y : b) {
        double d = px.distanceSquare(y.getCenter().toFloat());
        if (d < bestD) {
          bestD = d;
          best = new DrillItem[] {x, y};
        }
      }
    }
    return best;
  }

  private static List<DrillItem> drillItems(Collection<Item> items) {
    List<DrillItem> out = new ArrayList<>();
    for (Item it : items) {
      if (it instanceof DrillItem d) {
        out.add(d);
      }
    }
    out.sort(Comparator.comparingInt(Item::getId));
    return out;
  }

  private static boolean hasCopperAt(Pin pin, int layer) {
    for (Item it : pin.getNormalContacts()) {
      if (it.isOnLayer(layer) && !(it instanceof Pin)) {
        return true;
      }
    }
    return false;
  }

  /** The trace of the pin on the layer from its centre to the point (either orientation). */
  static PolylineTrace traceBetween(Pin pin, int layer, Point centre, Point a) {
    List<PolylineTrace> found = new ArrayList<>();
    for (Item it : pin.getNormalContacts()) {
      if (it instanceof PolylineTrace t
          && t.getLayer() == layer
          && ((t.firstCorner().equals(centre) && t.lastCorner().equals(a))
              || (t.lastCorner().equals(centre) && t.firstCorner().equals(a)))) {
        found.add(t);
      }
    }
    found.sort(Comparator.comparingInt(Item::getId));
    return found.isEmpty() ? null : found.get(0);
  }

  /**
   * The approach point the search must end at for this pin on this layer: that of its registered
   * tail when the tail is still on the board, from the centre to the approach point; else null (the
   * pin's centre, Freerouting's own end). A tail that has gone or changed is forgotten.
   */
  public static IntPoint plannedApproach(Pin pin, int layer) {
    BasicBoard board = pin.board;
    if (!active(board)) {
      return null;
    }
    KaePinEntries table = KaePinEntries.active(board.rules);
    int tailId = table.tailOf(pin.getId(), layer);
    if (tailId < 0) {
      return null;
    }
    IntPoint a = table.tailApproach(tailId);
    Point centre = pin.getCenter();
    for (Item it : pin.getNormalContacts()) {
      if (it instanceof PolylineTrace t
          && t.getId() == tailId
          && t.getLayer() == layer
          && ((t.firstCorner().equals(centre) && t.lastCorner().equals(a))
              || (t.lastCorner().equals(centre) && t.firstCorner().equals(a)))) {
        return a;
      }
    }
    table.forgetTail(pin.getId(), layer);
    return null;
  }

  /** The counts of the run, for the summary line. */
  static String counts() {
    return " planned="
        + PLANNED.get()
        + " plan_no_fit="
        + NO_FIT.get()
        + " plan_pair_close="
        + PAIR_CLOSE.get()
        + " plan_fallback="
        + FALLBACK.get();
  }
}
