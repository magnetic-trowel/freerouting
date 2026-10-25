package app.freerouting.board.model.items;

import java.util.HashMap;
import java.util.Map;

/**
 * Tells whether an item is connected to every other connectable item of a net, walking each net
 * once. The connected set is an equivalence class over the net's connectable items, so it either
 * covers the whole net for every item of the net or for none of them; asking item by item would
 * walk a net of n pins n times, which on large boards with plane nets takes minutes. The answers
 * are valid while the board does not change.
 */
public final class WholeNetConnectivity {

  private final Map<Integer, Boolean> connectedByNet = new HashMap<>();

  /** Returns whether {@code item.getUnconnectedSet(netNumber)} is empty. */
  public boolean isConnectedToWholeNet(Item item, int netNumber) {
    if (netNumber <= 0 || !item.containsNet(netNumber)) {
      return item.getUnconnectedSet(netNumber).isEmpty();
    }
    return connectedByNet.computeIfAbsent(netNumber, net -> item.getUnconnectedSet(net).isEmpty());
  }
}
