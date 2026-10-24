package app.freerouting.autoroute.pipeline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.core.StoppableThread;
import org.junit.jupiter.api.Test;

/**
 * A stop or the optimizer deadline that comes during a pass's candidate loop drops the pass, so a
 * stopped optimizer ends on a board the unstopped run also passes through.
 */
class BatchOptimizerStopTest {

  @Test
  void runningPassIsNotInterrupted() {
    assertFalse(BatchOptimizer.passInterrupted(null, null));
    assertFalse(BatchOptimizer.passInterrupted(newThread(), null));
    assertFalse(
        BatchOptimizer.passInterrupted(newThread(), System.currentTimeMillis() + 3_600_000L));
  }

  @Test
  void stopRequestInterruptsPass() {
    StoppableThread thread = newThread();
    thread.requestStop();

    assertTrue(BatchOptimizer.passInterrupted(thread, null));
  }

  @Test
  void autorouterOnlyStopDoesNotInterruptOptimizerPass() {
    StoppableThread thread = newThread();
    thread.requestStopAutoRouter();

    assertFalse(BatchOptimizer.passInterrupted(thread, null));
  }

  @Test
  void passedDeadlineInterruptsPass() {
    assertTrue(BatchOptimizer.passInterrupted(newThread(), System.currentTimeMillis() - 1));
  }

  private static StoppableThread newThread() {
    return new StoppableThread() {
      @Override
      protected void threadAction() {}
    };
  }
}
