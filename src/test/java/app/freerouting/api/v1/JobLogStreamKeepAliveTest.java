package app.freerouting.api.v1;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.freerouting.analytics.FRAnalytics;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.RoutingJobState;
import app.freerouting.core.Session;
import app.freerouting.management.jobs.RoutingJobScheduler;
import app.freerouting.management.sessions.SessionManager;
import app.freerouting.settings.GlobalSettings;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The log stream of a queued job never sends, so a client that disconnected kept its stream poll
 * and its job listener until the job ended. Keep-alive comments detect the vanished client.
 */
class JobLogStreamKeepAliveTest {
  private final UUID userId = UUID.randomUUID();
  private final RoutingJob job = new RoutingJob();
  private final JobProgressResource resource = new JobProgressResource();
  private final Sse sse = mock(Sse.class);
  private final OutboundSseEvent.Builder eventBuilder =
      mock(OutboundSseEvent.Builder.class, RETURNS_SELF);
  private final SseEventSink sink = mock(SseEventSink.class);

  @BeforeEach
  void setUp() {
    GlobalSettings.setCurrent(new GlobalSettings());
    FRAnalytics.setEnabled(false);
    resource.setUserIdOverride(userId);
    job.sessionId = UUID.randomUUID();
    job.state = RoutingJobState.QUEUED;
    when(sse.newEventBuilder()).thenReturn(eventBuilder);
    when(eventBuilder.build()).thenReturn(mock(OutboundSseEvent.class));
    when(sink.send(any(OutboundSseEvent.class)))
        .thenReturn(CompletableFuture.failedFuture(new IOException("Vanished client")));
  }

  @Test
  void keepAliveReleasesTheStreamOfAVanishedClient() throws Exception {
    long interval = SseKeepAlive.intervalMillis;
    SseKeepAlive.intervalMillis = 100;
    try {
      withJob(() -> resource.streamLogs(job.id.toString(), sink, sse));
      verify(eventBuilder, timeout(2000)).comment("keep-alive");
      verify(sink, timeout(2000)).close();
      assertTrue(awaitWorkerExit(), "A vanished client must not retain the stream worker");
    } finally {
      SseKeepAlive.intervalMillis = interval;
      job.state = RoutingJobState.CANCELLED;
      awaitWorkerExit();
    }
  }

  /** A burst of entries is sent in full, and the stream of a completed job closes after it. */
  @Test
  void everyEntryIsSentBeforeTheStreamOfCompletedJobCloses() throws Exception {
    when(sink.send(any(OutboundSseEvent.class)))
        .thenReturn(CompletableFuture.completedFuture(null));
    job.state = RoutingJobState.RUNNING;
    try {
      withJob(() -> resource.streamLogs(job.id.toString(), sink, sse));
      for (int i = 0; i < 30; i++) {
        job.logInfo("entry " + i);
      }
      job.state = RoutingJobState.COMPLETED;
      verify(sink, timeout(3000)).close();
      verify(sink, times(30)).send(any(OutboundSseEvent.class));
      assertTrue(awaitWorkerExit(), "A finished stream must release its poll");
    } finally {
      job.state = RoutingJobState.CANCELLED;
      awaitWorkerExit();
    }
  }

  private void withJob(Runnable request) {
    RoutingJobScheduler scheduler = mock(RoutingJobScheduler.class);
    SessionManager sessions = mock(SessionManager.class);
    Session session = mock(Session.class);
    when(scheduler.getJob(job.id.toString())).thenReturn(job);
    when(sessions.getSession(job.sessionId.toString(), userId, null)).thenReturn(session);
    try (var schedulerScope = mockStatic(RoutingJobScheduler.class);
        var sessionScope = mockStatic(SessionManager.class)) {
      schedulerScope.when(RoutingJobScheduler::getInstance).thenReturn(scheduler);
      sessionScope.when(SessionManager::getInstance).thenReturn(sessions);
      request.run();
    }
  }

  private static boolean awaitWorkerExit() throws InterruptedException {
    long deadline = System.nanoTime() + 2_000_000_000L;
    while (System.nanoTime() < deadline) {
      if (JobOutputStream.openStreams() == 0) {
        return true;
      }
      Thread.sleep(10);
    }
    return false;
  }
}
