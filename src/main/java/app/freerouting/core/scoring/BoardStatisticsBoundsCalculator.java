package app.freerouting.core.scoring;

import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.model.items.ConductionArea;
import app.freerouting.board.model.items.Connectable;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.items.Pin;
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

    Map<Integer, List<Item>> terminalItems = terminalItemsByNet(board);
    for (int netNumber = 1; netNumber <= board.rules.nets.maxNetNumber(); netNumber++) {
      Net net = board.rules.nets.get(netNumber);
      if (net == null) {
        continue;
      }

      List<Terminal> terminals =
          getTerminals(board, terminalItems.getOrDefault(netNumber, List.of()));
      if (terminals.size() < 2) {
        continue;
      }

      MstResult mst = calculateMst(terminals);
      minTraceLength += mst.length * boardUnitToMmFactor;
      minBendCount += mst.bendCount;
      minViaCount += calculateMinimumViaCount(net, terminals);
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
    Set<Integer> signalLayers = new HashSet<>();

    if (item instanceof Pin pin) {
      FloatPoint center = pin.getCenter().toFloat();
      x = center.x;
      y = center.y;
      for (int layer = pin.firstLayer(); layer <= pin.lastLayer(); layer++) {
        if (pin.getShape(layer - pin.firstLayer()) != null) {
          addSignalLayer(board, layer, signalLayers);
        }
      }
    } else if (item instanceof ConductionArea area) {
      FloatPoint center = area.getArea().getBorder().centreOfGravity();
      x = center.x;
      y = center.y;
      addSignalLayer(board, area.getLayer(), signalLayers);
    } else {
      return null;
    }

    return new Terminal(x, y, signalLayers);
  }

  private static void addSignalLayer(BasicBoard board, int layer, Set<Integer> signalLayers) {
    if (layer >= 0
        && layer < board.layerStructure.layers.length
        && board.layerStructure.layers[layer].isSignal) {
      signalLayers.add(layer);
    }
  }

  private static MstResult calculateMst(List<Terminal> terminals) {
    boolean[] used = new boolean[terminals.size()];
    double[] distances = new double[terminals.size()];
    int[] parents = new int[terminals.size()];
    Arrays.fill(distances, Double.POSITIVE_INFINITY);
    Arrays.fill(parents, -1);
    distances[0] = 0.0;

    double length = 0.0;
    int bendCount = 0;
    for (int edge = 0; edge < terminals.size(); edge++) {
      int current = -1;
      for (int index = 0; index < terminals.size(); index++) {
        if (!used[index] && (current < 0 || distances[index] < distances[current])) {
          current = index;
        }
      }
      if (current < 0) {
        break;
      }

      used[current] = true;
      if (parents[current] >= 0) {
        Terminal from = terminals.get(parents[current]);
        Terminal to = terminals.get(current);
        length += distances[current];
        if (from.x != to.x && from.y != to.y) {
          bendCount++;
        }
      }

      for (int index = 0; index < terminals.size(); index++) {
        if (!used[index]) {
          double distance = manhattanDistance(terminals.get(current), terminals.get(index));
          if (distance < distances[index]) {
            distances[index] = distance;
            parents[index] = current;
          }
        }
      }
    }
    return new MstResult(length, bendCount);
  }

  private static double manhattanDistance(Terminal first, Terminal second) {
    return Math.abs(first.x - second.x) + Math.abs(first.y - second.y);
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

  private record Terminal(double x, double y, Set<Integer> signalLayers) {}

  private record MstResult(double length, int bendCount) {}

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
