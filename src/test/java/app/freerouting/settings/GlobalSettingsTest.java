package app.freerouting.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.logger.FRLogger;
import app.freerouting.settings.sources.CliSettings;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GlobalSettingsTest {

  @BeforeEach
  void setUp() {
    FRLogger.getLogEntries().clear();
  }

  @Test
  void applyCommandLineArgumentsValidArguments() {
    GlobalSettings settings = new GlobalSettings();
    String[] args =
        new String[] {
          "-mp", "10",
          "-di", "/tmp",
          "-dl"
        };

    settings.applyCommandLineArguments(args);

    assertEquals("/tmp", settings.guiSettings.inputDirectory);
    assertFalse(settings.logging.file.enabled);
    // -mp and its value are a router option, which CliSettings reads.
    assertEquals(0, FRLogger.getLogEntries().getWarningCount());
  }

  @Test
  void applyCommandLineArgumentsLogLevel() {
    GlobalSettings settings = new GlobalSettings();
    String[] args = new String[] {"-ll", "DEBUG"};

    settings.applyCommandLineArguments(args);

    assertEquals("DEBUG", settings.logging.console.level);
  }

  @Test
  void applyCommandLineArgumentsUnknownFlag() {
    GlobalSettings settings = new GlobalSettings();
    String[] args = new String[] {"-unknownFlag"};

    settings.applyCommandLineArguments(args);

    // Expect a warning (this will fail until implemented)
    assertEquals(
        1, FRLogger.getLogEntries().getWarningCount(), "Should have 1 warning for unknown flag");
    assertTrue(
        Arrays.stream(FRLogger.getLogEntries().get())
            .anyMatch(s -> s.contains("Unknown command line argument: -unknownFlag")));
  }

  @Test
  void applyCommandLineArgumentsUnknownValue() {
    GlobalSettings settings = new GlobalSettings();
    // "-mp 10" consumes 2 args. "extraValue" is loose.
    String[] args = new String[] {"-mp", "10", "extraValue"};

    settings.applyCommandLineArguments(args);

    assertEquals(
        1, FRLogger.getLogEntries().getWarningCount(), "Should warn for the extra value only");
    assertTrue(
        Arrays.stream(FRLogger.getLogEntries().get())
            .anyMatch(s -> s.contains("Unknown command line argument: extraValue")));
  }

  /**
   * Router options on the command line are read once, by CliSettings: GlobalSettings used to parse
   * them too, into a copy nothing read, and warned about the documented scoring-version aliases.
   */
  @Test
  @SuppressWarnings("deprecation") // checks that the persisted router settings stay untouched
  void routerOptionsAreLeftToTheRouterSettingsSource() {
    GlobalSettings settings = new GlobalSettings();
    String[] args =
        new String[] {
          "--router.autorouter.max_threads=1",
          "--router.optimizer.max_threads=4",
          "--router.scoring.version=v1",
          "--router-scoring-version=v1",
          "--scoring-version=v1",
          "--optimizer-scoring-version=v1",
          "-mp",
          "10000"
        };

    settings.applyCommandLineArguments(args);

    assertEquals(0, FRLogger.getLogEntries().getWarningCount());
    assertEquals(null, settings.routerSettings.optimizer.maxThreads);
  }

  @Test
  void userDataPathInTheEnvironmentIsNotAnUnknownProperty() {
    GlobalSettings settings = new GlobalSettings();

    settings.applyNonRouterEnvironmentVariables(
        java.util.Map.of(
            "FREEROUTING__USER_DATA_PATH", "/data", "FREEROUTING__ROUTER__SCORING__VERSION", "v1"));

    assertEquals(0, FRLogger.getLogEntries().getWarningCount());
  }

  /** {@code -mp 10000} asks for many passes: it is capped at 9999, not read as unlimited (0). */
  @Test
  void tooManyPassesAreCappedNotUnlimited() {
    RouterSettings settings = new CliSettings(new String[] {"-mp", "10000"}).getSettings();

    settings.validate();

    assertEquals(9999, settings.autorouter.maxPasses);
  }

  @Test
  void setUnknownProperty() {
    GlobalSettings settings = new GlobalSettings();
    // This should log a warning instead of incorrect stack trace
    settings.setValue("unknown_settings.unknown_field", "true");

    assertEquals(
        1, FRLogger.getLogEntries().getWarningCount(), "Should log a warning for unknown property");
    assertEquals(
        0, FRLogger.getLogEntries().getErrorCount(), "Should NOT log an error with stack trace");
    assertTrue(
        Arrays.stream(FRLogger.getLogEntries().get())
            .anyMatch(
                s -> s.contains("Unknown settings property: unknown_settings.unknown_field")));
  }

  @Test
  void applyCommandLineArgumentsMalformedDoubleHyphen() {
    GlobalSettings settings = new GlobalSettings();
    String[] args = new String[] {"--someflag"};

    settings.applyCommandLineArguments(args);

    assertEquals(
        1,
        FRLogger.getLogEntries().getWarningCount(),
        "Should warn on double-dash arg without equals");
    assertTrue(
        Arrays.stream(FRLogger.getLogEntries().get())
            .anyMatch(s -> s.contains("Unknown command line argument: --someflag")));
  }
}
