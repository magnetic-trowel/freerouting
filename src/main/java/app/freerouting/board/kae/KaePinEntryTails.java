package app.freerouting.board.kae;

import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.board.trace.PolylineTrace;
import app.freerouting.geometry.planar.Direction;
import app.freerouting.geometry.planar.FloatPoint;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Line;
import app.freerouting.geometry.planar.Point;
import app.freerouting.geometry.planar.Polyline;
import app.freerouting.logger.FRLogger;
import app.freerouting.rules.KaePinEntries;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rule D tails at pins listed in the DSN's {@code kae_pin_entries} table ({@link KaePinEntries}).
 *
 * <p>Freerouting ends every trace at the pin centre. A trace that ends at a listed pin and does not
 * already arrive along one of the pin's planned entries is cut where it first comes within reach of
 * the pad (a circle around the pad's frame grown by a stub), joined to the entry's approach point
 * (straight, or around the grown frame's corners), and ended by a fixed tail approach, end, centre:
 * a {@code SHOVE_FIXED} trace, like the exit stubs of upstream's pin exit restrictions. Pull-tight
 * treats the approach point as a fixed end corner and tightens up to it; the shover cannot move the
 * tail. The end stays at the pin centre, which Freerouting's contact model needs ({@code
 * DrillItem.getNormalContacts}); the run from the planned end to the centre lies inside the land.
 *
 * <p>Runs after a connection is inserted ({@code FoundConnectionInserter}) and from {@code
 * PolylineTrace.pullTight}. Every piece is checked with {@code checkPolylineTrace}; when no entry
 * is legal the trace keeps its own end. Candidates are tried in table order, then by length, so the
 * result is deterministic. Only active with {@code router.kae_pin_entries}.
 */
public final class KaePinEntryTails {

  /** Cosine of the largest angle (10 degrees) a direct approach may make with a planned entry. */
  private static final double COS_ENTRY = Math.cos(Math.toRadians(10.0));

  /** A last piece may be this much shorter than the planned approach distance. */
  private static final double DEPTH_SLACK = 0.98;

  private static final AtomicLong TAILS = new AtomicLong();
  private static final AtomicLong REUSED = new AtomicLong();
  private static final AtomicLong FAILED = new AtomicLong();
  private static final AtomicLong SHORT = new AtomicLong();

  private KaePinEntryTails() {}

  /** Whether the table is in use on this board. */
  public static boolean active(BasicBoard board) {
    return board != null && KaePinEntries.active(board.rules) != null;
  }

  /**
   * After a connection is inserted: corrects the ends of the net's traces at the connection's start
   * and target items when they are listed pins.
   */
  public static void afterInsert(BasicBoard board, Item... items) {
    KaePinEntries table = board == null ? null : KaePinEntries.active(board.rules);
    if (table == null) {
      return;
    }
    for (Item item : items) {
      if (item instanceof Pin pin) {
        correctAtPin(pin);
      }
    }
  }

  /** Corrects every trace ending at the pin's centre. Returns the number of tails made. */
  static int correctAtPin(Pin pin) {
    int made = 0;
    // a change normalizes the trace and may split or merge traces: re-read the contacts
    for (int pass = 0; pass < 8; ++pass) {
      boolean changed = false;
      for (PolylineTrace trace : tracesEndingAt(pin)) {
        boolean atStart = trace.firstCorner().equals(pin.getCenter());
        if (correctEnd(trace, atStart, false)) {
          changed = true;
          ++made;
          break;
        }
      }
      if (!changed) {
        break;
      }
    }
    return made;
  }

  /** From pull-tight: corrects the trace's ends at listed pins. Returns true if it changed. */
  public static boolean correctEnds(PolylineTrace trace) {
    if (!active(trace.board)) {
      return false;
    }
    if (correctEnd(trace, true, true)) {
      return true;
    }
    return trace.isOnTheBoard() && correctEnd(trace, false, true);
  }

