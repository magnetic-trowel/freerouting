package app.freerouting.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.freerouting.core.events.RoutingJobLogEntryAddedEventListener;
import app.freerouting.util.gson.GsonProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RoutingJobListenersAndPriorityTest {

  /**
   * The SSE log stream removes its listener from inside the callback once the job finishes or the
   * client disconnects. That ran on the routing thread and threw ConcurrentModificationException
   * out of job.logInfo, which ended the pass early or terminated the job.
   */
  @Test
  void listenerMayRemoveItselfWhileBeingNotified() {
    RoutingJob job = new RoutingJob();
    AtomicInteger calls = new AtomicInteger();
    RoutingJobLogEntryAddedEventListener[] self = new RoutingJobLogEntryAddedEventListener[1];
    self[0] =
        event -> {
          calls.incrementAndGet();
          job.removeLogEntryAddedEventListener(self[0]);
        };
    job.addLogEntryAddedEventListener(self[0]);

    assertDoesNotThrow(() -> job.logInfo("first"));
    assertDoesNotThrow(() -> job.logInfo("second"));
    assertEquals(1, calls.get());
  }

  /**
   * A job enqueued from client JSON with a null or unknown priority made RoutingJob.compareTo
   * throw, and the scheduler sorts the whole queue on every tick, so no job started any more.
   */
  @Test
  void jobsWithMissingPriorityStillSort() {
    for (String body :
        new String[] {
          "{\"session_id\":\"aaaaaaaa-2222-3333-4444-555555555555\",\"priority\":\"BOGUS\"}",
          "{\"session_id\":\"aaaaaaaa-2222-3333-4444-555555555555\",\"priority\":null}"
        }) {
      RoutingJob fromClient = GsonProvider.GSON.fromJson(body, RoutingJob.class);
      RoutingJob high = new RoutingJob();
      high.priority = RoutingJobPriority.values()[0];
      List<RoutingJob> jobs = new ArrayList<>(List.of(fromClient, new RoutingJob(), high));

      assertDoesNotThrow(() -> Collections.sort(jobs));
      assertEquals(0, fromClient.compareTo(new RoutingJob()), "treated as NORMAL priority");
    }
  }
}
