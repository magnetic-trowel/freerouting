package app.freerouting.startup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.settings.GlobalSettings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Logging is configured from the {@code logging} settings, which {@code freerouting.json}, the
 * environment and the command line set like any other section. The bootstrap used to read only the
 * environment and the command line, with its own parser.
 */
class LoggingBootstrapTest {

  // File logging is switched off in every case: the location is still resolved (and its directory
  // created), but no appender holds the file open, which on Windows would stop @TempDir cleanup.
  private static final String NO_FILE_LOG = "--logging.file.enabled=false";

  @TempDir Path userData;

  @BeforeEach
  void useTemporaryUserData() {
    GlobalSettings.resetForTesting();
    GlobalSettings.setUserDataPath(userData);
  }

  @AfterEach
  void clearSystemProperties() {
    GlobalSettings.resetForTesting();
    System.clearProperty("freerouting.logging.console.enabled");
    System.clearProperty("freerouting.logging.console.level");
    System.clearProperty("freerouting.logging.file.enabled");
    System.clearProperty("freerouting.logging.file.level");
    System.clearProperty("freerouting.logging.file.location");
    System.clearProperty("freerouting.logging.file.pattern");
    ((LoggerContext) LogManager.getContext(false)).reconfigure();
  }

  @Test
  void settingsFileConfiguresLogging() throws IOException {
    writeSettingsFile(
        GlobalSettings.getReleaseSafeVersion(),
        """
        "logging": {
          "console": { "level": "ERROR" },
          "file": { "enabled": false, "level": "WARN", "location": "%s" }
        }
        """
            .formatted(jsonPath(userData.resolve("logs"))));

    LoggingBootstrap.LoggingConfig config = configure();

    assertEquals("ERROR", config.consoleLoggingLevel());
    assertTrue(config.consoleLoggingEnabled());
    assertFalse(config.fileLoggingEnabled());
    assertEquals("WARN", config.fileLoggingLevel());
    assertEquals(
        userData.resolve("logs").resolve("freerouting.log").toAbsolutePath().toString(),
        config.fileLoggingLocation());
  }

  @Test
  void commandLineOverridesTheSettingsFileAndParsesBooleansLikeOtherSettings() throws IOException {
    writeSettingsFile(
        GlobalSettings.getReleaseSafeVersion(),
        "\"logging\": { \"console\": { \"enabled\": true, \"level\": \"ERROR\" } }");

    LoggingBootstrap.LoggingConfig config =
        configure(
            "--logging.file.enabled=off",
            "--logging.console.enabled=no",
            "--logging.console.level=WARN");

    assertFalse(config.fileLoggingEnabled());
    assertFalse(config.consoleLoggingEnabled());
    assertEquals("WARN", config.consoleLoggingLevel());
  }

  /** Earlier versions wrote their defaults and the resolved log file there, and never read it. */
  @Test
  void loggingValuesWrittenByEarlierVersionsAreIgnored() throws IOException {
    writeSettingsFile(
        "2.5.0",
        """
        "logging": {
          "console": { "enabled": true, "level": "INFO" },
          "file": { "enabled": true, "level": "INFO", "location": "%s" }
        }
        """
            .formatted(jsonPath(userData.resolve("stale").resolve("freerouting.log"))));

    LoggingBootstrap.LoggingConfig config = configure(NO_FILE_LOG);

    assertEquals("DEBUG", config.fileLoggingLevel(), "the default file level applies");
    assertEquals(
        userData.resolve("freerouting.log").toAbsolutePath().toString(),
        config.fileLoggingLocation());
    assertFalse(Files.exists(userData.resolve("stale")));
  }

  @Test
  void locationMayNameTheLogFile() {
    Path logFile = userData.resolve("logs").resolve("fr.log");

    LoggingBootstrap.LoggingConfig config =
        configure(NO_FILE_LOG, "--logging.file.location=" + logFile);

    assertEquals(logFile.toAbsolutePath().toString(), config.fileLoggingLocation());
    assertFalse(Files.isDirectory(logFile), "the log file must not become a directory");
  }

  /**
   * {@code -l} is the language option. Logging bootstrap used to read it as the log file location
   * too, so {@code -l en} also wrote the log into a new {@code ./en} directory.
   */
  @Test
  void languageOptionDoesNotSetTheLogLocation() {
    String language = "zz-freerouting-test-locale-" + System.nanoTime();
    Path createdDir = Path.of(System.getProperty("user.dir"), language);
    try {
      LoggingBootstrap.LoggingConfig config =
          configure(NO_FILE_LOG, "-de", "board.dsn", "-l", language);

      assertEquals(
          userData.resolve("freerouting.log").toAbsolutePath().toString(),
          config.fileLoggingLocation());
      assertFalse(Files.exists(createdDir), "-l must not create a log directory");
    } finally {
      try {
        Files.deleteIfExists(createdDir);
      } catch (IOException ignored) {
        // best-effort test cleanup
      }
    }
  }

  private LoggingBootstrap.LoggingConfig configure(String... args) {
    GlobalSettings settings = GlobalSettingsBootstrap.loadSettings(args);
    return LoggingBootstrap.initialize(settings, userData, "command line");
  }

  private void writeSettingsFile(String version, String loggingMember) throws IOException {
    Files.writeString(
        userData.resolve("freerouting.json"),
        "{ \"version\": \"" + version + "\", " + loggingMember + " }");
  }

  private static String jsonPath(Path path) {
    return path.toAbsolutePath().toString().replace("\\", "\\\\");
  }
}
