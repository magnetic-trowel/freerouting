package app.freerouting.settings;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.logger.FRLogger;
import app.freerouting.startup.GlobalSettingsBootstrap;
import app.freerouting.util.gson.GsonProvider;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Command-line arguments and {@code FREEROUTING__} environment variables apply to the process they
 * were given to. The GUI saves {@code freerouting.json} several times per session, and those saves
 * must keep the file's own values for everything the command line or environment set, while still
 * saving what the user changed in the GUI.
 */
class ProcessOnlySettingsPersistenceTest {

  private static final String FILE_INPUT_DIRECTORY = "/file/boards";

  @TempDir Path userDataDir;

  @BeforeEach
  void setUp() throws IOException {
    GlobalSettings.resetForTesting();
    GlobalSettings.setUserDataPath(userDataDir);
    FRLogger.getLogEntries().clear();
    writeConfigurationFile(
        """
        {
          "version": "%s",
          "gui": { "input_directory": "%s" },
          "api_server": { "max_parallel_jobs": 3 }
        }
        """
            .formatted(GlobalSettings.getReleaseSafeVersion(), FILE_INPUT_DIRECTORY));
  }

  @AfterEach
  void tearDown() {
    GlobalSettings.resetForTesting();
  }

  @Test
  void apiServerOptionsGivenAtGuiLaunchAreNotPersisted() throws IOException {
    GlobalSettings globalSettings =
        startGuiSession(
            "--api_server.enabled=true",
            "--api_server.authentication.enabled=false",
            "--api_server.endpoints=http://0.0.0.0:37864",
            "--api_server.max_parallel_jobs=8");

    // The options are effective for this process.
    assertFalse(globalSettings.apiServerSettings.authentication.isEnabled);
    assertArrayEquals(
        new String[] {"http://0.0.0.0:37864"}, globalSettings.apiServerSettings.endpoints);

    GlobalSettings.saveAsJson(globalSettings);

    // A later headless run starts from the secure defaults and the file's own values again.
    GlobalSettings nextRun = GlobalSettings.load();
    assertNotNull(nextRun);
    assertTrue(nextRun.apiServerSettings.authentication.isEnabled);
    assertArrayEquals(new String[] {"http://127.0.0.1:37864"}, nextRun.apiServerSettings.endpoints);
    assertFalse(nextRun.apiServerSettings.isEnabled);
    assertEquals(3, nextRun.apiServerSettings.maxParallelJobs);
  }

  @Test
  void guiChangeOfACommandLineValueIsPersisted() throws IOException {
    GlobalSettings globalSettings =
        startGuiSession("-di", "/cli/boards", "--gui.show_routing_summary=false");
    assertEquals("/cli/boards", globalSettings.guiSettings.inputDirectory);

    // Values the command line did not touch are saved as changed in the GUI.
    globalSettings.userProfileSettings.userEmail = "user@example.com";
    globalSettings.statistics.incrementJobsCompleted();
    GlobalSettings.saveAsJson(globalSettings);
    GlobalSettings saved = GlobalSettings.load();
    assertEquals(FILE_INPUT_DIRECTORY, saved.guiSettings.inputDirectory);
    assertTrue(saved.guiSettings.showRoutingSummary);
    assertEquals("user@example.com", saved.userProfileSettings.userEmail);
    assertEquals(1, saved.statistics.jobsCompleted);

    // The user opens a board elsewhere and ticks "show summary" in the GUI.
    globalSettings.guiSettings.inputDirectory = "/gui/boards";
    globalSettings.guiSettings.showRoutingSummary = true;
    GlobalSettings.saveAsJson(globalSettings);
    assertEquals("/gui/boards", GlobalSettings.load().guiSettings.inputDirectory);

    // Once the GUI has changed a value, it is the user's value, even when it goes back to the
    // one from the command line.
    globalSettings.guiSettings.inputDirectory = "/cli/boards";
    globalSettings.guiSettings.showRoutingSummary = false;
    GlobalSettings.saveAsJson(globalSettings);
    saved = GlobalSettings.load();
    assertEquals("/cli/boards", saved.guiSettings.inputDirectory);
    assertFalse(saved.guiSettings.showRoutingSummary);
  }

  @Test
  void explicitlySavedValueOnACommandLinePathSurvivesTheNextGuiSave() throws IOException {
    GlobalSettings globalSettings = startGuiSession("-di", "/cli/boards");

    // Opening a board from the command-line directory saves it explicitly (BoardFrameFileActions).
    globalSettings.setDefaultValue("gui.input_directory", "/cli/boards");
    GlobalSettings.saveAsJson(globalSettings);

    assertEquals("/cli/boards", GlobalSettings.load().guiSettings.inputDirectory);
  }

