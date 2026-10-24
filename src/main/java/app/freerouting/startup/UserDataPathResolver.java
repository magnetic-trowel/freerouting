package app.freerouting.startup;

import app.freerouting.settings.AppPaths;
import app.freerouting.settings.GlobalSettings;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

/**
 * Resolves the user-data path according to OS defaults, environment variables, and command-line
 * arguments, checks permissions, and locks the path in GlobalSettings.
 */
public final class UserDataPathResolver {

  /** Container for the resolved user data path and its source description. */
  public record ResolvedUserDataPath(Path path, String source) {}

  private UserDataPathResolver() {}

  /**
   * Resolves, verifies, and locks the user data path in {@link GlobalSettings}.
   *
   * @param args command line arguments
   * @return the resolved path and description
   */
  public static ResolvedUserDataPath resolveAndLock(String[] args) {
    ResolvedUserDataPath resolved = resolve(args, System.getenv());
    Path userdataPath = resolved.path();
    String userdataPathSource = resolved.source();

    File dir = userdataPath.toFile();
    if (!dir.exists()) {
      if (!dir.mkdirs()) {
        System.err.println(
            "WARNING: Could not create user-data directory '"
                + userdataPath
                + "' (source: "
                + userdataPathSource
                + "). "
                + "Freerouting will attempt to create it when writing files. "
                + "If this persists, check permissions for the specified path.");
      }
    } else {
      if (!dir.canRead()) {
        System.err.println(
            "WARNING: User-data directory '"
                + userdataPath
                + "' (source: "
                + userdataPathSource
                + ") exists but is NOT READABLE. "
                + "freerouting.json cannot be loaded. "
                + "Check that the process has read permission on the directory. "
                + "In Docker deployments, verify the volume mount and file ownership.");
      }
      if (!dir.canWrite()) {
        System.err.println(
            "WARNING: User-data directory '"
                + userdataPath
                + "' (source: "
                + userdataPathSource
                + ") exists but is NOT WRITABLE. "
                + "freerouting.json cannot be saved and settings won't be persisted. "
                + "Check that the process has write permission on the directory. "
                + "In Docker deployments, verify the volume mount and file ownership.");
      }
    }

    GlobalSettings.setUserDataPath(userdataPath);
    GlobalSettings.lockUserDataPath();

    return resolved;
  }

  /**
   * Resolves the user data path: the {@code --user_data_path} argument, else the {@code
   * FREEROUTING__USER_DATA_PATH} environment variable, else the directory of the log location in
   * {@code FREEROUTING__LOGGING__FILE__LOCATION} (deprecated: the directory, also when the location
   * names a log file), else the OS default.
   *
   * @param args command line arguments
   * @param environment environment variables by name
   * @return the path and a description of where it came from
   */
  static ResolvedUserDataPath resolve(String[] args, Map<String, String> environment) {
    if (args != null) {
      var userDataPathArg =
          Arrays.stream(args).filter(s -> s.startsWith("--user_data_path=")).findFirst();
      if (userDataPathArg.isPresent()) {
        return new ResolvedUserDataPath(
            Path.of(userDataPathArg.get().substring("--user_data_path=".length())),
            "CLI argument --user_data_path");
      }
    }
    String userDataPath = environment.get("FREEROUTING__USER_DATA_PATH");
    if (userDataPath != null) {
      return new ResolvedUserDataPath(
          Path.of(userDataPath), "environment variable FREEROUTING__USER_DATA_PATH");
    }
    String logLocation = environment.get("FREEROUTING__LOGGING__FILE__LOCATION");
    if (logLocation != null && !logLocation.isBlank()) {
      return new ResolvedUserDataPath(
          LoggingBootstrap.logDirectory(logLocation),
          "environment variable FREEROUTING__LOGGING__FILE__LOCATION (deprecated fallback)");
    }
    return new ResolvedUserDataPath(
        AppPaths.getDefaultUserDataPath(), "default (OS standard user-data path)");
  }
}
