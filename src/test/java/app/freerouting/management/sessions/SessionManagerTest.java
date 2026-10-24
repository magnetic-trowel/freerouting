package app.freerouting.management.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.analytics.FRAnalytics;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.RoutingJobState;
import app.freerouting.core.Session;
import app.freerouting.management.jobs.RoutingJobScheduler;
import app.freerouting.settings.GlobalSettings;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** SessionManagerTest. */
public class SessionManagerTest {

  @BeforeEach
  void setUpGlobalSettings() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  @Test
  void testGetInstance() {
    SessionManager sessionManager1 = SessionManager.getInstance();
    SessionManager sessionManager2 = SessionManager.getInstance();
    assertSame(sessionManager1, sessionManager2, "SessionManager should be a singleton.");
  }

  @Test
  void testCreateAndGetSession() {
    SessionManager sessionManager = SessionManager.getInstance();
    UUID userId = UUID.randomUUID();
    Session session =
        sessionManager.createSession(userId, "Freerouting/" + Freerouting.VERSION_NUMBER_STRING);

    assertNotNull(session, "Created session should not be null.");
    assertNotNull(session.id, "Session ID should be generated.");
    assertEquals(userId, session.userId, "Session should be associated with the correct user ID.");

    Session retrievedSession = sessionManager.getSession(session.id.toString());
    assertEquals(session, retrievedSession, "Retrieved session should match the created session.");
  }

  @Test
  void testRemoveSession() {
    SessionManager sessionManager = SessionManager.getInstance();
    UUID userId = UUID.randomUUID();
    Session session =
        sessionManager.createSession(userId, "Freerouting/" + Freerouting.VERSION_NUMBER_STRING);

    sessionManager.removeSession(session.id.toString());
    Session retrievedSession = sessionManager.getSession(session.id.toString());
    assertNull(retrievedSession, "Removed session should not be retrievable.");
  }

  @Test
  void testGetActiveSessionsCount() {
    SessionManager sessionManager = SessionManager.getInstance();
    int initialCount = sessionManager.getActiveSessionsCount();

    sessionManager.createSession(
        UUID.randomUUID(), "Freerouting/" + Freerouting.VERSION_NUMBER_STRING);
    sessionManager.createSession(
        UUID.randomUUID(), "Freerouting/" + Freerouting.VERSION_NUMBER_STRING);
    assertEquals(
        initialCount + 2,
        sessionManager.getActiveSessionsCount(),
        "Active session count should be incremented.");
  }

  @Test
  void testListSessionIds() {
    UUID userId = UUID.randomUUID();

    SessionManager sessionManager = SessionManager.getInstance();
    sessionManager.createSession(userId, "Freerouting/" + Freerouting.VERSION_NUMBER_STRING);
    sessionManager.createSession(userId, "Freerouting/" + Freerouting.VERSION_NUMBER_STRING);

    String[] sessionIds = sessionManager.listSessionIds(userId);
    assertTrue(sessionIds.length >= 2, "Session ID list should contain at least two IDs.");
  }

  @Test
  void testGetAndSetPrimarySession() {
    SessionManager sessionManager = SessionManager.getInstance();
    UUID userId = UUID.randomUUID();
    Session session =
        sessionManager.createSession(userId, "Freerouting/" + Freerouting.VERSION_NUMBER_STRING);

    assertThrows(
        IllegalArgumentException.class,
        sessionManager::getPrimarySession,
        "Getting GUI session without setting it should throw an exception.");

    sessionManager.setPrimarySession(session.id);
    Session guiSession = sessionManager.getPrimarySession();
    assertEquals(session, guiSession, "Retrieved GUI session should match the set session.");
  }

  @Test
  void testApiKeyHashPreservedInternallyButExcludedFromSerialization() {
    SessionManager sessionManager = SessionManager.getInstance();
    UUID userId = UUID.randomUUID();
    String sampleHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    Session session = sessionManager.createSession(userId, "Agent/1.0", sampleHash);

    assertNotNull(session);
    assertEquals(
        sampleHash, session.apiKeyHash, "apiKeyHash should be retained in-memory for analytics.");

    String sessionJson = app.freerouting.util.gson.GsonProvider.GSON.toJson(session);
    org.junit.jupiter.api.Assertions.assertFalse(
        sessionJson.contains("api_key_hash"),
        "apiKeyHash must not be exposed in serialized session JSON.");
    org.junit.jupiter.api.Assertions.assertFalse(
        sessionJson.contains(sampleHash),
        "Sample hash value must not be exposed in serialized session JSON.");

    app.freerouting.core.RoutingJob job = new app.freerouting.core.RoutingJob(session.id);
    job.userId = userId;
    job.apiKeyHash = sampleHash;
    String jobJson = app.freerouting.util.gson.GsonProvider.GSON.toJson(job);
    org.junit.jupiter.api.Assertions.assertFalse(
        jobJson.contains("api_key_hash"), "apiKeyHash must not be exposed in serialized job JSON.");
    org.junit.jupiter.api.Assertions.assertFalse(
        jobJson.contains(sampleHash),
        "Sample hash value must not be exposed in serialized job JSON.");
  }

