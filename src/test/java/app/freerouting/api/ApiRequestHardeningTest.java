package app.freerouting.api;

import static app.freerouting.api.EmbeddedServerTestSupport.HTTP_TIMEOUT;
import static app.freerouting.api.EmbeddedServerTestSupport.stopServerGracefully;
import static app.freerouting.api.EmbeddedServerTestSupport.waitForServerStarted;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import app.freerouting.Freerouting;
import app.freerouting.api.security.ApiKeyValidationService;
import app.freerouting.settings.ApiServerSettings;
import app.freerouting.settings.GlobalSettings;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Client mistakes and oversized requests are answered with a 4xx status before they reach the
 * resources: the API used to read every body into memory without a limit (the analytics endpoints
 * need no API key) and to answer client errors with 500.
 */
@Tag("serial")
class ApiRequestHardeningTest {

  private static final String ENVIRONMENT_HOST = "Freerouting-Environment-Host";

  private Server apiServer;
  private URI baseUri;
  private HttpClient httpClient;

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
    httpClient = HttpClient.newHttpClient();
  }

  @AfterEach
  void tearDown() throws Exception {
    stopServerGracefully(apiServer);
    ApiKeyValidationService.resetForTesting();
  }

  @Test
  void oversizedAnalyticsBodyIsRejectedBeforeItIsRead() throws Exception {
    byte[] body = oversizedAnalyticsBody();

    HttpResponse<String> response =
        send(
            HttpRequest.newBuilder(baseUri.resolve("/v1/analytics/track"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body)));

    assertEquals(413, response.statusCode());
  }

  @Test
  void oversizedChunkedAnalyticsBodyIsRejected() throws Exception {
    byte[] body = oversizedAnalyticsBody();

    // A body published from a stream has no Content-Length, so it is sent chunked.
    HttpResponse<String> response =
        send(
            HttpRequest.newBuilder(baseUri.resolve("/v1/analytics/track"))
                .header("Content-Type", "application/json")
                .POST(
                    HttpRequest.BodyPublishers.ofInputStream(
                        () -> new ByteArrayInputStream(body))));

    assertEquals(413, response.statusCode());
  }

  @Test
  void missingProfileHeaderIsABadRequest() throws Exception {
    HttpResponse<String> response =
        send(
            HttpRequest.newBuilder(baseUri.resolve("/v1/sessions/create"))
                .header(ENVIRONMENT_HOST, "TestClient/1.0")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}")));

    assertEquals(400, response.statusCode());
  }

  @Test
  void unsupportedContentTypeKeepsItsStatus() throws Exception {
    HttpResponse<String> response =
        send(
            HttpRequest.newBuilder(baseUri.resolve("/v1/jobs/enqueue"))
                .header(ENVIRONMENT_HOST, "TestClient/1.0")
                .header("Freerouting-Profile-ID", "aaaaaaaa-2222-3333-4444-555555555555")
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("not json")));

    assertEquals(415, response.statusCode());
    assertFalse(response.body().isEmpty());
  }

  private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
    return httpClient.send(
        request.timeout(HTTP_TIMEOUT).build(), HttpResponse.BodyHandlers.ofString());
  }

  private static byte[] oversizedAnalyticsBody() {
    byte[] body = new byte[(int) RequestBodyLimitFilter.MAX_ANALYTICS_BODY_BYTES + 1024];
    Arrays.fill(body, (byte) ' ');
    body[0] = '{';
    body[body.length - 1] = '}';
    return body;
  }
}
