package app.freerouting.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.api.dto.AutorouteRequest;
import app.freerouting.core.RoutingJob;
import app.freerouting.fixtures.RoutingFixtureTest;
import app.freerouting.settings.GlobalSettings;
import app.freerouting.settings.RouterSettings;
import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AutorouteControllerV1Test extends RoutingFixtureTest {

  private AutorouteControllerV1 controller;

  @BeforeEach
  protected void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
    Freerouting.globalSettings.apiServerSettings.authentication.isEnabled = false;
    controller = new AutorouteControllerV1();
    controller.setUserIdOverride(UUID.randomUUID());
  }

  @Test
  void testAutorouteRejectsMissingPayload() {
    Response response = controller.autoroute(null);
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void testAutorouteRejectsMissingFileContent() {
    AutorouteRequest req = new AutorouteRequest();
    Response response = controller.autoroute(req);
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void testAutorouteExecutesSuccessfully() throws Exception {
    RoutingJob sampleJob = getRoutingJob("Issue508-DAC2020_bm01.dsn");
    assertNotNull(sampleJob);
    byte[] dsnBytes = sampleJob.input.getData().readAllBytes();
    String dsnContent = new String(dsnBytes, StandardCharsets.UTF_8);

    AutorouteRequest req = new AutorouteRequest();
    req.fileContent = dsnContent;
    req.outputFormats = List.of("SES", "DRC_SUMMARY");
    req.timeoutSeconds = 60;
    req.routerSettings = new RouterSettings();
    req.routerSettings.autorouter.maxPasses = 1;
    req.routerSettings.autorouter.maxItems = 10;

    Response response = controller.autoroute(req);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());
    String json = response.getEntity().toString();
    assertTrue(json.contains("\"job_id\"") || json.contains("\"jobId\""));
    assertTrue(json.contains("\"outputs\""));
    // From the statistics the routing thread took of the final board.
    assertTrue(json.contains("\"normalized_score\""), json);
  }

  @Test
  void testAutorouteStopsTheRouterAtTheRequestedTimeout() throws Exception {
    RoutingJob sampleJob = getRoutingJob("Issue508-DAC2020_bm01.dsn");
    byte[] dsnBytes = sampleJob.input.getData().readAllBytes();

    AutorouteRequest req = new AutorouteRequest();
    req.fileContent = new String(dsnBytes, StandardCharsets.UTF_8);
    req.outputFormats = List.of("SES");
    req.timeoutSeconds = 2;
    req.routerSettings = new RouterSettings();
    req.routerSettings.jobTimeoutString = "01:00:00";

    long start = System.nanoTime();
    Response response = controller.autoroute(req);
    double seconds = (System.nanoTime() - start) / 1e9;

    // bm01 does not route in two seconds; the job clock has to stop it well before the hour the
    // router settings ask for.
    assertTrue(seconds < 60, "request took " + seconds + " s");
    String json = response.getEntity().toString();
    assertTrue(json.contains("TIMED_OUT"), json);
    assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
  }
}
