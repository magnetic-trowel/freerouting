package app.freerouting.autoroute.pipeline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.core.StoppableThread;
import org.junit.jupiter.api.Test;

/** The stop flag of an optimizer pass's candidates. */
class BatchOptimizerPassStopTest {

  @Test
  void stopsWithTheJobAndAtPassEnd() {
    JobThread job = new JobThread();
    BatchOptimizer.PassStop stop = new BatchOptimizer.PassStop(job);
    assertFalse(stop.isStopRequested());
    assertFalse(stop.isStopAutoRouterRequested());

    job.requestStopAutoRouter();
    assertFalse(stop.isStopRequested());
    assertTrue(stop.isStopAutoRouterRequested());

    BatchOptimizer.PassStop ended = new BatchOptimizer.PassStop(new JobThread());
    ended.endPass();
    assertTrue(ended.isStopRequested());
    assertTrue(ended.isStopAutoRouterRequested());
  }

  @Test
  void endingThePassLeavesTheJobRunningAndRequestsReachTheJob() {
    JobThread job = new JobThread();
    BatchOptimizer.PassStop stop = new BatchOptimizer.PassStop(job);
    stop.endPass();
    assertFalse(job.isStopRequested());
    assertFalse(job.isStopAutoRouterRequested());

    new BatchOptimizer.PassStop(job).requestStop();
    assertTrue(job.isStopRequested());
  }

  private static final class JobThread extends StoppableThread {
    @Override
    protected void threadAction() {}
  }
}
