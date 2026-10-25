package app.freerouting.core.scoring;

import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.model.items.ConductionArea;
import app.freerouting.board.model.items.Connectable;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.items.Via;
import app.freerouting.board.model.structure.AngleRestriction;
import app.freerouting.board.model.structure.Unit;
import app.freerouting.core.library.Padstack;
import app.freerouting.datastructures.UndoableObjects;
import app.freerouting.geometry.planar.ConvexShape;
import app.freerouting.geometry.planar.FloatPoint;
import app.freerouting.rules.Net;
import app.freerouting.rules.NetClass;
import app.freerouting.rules.ViaInfo;
import app.freerouting.rules.ViaRule;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Computes board-only lower bounds for optimizer scoring. */
final class BoardStatisticsBoundsCalculator {

  private static final Map<BasicBoard, BoardStatisticsBounds> CACHE = new WeakHashMap<>();

  private BoardStatisticsBoundsCalculator() {}

  /**
   * Returns the cached bounds of the board, computing them on first use. The computation runs
   * outside the lock so optimizer workers, each on its own board copy, do not queue behind each
   * other; the bounds are a pure function of the board, so a lost race only repeats work.
   */
  static BoardStatisticsBounds calculate(BasicBoard board) {
    synchronized (CACHE) {
      BoardStatisticsBounds cached = CACHE.get(board);
      if (cached != null) {
        return cached;
      }
    }
    BoardStatisticsBounds result = calculateUncached(board);
    synchronized (CACHE) {
      BoardStatisticsBounds existing = CACHE.putIfAbsent(board, result);
      return existing != null ? existing : result;
    }
  }

  /** Drops the cached bounds of the board, for example after its rules changed. */
  static void invalidate(BasicBoard board) {
    synchronized (CACHE) {
      CACHE.remove(board);
    }
  }

  private static BoardStatisticsBounds calculateUncached(BasicBoard board) {
    BoardStatisticsBounds result = new BoardStatisticsBounds();
    double minTraceLength = 0.0;
    int minViaCount = 0;
    int minBendCount = 0;
    double boardUnitToMmFactor = board.communication.boardUnitSize(Unit.MM);

    AngleRestriction angleRestriction = board.rules.getTraceAngleRestriction();
    Map<Integer, List<Item>> terminalItems = terminalItemsByNet(board);
    for (int netNumber = 1; netNumber <= board.rules.nets.maxNetNumber(); netNumber++) {
      Net net = board.rules.nets.get(netNumber);
      if (net == null) {
        continue;
      }

      List<Terminal> terminals =
          getTerminals(board, terminalItems.getOrDefault(netNumber, List.of()));
      // Fixed vias stay on the board; the vias the net needs besides them are counted with
      // their layers joined, since a fixed via already changes layers.
      int fixedViaCount = 0;
      List<Terminal> pins = new ArrayList<>();
      boolean hasPour = false;
      for (Terminal terminal : terminals) {
        switch (terminal.kind) {
          case FIXED_VIA -> fixedViaCount++;
          case POUR -> hasPour = true;
          case PIN -> pins.add(terminal);
        }
      }
      minViaCount += fixedViaCount;
      if (terminals.size() - fixedViaCount < 2) {
        continue;
      }
      minViaCount += calculateMinimumViaCount(net, terminals);
      // A pour connects the pins it reaches without any trace, so it gives no length bound.
      if (!hasPour && pins.size() >= 2) {
        TreeBound tree = calculateTreeBound(pins, angleRestriction);
        minTraceLength += tree.length * boardUnitToMmFactor;
        minBendCount += tree.bendCount;
      }
    }

    result.minTraceLengthMm = (float) minTraceLength;
    result.minViaCount = minViaCount;
    result.minBendCount = minBendCount;
    calculateExchangeRates(board, result, boardUnitToMmFactor);
    return result;
  }

  /**
   * Sets the trace length that one via and one bend are worth. A via takes its pad plus clearance
   * out of every layer it spans; a trace takes one trace width plus clearance per unit of length on
   * one layer. Uses the board's first via and the default net class.
   */
  private static void calculateExchangeRates(
      BasicBoard board, BoardStatisticsBounds result, double boardUnitToMmFactor) {
    NetClass netClass = board.rules.getDefaultNetClass();
    int clearanceClass = netClass.getTraceClearanceClass();
    double clearance =
        board.rules.clearanceMatrix.getValue(clearanceClass, clearanceClass, 0, false);
    double pitch = Math.max(1.0, 2.0 * netClass.getTraceHalfWidth(0) + clearance);
    double viaArea = 0.0;
    if (board.rules.viaInfos.count() > 0) {
      Padstack padstack = board.rules.viaInfos.get(0).getPadstack();
      for (int layer = padstack.fromLayer(); layer <= padstack.toLayer(); layer++) {
        ConvexShape shape = padstack.getShape(layer);
        if (shape != null) {
          double side = shape.maxWidth() + clearance;
          viaArea += side * side;
        }
      }
    }
    result.viaEquivalentLengthMm = (float) (viaArea / pitch * boardUnitToMmFactor);
    result.bendEquivalentLengthMm = (float) (pitch * boardUnitToMmFactor);
  }

