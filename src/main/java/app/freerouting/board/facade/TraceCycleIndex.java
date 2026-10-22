package app.freerouting.board.facade;

import app.freerouting.board.model.items.ConductionArea;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.items.Trace;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Tells {@link BasicBoard#normalizeTraces} which traces cannot be on a cycle, so that it does not
 * have to run the depth-first search of {@link Trace#isCycle()} for every trace of the net.
 *
 * <p>{@code Trace.isCycle()} is true if an item touches both ends of the trace, or if an item
 * touching its end can be reached from an item touching its start without passing through the trace
 * (and, if the net class ignores cycles with areas, without passing through a conduction area). On
 * a symmetric contact graph the second condition holds exactly when an edge from the trace to a
 * start contact and an edge from the trace to an end contact lie in the same biconnected block. The
 * index therefore collects the contact graph of a connected component once, splits it into blocks
 * with Tarjan's algorithm and then answers every trace of the component from its own contacts.
 *
 * <p>The index is a filter only: {@link #mayBeCycle} returns false only when {@code isCycle()} is
 * false, and the caller still runs {@code isCycle()} when it returns true. A component whose
 * contact relation is not symmetric is not analysed and always answers true. All results are
 * dropped when the content version of the board's item list changes.
 */
final class TraceCycleIndex {

  private final BasicBoard board;
  private final ContactGraph[] graphs = new ContactGraph[2];
  private long contentVersion;

  TraceCycleIndex(BasicBoard board) {
    this.board = board;
    this.contentVersion = board.itemList.getContentVersion();
  }

  /**
   * Returns false only if {@code trace.isCycle()} is false on the current board; true means that
   * {@code isCycle()} has to decide.
   */
  boolean mayBeCycle(Trace trace) {
    long currentVersion = board.itemList.getContentVersion();
    if (currentVersion != contentVersion) {
      contentVersion = currentVersion;
      graphs[0] = null;
      graphs[1] = null;
    }
    int graphIndex = trace.ignoresCyclesWithAreas() ? 1 : 0;
    if (graphs[graphIndex] == null) {
      graphs[graphIndex] = new ContactGraph(graphIndex == 1);
    }
    return graphs[graphIndex].mayBeCycle(trace);
  }

  /** The start and end contacts of a trace, as {@code Trace.isCycle()} sees them. */
  private record TraceEnds(boolean overlap, Set<Item> start, Set<Item> end) {}

  /** The contact graph of the components analysed so far, for one ignore-areas setting. */
  private static final class ContactGraph {

    private final boolean ignoreAreas;
    private final Map<Item, Integer> nodeIndex = new IdentityHashMap<>();
    private final List<int[]> adjacency = new ArrayList<>();
    private final List<Integer> componentOfNode = new ArrayList<>();
    private final List<Boolean> componentIsSymmetric = new ArrayList<>();
    private final Map<Trace, TraceEnds> traceEnds = new IdentityHashMap<>();
    private final Map<Long, Integer> blockOfEdge = new HashMap<>();
    private int blockCount;

    ContactGraph(boolean ignoreAreas) {
      this.ignoreAreas = ignoreAreas;
    }

    boolean mayBeCycle(Trace trace) {
      if (!nodeIndex.containsKey(trace)) {
        addComponent(trace);
      }
      TraceEnds ends = traceEnds.get(trace);
      if (ends.overlap()) {
        return true;
      }
      int traceNode = nodeIndex.get(trace);
      if (!componentIsSymmetric.get(componentOfNode.get(traceNode))) {
        return true;
      }
      if (ends.start().isEmpty() || ends.end().isEmpty()) {
        return false;
      }
      int[] startBlocks = new int[ends.start().size()];
      int i = 0;
      for (Item startContact : ends.start()) {
        startBlocks[i++] = blockOfEdge.get(edgeKey(traceNode, nodeIndex.get(startContact)));
      }
      for (Item endContact : ends.end()) {
        int endBlock = blockOfEdge.get(edgeKey(traceNode, nodeIndex.get(endContact)));
        for (int startBlock : startBlocks) {
          if (startBlock == endBlock) {
            return true;
          }
        }
      }
      return false;
    }

    /** Collects the component containing start, checks its symmetry and splits it into blocks. */
    private void addComponent(Item start) {
      int component = componentIsSymmetric.size();
      int firstNode = adjacency.size();
      List<Item> items = new ArrayList<>();
      List<Set<Item>> contactSets = new ArrayList<>();
      addNode(start, component, items);
      // Breadth-first: the i-th item taken from the queue is items.get(i).
      ArrayDeque<Item> queue = new ArrayDeque<>();
      queue.add(start);
      while (!queue.isEmpty()) {
        Item current = queue.poll();
        Set<Item> contacts = contactsOf(current);
        contactSets.add(contacts);
        for (Item contact : contacts) {
          if (!nodeIndex.containsKey(contact)) {
            addNode(contact, component, items);
            queue.add(contact);
          }
        }
      }

      boolean symmetric = true;
      for (int i = 0; i < items.size() && symmetric; i++) {
        Item item = items.get(i);
        for (Item contact : contactSets.get(i)) {
          int contactNode = nodeIndex.get(contact);
          // A contact in an earlier component means that it does not see this item.
          if (componentOfNode.get(contactNode) != component
              || !contactSets.get(contactNode - firstNode).contains(item)) {
            symmetric = false;
            break;
          }
        }
      }
      componentIsSymmetric.add(symmetric);
      if (!symmetric) {
        return;
      }
      for (int i = 0; i < items.size(); i++) {
        Set<Item> contacts = contactSets.get(i);
        int[] neighbours = new int[contacts.size()];
        int j = 0;
        for (Item contact : contacts) {
          neighbours[j++] = nodeIndex.get(contact) - firstNode;
        }
        adjacency.set(firstNode + i, neighbours);
      }
      splitIntoBlocks(firstNode, items.size());
    }

    private void addNode(Item item, int component, List<Item> items) {
      nodeIndex.put(item, adjacency.size());
      adjacency.add(null);
      componentOfNode.add(component);
      items.add(item);
    }

    /** Returns the contacts the depth-first search of {@code Trace.isCycle()} follows from item. */
    private Set<Item> contactsOf(Item item) {
      Set<Item> contacts;
      if (item instanceof Trace trace) {
        // Trace.getNormalContacts() is the union of the start and the end contacts.
        Set<Item> start = trace.getStartContacts();
        Set<Item> end = trace.getEndContacts();
        boolean overlap = !Collections.disjoint(start, end);
        if (ignoreAreas) {
          start.removeIf(ConductionArea.class::isInstance);
          end.removeIf(ConductionArea.class::isInstance);
        }
        traceEnds.put(trace, new TraceEnds(overlap, start, end));
        contacts = new TreeSet<>(start);
        contacts.addAll(end);
      } else {
        contacts = new TreeSet<>(item.getNormalContacts());
        if (ignoreAreas) {
          contacts.removeIf(ConductionArea.class::isInstance);
        }
      }
      contacts.remove(item);
      return contacts;
    }

    /**
     * Iterative Tarjan biconnected components over one connected component, whose nodes are
     * firstNode .. firstNode + count - 1 and whose adjacency holds component-local numbers. Every
     * edge of the component gets the number of its block.
     */
    private void splitIntoBlocks(int firstNode, int count) {
      int[] discovery = new int[count];
      int[] low = new int[count];
      int[] parent = new int[count];
      int[] nextNeighbour = new int[count];
      Arrays.fill(discovery, -1);
      int[] nodeStack = new int[count];
      int nodeStackSize = 0;
      long[] edgeStack = new long[16];
      int edgeStackSize = 0;
      int time = 0;

      discovery[0] = time;
      low[0] = time;
      time++;
      parent[0] = -1;
      nodeStack[nodeStackSize++] = 0;
      while (nodeStackSize > 0) {
        int u = nodeStack[nodeStackSize - 1];
        int[] neighbours = adjacency.get(firstNode + u);
        if (nextNeighbour[u] < neighbours.length) {
          int v = neighbours[nextNeighbour[u]++];
          if (discovery[v] < 0) {
            if (edgeStackSize == edgeStack.length) {
              edgeStack = Arrays.copyOf(edgeStack, 2 * edgeStackSize);
            }
            edgeStack[edgeStackSize++] = edgeKey(firstNode + u, firstNode + v);
            discovery[v] = time;
            low[v] = time;
            time++;
            parent[v] = u;
            nodeStack[nodeStackSize++] = v;
          } else if (v != parent[u] && discovery[v] < discovery[u]) {
            if (edgeStackSize == edgeStack.length) {
              edgeStack = Arrays.copyOf(edgeStack, 2 * edgeStackSize);
            }
            edgeStack[edgeStackSize++] = edgeKey(firstNode + u, firstNode + v);
            low[u] = Math.min(low[u], discovery[v]);
          }
        } else {
          nodeStackSize--;
          int p = parent[u];
          if (p >= 0) {
            low[p] = Math.min(low[p], low[u]);
            if (low[u] >= discovery[p]) {
              long treeEdge = edgeKey(firstNode + p, firstNode + u);
              int block = blockCount++;
              long edge;
              do {
                edge = edgeStack[--edgeStackSize];
                blockOfEdge.put(edge, block);
              } while (edge != treeEdge);
            }
          }
        }
      }
    }

    private static long edgeKey(int a, int b) {
      return a < b ? ((long) a << 32) | b : ((long) b << 32) | a;
    }
  }
}