  private static List<PolylineTrace> tracesEndingAt(Pin pin) {
    List<PolylineTrace> out = new ArrayList<>();
    Point c = pin.getCenter();
    for (Item it : pin.getNormalContacts()) {
      if (it instanceof PolylineTrace t
          && !t.isShoveFixed()
          && t.cornerCount() >= 2
          && (t.firstCorner().equals(c) || t.lastCorner().equals(c))) {
        out.add(t);
      }
    }
    out.sort(Comparator.comparingInt(Item::getId));
    return out;
  }

  private static Pin pinAt(PolylineTrace trace, boolean atStart) {
    Point end = atStart ? trace.firstCorner() : trace.lastCorner();
    List<Pin> pins = new ArrayList<>();
    for (Item it : atStart ? trace.getStartContacts() : trace.getEndContacts()) {
      if (it instanceof Pin p && p.getCenter().equals(end)) {
        pins.add(p);
      }
    }
    pins.sort(Comparator.comparingInt(Item::getId));
    return pins.isEmpty() ? null : pins.get(0);
  }

  /** Whether a trace whose last piece runs from {@code p} to the pin centre is a rule D entry. */
  static boolean accepted(KaePinEntries.PinLayer pl, int halfWidth, FloatPoint p) {
    double cx = pl.centre.x;
    double cy = pl.centre.y;
    double dx = p.x - cx;
    double dy = p.y - cy;
    double d = Math.hypot(dx, dy);
    if (d <= 0) {
      return false;
    }
    for (KaePinEntries.Entry e : pl.entries) {
      if (e.neckHalfWidth > 0 && halfWidth > e.neckHalfWidth) {
        continue;
      }
      double ax = e.approach.x - cx;
      double ay = e.approach.y - cy;
      if (pl.radial) {
        if (d >= DEPTH_SLACK * Math.hypot(ax, ay)) {
          return true;
        }
        continue;
      }
      double vx = e.approach.x - (double) e.end.x;
      double vy = e.approach.y - (double) e.end.y;
      double vl = Math.hypot(vx, vy);
      if (vl <= 0) {
        continue;
      }
      vx /= vl;
      vy /= vl;
      double cos = (dx * vx + dy * vy) / d;
      if (cos >= COS_ENTRY && d * cos >= DEPTH_SLACK * (ax * vx + ay * vy)) {
        return true;
      }
    }
    return false;
  }

  /** One candidate: the new corners from the approach point out to the cut point, and its tail. */
  private record Candidate(
      double cost, int entryIndex, int order, IntPoint[] path, KaePinEntries.Entry entry) {}

