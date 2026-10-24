package app.freerouting.drc;

import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.model.items.ConductionArea;
import app.freerouting.board.model.items.DrillItem;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.items.Trace;
import app.freerouting.geometry.planar.FloatPoint;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Connectivity of a net through the actual fill of its pours instead of the pour polygons.
 *
 * <p>The board's connectivity ({@link Item#getConnectedSet(int)}) treats a pour (a {@link
 * ConductionArea} without a component) as one conductor wherever its polygon reaches. Its copper is
 * the polygon minus the clearances of foreign copper ({@link ConductionArea#getConnectingFill}), so
 * foreign tracks routed through a pour can cut a pad or via off from the copper around it. This
 * class groups the net's items by the copper that joins them without a pour, joins the groups whose
 * drill centres or trace ends lie in the same fill island of a pour of the net (on any layer, so a
 * through-hole pin joins the islands of every layer it is on), and reports the groups the board
 * counts as connected to the largest group although no copper and no fill joins them.
 *
 * <p>The fill is the one that connects ({@link ConductionArea#getConnectingFill}): it keeps no
 * clearance to other nets' pours, whose priorities the design file does not give, so a pad that
 * another pour cuts off is not reported. Same-net pads join the fill through thermal spokes, as in
 * the drawn fill; a small zone joined solidly to its pad, such as a teardrop, can therefore be
 * reported although the host tool connects it.
 *
 * <p>The cost is that of the zone-island check of the DRC: one fill and one island decomposition
 * per pour. It is too slow to run at every connectivity query of the router.
 */
public final class PlaneFillConnectivity {

  /** The largest fill-connected group of a net and the groups cut off from it. */
  public static final class Result {
    /** The items of the largest group (most items, then lowest item id), empty if none. */
    public final Set<Item> mainGroup;

    /**
     * Groups the board counts as connected to the main group through a pour polygon, but which no
     * copper and no fill island joins to it; ordered by their lowest item id.
     */
    public final List<Set<Item>> cutOffGroups;

    Result(Set<Item> mainGroup, List<Set<Item>> cutOffGroups) {
      this.mainGroup = mainGroup;
      this.cutOffGroups = cutOffGroups;
    }

    /** Returns the number of items in all cut-off groups. */
    public int cutOffItemCount() {
      return cutOffGroups.stream().mapToInt(Set::size).sum();
    }
  }

  private static final Result EMPTY = new Result(new TreeSet<>(), List.of());

  private PlaneFillConnectivity() {}

  /** Returns true for a pour: a conduction area that does not belong to a component. */
  static boolean isPour(Item item) {
    return item instanceof ConductionArea && item.getComponentId() <= 0;
  }

  /** Returns the numbers of the nets that own a pour, in ascending order. */
  public static Set<Integer> pourNets(BasicBoard board) {
    Set<Integer> result = new TreeSet<>();
    Collection<ConductionArea> areas = board.getConductionAreas();
    if (areas != null) {
      for (ConductionArea area : areas) {
        if (isPour(area)) {
          for (int i = 0; i < area.netCount(); i++) {
            result.add(area.getNetNumber(i));
          }
        }
      }
    }
    return result;
  }

  /**
   * Analyses the net with number {@code netNumber}. Computes the fill of each pour of the net,
   * unless the net's copper is one group without its pours.
   */
  public static Result analyze(BasicBoard board, int netNumber) {
    List<ConductionArea> pours = new ArrayList<>();
    Collection<ConductionArea> areas = board.getConductionAreas();
    if (areas != null) {
      for (ConductionArea area : areas) {
        if (isPour(area) && area.containsNet(netNumber)) {
          pours.add(area);
        }
      }
    }
    if (pours.isEmpty()) {
      return EMPTY;
    }
    pours.sort(Comparator.comparingInt(Item::getId));

    // Copper groups: connected without running through a pour.
    List<Item> items = new ArrayList<>();
    for (Item item : board.getConnectableItems(netNumber)) {
      if (!isPour(item)) {
        items.add(item);
      }
    }
    items.sort(Comparator.comparingInt(Item::getId));
    List<Set<Item>> groups = new ArrayList<>();
    Map<Item, Integer> groupOf = new HashMap<>();
    for (Item item : items) {
      if (groupOf.containsKey(item)) {
        continue;
      }
      Set<Item> group = new TreeSet<>();
      for (Item member : item.getConnectedSet(netNumber, true)) {
        if (!isPour(member)) {
          group.add(member);
        }
      }
      for (Item member : group) {
        groupOf.put(member, groups.size());
      }
      groups.add(group);
    }
    if (groups.size() <= 1) {
      return EMPTY;
    }

    int[] parent = new int[groups.size()];
    for (int i = 0; i < parent.length; i++) {
      parent[i] = i;
    }
    for (ConductionArea pour : pours) {
      Area fill = pour.getConnectingFill();
      if (fill == null || fill.isEmpty()) {
        continue;
      }
      List<Area> islands = DesignRulesChecker.decomposeAreaIntoIslands(fill);
      List<Rectangle2D> bounds = new ArrayList<>(islands.size());
      for (Area island : islands) {
        bounds.add(island.getBounds2D());
      }
      int[] firstGroupInIsland = new int[islands.size()];
      Arrays.fill(firstGroupInIsland, -1);
      for (Item item : items) {
        if (!item.sharesLayer(pour)) {
          continue;
        }
        for (FloatPoint point : contactPoints(item)) {
          for (int i = 0; i < islands.size(); i++) {
            if (bounds.get(i).contains(point.x, point.y)
                && islands.get(i).contains(point.x, point.y)) {
              int group = groupOf.get(item);
              if (firstGroupInIsland[i] < 0) {
                firstGroupInIsland[i] = group;
              } else {
                union(parent, firstGroupInIsland[i], group);
              }
              break; // the islands do not overlap
            }
          }
        }
      }
    }

    // The main group is the fill-connected class with the most items, ties to the lowest id.
    int[] classSize = new int[groups.size()];
    for (int i = 0; i < groups.size(); i++) {
      classSize[find(parent, i)] += groups.get(i).size();
    }
    int mainClass = -1;
    for (int i = 0; i < groups.size(); i++) {
      int root = find(parent, i);
      if (mainClass < 0 || classSize[root] > classSize[mainClass]) {
        mainClass = root;
      }
    }
    Set<Item> mainGroup = new TreeSet<>();
    for (int i = 0; i < groups.size(); i++) {
      if (find(parent, i) == mainClass) {
        mainGroup.addAll(groups.get(i));
      }
    }
    Set<Item> boardConnected = mainGroup.iterator().next().getConnectedSet(netNumber);

    // Classes outside the main one that the board counts as connected to it are cut off; the
    // others are ordinary incomplete connections.
    Map<Integer, Set<Item>> cutOff = new TreeMap<>();
    for (int i = 0; i < groups.size(); i++) {
      int root = find(parent, i);
      Set<Item> group = groups.get(i);
      if (root != mainClass && boardConnected.contains(group.iterator().next())) {
        cutOff.computeIfAbsent(root, k -> new TreeSet<>()).addAll(group);
      }
    }
    List<Set<Item>> cutOffGroups = new ArrayList<>(cutOff.values());
    cutOffGroups.sort(Comparator.comparingInt(group -> group.iterator().next().getId()));
    return new Result(mainGroup, cutOffGroups);
  }

  /** The points at which an item meets the fill: drill centres and trace ends. */
  private static List<FloatPoint> contactPoints(Item item) {
    if (item instanceof DrillItem drillItem) {
      return drillItem.getCenter() == null ? List.of() : List.of(drillItem.getCenter().toFloat());
    }
    if (item instanceof Trace trace) {
      return List.of(trace.firstCorner().toFloat(), trace.lastCorner().toFloat());
    }
    return List.of();
  }

  private static int find(int[] parent, int i) {
    while (parent[i] != i) {
      parent[i] = parent[parent[i]];
      i = parent[i];
    }
    return i;
  }

  private static void union(int[] parent, int a, int b) {
    int rootA = find(parent, a);
    int rootB = find(parent, b);
    if (rootA != rootB) {
      // The lower root wins, so the classes do not depend on the union order.
      parent[Math.max(rootA, rootB)] = Math.min(rootA, rootB);
    }
  }
}
