package app.freerouting.board.model.items;

import app.freerouting.board.facade.BasicBoard;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The connected sets of {@link Item#getConnectedSet(int)}, each connected component of a net walked
 * once while the board does not change.
 *
 * <p>The walk from an item collects every item of the net it reaches through normal contacts. When
 * the contacts inside the collected set are symmetric (every member is a normal contact of each of
 * its own contacts), each member reaches exactly that set, so one walk answers for all of them.
 * Asking item by item would walk a net of n pins n times, which on large boards with plane nets
 * takes minutes, because the contacts of a conduction area are every item under it. A set whose
 * contacts are not symmetric answers only for the item it was walked from. The answers are dropped
 * whenever the content version of the board's item list changes, so they are always those of the
 * current board.
 */
public final class WholeNetConnectivity {

  private BasicBoard board;
  private long contentVersion;

  /** Net number to the connected set of every item whose set has been walked. */
  private final Map<Integer, Map<Item, Set<Item>>> connectedSetsByNet = new HashMap<>();

  /** Returns {@code item.getConnectedSet(netNumber)}, unmodifiable. */
  public Set<Item> connectedSet(Item item, int netNumber) {
    if (netNumber <= 0 || !item.containsNet(netNumber) || item.board == null) {
      return Collections.unmodifiableSet(item.getConnectedSet(netNumber));
    }
    if (item.board != board || item.board.itemList.getContentVersion() != contentVersion) {
      board = item.board;
      contentVersion = board.itemList.getContentVersion();
      connectedSetsByNet.clear();
    }
    Map<Item, Set<Item>> connectedSets =
        connectedSetsByNet.computeIfAbsent(netNumber, net -> new IdentityHashMap<>());
    Set<Item> result = connectedSets.get(item);
    if (result == null) {
      result = walk(item, netNumber, connectedSets);
    }
    return result;
  }

  /** Returns {@code item.getUnconnectedSet(netNumber)}. */
  public Set<Item> unconnectedSet(Item item, int netNumber) {
    if (netNumber <= 0 || !item.containsNet(netNumber) || item.board == null) {
      return item.getUnconnectedSet(netNumber);
    }
    Set<Item> result = new TreeSet<>(item.board.getConnectableItems(netNumber));
    result.removeAll(connectedSet(item, netNumber));
    return result;
  }

  /** Returns whether {@code item.getUnconnectedSet(netNumber)} is empty. */
  public boolean isConnectedToWholeNet(Item item, int netNumber) {
    if (netNumber <= 0 || !item.containsNet(netNumber) || item.board == null) {
      return item.getUnconnectedSet(netNumber).isEmpty();
    }
    return connectedSet(item, netNumber).containsAll(item.board.getConnectableItems(netNumber));
  }

  /**
   * Collects the items of net netNumber reachable from start through normal contacts, as {@code
   * Item.getConnectedSet} does, and stores the set for start, or for every member if their contacts
   * are symmetric.
   */
  private static Set<Item> walk(Item start, int netNumber, Map<Item, Set<Item>> connectedSets) {
    Set<Item> reached = new TreeSet<>();
    Map<Item, Set<Item>> contactsOf = new IdentityHashMap<>();
    reached.add(start);
    ArrayDeque<Item> toVisit = new ArrayDeque<>();
    toVisit.push(start);
    while (!toVisit.isEmpty()) {
      Item current = toVisit.pop();
      Set<Item> contacts = new TreeSet<>();
      Collection<Item> normalContacts = current.getNormalContacts();
      if (normalContacts != null) {
        for (Item contact : normalContacts) {
          if (contact.containsNet(netNumber)) {
            contacts.add(contact);
          }
        }
      }
      contactsOf.put(current, contacts);
      for (Item contact : contacts) {
        if (reached.add(contact)) {
          toVisit.push(contact);
        }
      }
    }
    Set<Item> result = Collections.unmodifiableSet(reached);
    if (isSymmetric(contactsOf)) {
      for (Item member : reached) {
        connectedSets.put(member, result);
      }
    } else {
      connectedSets.put(start, result);
    }
    return result;
  }

  private static boolean isSymmetric(Map<Item, Set<Item>> contactsOf) {
    for (Map.Entry<Item, Set<Item>> entry : contactsOf.entrySet()) {
      for (Item contact : entry.getValue()) {
        if (!contactsOf.get(contact).contains(entry.getKey())) {
          return false;
        }
      }
    }
    return true;
  }
}
