package app.freerouting.datastructures;

/**
 * Class used to cancel a performance critical algorithm after a limit is exceeded.
 *
 * <p>The limit is a deterministic work budget, not wall-clock time: every call of {@link
 * #limitExceeded()} counts as one step, so an algorithm stops at the same point on every machine
 * and under any load. The budget is given in nominal milliseconds.
 */
public class TimeLimit {

  /** Steps per nominal millisecond, about twice the highest poll rate measured on large boards. */
  private static final long STEPS_PER_MILLISECOND = 250;

  private final long maxSteps;
  private long steps;

  /** Creates a new instance with a budget of milliSeconds nominal milliseconds. */
  public TimeLimit(int milliSeconds) {
    this.maxSteps = milliSeconds * STEPS_PER_MILLISECOND;
  }

  /** Counts one step and returns true, if the budget provided in the constructor is exceeded. */
  public boolean limitExceeded() {
    return ++steps > maxSteps;
  }
}
