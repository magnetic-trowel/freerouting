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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
  void earlyRejectionReadsTheRestOfTheBodyInsteadOfResettingTheConnection() throws Exception {
    // Larger than the socket buffers, so the client is still uploading when the 413 is sent.
    int bodyLength = 8 * 1024 * 1024;
    int port = baseUri.getPort();
    try (Socket socket = new Socket("127.0.0.1", port)) {
      socket.setSoTimeout((int) HTTP_TIMEOUT.toMillis());
      OutputStream out = socket.getOutputStream();
      out.write(
          ("POST /v1/analytics/track HTTP/1.1\r\nHost: 127.0.0.1:"
                  + port
                  + "\r\nContent-Type: application/json\r\nContent-Length: "
                  + bodyLength
                  + "\r\n\r\n")
              .getBytes(StandardCharsets.US_ASCII));
      // Without the drain, Jetty closed the socket with the body unread and this write failed
      // with "Broken pipe" or "Connection reset", which can also discard the 413 at the client.
      byte[] chunk = new byte[64 * 1024];
      Arrays.fill(chunk, (byte) ' ');
      for (int sent = 0; sent < bodyLength; sent += chunk.length) {
        out.write(chunk, 0, Math.min(chunk.length, bodyLength - sent));
      }
      out.flush();

      BufferedReader in =
          new BufferedReader(
              new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
      assertEquals("HTTP/1.1 413 Payload Too Large", in.readLine());
    }
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