  /**
   * Corrects one end of a trace at a listed pin. Returns true if the trace was changed. With {@code
   * useCache}, an end whose correction already failed with the same geometry is not tried again.
   */
  static boolean correctEnd(PolylineTrace trace, boolean atStart, boolean useCache) {
    BasicBoard board = trace.board;
    KaePinEntries table = KaePinEntries.active(board.rules);
    if (table == null || trace.isShoveFixed() || trace.cornerCount() < 2) {
      return false;
    }
    Pin pin = pinAt(trace, atStart);
    if (pin == null) {
      return false;
    }
    KaePinEntries.PinLayer pl = table.get(pin.getId(), trace.getLayer());
    if (pl == null) {
      return false;
    }
    Polyline poly = atStart ? trace.polyline() : trace.polyline().reverse();
    int n = poly.cornerCount();
    FloatPoint[] c = new FloatPoint[n];
    for (int i = 0; i < n; i++) {
      c[i] = poly.cornerApprox(i);
    }
    int hw = trace.getHalfWidth();
    if (accepted(pl, hw, c[1])) {
      return false;
    }
    long cacheKey = ((long) trace.getId() << 1) | (atStart ? 0 : 1);
    int geometry = geometryHash(c, hw);
    if (useCache && Integer.valueOf(geometry).equals(table.failedTails.get(cacheKey))) {
      return false;
    }
    List<Candidate> cands = new ArrayList<>();
    boolean anyReach = false;
    for (int k = 0; k < pl.entries.size(); k++) {
      KaePinEntries.Entry e = pl.entries.get(k);
      anyReach |= candidates(pl, e, k, c, hw, cands);
    }
    if (!anyReach) {
      SHORT.incrementAndGet();
    }
    cands.sort(
        Comparator.comparingDouble(Candidate::cost)
            .thenComparingInt(Candidate::entryIndex)
            .thenComparingInt(Candidate::order));
    int[] nets = trace.netNumbers;
    int cl = trace.clearanceClassIndex();
    int layer = trace.getLayer();
    for (Candidate cand : cands) {
      KaePinEntries.Entry e = cand.entry;
      int tailHw = e.neckHalfWidth > 0 && e.neckHalfWidth < hw ? e.neckHalfWidth : hw;
      Polyline tail = tailPolyline(e, pl.centre);
      if (tail == null) {
        continue;
      }
      Polyline joined = joinedPolyline(poly, cand.path, c);
      if (joined == null) {
        continue;
      }
      // the joined trace is checked as it will be: its corner at the cut point is where the new
      // last line meets the trace's own line, which may lie off the rounded cut point
      if (!followsPath(joined, cand.path, c)
          || !board.checkPolylineTrace(joined, layer, hw, nets, cl)
          || !board.checkPolylineTrace(tail, layer, tailHw, nets, cl)) {
        continue;
      }
      PolylineTrace existing = existingTail(pin, layer, e.approach);
      trace.change(atStart ? joined : joined.reverse());
      if (existing == null) {
        board.insertTrace(tail, layer, tailHw, nets, cl, FixedState.SHOVE_FIXED);
        TAILS.incrementAndGet();
      } else {
        REUSED.incrementAndGet();
      }
      table.failedTails.remove(cacheKey);
      return true;
    }
    FAILED.incrementAndGet();
    table.failedTails.put(cacheKey, geometry);
    return false;
  }

  private static int geometryHash(FloatPoint[] c, int hw) {
    int h = hw;
    for (FloatPoint p : c) {
      h = 31 * h + Long.hashCode(Math.round(p.x));
      h = 31 * h + Long.hashCode(Math.round(p.y));
    }
    return h;
  }

  /** A fixed tail of this pin on this layer that ends at the approach point, or null. */
  private static PolylineTrace existingTail(Pin pin, int layer, IntPoint approach) {
    List<PolylineTrace> found = new ArrayList<>();
    for (Item it : pin.getNormalContacts()) {
      if (it instanceof PolylineTrace t
          && t.getLayer() == layer
          && t.getFixedState() == FixedState.SHOVE_FIXED
          && (t.firstCorner().equals(approach) || t.lastCorner().equals(approach))) {
        found.add(t);
      }
    }
    found.sort(Comparator.comparingInt(Item::getId));
    return found.isEmpty() ? null : found.get(0);
  }

  /** The tail approach, end, centre (the end left out when it is the centre or on the line). */
  static Polyline tailPolyline(KaePinEntries.Entry e, IntPoint centre) {
    IntPoint a = e.approach;
    IntPoint m = e.end;
    if (a.equals(centre)) {
      return null;
    }
    // the planned end within a few units of the line approach-centre (or of the centre) is
    // dropped: a corner that close only leaves a sliver of a segment
    double cross =
        (double) (m.x - a.x) * (centre.y - a.y) - (double) (m.y - a.y) * (centre.x - a.x);
    double ac = a.toFloat().distance(centre.toFloat());
    boolean straight =
        m.equals(a)
            || m.toFloat().distance(centre.toFloat()) <= CORNER_TOL
            || Math.abs(cross) <= CORNER_TOL * ac;
    Point[] pts = straight ? new Point[] {a, centre} : new Point[] {a, m, centre};
    Polyline p = new Polyline(pts);
    return p.lines.length >= 3 ? p : null;
  }