  @Test
  void environmentVariablesAreNotPersisted() throws IOException {
    GlobalSettings globalSettings = GlobalSettings.load();
    assertNotNull(globalSettings);
    globalSettings.applyNonRouterEnvironmentVariables(
        Map.of(
            "FREEROUTING__API_SERVER__AUTHENTICATION__ENABLED", "false",
            "FREEROUTING__API_SERVER__ENDPOINTS", "http://0.0.0.0:37864",
            "FREEROUTING__GUI__INPUT_DIRECTORY", "/env/boards",
            "PATH", "/usr/bin"));
    assertFalse(globalSettings.apiServerSettings.authentication.isEnabled);
    assertEquals("/env/boards", globalSettings.guiSettings.inputDirectory);

    GlobalSettings.saveAsJson(globalSettings);

    JsonObject saved = readConfigurationFile();
    JsonObject apiServer = saved.getAsJsonObject("api_server");
    assertTrue(apiServer.getAsJsonObject("authentication").get("enabled").getAsBoolean());
    assertEquals(
        "http://127.0.0.1:37864", apiServer.getAsJsonArray("endpoints").get(0).getAsString());
    assertEquals(
        FILE_INPUT_DIRECTORY, saved.getAsJsonObject("gui").get("input_directory").getAsString());
  }

  @Test
  void fileValueOfAnOverriddenPathIsKeptWhenTheFileCannotBeRead() throws IOException {
    GlobalSettings globalSettings = startGuiSession("--api_server.authentication.enabled=false");
    Files.delete(GlobalSettings.getConfigurationFilePath());

    GlobalSettings.saveAsJson(globalSettings);

    assertTrue(GlobalSettings.load().apiServerSettings.authentication.isEnabled);
  }

  @Test
  void saveWithoutOverridesWritesTheSettingsUnchanged() throws IOException {
    GlobalSettings globalSettings = startGuiSession();
    globalSettings.guiSettings.inputDirectory = "/gui/boards";

    GlobalSettings.saveAsJson(globalSettings);

    assertEquals(
        GsonProvider.GSON.toJson(globalSettings),
        Files.readString(GlobalSettings.getConfigurationFilePath(), StandardCharsets.UTF_8));
  }

  @Test
  void analyticsOptOutGivenOnTheCommandLineIsKept() throws IOException {
    GlobalSettings globalSettings = startGuiSession("-da");

    GlobalSettings.saveAsJson(globalSettings);

    assertTrue(GlobalSettings.load().usageAndDiagnosticData.disableAnalytics);
  }

  @Test
  void runtimeStateAppliedForTheProcessIsNotSaved() throws IOException {
    writeConfigurationFile(
        """
        {
          "version": "%s",
          "api_server": { "enabled": true }
        }
        """
            .formatted(GlobalSettings.getReleaseSafeVersion()));
    GlobalSettings globalSettings = startGuiSession();
    // The API server failed to start, and the MCP target was derived from a bound port.
    globalSettings.applyProcessOnly(
        () -> {
          globalSettings.apiServerSettings.isEnabled = false;
          globalSettings.mcpServerSettings.targetApiBaseUrl = "http://127.0.0.1:40000";
        });
    globalSettings.guiSettings.inputDirectory = "/gui/boards";

    GlobalSettings.saveAsJson(globalSettings);

    GlobalSettings nextRun = GlobalSettings.load();
    assertEquals("/gui/boards", nextRun.guiSettings.inputDirectory);
    assertTrue(nextRun.apiServerSettings.isEnabled);
    assertEquals(
        new McpServerSettings().targetApiBaseUrl, nextRun.mcpServerSettings.targetApiBaseUrl);
  }

  private static GlobalSettings startGuiSession(String... args) {
    return GlobalSettingsBootstrap.loadSettings(args);
  }

  private static JsonObject readConfigurationFile() throws IOException {
    return JsonParser.parseString(
            Files.readString(GlobalSettings.getConfigurationFilePath(), StandardCharsets.UTF_8))
        .getAsJsonObject();
  }

  private static void writeConfigurationFile(String json) throws IOException {
    Files.writeString(GlobalSettings.getConfigurationFilePath(), json, StandardCharsets.UTF_8);
  }
}
