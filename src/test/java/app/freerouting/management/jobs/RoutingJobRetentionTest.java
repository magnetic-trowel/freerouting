package app.freerouting.management.jobs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import app.freerouting.Freerouting;
import app.freerouting.TestFixtures;
import app.freerouting.analytics.FRAnalytics;
import app.freerouting.api.v1.JobOutputResource;
import app.freerouting.api.v1.JobProgressResource;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.RoutingJobState;
import app.freerouting.core.Session;
import app.freerouting.core.StoppableThread;
import app.freerouting.io.specctra.DsnTestFixtures;
import app.freerouting.logger.FRLogger;
import app.freerouting.management.sessions.SessionManager;
import app.freerouting.settings.ApiServerSettings;
import app.freerouting.settings.GlobalSettings;
import app.freerouting.settings.SettingsMerger;
import app.freerouting.settings.sources.DefaultSettings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.ws.rs.core.Response;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Retention of finished jobs and of their routing boards in the scheduler. */
class RoutingJobRetentionTest {

  private static final String FIXTURE = "Issue026-J2_reference.dsn";
  private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

  @TempDir Path tempDir;
  private RoutingJobScheduler scheduler;

  @BeforeEach
  void setUp() {
    GlobalSettings.resetForTesting();
    GlobalSettings.setUserDataPath(tempDir);
    Freerouting.globalSettings = new GlobalSettings();
    FRAnalytics.setEnabled(false);
    scheduler = RoutingJobScheduler.getInstance();
    synchronized (scheduler.jobs) {
      scheduler.jobs.clear();
    }
  }

  @AfterEach
  void tearDown() {
    synchronized (scheduler.jobs) {
      scheduler.jobs.clear();
    }
    GlobalSettings.resetForTesting();
    Freerouting.globalSettings = new GlobalSettings();
  }

  @Test
  void finishedJobReleasesItsBoardButKeepsTheApiResult() throws Exception {
    UUID userId = UUID.randomUUID();
    Session session = SessionManager.getInstance().createSession(userId, "Test/1.0");
    RoutingJob job = new RoutingJob(session.id);
    job.setInput(TestFixtures.resolveFile(FIXTURE));
    // The scheduler merges the settings of every job it starts; the score needs the defaults.
    job.routerSettings = new SettingsMerger(new DefaultSettings()).merge();
    job.board = DsnTestFixtures.loadBoard(FIXTURE);
    assertTrue(RoutingJobOutputWriter.write(job), "the fixture must produce an SES output");
    final byte[] output = job.output.getData().readAllBytes();
    job.finalStatistics = job.board.getStatistics();
    job.state = RoutingJobState.COMPLETED;
    job.finishedAt = Instant.now();
    apiServer().maxFinishedJobBoards = 0;
    add(job);

    scheduler.enforceRetention();

    assertNull(job.board, "a finished job beyond the board limit releases its board");
    assertTrue(job.isBoardReleased());
    assertSame(job, scheduler.getJob(job.id.toString()), "the job itself stays available");

    JobOutputResource outputs = new JobOutputResource();
    outputs.setUserIdOverride(userId);
    try (Response response = outputs.downloadOutput(job.id.toString())) {
      assertEquals(200, response.getStatus());
      JsonObject payload = JsonParser.parseString((String) response.getEntity()).getAsJsonObject();
      assertArrayEquals(output, Base64.getDecoder().decode(payload.get("data").getAsString()));
    }

    JobProgressResource progress = new JobProgressResource();
    progress.setUserIdOverride(userId);
    try (Response response = progress.getJob(job.id.toString(), true)) {
      assertEquals(200, response.getStatus());
      JsonObject statistics =
          JsonParser.parseString((String) response.getEntity())
              .getAsJsonObject()
              .getAsJsonObject("statistics");
      assertNotNull(statistics, "the compact status still reports the final statistics");
      assertEquals(
          job.finalStatistics.connections.incompleteCount,
          statistics.get("unrouted_connections").getAsInt());
      assertEquals(
          job.finalStatistics.clearanceViolations.totalCount,
          statistics.get("clearance_violations").getAsInt());
    }

    try (Response response = outputs.getDrcReport(job.id.toString(), false)) {
      assertEquals(410, response.getStatus());
    }
    try (Response response = outputs.getDrcSummary(job.id.toString())) {
      assertEquals(410, response.getStatus());
    }
    assertNull(job.board, "the DRC endpoints must not put the unrouted input in its place");
  }