  /**
   * The trace's new polyline, pin side first: the path from the approach point to the cut point,
   * then the trace's own lines from the segment the cut point lies on. Null when it degenerates.
   */
  private static Polyline joinedPolyline(Polyline poly, IntPoint[] path, FloatPoint[] c) {
    // path: approach, corners..., cut point (on the trace, rounded)
    int seg = cutSegment(path, c);
    if (seg < 0) {
      return null;
    }
    List<Line> lines = new ArrayList<>();
    Direction first = Direction.getInstance(path[0], path[1]);
    if (first == null) {
      return null;
    }
    lines.add(Line.getInstance(path[0], first.turn45Degree(2)));
    for (int i = 0; i + 1 < path.length; i++) {
      if (!path[i].equals(path[i + 1])) {
        lines.add(new Line(path[i], path[i + 1]));
      }
    }
    // segment seg runs from corner seg to corner seg + 1: it is line seg + 1 of the polyline
    for (int j = seg + 1; j < poly.lines.length; j++) {
      lines.add(poly.lines[j]);
    }
    Polyline result = withoutZeroLengthLines(new Polyline(lines.toArray(new Line[0])));
    return result != null && result.lines.length >= 3 ? result : null;
  }

  /**
   * The polyline without lines between two equal corners (several lines through one point, as a
   * join next to a via can leave): they would be written as zero-length wires.
   */
  static Polyline withoutZeroLengthLines(Polyline p) {
    Polyline cur = p;
    for (int guard = 0; guard < 64 && cur.lines.length >= 3; guard++) {
      int drop = -1;
      for (int i = 1; i < cur.cornerCount(); i++) {
        if (cur.corner(i).equals(cur.corner(i - 1))) {
          drop = i;
          break;
        }
      }
      if (drop < 0) {
        return cur;
      }
      // line `drop` runs from corner drop - 1 to corner drop; an end line stays (it fixes the end)
      int line = drop;
      if (line >= cur.lines.length - 1) {
        return cur;
      }
      Line[] fewer = new Line[cur.lines.length - 1];
      System.arraycopy(cur.lines, 0, fewer, 0, line);
      System.arraycopy(cur.lines, line + 1, fewer, line, fewer.length - line);
      Polyline next = new Polyline(fewer);
      if (next.lines.length >= cur.lines.length) {
        return cur;
      }
      cur = next;
    }
    return cur;
  }

  /**
   * Whether a joined polyline (pin side first) has the path's corners where the path puts them:
   * each within a few board units, so a nearly parallel join cannot move the cut corner away.
   */
  private static boolean followsPath(Polyline joined, IntPoint[] path, FloatPoint[] c) {
    int k = 0;
    for (int i = 0; i < path.length; i++) {
      if (i > 0 && path[i].equals(path[i - 1])) {
        continue;
      }
      if (k >= joined.cornerCount()) {
        return false;
      }
      FloatPoint q = joined.cornerApprox(k++);
      if (q.distance(path[i].toFloat()) > CORNER_TOL) {
        return false;
      }
    }
    return true;
  }

  /** Board units a joined corner may lie from the path's (rounding of the cut point). */
  private static final double CORNER_TOL = 4.0;

  /** The polyline segment (pin side first) the cut point of a path lies on. */
  private static int cutSegment(IntPoint[] path, FloatPoint[] c) {
    IntPoint q = path[path.length - 1];
    int best = -1;
    double bestD = Double.MAX_VALUE;
    for (int i = 0; i + 1 < c.length; i++) {
      double d = segmentDistance(q.x, q.y, c[i], c[i + 1]);
      if (d < bestD - 1e-9) {
        bestD = d;
        best = i;
      }
    }
    return bestD <= 2.0 ? best : -1;
  }

  private static double segmentDistance(double px, double py, FloatPoint a, FloatPoint b) {
    double dx = b.x - a.x;
    double dy = b.y - a.y;
    double l2 = dx * dx + dy * dy;
    double t = l2 <= 0 ? 0 : Math.max(0, Math.min(1, ((px - a.x) * dx + (py - a.y) * dy) / l2));
    return Math.hypot(px - (a.x + t * dx), py - (a.y + t * dy));
  }