  @Test
  void fullRegistryEvictsTheOldestIdleSession() {
    FRAnalytics.setEnabled(false);
    RoutingJobScheduler scheduler = RoutingJobScheduler.getInstance();
    SessionManager manager = new SessionManager(4);
    Session primary = manager.createSession(UUID.randomUUID(), guiHost());
    manager.setPrimarySession(primary.id);
    Session oldestIdle = manager.createSession(UUID.randomUUID(), "Test/1.0");
    Session busy = manager.createSession(UUID.randomUUID(), "Test/1.0");
    Session idleWithResult = manager.createSession(UUID.randomUUID(), "Test/1.0");
    RoutingJob queued = job(busy, RoutingJobState.QUEUED);
    RoutingJob finished = job(idleWithResult, RoutingJobState.COMPLETED);
    addJobs(scheduler, queued, finished);
    try {
      final Session first = manager.createSession(UUID.randomUUID(), "Test/1.0");

      assertNull(manager.getSession(oldestIdle.id.toString()), "the oldest idle session goes");
      assertNotNull(manager.getSession(primary.id.toString()), "the primary session stays");
      assertNotNull(manager.getSession(busy.id.toString()), "a session with a queued job stays");
      assertNotNull(manager.getSession(idleWithResult.id.toString()));
      assertNotNull(manager.getSession(first.id.toString()));
      assertEquals(4, manager.getActiveSessionsCount());

      final Session second = manager.createSession(UUID.randomUUID(), "Test/1.0");

      assertNull(manager.getSession(idleWithResult.id.toString()), "the next oldest idle goes");
      assertNull(
          scheduler.getJob(finished.id.toString()), "its finished jobs are no longer reachable");
      assertSame(queued, scheduler.getJob(queued.id.toString()), "active jobs are never removed");
      assertNotNull(manager.getSession(primary.id.toString()));
      assertNotNull(manager.getSession(busy.id.toString()));
      assertNotNull(manager.getSession(first.id.toString()));
      assertNotNull(manager.getSession(second.id.toString()));
      assertEquals(4, manager.getActiveSessionsCount());
    } finally {
      removeJobs(scheduler, queued, finished);
    }
  }

  @Test
  void fullRegistryOfActiveSessionsGrowsInsteadOfEvicting() {
    FRAnalytics.setEnabled(false);
    RoutingJobScheduler scheduler = RoutingJobScheduler.getInstance();
    SessionManager manager = new SessionManager(2);
    Session primary = manager.createSession(UUID.randomUUID(), guiHost());
    manager.setPrimarySession(primary.id);
    Session running = manager.createSession(UUID.randomUUID(), "Test/1.0");
    RoutingJob job = job(running, RoutingJobState.RUNNING);
    addJobs(scheduler, job);
    try {
      final Session extra = manager.createSession(UUID.randomUUID(), "Test/1.0");

      assertEquals(3, manager.getActiveSessionsCount(), "no session may be evicted, so it grows");
      assertNotNull(manager.getSession(primary.id.toString()));
      assertNotNull(manager.getSession(running.id.toString()));
      assertNotNull(manager.getSession(extra.id.toString()));

      job.state = RoutingJobState.COMPLETED;
      final Session afterIdle = manager.createSession(UUID.randomUUID(), "Test/1.0");

      assertEquals(2, manager.getActiveSessionsCount(), "idle sessions bring it back to its limit");
      assertNotNull(manager.getSession(primary.id.toString()));
      assertNotNull(manager.getSession(afterIdle.id.toString()));
      assertNull(manager.getSession(running.id.toString()));
      assertNull(manager.getSession(extra.id.toString()));
    } finally {
      removeJobs(scheduler, job);
    }
  }

  @Test
  void evictionChoosesTheOldestSessionThatIsNeitherPrimaryNorActive() {
    Session primary = new Session(UUID.randomUUID(), guiHost());
    primary.isPrimary = true;
    Session active = new Session(UUID.randomUUID(), "Test/1.0");
    Session idle = new Session(UUID.randomUUID(), "Test/1.0");
    Session newerIdle = new Session(UUID.randomUUID(), "Test/1.0");

    assertSame(
        idle,
        SessionManager.selectSessionToEvict(
            List.of(newerIdle, primary, idle, active), Set.of(active.id)));
    assertNull(SessionManager.selectSessionToEvict(List.of(primary, active), Set.of(active.id)));
  }

  private static String guiHost() {
    return "Freerouting/" + Freerouting.VERSION_NUMBER_STRING;
  }

  private static RoutingJob job(Session session, RoutingJobState state) {
    RoutingJob job = new RoutingJob(session.id);
    job.state = state;
    return job;
  }

  private static void addJobs(RoutingJobScheduler scheduler, RoutingJob... jobs) {
    synchronized (scheduler.jobs) {
      scheduler.jobs.addAll(List.of(jobs));
    }
  }

  private static void removeJobs(RoutingJobScheduler scheduler, RoutingJob... jobs) {
    synchronized (scheduler.jobs) {
      scheduler.jobs.removeAll(List.of(jobs));
    }
  }
}