  @Test
  void retentionRemovesOnlyTheOldestFinishedJobs() throws Exception {
    apiServer().maxFinishedJobs = 2;
    UUID sessionId = newSession();
    CountDownLatch routingStopped = new CountDownLatch(1);
    StoppableThread stillStopping =
        new StoppableThread() {
          @Override
          protected void threadAction() {
            try {
              routingStopped.await();
            } catch (InterruptedException _) {
              Thread.currentThread().interrupt();
            }
          }
        };
    stillStopping.setDaemon(true);
    stillStopping.start();
    try {
      RoutingJob oldestCompleted = finished(sessionId, RoutingJobState.COMPLETED, 1);
      RoutingJob cancelled = finished(sessionId, RoutingJobState.CANCELLED, 2);
      RoutingJob timedOut = finished(sessionId, RoutingJobState.TIMED_OUT, 3);
      RoutingJob newestInvalid = finished(sessionId, RoutingJobState.INVALID, 4);
      RoutingJob queued = active(sessionId, RoutingJobState.QUEUED);
      RoutingJob running = active(sessionId, RoutingJobState.RUNNING);
      RoutingJob stopping = active(sessionId, RoutingJobState.STOPPING);
      // Timed out by the monitor while its routing thread has not stopped yet.
      RoutingJob timedOutStillRouting = finished(sessionId, RoutingJobState.TIMED_OUT, 0);
      timedOutStillRouting.thread = stillStopping;
      add(
          oldestCompleted,
          queued,
          cancelled,
          running,
          timedOut,
          stopping,
          timedOutStillRouting,
          newestInvalid);

      scheduler.enforceRetention();

      List<RoutingJob> retained = List.of(scheduler.listJobs());
      assertFalse(retained.contains(oldestCompleted), "the oldest finished job is removed");
      assertFalse(retained.contains(cancelled), "the second oldest finished job is removed");
      assertTrue(retained.contains(timedOut));
      assertTrue(retained.contains(newestInvalid));
      assertTrue(retained.contains(queued), "queued jobs are never removed");
      assertTrue(retained.contains(running), "running jobs are never removed");
      assertTrue(retained.contains(stopping), "stopping jobs are never removed");
      assertTrue(retained.contains(timedOutStillRouting), "a job whose thread runs stays");
      assertEquals(6, retained.size());
    } finally {
      routingStopped.countDown();
      stillStopping.join(5_000);
    }
  }

  @Test
  void onlyTheNewestFinishedJobsKeepTheirBoards() {
    apiServer().maxFinishedJobBoards = 1;
    UUID sessionId = newSession();
    RoutingJob older = withBoard(finished(sessionId, RoutingJobState.COMPLETED, 1));
    RoutingJob heldByCaller = withBoard(finished(sessionId, RoutingJobState.COMPLETED, 2));
    heldByCaller.keepBoardWhenFinished = true;
    RoutingJob newest = withBoard(finished(sessionId, RoutingJobState.TIMED_OUT, 3));
    RoutingJob running = withBoard(active(sessionId, RoutingJobState.RUNNING));
    add(older, heldByCaller, newest, running);

    scheduler.enforceRetention();

    assertNull(older.board);
    assertTrue(older.isBoardReleased());
    assertNotNull(heldByCaller.board, "a caller that still reads the board keeps it");
    assertNotNull(newest.board, "the newest finished job keeps its board");
    assertNotNull(running.board, "a running job keeps its board");
    assertFalse(running.isBoardReleased());

    heldByCaller.keepBoardWhenFinished = false;
    scheduler.enforceRetention();

    assertNull(heldByCaller.board, "once the caller is done the limit applies again");
    assertNotNull(newest.board);
  }

  @Test
  void primarySessionJobIsNeverRemovedOrStripped() {
    apiServer().maxFinishedJobs = 1;
    apiServer().maxFinishedJobBoards = 0;
    SessionManager sessions = SessionManager.getInstance();
    Session primary;
    boolean createdPrimary = false;
    try {
      primary = sessions.getPrimarySession();
    } catch (IllegalArgumentException _) {
      primary =
          sessions.createSession(
              UUID.randomUUID(), "Freerouting/" + Freerouting.VERSION_NUMBER_STRING);
      sessions.setPrimarySession(primary.id);
      createdPrimary = true;
    }
    try {
      RoutingJob guiJob = withBoard(finished(primary.id, RoutingJobState.COMPLETED, 0));
      RoutingJob olderApiJob = finished(newSession(), RoutingJobState.COMPLETED, 1);
      RoutingJob newerApiJob = finished(newSession(), RoutingJobState.COMPLETED, 2);
      add(guiJob, olderApiJob, newerApiJob);

      scheduler.enforceRetention();

      assertSame(guiJob, scheduler.getJob(guiJob.id.toString()), "the GUI job is never removed");
      assertNotNull(guiJob.board, "the GUI job keeps its board");
      assertNull(scheduler.getJob(olderApiJob.id.toString()));
      assertSame(newerApiJob, scheduler.getJob(newerApiJob.id.toString()));
    } finally {
      if (createdPrimary) {
        sessions.removeSession(primary.id.toString());
      }
    }
  }