  /**
   * Adds the candidate paths of one entry: from its approach point straight to the cut point, or
   * around one or two corners of the pad's frame grown by the stub. A path whose copper would touch
   * the pad's frame (grown by the half width) is left out. Returns false when the trace never
   * leaves the reach circle (no cut point).
   */
  private static boolean candidates(
      KaePinEntries.PinLayer pl,
      KaePinEntries.Entry e,
      int entryIndex,
      FloatPoint[] c,
      int hw,
      List<Candidate> out) {
    double ux = pl.ux;
    double uy = pl.uy;
    double vx = -uy;
    double vy = ux;
    double cx = pl.frameX;
    double cy = pl.frameY;
    double halfL = pl.frameLong / 2;
    double halfS = pl.frameShort / 2;
    double au = (e.approach.x - cx) * ux + (e.approach.y - cy) * uy;
    double av = (e.approach.x - cx) * vx + (e.approach.y - cy) * vy;
    double stub = Math.max(Math.max(Math.abs(au) - halfL, Math.abs(av) - halfS), 2.0 * hw);
    double gu = halfL + stub;
    double gv = halfS + stub;
    double reach = Math.hypot(gu, gv) + hw;
    // the cut point: where the trace first leaves the reach circle
    FloatPoint q = null;
    for (int i = 0; i + 1 < c.length && q == null; i++) {
      double d1 = Math.hypot(c[i + 1].x - cx, c[i + 1].y - cy);
      if (d1 >= reach) {
        q = circleExit(c[i], c[i + 1], cx, cy, reach);
      }
    }
    if (q == null) {
      return false;
    }
    IntPoint qi = q.round();
    IntPoint a = e.approach;
    double[][] corners = {{gu, gv}, {-gu, gv}, {-gu, -gv}, {gu, -gv}};
    IntPoint[] k = new IntPoint[4];
    for (int i = 0; i < 4; i++) {
      k[i] =
          new FloatPoint(
                  cx + corners[i][0] * ux + corners[i][1] * vx,
                  cy + corners[i][0] * uy + corners[i][1] * vy)
              .round();
    }
    double ru = halfL + hw;
    double rv = halfS + hw;
    double tailCost =
        e.approach.toFloat().distance(e.end.toFloat())
            + e.end.toFloat().distance(pl.centre.toFloat());
    // in pin-side-first order: approach, corners..., cut point
    List<IntPoint[]> paths = new ArrayList<>();
    paths.add(new IntPoint[] {a, qi});
    for (int i = 0; i < 4; i++) {
      paths.add(new IntPoint[] {a, k[i], qi});
    }
    for (int i = 0; i < 4; i++) {
      paths.add(new IntPoint[] {a, k[i], k[(i + 1) % 4], qi});
      paths.add(new IntPoint[] {a, k[(i + 1) % 4], k[i], qi});
    }
    int order = 0;
    for (IntPoint[] path : paths) {
      ++order;
      boolean clear = true;
      double len = 0;
      for (int i = 0; i + 1 < path.length && clear; i++) {
        clear = !hitsFrame(path[i], path[i + 1], cx, cy, ux, uy, vx, vy, ru, rv);
        len += path[i].toFloat().distance(path[i + 1].toFloat());
      }
      if (!clear || !turnsAtMost90(path, e)) {
        continue;
      }
      out.add(new Candidate(len + tailCost, entryIndex, order, path, e));
    }
    return true;
  }

  /**
   * The tail must not turn back on the approach: the corner at the approach point is at most 90.
   */
  private static boolean turnsAtMost90(IntPoint[] path, KaePinEntries.Entry e) {
    // path[1] -> path[0] is the arrival; path[0] -> end is the tail
    double ix = path[0].x - (double) path[1].x;
    double iy = path[0].y - (double) path[1].y;
    double tx = e.end.x - (double) path[0].x;
    double ty = e.end.y - (double) path[0].y;
    return ix * tx + iy * ty >= -1e-6 * Math.hypot(ix, iy) * Math.hypot(tx, ty);
  }