  /**
   * Collects {@link Net#getTerminalItems()} for every net in one pass over the item list, in the
   * same item order. Calling it per net scanned all items once per net.
   */
  private static Map<Integer, List<Item>> terminalItemsByNet(BasicBoard board) {
    Map<Integer, List<Item>> result = new HashMap<>();
    Iterator<UndoableObjects.UndoableObjectNode> it = board.itemList.startReadObject();
    for (Item item = (Item) board.itemList.readObject(it);
        item != null;
        item = (Item) board.itemList.readObject(it)) {
      if (!(item instanceof Connectable) || item.isRoutable()) {
        continue;
      }
      for (int i = 0; i < item.netCount(); i++) {
        List<Item> netItems = result.computeIfAbsent(item.getNetNumber(i), k -> new ArrayList<>());
        if (netItems.isEmpty() || netItems.getLast() != item) {
          netItems.add(item);
        }
      }
    }
    return result;
  }

  private static List<Terminal> getTerminals(BasicBoard board, List<Item> terminalItems) {
    List<Terminal> result = new ArrayList<>();
    for (Item item : terminalItems) {
      Terminal terminal = toTerminal(board, item);
      if (terminal != null && !terminal.signalLayers.isEmpty()) {
        result.add(terminal);
      }
    }
    return result;
  }

  private static Terminal toTerminal(BasicBoard board, Item item) {
    double x;
    double y;
    TerminalKind kind;
    Set<Integer> signalLayers = new HashSet<>();

    if (item instanceof Pin pin) {
      FloatPoint center = pin.getCenter().toFloat();
      x = center.x;
      y = center.y;
      kind = TerminalKind.PIN;
      for (int layer = pin.firstLayer(); layer <= pin.lastLayer(); layer++) {
        if (pin.getShape(layer - pin.firstLayer()) != null) {
          addSignalLayer(board, layer, signalLayers);
        }
      }
    } else if (item instanceof ConductionArea area) {
      FloatPoint center = area.getArea().getBorder().centreOfGravity();
      x = center.x;
      y = center.y;
      kind = TerminalKind.POUR;
      addSignalLayer(board, area.getLayer(), signalLayers);
    } else if (item instanceof Via via) {
      // Only fixed vias get here: routable items are no terminals.
      FloatPoint center = via.getCenter().toFloat();
      x = center.x;
      y = center.y;
      kind = TerminalKind.FIXED_VIA;
      for (int layer = via.firstLayer(); layer <= via.lastLayer(); layer++) {
        addSignalLayer(board, layer, signalLayers);
      }
    } else {
      return null;
    }

    return new Terminal(x, y, signalLayers, kind);
  }

  private static void addSignalLayer(BasicBoard board, int layer, Set<Integer> signalLayers) {
    if (layer >= 0
        && layer < board.layerStructure.layers.length
        && board.layerStructure.layers[layer].isSignal) {
      signalLayers.add(layer);
    }
  }

  /**
   * Returns a lower bound on the length of any tree of traces that connects the pins under the
   * angle restriction. The tree holds a path between every two pins, so it is at least as long as
   * their largest distance in the restriction's metric. It is also at least as long as the shortest
   * Steiner tree, which is at least 2/3 of the rectilinear minimum spanning tree with 90 degree
   * traces (Hwang 1976) and at least sqrt(3)/2 of the Euclidean one otherwise (Du and Hwang 1990):
   * a 45 degree tree is no shorter than a Euclidean one. Two pins get their exact distance.
   *
   * <p>The bend bound counts the spanning tree's edges that no straight trace allowed by the
   * restriction can follow.
   */
  static TreeBound calculateTreeBound(List<Terminal> pins, AngleRestriction angleRestriction) {
    boolean rectilinear = angleRestriction == AngleRestriction.NINETY_DEGREE;
    int count = pins.size();
    boolean[] used = new boolean[count];
    double[] distances = new double[count];
    int[] parents = new int[count];
    Arrays.fill(distances, Double.POSITIVE_INFINITY);
    Arrays.fill(parents, -1);
    distances[0] = 0.0;

    double spanningTreeLength = 0.0;
    double diameter = 0.0;
    int bendCount = 0;
    for (int edge = 0; edge < count; edge++) {
      int current = -1;
      for (int index = 0; index < count; index++) {
        if (!used[index] && (current < 0 || distances[index] < distances[current])) {
          current = index;
        }
      }
      used[current] = true;
      Terminal to = pins.get(current);
      if (parents[current] >= 0) {
        spanningTreeLength += distances[current];
        if (needsBend(pins.get(parents[current]), to, angleRestriction)) {
          bendCount++;
        }
      }
      for (int index = 0; index < count; index++) {
        Terminal other = pins.get(index);
        diameter = Math.max(diameter, distance(to, other, angleRestriction));
        if (!used[index]) {
          double dx = Math.abs(to.x - other.x);
          double dy = Math.abs(to.y - other.y);
          double distance = rectilinear ? dx + dy : Math.hypot(dx, dy);
          if (distance < distances[index]) {
            distances[index] = distance;
            parents[index] = current;
          }
        }
      }
    }
    double steinerRatio = rectilinear ? 2.0 / 3.0 : Math.sqrt(3.0) / 2.0;
    return new TreeBound(Math.max(diameter, steinerRatio * spanningTreeLength), bendCount);
  }

