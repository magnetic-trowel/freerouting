package app.freerouting.startup;

import app.freerouting.logger.FRLogger;
import app.freerouting.settings.AppPaths;
import app.freerouting.settings.GlobalSettings;
import app.freerouting.settings.LoggingSettings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

/**
 * Configures Log4j2 from the {@code logging} settings, which {@code freerouting.json}, {@code
 * FREEROUTING__LOGGING__*} environment variables and {@code --logging.*} command-line arguments set
 * like any other section (see {@link GlobalSettingsBootstrap#loadSettings}).
 *
 * <p>The settings are loaded before they can be applied, so {@link #initializeForStartup} first
 * logs to the console only: messages from loading the settings appear on the console, not in the
 * log file.
 */
public final class LoggingBootstrap {

  private static final String DEFAULT_CONSOLE_LEVEL = "INFO";
  private static final String DEFAULT_FILE_LEVEL = "DEBUG";
  private static final String LOG_FILE_NAME = "freerouting.log";

  /** Configuration snapshot produced by logging initialization. */
  public record LoggingConfig(
      boolean fileLoggingEnabled,
      boolean consoleLoggingEnabled,
      String fileLoggingLevel,
      String consoleLoggingLevel,
      String fileLoggingLocation,
      String fileLoggingPattern) {}

  private LoggingBootstrap() {}

  /** Logs to the console at the default level until {@link #initialize} applies the settings. */
  public static void initializeForStartup() {
    apply(new LoggingConfig(false, true, DEFAULT_FILE_LEVEL, DEFAULT_CONSOLE_LEVEL, "", null));
  }

  /**
   * Configures Log4j2 from the loaded settings.
   *
   * @param settings the settings, with the file, the environment and the command line applied
   * @param userdataPath resolved user data directory
   * @param userdataPathSource source of the user data directory
   * @return the active logging configuration
   */
  public static LoggingConfig initialize(
      GlobalSettings settings, Path userdataPath, String userdataPathSource) {
    LoggingSettings logging = settings.logging;
    String fileLevel = valueOr(logging.file.level, DEFAULT_FILE_LEVEL);
    if (settings.debugSettings.enableDetailedLogging) {
      fileLevel = "TRACE";
      FRLogger.granularTraceEnabled = true;
    }
    Path defaultLogDir =
        (userdataPathSource != null && userdataPathSource.startsWith("default"))
            ? AppPaths.getDefaultLogDirectory()
            : userdataPath;
    LoggingConfig config =
        new LoggingConfig(
            valueOr(logging.file.enabled, true),
            valueOr(logging.console.enabled, true),
            fileLevel,
            valueOr(logging.console.level, DEFAULT_CONSOLE_LEVEL),
            resolveLogPath(logging.file.location, defaultLogDir).toString(),
            logging.file.pattern);
    apply(config);
    return config;
  }

  private static void apply(LoggingConfig config) {
    System.setProperty("log4j2.disableJndi", "true");
    System.setProperty(
        "log4j2.configurationFactory", "app.freerouting.logger.Log4j2ConfigurationFactory");
    System.setProperty(
        "freerouting.logging.console.enabled", String.valueOf(config.consoleLoggingEnabled()));
    System.setProperty("freerouting.logging.console.level", config.consoleLoggingLevel());
    System.setProperty(
        "freerouting.logging.file.enabled", String.valueOf(config.fileLoggingEnabled()));
    System.setProperty("freerouting.logging.file.level", config.fileLoggingLevel());
    System.setProperty("freerouting.logging.file.location", config.fileLoggingLocation());
    if (config.fileLoggingPattern() != null) {
      System.setProperty("freerouting.logging.file.pattern", config.fileLoggingPattern());
    } else {
      System.clearProperty("freerouting.logging.file.pattern");
    }

    ((LoggerContext) LogManager.getContext(false)).reconfigure();
  }

  private static <T> T valueOr(T value, T defaultValue) {
    return value != null ? value : defaultValue;
  }

  /**
   * Returns the directory that a log location names: the location itself, or its parent when it
   * names a file ({@code *.log}).
   *
   * @param location a log file or directory
   * @return the normalized absolute directory
   */
  public static Path logDirectory(String location) {
    Path path = absolute(location);
    return isLogFile(path) && path.getParent() != null ? path.getParent() : path;
  }

  /**
   * Resolves a relative or absolute log path against the default directory.
   *
   * @param input optional specified log location
   * @param defaultDir fallback directory
   * @return normalized absolute log path
   */
  public static Path resolveLogPath(String input, Path defaultDir) {
    if (input == null || input.isBlank()) {
      return defaultDir.resolve(LOG_FILE_NAME).normalize().toAbsolutePath();
    }

    Path path = absolute(input);
    String filename = isLogFile(path) ? path.getFileName().toString() : LOG_FILE_NAME;
    Path folderPath = logDirectory(input);

    // Check if the directory exists, and create it if needed
    if (!folderPath.toFile().exists()) {
      try {
        Files.createDirectories(folderPath);
      } catch (IOException e) {
        // Failed to create directory, fallback to default
        return defaultDir.resolve(filename).normalize().toAbsolutePath();
      }
    }

    return folderPath.resolve(filename).normalize().toAbsolutePath();
  }

  private static Path absolute(String location) {
    // In Windows the leading "." character means current directory
    if (location.startsWith(".")) {
      location = Path.of(System.getProperty("user.dir")) + location.substring(1);
    }
    return Path.of(location).normalize().toAbsolutePath();
  }

  private static boolean isLogFile(Path path) {
    return path.getFileName() != null
        && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".log");
  }
}
