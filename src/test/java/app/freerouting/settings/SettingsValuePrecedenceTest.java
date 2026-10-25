package app.freerouting.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.freerouting.logger.FRLogger;
import app.freerouting.settings.sources.CliSettings;
import app.freerouting.settings.sources.DefaultSettings;
import app.freerouting.startup.GlobalSettingsBootstrap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Values from {@code freerouting.json} and the command line reach the settings they name. */
class SettingsValuePrecedenceTest {

  @TempDir Path userDataDir;

  @BeforeEach
  void setUp() {
    GlobalSettings.resetForTesting();
    GlobalSettings.setUserDataPath(userDataDir);
    FRLogger.getLogEntries().clear();
  }

  @AfterEach
  void tearDown() {
    GlobalSettings.resetForTesting();
  }

  /**
   * {@code false} and {@code 0} saved for a primitive setting whose default is {@code true} or
   * non-zero were dropped on load, because copying skipped primitive values equal to the Java
   * default.
   */
  @Test
  void falseAndZeroInTheSettingsFileAreLoaded() throws IOException {
    Files.writeString(
        userDataDir.resolve("freerouting.json"),
        """
        {
          "version": "%s",
          "gui": { "dialog_confirmation_timeout": 0 },
          "drc": {
            "include_warnings": false,
            "include_errors": false,
            "clearance_tolerance_um": 0.0
          }
        }
        """
            .formatted(GlobalSettings.getReleaseSafeVersion()),
        StandardCharsets.UTF_8);

    GlobalSettings loaded = GlobalSettings.load();

    assertNotNull(loaded);
    assertEquals(0, loaded.guiSettings.dialogConfirmationTimeout);
    assertFalse(loaded.drcSettings.includeWarnings);
    assertFalse(loaded.drcSettings.includeErrors);
    assertEquals(0.0, loaded.drcSettings.clearanceToleranceUm);
  }

  /**
   * The documentation allows {@code -} and {@code :} as level separators in a property name, but
   * router properties written that way were ignored by the router settings source.
   */
  @Test
  void routerPropertiesAcceptDashAndColonSeparators() {
    RouterSettings dashed =
        new SettingsMerger(
                new DefaultSettings(),
                new CliSettings(new String[] {"--router-autorouter-max_passes=5"}))
            .merge();
    RouterSettings colons =
        new SettingsMerger(
                new DefaultSettings(),
                new CliSettings(new String[] {"--router:optimizer:enabled=false"}))
            .merge();

    assertEquals(5, dashed.autorouter.maxPasses);
    assertFalse(colons.optimizer.enabled);
  }

  @Test
  @SuppressWarnings("deprecation")
  void dashedRouterPropertyIsNotSavedAsFileValue() {
    GlobalSettings globalSettings = new GlobalSettings();

    globalSettings.applyCommandLineArguments(new String[] {"--router-autorouter-max_passes=5"});

    assertEquals(null, globalSettings.routerSettings.autorouter.maxPasses);
  }

  @Test
  void scoringVersionAliasesNameTheScoringVersionFields() {
    assertEquals(
        List.of("router_scoring.version"),
        RouterPropertyNames.fieldPaths("router-scoring-version"));
    assertEquals(
        List.of("router_scoring.version"),
        RouterPropertyNames.fieldPaths("router.scoring.version"));
    assertEquals(
        List.of("optimizer_scoring.version"),
        RouterPropertyNames.fieldPaths("optimizer-scoring-version"));
    assertEquals(
        List.of("router_scoring.version", "optimizer_scoring.version"),
        RouterPropertyNames.fieldPaths("scoring-version"));
    assertEquals(List.of(), RouterPropertyNames.fieldPaths("api_server-endpoints"));
  }

  /**
   * A settings file without a profile id, such as one written by hand with only router options,
   * left the id null for good, and every command-line routing run failed in UUID.fromString.
   */
  @Test
  void settingsFileWithoutProfileIdGetsOne() throws IOException {
    Files.writeString(
        userDataDir.resolve("freerouting.json"),
        """
        { "version": "%s", "router": { "autorouter": { "max_passes": 3 } } }
        """
            .formatted(GlobalSettings.getReleaseSafeVersion()),
        StandardCharsets.UTF_8);

    GlobalSettings globalSettings = GlobalSettingsBootstrap.loadSettings(new String[0]);

    String userId = globalSettings.userProfileSettings.userId;
    assertNotNull(userId);
    assertEquals(userId, UUID.fromString(userId).toString());
    assertEquals(userId, GlobalSettings.load().userProfileSettings.userId, "the id is saved");
  }
}
