package app.freerouting.board.facade;

/**
 * A serialized routing board. Every {@link #copy()} equals what {@link RoutingBoard#deepCopy()}
 * returned when the image was taken. Copies can be made concurrently, because they read only the
 * immutable bytes and never the source board.
 */
public final class BoardImage {

  private final byte[] bytes;

  /** The source board's baseline of pre-existing clearance violations; it is not serialized. */
  private final app.freerouting.drc.ClearanceBaseline clearanceBaseline;

  BoardImage(byte[] bytes, app.freerouting.drc.ClearanceBaseline clearanceBaseline) {
    this.bytes = bytes;
    this.clearanceBaseline = clearanceBaseline;
  }

  /** Returns a new board deserialized from the image, or null if deserialization fails. */
  public RoutingBoard copy() {
    return RoutingBoardUndoFacade.copyOf(this.bytes, this.clearanceBaseline);
  }
}
