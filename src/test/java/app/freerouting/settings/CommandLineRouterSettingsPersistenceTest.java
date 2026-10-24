package app.freerouting.settings;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.freerouting.logger.FRLogger;
import app.freerouting.settings.sources.CliSettings;
import app.freerouting.settings.sources.DefaultSettings;
import app.freerouting.settings.sources.JsonFileSettings;
import app.freerouting.startup.GlobalSettingsBootstrap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Router options given on the command line apply to the run they were given for. A GUI session
 * saves {@code freerouting.json} several times (opening a board, finishing a routing run, closing
 * the window), and those saves must not turn the command-line router options into persistent
 * defaults that {@link JsonFileSettings} then applies to every later CLI and API run.
 */
class CommandLineRouterSettingsPersistenceTest {

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
          "router": {
            "autorouter": { "max_passes": 100 },
            "optimizer": { "improvement_threshold": 2.5 }
          }
        }
        """
            .formatted(GlobalSettings.getReleaseSafeVersion()));
  }

  @AfterEach
  void tearDown() {
    GlobalSettings.resetForTesting();
  }

  @Test
  void guiSaveDoesNotPersistCommandLineRouterOptions() throws IOException {
    String[] args = {
      "--router.autorouter.max_passes=3",
      "--router.optimizer.max_passes=1",
      "-mt",
      "1",
      "-oit",
      "0.5",
      "-inc",
      "GND"
    };
    GlobalSettings globalSettings = startGuiSession(args);

    // The options are effective for this run.
    assertEquals(3, globalSettings.getMaxPasses());
    assertEquals(3, mergeForThisRun(args).autorouter.maxPasses);
    assertEquals(1, mergeForThisRun(args).optimizer.maxPasses);

    // The GUI saves the settings, e.g. after a routing run or when the window is closed.
    GlobalSettings.saveAsJson(globalSettings);

    RouterSettings persisted = readPersistedRouterSettings();
    assertEquals(
        100, persisted.autorouter.maxPasses, "the file value must survive, not the CLI value");
    assertEquals(2.5f, persisted.optimizer.optimizationImprovementThreshold, 0.0001f);
    assertNull(persisted.optimizer.maxPasses, "--router.optimizer.max_passes must not be saved");
    assertNull(persisted.optimizer.maxThreads, "-mt must not be saved");
    assertNull(persisted.autorouter.ignoreNetClasses, "-inc must not be saved");

    JsonObject router = readConfigurationFile().getAsJsonObject("router");
    assertFalse(router.getAsJsonObject("optimizer").has("max_threads"));
    assertFalse(router.getAsJsonObject("optimizer").has("max_passes"));
  }

  @Test
  void legacyMaxPassesFlagIsNotPersisted() throws IOException {
    writeConfigurationFile(
        "{ \"version\": \"%s\" }".formatted(GlobalSettings.getReleaseSafeVersion()));
    GlobalSettings globalSettings = startGuiSession(new String[] {"-mp", "5"});
    assertEquals(5, globalSettings.getMaxPasses());

    GlobalSettings.saveAsJson(globalSettings);

    assertNull(readPersistedRouterSettings().autorouter.maxPasses);
    // A later run without command-line options falls back to the default again.
    assertEquals(
        new DefaultSettings().getSettings().autorouter.maxPasses,
        mergeForThisRun(new String[0]).autorouter.maxPasses);
  }

  @Test
  void guiSaveKeepsRouterSettingsFromFileAndPersistsGuiChanges() throws IOException {
    GlobalSettings globalSettings = startGuiSession(new String[] {"-mt", "1"});
    String boardDirectory = userDataDir.resolve("boards").toString();
    globalSettings.guiSettings.inputDirectory = boardDirectory;

    GlobalSettings.saveAsJson(globalSettings);

    GlobalSettings reloaded = GlobalSettings.load();
    assertNotNull(reloaded);
    assertEquals(boardDirectory, reloaded.guiSettings.inputDirectory);
    RouterSettings persisted = readPersistedRouterSettings();
    assertEquals(100, persisted.autorouter.maxPasses);
    assertEquals(2.5f, persisted.optimizer.optimizationImprovementThreshold, 0.0001f);
  }

  @Test
  void explicitlySavedRouterValueIsPersisted() throws IOException {
    startGuiSession(new String[] {"--router.autorouter.max_passes=3"});

    assertEquals(
        Boolean.TRUE, GlobalSettings.setDefaultValue("router.autorouter.max_passes", "42"));
    assertEquals(
        Boolean.TRUE,
        GlobalSettings.setDefaultValue("router.autorouter.ignore_net_classes", "GND,VCC"));

    RouterSettings persisted = readPersistedRouterSettings();
    assertEquals(42, persisted.autorouter.maxPasses);
    assertArrayEquals(new String[] {"GND", "VCC"}, persisted.autorouter.ignoreNetClasses);
    assertEquals(2.5f, persisted.optimizer.optimizationImprovementThreshold, 0.0001f);
  }

  @Test
  void commandLineRouterOptionsDoNotChangeTheLoadedFileValues() throws IOException {
    GlobalSettings globalSettings = GlobalSettings.load();
    assertNotNull(globalSettings);

    globalSettings.applyCommandLineArguments(
        new String[] {"--router.autorouter.max_passes=3", "-mp", "7", "-drc"});

    assertEquals(7, globalSettings.getMaxPasses());
    @SuppressWarnings("deprecation") // checks the serialization bridge itself
    RouterSettings fileLayer = globalSettings.routerSettings;
    assertEquals(100, fileLayer.autorouter.maxPasses);
    assertNull(fileLayer.autorouter.enabled, "-drc must not disable the router in the file");
  }

  private static GlobalSettings startGuiSession(String[] args) {
    return GlobalSettingsBootstrap.initialize(args, "", true);
  }

  private static RouterSettings mergeForThisRun(String[] args) {
    return new SettingsMerger(
            new DefaultSettings(),
            new JsonFileSettings(GlobalSettings.getConfigurationFilePath()),
            new CliSettings(args))
        .merge();
  }

  private static RouterSettings readPersistedRouterSettings() {
    return new JsonFileSettings(GlobalSettings.getConfigurationFilePath()).getSettings();
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
