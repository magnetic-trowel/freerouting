package app.freerouting.board.facade;

/**
 * A serialized routing board. Every {@link #copy()} equals what {@link RoutingBoard#deepCopy()}
 * returned when the image was taken. Copies can be made concurrently, because they read only the
 * immutable bytes and never the source board.
 */
public final class BoardImage {

  private final byte[] bytes;

  BoardImage(byte[] bytes) {
    this.bytes = bytes;
  }

  /** Returns a new board deserialized from the image, or null if deserialization fails. */
  public RoutingBoard copy() {
    return RoutingBoardUndoFacade.copyOf(this.bytes);
  }
}
