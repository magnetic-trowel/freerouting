package app.freerouting.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.freerouting.Freerouting;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.RoutingJobPriority;
import app.freerouting.core.RoutingJobState;
import app.freerouting.core.Session;
import app.freerouting.management.jobs.RoutingJobScheduler;
import app.freerouting.management.sessions.SessionManager;
import app.freerouting.settings.GlobalSettings;
import app.freerouting.util.gson.GsonProvider;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JobInputResourceTest {

  private JobInputResource resource;
  private UUID userId;
  private String sessionId;
  private String jobId;

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
    Freerouting.globalSettings.apiServerSettings.authentication.isEnabled = false;

    userId = UUID.randomUUID();
    Session session = SessionManager.getInstance().createSession(userId, "test/1.0");
    sessionId = session.id.toString();

    RoutingJob job = new RoutingJob(session.id);
    RoutingJobScheduler.getInstance().enqueueJob(job);
    jobId = job.id.toString();

    resource = new JobInputResource();
    resource.setUserIdOverride(userId);
  }

  @Test
  void uploadInputRejectsMalformedBase64() {
    String malformedPayload = "{\"data\":\"%%%not-valid-base64%%%\"}";
    Response response = resource.uploadInput(jobId, malformedPayload);
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void uploadInputJsonRejectsEmptyPayload() {
    Response response = resource.uploadInputJson(jobId, "");
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void uploadRulesRejectsMalformedBase64() {
    String malformedPayload = "{\"data\":\"%%%not-valid-base64%%%\"}";
    Response response = resource.uploadRules(jobId, malformedPayload);
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void uploadRulesAcceptsValidBase64() {
    String rulesContent = "(rules PCB test (snap_angle fortyfive_degree))";
    String base64 =
        java.util.Base64.getEncoder()
            .encodeToString(rulesContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    String payload = "{\"data\":\"" + base64 + "\",\"filename\":\"test.rules\"}";
    Response response = resource.uploadRules(jobId, payload);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    RoutingJob job = RoutingJobScheduler.getInstance().getJob(jobId);
    org.junit.jupiter.api.Assertions.assertNotNull(job.rules);
    assertEquals("test.rules", job.rules.getFilename());
  }

  /**
   * Enqueue read the whole job from the client, so a client could reuse another job's identifier
   * (locking its owner out) or choose the output file name that job persistence wrote or deleted.
   */
  @Test
  void enqueueIgnoresServerOwnedFieldsFromTheClient() {
    String body =
        """
        {"session_id": "%s", "name": "my-board", "priority": "HIGH", "id": "%s",
         "state": "COMPLETED", "output": {"filename": "/tmp/elsewhere/victim.txt"}}
        """
            .formatted(sessionId, jobId);

    Response response = resource.enqueueJob(body);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    RoutingJob created =
        GsonProvider.GSON.fromJson((String) response.getEntity(), RoutingJob.class);
    assertNotEquals(jobId, created.id.toString());
    RoutingJob enqueued = RoutingJobScheduler.getInstance().getJob(created.id.toString());
    assertEquals("my-board", enqueued.name);
    assertEquals(RoutingJobPriority.HIGH, enqueued.priority);
    assertEquals(RoutingJobState.QUEUED, enqueued.state);
    assertNull(enqueued.output);
    assertEquals(sessionId, enqueued.sessionId.toString());
  }

  @Test
  void enqueueRejectsMalformedJson() {
    Response response = resource.enqueueJob("{\"session_id\": [");

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
}
