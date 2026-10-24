package app.freerouting.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import app.freerouting.analytics.FRAnalytics;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.RoutingJobState;
import app.freerouting.core.Session;
import app.freerouting.management.jobs.RoutingJobScheduler;
import app.freerouting.management.sessions.SessionManager;
import app.freerouting.settings.GlobalSettings;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A job of another profile is answered exactly like a job that does not exist: the response neither
 * shows the other session's id nor tells whether the job id exists.
 */
class JobOwnershipTest {
  private final UUID owner = UUID.randomUUID();
  private final UUID stranger = UUID.randomUUID();
  private final RoutingJob job = new RoutingJob();

  @BeforeEach
  void setUp() {
    GlobalSettings.setCurrent(new GlobalSettings());
    FRAnalytics.setEnabled(false);
    job.sessionId = UUID.randomUUID();
    job.state = RoutingJobState.QUEUED;
  }

  @Test
  void anotherProfilesJobLooksAbsent() {
    String absentId = UUID.randomUUID().toString();
    String jobId = job.id.toString();
    for (Function<String, Response> endpoint :
        java.util.List.<Function<String, Response>>of(
            id -> resource(new JobProgressResource()).getJob(id, false),
            id -> resource(new JobProgressResource()).logs(id),
            id -> resource(new JobProgressResource()).startJob(id),
            id -> resource(new JobOutputResource()).downloadOutput(id),
            id -> resource(new JobOutputResource()).getDrcSummary(id),
            id -> resource(new JobInputResource()).getEffectiveSettings(id))) {
      Response foreign = withJob(() -> endpoint.apply(jobId));
      Response absent = withJob(() -> endpoint.apply(absentId));

      assertEquals(404, foreign.getStatus());
      assertEquals(absent.getStatus(), foreign.getStatus());
      assertEquals(String.valueOf(absent.getEntity()), String.valueOf(foreign.getEntity()));
      assertFalse(String.valueOf(foreign.getEntity()).contains(job.sessionId.toString()));
    }
  }

  private <T extends app.freerouting.api.BaseController> T resource(T resource) {
    resource.setUserIdOverride(stranger);
    return resource;
  }

  private Response withJob(java.util.function.Supplier<Response> request) {
    RoutingJobScheduler scheduler = mock(RoutingJobScheduler.class);
    SessionManager sessions = mock(SessionManager.class);
    when(scheduler.getJob(job.id.toString())).thenReturn(job);
    when(sessions.getSession(job.sessionId.toString(), owner)).thenReturn(mock(Session.class));
    try (var schedulerScope = mockStatic(RoutingJobScheduler.class);
        var sessionScope = mockStatic(SessionManager.class)) {
      schedulerScope.when(RoutingJobScheduler::getInstance).thenReturn(scheduler);
      sessionScope.when(SessionManager::getInstance).thenReturn(sessions);
      return request.get();
    }
  }
}