  /** The length of the shortest trace from one terminal to the other under the restriction. */
  private static double distance(Terminal from, Terminal to, AngleRestriction angleRestriction) {
    double dx = Math.abs(from.x - to.x);
    double dy = Math.abs(from.y - to.y);
    return switch (angleRestriction) {
      case NINETY_DEGREE -> dx + dy;
      case FORTYFIVE_DEGREE -> Math.max(dx, dy) + (Math.sqrt(2.0) - 1.0) * Math.min(dx, dy);
      case NONE -> Math.hypot(dx, dy);
    };
  }

  /** True if no straight trace the restriction allows runs from one terminal to the other. */
  private static boolean needsBend(Terminal from, Terminal to, AngleRestriction angleRestriction) {
    double dx = Math.abs(from.x - to.x);
    double dy = Math.abs(from.y - to.y);
    return switch (angleRestriction) {
      case NINETY_DEGREE -> dx != 0 && dy != 0;
      case FORTYFIVE_DEGREE -> dx != 0 && dy != 0 && dx != dy;
      case NONE -> false;
    };
  }

  private static int calculateMinimumViaCount(Net net, List<Terminal> terminals) {
    LayerGroups groups = new LayerGroups();
    for (Terminal terminal : terminals) {
      groups.addLayers(terminal.signalLayers);
    }
    if (groups.count() <= 1) {
      return 0;
    }

    ViaRule viaRule = net.getNetClass() != null ? net.getNetClass().getViaRule() : null;
    if (viaRule == null) {
      return 0;
    }

    Set<Integer> remainingGroups = groups.roots();
    List<ViaSpan> spans = new ArrayList<>();
    for (int index = 0; index < viaRule.viaCount(); index++) {
      ViaInfo viaInfo = viaRule.getVia(index);
      int firstLayer = viaInfo.getPadstack().fromLayer();
      int lastLayer = viaInfo.getPadstack().toLayer();
      spans.add(new ViaSpan(Math.min(firstLayer, lastLayer), Math.max(firstLayer, lastLayer)));
    }

    int viaCount = 0;
    while (!remainingGroups.isEmpty()) {
      ViaSpan bestSpan = null;
      Set<Integer> bestCoveredGroups = Set.of();
      for (ViaSpan span : spans) {
        Set<Integer> coveredGroups = groups.groupsCoveredBy(span);
        coveredGroups.retainAll(remainingGroups);
        if (coveredGroups.size() > bestCoveredGroups.size()) {
          bestSpan = span;
          bestCoveredGroups = coveredGroups;
        }
      }
      if (bestSpan == null || bestCoveredGroups.isEmpty()) {
        break;
      }
      remainingGroups.removeAll(bestCoveredGroups);
      viaCount++;
    }
    return viaCount;
  }

  enum TerminalKind {
    PIN,
    POUR,
    FIXED_VIA
  }

  record Terminal(double x, double y, Set<Integer> signalLayers, TerminalKind kind) {}

  record TreeBound(double length, int bendCount) {}

  private record ViaSpan(int firstLayer, int lastLayer) {}

  private static final class LayerGroups {
    private final Map<Integer, Integer> parent = new HashMap<>();

    void addLayers(Collection<Integer> layers) {
      Integer firstLayer = null;
      for (int layer : layers) {
        parent.putIfAbsent(layer, layer);
        if (firstLayer == null) {
          firstLayer = layer;
        } else {
          union(firstLayer, layer);
        }
      }
    }

    int count() {
      return roots().size();
    }

    Set<Integer> roots() {
      Set<Integer> roots = new HashSet<>();
      for (int layer : parent.keySet()) {
        roots.add(find(layer));
      }
      return roots;
    }

    Set<Integer> groupsCoveredBy(ViaSpan span) {
      Set<Integer> result = new HashSet<>();
      for (int layer : parent.keySet()) {
        if (layer >= span.firstLayer && layer <= span.lastLayer) {
          result.add(find(layer));
        }
      }
      return result;
    }

    private int find(int layer) {
      int currentParent = parent.get(layer);
      if (currentParent != layer) {
        currentParent = find(currentParent);
        parent.put(layer, currentParent);
      }
      return currentParent;
    }

    private void union(int firstLayer, int secondLayer) {
      int firstRoot = find(firstLayer);
      int secondRoot = find(secondLayer);
      if (firstRoot != secondRoot) {
        parent.put(secondRoot, firstRoot);
      }
    }
  }
}
