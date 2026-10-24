package app.freerouting.startup;

import app.freerouting.constants.Constants;
import app.freerouting.logger.FRLogger;
import app.freerouting.settings.GlobalSettings;
import app.freerouting.settings.RuntimeEnvironment;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Loads or initialises GlobalSettings, binds environment variables, records runtime environment
 * metrics, and parses command-line arguments into GlobalSettings.
 */
public final class GlobalSettingsBootstrap {

  private GlobalSettingsBootstrap() {}

  /**
   * Loads the settings ({@link #loadSettings}) and records the runtime environment ({@link
   * #recordRuntimeEnvironment}).
   *
   * @param args command line arguments
   * @param deferCpuCalibration whether to defer synthetic CPU benchmark calibration
   * @return initialized GlobalSettings object
   */
  public static GlobalSettings initialize(String[] args, boolean deferCpuCalibration) {
    GlobalSettings globalSettings = loadSettings(args);
    recordRuntimeEnvironment(globalSettings, args, deferCpuCalibration);
    return globalSettings;
  }

  /**
   * Loads GlobalSettings from {@code freerouting.json} (or the defaults), then applies the {@code
   * FREEROUTING__} environment variables and the command-line arguments, and makes them the {@link
   * GlobalSettings#current() current} settings.
   *
   * @param args command line arguments
   * @return the loaded settings
   */
  public static GlobalSettings loadSettings(String[] args) {
    GlobalSettings globalSettings = null;

    try {
      globalSettings = GlobalSettings.load();
      FRLogger.debug("Settings loaded from '" + GlobalSettings.getConfigurationFilePath() + "'.");
    } catch (NoSuchFileException _) {
      FRLogger.debug(
          "No freerouting.json found at '"
              + GlobalSettings.getConfigurationFilePath()
              + "' — will create one with default settings.");
    } catch (AccessDeniedException e) {
      FRLogger.warn(
          "Cannot read freerouting.json at '"
              + GlobalSettings.getConfigurationFilePath()
              + "': "
              + e.getReason()
              + ". The file and/or its parent directory may have incorrect permissions. "
              + "Check that the process has read access. "
              + "In Docker deployments, verify the volume mount configuration. "
              + "Freerouting will start with default settings.");
    } catch (IOException e) {
      FRLogger.warn(
          "Failed to load freerouting.json from '"
              + GlobalSettings.getConfigurationFilePath()
              + "': "
              + e.getMessage()
              + ". Freerouting will start with default settings.");
    }

    // A freerouting.json without a valid profile id (for example one written by hand with only
    // router options) loads with a null id, which the CLI and API pass to UUID.fromString.
    if (globalSettings != null && !isValidUserId(globalSettings.userProfileSettings.userId)) {
      globalSettings.userProfileSettings.userId = UUID.randomUUID().toString();
      try {
        GlobalSettings.saveAsJson(globalSettings);
      } catch (IOException e) {
        FRLogger.warn(
            "Failed to save the new profile id to '"
                + GlobalSettings.getConfigurationFilePath()
                + "': "
                + e.getMessage());
      }
    }

    if ((globalSettings == null)
        || !GlobalSettings.getReleaseSafeVersion().equals(globalSettings.version)) {
      String userId =
          globalSettings == null
              ? UUID.randomUUID().toString()
              : globalSettings.userProfileSettings.userId;

      globalSettings = new GlobalSettings();
      globalSettings.userProfileSettings.userId = userId;
      globalSettings.version = GlobalSettings.getReleaseSafeVersion();

      try {
        GlobalSettings.saveAsJson(globalSettings);
        FRLogger.debug(
            "Default settings saved to '" + GlobalSettings.getConfigurationFilePath() + "'.");
      } catch (AccessDeniedException e) {
        FRLogger.warn(
            "Cannot write freerouting.json to '"
                + GlobalSettings.getConfigurationFilePath()
                + "': "
                + e.getReason()
                + ". The directory and/or file may have incorrect permissions. "
                + "Check that the process has write access. "
                + "In Docker deployments, verify the volume mount configuration. "
                + "Settings won't be persisted across restarts.");
      } catch (IOException e) {
        FRLogger.warn(
            "Failed to save freerouting.json to '"
                + GlobalSettings.getConfigurationFilePath()
                + "': "
                + e.getMessage()
                + ". Settings won't be persisted across restarts.");
      }
    }

    globalSettings.applyNonRouterEnvironmentVariables();
    globalSettings.applyCommandLineArguments(args);
    GlobalSettings.setCurrent(globalSettings);
    return globalSettings;
  }

  /**
   * Records the version, the command line and the hardware in the runtime environment of the
   * settings, and logs the hardware.
   *
   * @param globalSettings the loaded settings
   * @param args command line arguments
   * @param deferCpuCalibration whether to defer synthetic CPU benchmark calibration
   */
  public static void recordRuntimeEnvironment(
      GlobalSettings globalSettings, String[] args, boolean deferCpuCalibration) {
    globalSettings.runtimeEnvironment.freeroutingVersion =
        Constants.FREEROUTING_VERSION + "," + Constants.FREEROUTING_BUILD_DATE;
    globalSettings.runtimeEnvironment.appStartedAt = Instant.now();
    globalSettings.runtimeEnvironment.commandLineArguments =
        RuntimeEnvironment.sanitizeCommandLineArguments(args);
    globalSettings.runtimeEnvironment.architecture =
        System.getProperty("os.name")
            + ","
            + System.getProperty("os.arch")
            + ","
            + System.getProperty("os.version");
    globalSettings.runtimeEnvironment.java =
        System.getProperty("java.version") + "," + System.getProperty("java.vendor");
    globalSettings.runtimeEnvironment.systemLanguage =
        Locale.getDefault().getLanguage() + "," + Locale.getDefault();
    globalSettings.runtimeEnvironment.cpuCores = Runtime.getRuntime().availableProcessors();
    globalSettings.runtimeEnvironment.ram = (int) (Runtime.getRuntime().maxMemory() / 1024 / 1024);
    if (!deferCpuCalibration) {
      globalSettings.runtimeEnvironment.cpuScore = RuntimeEnvironment.measureCpuScore();
      FRLogger.info(
          "Hardware: "
              + globalSettings.runtimeEnvironment.cpuCores
              + " CPU cores, "
              + globalSettings.runtimeEnvironment.cpuScore
              + " CPU score, "
              + globalSettings.runtimeEnvironment.ram
              + " MB RAM");
    } else {
      FRLogger.info(
          "Hardware: "
              + globalSettings.runtimeEnvironment.cpuCores
              + " CPU cores, "
              + globalSettings.runtimeEnvironment.ram
              + " MB RAM");
    }
    FRLogger.debug("UTC Time: " + globalSettings.runtimeEnvironment.appStartedAt);
  }

  private static boolean isValidUserId(String userId) {
    if (userId == null || userId.isBlank()) {
      return false;
    }
    try {
      UUID.fromString(userId);
      return true;
    } catch (IllegalArgumentException _) {
      return false;
    }
  }
}
