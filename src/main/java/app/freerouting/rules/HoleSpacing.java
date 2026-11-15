package app.freerouting.rules;

import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.model.items.DrillItem;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.searchtree.SearchTreeObject;
import app.freerouting.core.library.Padstack;
import app.freerouting.datastructures.ShapeTree;
import app.freerouting.geometry.planar.FloatPoint;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * KiCad's hole-to-hole rule. KiCad keeps no copper clearance within a net: two vias of one net, or
 * a via beside a pad of its net, may sit at any distance. What it does keep, between drills of any
 * nets, is the board's hole-to-hole minimum ({@link BoardRules#getMinHoleToHole()}), measured edge
 * to edge. The router checks it where it places or moves a via and the judge reports it ({@link
 * app.freerouting.board.model.items.Via#clearanceViolations()}), so both use the same rule. Only
 * pairs with a via are checked: two pins' holes are the footprint's design, not the router's.
 */
public final class HoleSpacing {

  /** Drilled item and its edge-to-edge clearance gap. */
  public static class TooCloseItem {
    public final DrillItem item;
    public final double gap;

    /** Creates a new TooCloseItem. */
    public TooCloseItem(DrillItem item, double gap) {
      this.item = item;
      this.gap = gap;
    }
  }

  private HoleSpacing() {}

  /** Whether a padstack has a drilled hole: it spans layers or is a bare hole. */
  public static boolean hasHole(Padstack padstack) {
    return padstack != null
        && (padstack.holeOnly || padstack.fromLayer() < padstack.toLayer())
        && padstack.getDrillRadius() > 0;
  }

  /**
   * The edge-to-edge gap between the holes of radii {@code radius1} and {@code radius2} at the two
   * centres, negative where they overlap.
   */
  public static double gap(FloatPoint center1, double radius1, FloatPoint center2, double radius2) {
    return center1.distance(center2) - radius1 - radius2;
  }

  /**
   * Whether a via of {@code padstack} at {@code location} keeps the hole-to-hole minimum from every
   * drilled item on the board.
   */
  public static boolean clear(BasicBoard board, Padstack padstack, Point location) {
    return clear(board, padstack, location, null, null);
  }

  /**
   * Whether a via of {@code padstack} at {@code location} keeps the hole-to-hole minimum from every
   * drilled item on the board except {@code self}.
   */
  public static boolean clear(BasicBoard board, Padstack padstack, Point location, Item self) {
    return clear(board, padstack, location, self, null);
  }

  /**
   * Whether a via of {@code padstack} at {@code location} keeps the hole-to-hole minimum from every
   * drilled item on the board except {@code self} (the via itself, when it moves) and {@code
   * ignore}.
   */
  public static boolean clear(
      BasicBoard board, Padstack padstack, Point location, Item self, Collection<Item> ignore) {
    if (board == null || board.rules == null) {
      return true;
    }
    int minimum = board.rules.getMinHoleToHole();
    if (minimum <= 0 || !hasHole(padstack)) {
      return true;
    }
    return firstTooClose(
            board,
            location.toFloat(),
            padstack.getDrillRadius(),
            padstack.fromLayer(),
            padstack.toLayer(),
            minimum,
            self,
            ignore)
        == null;
  }

  /**
   * The drilled items whose holes are closer than {@code minimum} to a hole of {@code radius} at
   * {@code center} through layers {@code fromLayer} to {@code toLayer}, with their gaps, in the
   * default tree's order.
   */
  public static List<TooCloseItem> tooClose(
      BasicBoard board,
      FloatPoint center,
      double radius,
      int fromLayer,
      int toLayer,
      double minimum,
      Item self) {
    List<TooCloseItem> result = new ArrayList<>();
    visit(
        board,
        center,
        radius,
        fromLayer,
        toLayer,
        minimum,
        self,
        null,
        (item, gap) -> {
          result.add(new TooCloseItem(item, gap));
          return true;
        });
    return result;
  }

  /**
   * The first drilled item found whose hole is closer than {@code minimum}, or null. Only whether
   * there is one matters to its callers, so unlike {@link #visit} it asks the search tree for the
   * drilled items alone: the exact overlap test then runs on them only, and not on every trace and
   * plane under the box. The answer is the one {@link #visit} gives.
   */
  private static DrillItem firstTooClose(
      BasicBoard board,
      FloatPoint center,
      double radius,
      int fromLayer,
      int toLayer,
      double minimum,
      Item self,
      Collection<Item> ignore) {
    IntBox box = searchBox(board, center, radius, minimum);
    for (SearchTreeObject object :
        board
            .searchTreeManager
            .getDefaultTree()
            .overlappingObjects(
                box,
                -1,
                obj ->
                    obj instanceof DrillItem other
                        && other != self
                        && other.lastLayer() >= fromLayer
                        && other.firstLayer() <= toLayer
                        && hasHole(other.getPadstack()))) {
      DrillItem other = (DrillItem) object;
      if (ignore != null && ignore.contains(other)) {
        continue;
      }
      double gap =
          gap(center, radius, other.getCenter().toFloat(), other.getPadstack().getDrillRadius());
      if (gap < minimum) {
        return other;
      }
    }
    return null;
  }

  /** The box around {@code center} that holds every hole closer than {@code minimum} to it. */
  private static IntBox searchBox(
      BasicBoard board, FloatPoint center, double radius, double minimum) {
    double maxOtherDrill =
        (board != null && board.library != null && board.library.padstacks != null)
            ? board.library.padstacks.getMaxDrillRadius()
            : 0.0;
    int reach = (int) Math.ceil(radius + minimum + maxOtherDrill) + 1;
    IntPoint c = center.round();
    return new IntBox(c.x - reach, c.y - reach, c.x + reach, c.y + reach);
  }

  private static void visit(
      BasicBoard board,
      FloatPoint center,
      double radius,
      int fromLayer,
      int toLayer,
      double minimum,
      Item self,
      Collection<Item> ignore,
      BiPredicate<DrillItem, Double> report) {
    IntBox box = searchBox(board, center, radius, minimum);
    List<ShapeTree.TreeEntry> entries = new ArrayList<>();
    board.searchTreeManager.getDefaultTree().overlappingTreeEntries(box, -1, entries);
    Set<DrillItem> reported = null;
    for (ShapeTree.TreeEntry entry : entries) {
      if (!(entry.object instanceof DrillItem other)
          || other == self
          || other.lastLayer() < fromLayer
          || other.firstLayer() > toLayer) {
        continue;
      }
      Padstack otherPadstack = other.getPadstack();
      if (otherPadstack == null || !hasHole(otherPadstack)) {
        continue;
      }
      if (ignore != null && ignore.contains(other)) {
        continue;
      }
      double gap = gap(center, radius, other.getCenter().toFloat(), otherPadstack.getDrillRadius());
      if (gap >= minimum) {
        continue;
      }
      if (reported != null && reported.contains(other)) {
        continue;
      }
      if (reported == null) {
        reported = new HashSet<>();
      }
      reported.add(other);
      if (!report.test(other, gap)) {
        return;
      }
    }
  }
}