  private static FloatPoint circleExit(FloatPoint a, FloatPoint b, double cx, double cy, double r) {
    double dx = b.x - a.x;
    double dy = b.y - a.y;
    double fx = a.x - cx;
    double fy = a.y - cy;
    double aa = dx * dx + dy * dy;
    if (aa <= 0) {
      return null;
    }
    double bb = 2 * (fx * dx + fy * dy);
    double cc = fx * fx + fy * fy - r * r;
    double disc = bb * bb - 4 * aa * cc;
    if (disc < 0) {
      return b;
    }
    double t = (-bb + Math.sqrt(disc)) / (2 * aa);
    t = Math.max(0, Math.min(1, t));
    return new FloatPoint(a.x + t * dx, a.y + t * dy);
  }

  /** Whether the segment p-q meets the frame rectangle |u| <= ru, |v| <= rv (Liang-Barsky). */
  private static boolean hitsFrame(
      IntPoint p,
      IntPoint q,
      double cx,
      double cy,
      double ux,
      double uy,
      double vx,
      double vy,
      double ru,
      double rv) {
    double pu = (p.x - cx) * ux + (p.y - cy) * uy;
    double pv = (p.x - cx) * vx + (p.y - cy) * vy;
    double du = (q.x - cx) * ux + (q.y - cy) * uy - pu;
    double dv = (q.x - cx) * vx + (q.y - cy) * vy - pv;
    double t0 = 0;
    double t1 = 1;
    double[] pp = {-du, du, -dv, dv};
    double[] qq = {pu + ru, ru - pu, pv + rv, rv - pv};
    for (int i = 0; i < 4; i++) {
      if (pp[i] == 0) {
        if (qq[i] < 0) {
          return false;
        }
      } else {
        double t = qq[i] / pp[i];
        if (pp[i] < 0) {
          t0 = Math.max(t0, t);
        } else {
          t1 = Math.min(t1, t);
        }
        if (t0 > t1) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Counts the ends at listed pins on the board as it is: fixed tails, direct ends that already
   * follow an entry, and raw ends that do not. Logged as {@code kae_pin_entries: ...}, with the
   * tails inserted, reused and failed during the run.
   */
  public static String summary(BasicBoard board) {
    KaePinEntries table = board == null ? null : KaePinEntries.active(board.rules);
    if (table == null) {
      return null;
    }
    int tails = 0;
    int bent = 0;
    int direct = 0;
    int raw = 0;
    for (Pin pin : board.getPins()) {
      for (int layer = pin.firstLayer(); layer <= pin.lastLayer(); layer++) {
        KaePinEntries.PinLayer pl = table.get(pin.getId(), layer);
        if (pl == null) {
          continue;
        }
        Point centre = pin.getCenter();
        for (Item it : pin.getNormalContacts()) {
          if (!(it instanceof PolylineTrace t) || t.getLayer() != layer || t.cornerCount() < 2) {
            continue;
          }
          boolean atStart = t.firstCorner().equals(centre);
          if (!atStart && !t.lastCorner().equals(centre)) {
            continue;
          }
          Point other = atStart ? t.lastCorner() : t.firstCorner();
          KaePinEntries.Entry tailOf = null;
          for (KaePinEntries.Entry e : pl.entries) {
            if (other.equals(e.approach)) {
              tailOf = e;
              break;
            }
          }
          if (t.getFixedState() == FixedState.SHOVE_FIXED && tailOf != null) {
            ++tails;
            if (!tailOf.end.equals(pl.centre)) {
              ++bent;
            }
          } else {
            Polyline p = atStart ? t.polyline() : t.polyline().reverse();
            if (accepted(pl, t.getHalfWidth(), p.cornerApprox(1))) {
              ++direct;
            } else {
              ++raw;
            }
          }
        }
      }
    }
    String s =
        "kae_pin_entries: pin_layers="
            + table.pinLayers().size()
            + " tails="
            + tails
            + " tails_via_end="
            + bent
            + " direct="
            + direct
            + " raw="
            + raw
            + " inserted="
            + TAILS.get()
            + " reused="
            + REUSED.get()
            + " failed="
            + FAILED.get()
            + " short="
            + SHORT.get()
            + " class_floors="
            + table.classFloorCount();
    FRLogger.info(s);
    return s;
  }
}
