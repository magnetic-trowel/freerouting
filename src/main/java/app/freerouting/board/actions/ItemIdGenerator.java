package app.freerouting.board.actions;

import app.freerouting.datastructures.IdGenerator;
import java.io.Serializable;

/**
 * Creates unique identification numbers for items on the board.
 *
 * <p>The counter starts at 1 and only grows. Callers rely on that: {@code Item.compareTo} orders
 * items by id, and the router tells the items it created from the old ones by comparing ids with
 * {@link #maxGeneratedId()} taken before a step. An id that came round again would collide with a
 * live item (the pins keep their small ids) and make new items look old, so the generator fails
 * loudly when the {@code int} range is used up instead of wrapping.
 */
public class ItemIdGenerator implements IdGenerator, Serializable {

  // The form saved by earlier versions, which also had a wrap-around counter.
  private static final long serialVersionUID = -7813083654137509890L;

  private int lastGeneratedId;

  /** Creates a new ItemIdGenerator. */
  public ItemIdGenerator() {}

  /**
   * Creates a new unique identification number.
   *
   * @throws IllegalStateException when every positive {@code int} has been used
   */
  @Override
  public int newId() {
    if (lastGeneratedId == Integer.MAX_VALUE) {
      throw new IllegalStateException(
          "The board has used up its item ids (" + Integer.MAX_VALUE + ").");
    }
    return ++lastGeneratedId;
  }

  /** Returns the maximum generated ID number so far. */
  @Override
  public int maxGeneratedId() {
    return lastGeneratedId;
  }
}
