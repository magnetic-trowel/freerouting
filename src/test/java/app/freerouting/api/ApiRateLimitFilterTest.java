package app.freerouting.api;

import static app.freerouting.api.EmbeddedServerTestSupport.HTTP_TIMEOUT;
import static app.freerouting.api.EmbeddedServerTestSupport.stopServerGracefully;
import static app.freerouting.api.EmbeddedServerTestSupport.waitForServerStarted;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.freerouting.Freerouting;
import app.freerouting.api.security.ApiKeyValidationService;
import app.freerouting.settings.ApiServerSettings;
import app.freerouting.settings.GlobalSettings;
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

@Tag("serial")
class ApiRateLimitFilterTest {

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
    Freerouting.globalSettings.apiServerSettings.rateLimit.enabled = true;
    Freerouting.globalSettings.apiServerSettings.rateLimit.requestsPerWindow = 2;
    Freerouting.globalSettings.apiServerSettings.rateLimit.windowSeconds = 60;

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
  void apiRateLimitBlocksAfterConfiguredThreshold() throws Exception {
    HttpRequest req =
        HttpRequest.newBuilder(baseUri.resolve("/v1/system/status"))
            .GET()
            .timeout(HTTP_TIMEOUT)
            .build();

    HttpResponse<String> r1 = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    HttpResponse<String> r2 = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    HttpResponse<String> r3 = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, r1.statusCode());
    assertEquals(200, r2.statusCode());
    assertEquals(429, r3.statusCode());
  }

  /**
   * An unauthenticated caller was counted by its client-chosen Freerouting-Profile-ID header, so a
   * new header value on every request was never limited.
   */
  @Test
  void rotatingProfileHeadersDoNotBypassTheLimit() throws Exception {
    int[] statuses = new int[3];
    for (int i = 0; i < statuses.length; i++) {
      HttpRequest req =
          HttpRequest.newBuilder(baseUri.resolve("/v1/system/status"))
              .header("Freerouting-Profile-ID", java.util.UUID.randomUUID().toString())
              .GET()
              .timeout(HTTP_TIMEOUT)
              .build();
      statuses[i] = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    assertEquals(200, statuses[0]);
    assertEquals(200, statuses[1]);
    assertEquals(429, statuses[2]);
  }
}
