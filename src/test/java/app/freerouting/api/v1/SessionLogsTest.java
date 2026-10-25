package app.freerouting.api.v1;

import static app.freerouting.api.EmbeddedServerTestSupport.HTTP_TIMEOUT;
import static app.freerouting.api.EmbeddedServerTestSupport.stopServerGracefully;
import static app.freerouting.api.EmbeddedServerTestSupport.waitForApiServerReady;
import static app.freerouting.api.EmbeddedServerTestSupport.waitForServerStarted;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.api.security.ApiKeyValidationService;
import app.freerouting.management.jobs.RoutingJobScheduler;
import app.freerouting.settings.ApiServerSettings;
import app.freerouting.settings.GlobalSettings;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * {@code GET /v1/sessions/{id}/logs} returns the log of the session's jobs: jobs log under their
 * own id, and nothing logs under a session id, so the endpoint used to return an empty array.
 */
@Tag("serial")
class SessionLogsTest {

  private static final String PROFILE_ID = "bbbbbbbb-2222-3333-4444-555555555555";

  private Server apiServer;
  private URI baseUri;
  private final HttpClient httpClient = HttpClient.newHttpClient();

  @BeforeEach
  void setUp() throws Exception {
    ApiKeyValidationService.resetForTesting();
    Freerouting.globalSettings = new GlobalSettings();

    ApiServerSettings settings = new ApiServerSettings();
    settings.isEnabled = true;
    settings.isHttpAllowed = true;
    settings.endpoints = new String[] {"http://127.0.0.1:0"};
    settings.authentication.isEnabled = false;
    Freerouting.globalSettings.apiServerSettings.authentication.isEnabled = false;
    Freerouting.globalSettings.apiServerSettings.rateLimit.enabled = false;

    apiServer = Freerouting.initializeAPI(settings);
    waitForServerStarted(apiServer);
    int port = ((ServerConnector) apiServer.getConnectors()[0]).getLocalPort();
    baseUri = URI.create("http://127.0.0.1:" + port);
    waitForApiServerReady(baseUri);
  }

  @AfterEach
  void tearDown() throws Exception {
    stopServerGracefully(apiServer);
    ApiKeyValidationService.resetForTesting();
  }

  @Test
  void sessionLogsContainTheEntriesOfItsJobs() throws Exception {
    String sessionId = idOf(post("/v1/sessions/create", ""));
    String jobId = idOf(post("/v1/jobs/enqueue", "{\"session_id\":\"" + sessionId + "\"}"));
    RoutingJobScheduler.getInstance().getJob(jobId).logWarning("session-log-probe");
    String otherSessionId = idOf(post("/v1/sessions/create", ""));

    HttpResponse<String> logs = get("/v1/sessions/" + sessionId + "/logs");
    assertEquals(200, logs.statusCode());
    assertTrue(logs.body().contains("session-log-probe"), logs.body());

    HttpResponse<String> otherLogs = get("/v1/sessions/" + otherSessionId + "/logs");
    assertEquals(200, otherLogs.statusCode());
    assertEquals("[]", otherLogs.body().trim());
  }

  private static String idOf(HttpResponse<String> response) {
    assertEquals(200, response.statusCode(), response.body());
    return JsonParser.parseString(response.body()).getAsJsonObject().get("id").getAsString();
  }

  private HttpResponse<String> post(String path, String body) throws Exception {
    return httpClient.send(
        request(path).POST(HttpRequest.BodyPublishers.ofString(body)).build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> get(String path) throws Exception {
    return httpClient.send(request(path).GET().build(), HttpResponse.BodyHandlers.ofString());
  }

  private HttpRequest.Builder request(String path) {
    return HttpRequest.newBuilder(baseUri.resolve(path))
        .timeout(HTTP_TIMEOUT)
        .header("Freerouting-Profile-ID", PROFILE_ID)
        .header("Freerouting-Environment-Host", "test/1.0")
        .header("Content-Type", "application/json");
  }
}
