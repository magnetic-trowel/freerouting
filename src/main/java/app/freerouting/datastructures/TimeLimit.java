package app.freerouting.datastructures;

/**
 * Class used to cancel a performance critical algorithm after a limit is exceeded.
 *
 * <p>The limit is a deterministic work budget, not wall-clock time: every call of {@link
 * #limitExceeded()} counts as one step, so an algorithm stops at the same point on every machine
 * and under any load. The budget is given in nominal milliseconds.
 */
public class TimeLimit {

  /**
   * Steps per nominal millisecond. The integration build uses 1000 times the upstream value (250)
   * so that search budgets do not bind on real boards; job timeouts and cancellation still stop
   * work.
   */
  private static final long STEPS_PER_MILLISECOND = 250_000;

  /**
   * Steps counted by every limit on this thread: a deterministic measure of the work the searches did
   * (the pass loop's work budget, autorouter.max_work, reads it). One thread, one count, so a
   * parallel optimizer does not make the autorouter's count depend on scheduling.
   */
  private static final ThreadLocal<long[]> THREAD_STEPS = ThreadLocal.withInitial(() -> new long[1]);

  private final long maxSteps;
  private long steps;

  /** Steps counted on the calling thread so far. */
  public static long threadSteps() {
    return THREAD_STEPS.get()[0];
  }

  /** Creates a new instance with a budget of milliSeconds nominal milliseconds. */
  public TimeLimit(int milliSeconds) {
    this.maxSteps = milliSeconds * STEPS_PER_MILLISECOND;
  }

  /** Counts one step and returns true, if the budget provided in the constructor is exceeded. */
  public boolean limitExceeded() {
    THREAD_STEPS.get()[0]++;
    return ++steps > maxSteps;
  }
}
