package app.freerouting.board.facade;

import app.freerouting.board.model.items.Item;
import app.freerouting.datastructures.UndoableObjects;
import app.freerouting.logger.FRLogger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

/** Owns routing-board undo/redo side effects and deep-copy restoration. */
public final class RoutingBoardUndoFacade {

  private final RoutingBoard board;

  RoutingBoardUndoFacade(RoutingBoard board) {
    this.board = board;
  }

  boolean undo(Set<Integer> changedNets) {
    board.components.undo(board.communication.observers);
    Collection<UndoableObjects.Storable> cancelledObjects = new LinkedList<>();
    Collection<UndoableObjects.Storable> restoredObjects = new LinkedList<>();
    boolean result = board.itemList.undo(cancelledObjects, restoredObjects);
    applyUndoRedoSideEffects(cancelledObjects, restoredObjects, changedNets);
    return result;
  }

  boolean redo(Set<Integer> changedNets) {
    board.components.redo(board.communication.observers);
    Collection<UndoableObjects.Storable> cancelledObjects = new LinkedList<>();
    Collection<UndoableObjects.Storable> restoredObjects = new LinkedList<>();
    boolean result = board.itemList.redo(cancelledObjects, restoredObjects);
    applyUndoRedoSideEffects(cancelledObjects, restoredObjects, changedNets);
    return result;
  }

  synchronized RoutingBoard deepCopy() {
    byte[] image = serialize();
    return image == null ? null : copyOf(image, board.clearanceBaseline);
  }

  /** Serializes the board for {@link #copyOf}. Returns null if serialization fails. */
  synchronized byte[] serialize() {
    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream)) {
      outputStream.writeObject(board);
      outputStream.flush();
      return byteArrayOutputStream.toByteArray();
    } catch (Exception exception) {
      FRLogger.error("Exception in deep_copy_routing_board" + exception, exception);
      return null;
    }
  }

  /**
   * Creates the board that {@link #deepCopy} would return from serialized board bytes. The baseline
   * of pre-existing clearance violations is transient, so it is passed separately; it is immutable
   * and shared by the copies. Without it every pre-existing violation counts as new on the copy.
   */
  static RoutingBoard copyOf(byte[] image, app.freerouting.drc.ClearanceBaseline baseline) {
    try (ObjectInputStream inputStream =
        new app.freerouting.util.SafeObjectInputStream(new ByteArrayInputStream(image))) {
      RoutingBoard boardCopy = (RoutingBoard) inputStream.readObject();
      boardCopy.clearAllItemTemporaryAutorouteData();
      boardCopy.finishAutoroute();
      boardCopy.clearanceBaseline = baseline;
      return boardCopy;
    } catch (Exception exception) {
      FRLogger.error("Exception in deep_copy_routing_board" + exception, exception);
      return null;
    }
  }

  private void applyUndoRedoSideEffects(
      Collection<UndoableObjects.Storable> cancelledObjects,
      Collection<UndoableObjects.Storable> restoredObjects,
      Set<Integer> changedNets) {
    for (UndoableObjects.Storable storable : cancelledObjects) {
      Item currentItem = (Item) storable;
      board.searchTreeManager.remove(currentItem);
      board.routingLedger().noteRemoved(currentItem);
      if (board.communication != null && board.communication.observers != null) {
        board.communication.observers.notifyDeleted(currentItem);
      }
      if (changedNets != null) {
        for (int i = 0; i < currentItem.netCount(); i++) {
          changedNets.add(currentItem.getNetNumber(i));
        }
      }
    }
    for (UndoableObjects.Storable storable : restoredObjects) {
      Item currentItem = (Item) storable;
      currentItem.board = board;
      board.searchTreeManager.insert(currentItem);
      board.routingLedger().noteInserted(currentItem);
      currentItem.clearAutorouteInfo();
      if (board.communication != null && board.communication.observers != null) {
        board.communication.observers.notifyNew(currentItem);
      }
      if (changedNets != null) {
        for (int i = 0; i < currentItem.netCount(); i++) {
          changedNets.add(currentItem.getNetNumber(i));
        }
      }
    }
    board.incrementRevision();
  }
}
