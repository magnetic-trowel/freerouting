package app.freerouting.datastructures;

/**
 * Counts, per thread, the work of computing pour fills: one unit per fill computed plus one per
 * clearance shape cut out of it. A fill is computed again after any change of the board that can
 * change it, so on a board with large pours inserting a trace can cost far more than the maze
 * search that found it, and no search step counts it. The count is deterministic: the same
 * computations in the same order give the same count on any machine. A pass loop or optimizer
 * candidate runs on one thread, so the difference of two readings on that thread is its fill work.
 */
public final class FillWorkMeter {

  private static final ThreadLocal<long[]> COUNT = ThreadLocal.withInitial(() -> new long[1]);

  private FillWorkMeter() {}

  /** Counts {@code units} of fill work on the current thread. */
  public static void add(long units) {
    COUNT.get()[0] += units;
  }

  /** The fill work counted on the current thread so far. */
  public static long count() {
    return COUNT.get()[0];
  }
}