  @Test
  void removedJobsTakeTheirLogEntriesWhileTheEndpointServesRunningJobs() {
    apiServer().maxFinishedJobs = 1;
    UUID userId = UUID.randomUUID();
    Session session = SessionManager.getInstance().createSession(userId, "Test/1.0");
    RoutingJob evicted = finished(session.id, RoutingJobState.COMPLETED, 1);
    RoutingJob kept = finished(session.id, RoutingJobState.COMPLETED, 2);
    RoutingJob running = active(session.id, RoutingJobState.RUNNING);
    evicted.logInfo("message of the evicted job");
    kept.logInfo("message of the kept job");
    running.logInfo("message of the running job");
    add(evicted, kept, running);

    scheduler.enforceRetention();

    assertNull(scheduler.getJob(evicted.id.toString()));
    assertEquals(0, FRLogger.getLogEntries().getEntries(null, evicted.id).length);
    assertEquals(1, FRLogger.getLogEntries().getEntries(null, kept.id).length);
    JobProgressResource progress = new JobProgressResource();
    progress.setUserIdOverride(userId);
    try (Response response = progress.logs(running.id.toString())) {
      assertEquals(200, response.getStatus());
      assertTrue(((String) response.getEntity()).contains("message of the running job"));
    }
    try (Response response = progress.logs(evicted.id.toString())) {
      assertEquals(404, response.getStatus());
    }
  }

  @Test
  void enqueueAppliesTheFinishedJobLimit() {
    apiServer().maxFinishedJobs = 1;
    UUID sessionId = newSession();
    RoutingJob older = finished(sessionId, RoutingJobState.COMPLETED, 1);
    RoutingJob newer = finished(sessionId, RoutingJobState.CANCELLED, 2);
    add(older, newer);

    RoutingJob next = new RoutingJob(sessionId);
    scheduler.enqueueJob(next);

    assertNull(scheduler.getJob(older.id.toString()));
    assertSame(newer, scheduler.getJob(newer.id.toString()));
    assertSame(next, scheduler.getJob(next.id.toString()));
    assertEquals(RoutingJobState.QUEUED, next.state);
  }

  @Test
  void limitsFallBackToTheirDefaults() {
    // By default boards are kept for twice as many finished jobs as run at once, at least 4.
    int defaultBoards =
        Math.max(
            RoutingJobScheduler.DEFAULT_MAX_FINISHED_JOB_BOARDS,
            2 * scheduler.getMaxParallelJobs());
    assertEquals(RoutingJobScheduler.DEFAULT_MAX_FINISHED_JOBS, scheduler.getMaxFinishedJobs());
    assertEquals(defaultBoards, scheduler.getMaxFinishedJobBoards());

    apiServer().maxFinishedJobs = 0;
    apiServer().maxFinishedJobBoards = -1;
    assertEquals(RoutingJobScheduler.DEFAULT_MAX_FINISHED_JOBS, scheduler.getMaxFinishedJobs());
    assertEquals(defaultBoards, scheduler.getMaxFinishedJobBoards());

    apiServer().maxParallelJobs = 6;
    apiServer().maxFinishedJobBoards = null;
    assertEquals(12, scheduler.getMaxFinishedJobBoards(), "twice the parallel jobs");

    apiServer().maxFinishedJobs = 25;
    apiServer().maxFinishedJobBoards = 0;
    assertEquals(25, scheduler.getMaxFinishedJobs());
    assertEquals(0, scheduler.getMaxFinishedJobBoards(), "0 keeps no finished board");
  }

  private static ApiServerSettings apiServer() {
    return Freerouting.globalSettings.apiServerSettings;
  }

  private static UUID newSession() {
    return SessionManager.getInstance().createSession(UUID.randomUUID(), "Test/1.0").id;
  }

  private static RoutingJob finished(UUID sessionId, RoutingJobState state, int minute) {
    RoutingJob job = new RoutingJob(sessionId);
    job.state = state;
    job.finishedAt = BASE.plusSeconds(60L * minute);
    return job;
  }

  private static RoutingJob active(UUID sessionId, RoutingJobState state) {
    RoutingJob job = new RoutingJob(sessionId);
    job.state = state;
    return job;
  }

  private static RoutingJob withBoard(RoutingJob job) {
    job.board = mock(RoutingBoard.class);
    return job;
  }

  private void add(RoutingJob... jobs) {
    synchronized (scheduler.jobs) {
      scheduler.jobs.addAll(List.of(jobs));
    }
  }
}
