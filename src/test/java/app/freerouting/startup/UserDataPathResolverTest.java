package app.freerouting.startup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UserDataPathResolverTest {

  @TempDir Path dir;

  /**
   * The deprecated fallback to {@code FREEROUTING__LOGGING__FILE__LOCATION} takes the log's
   * directory: a log file path used to become the user-data directory itself, so a directory named
   * after the log file was created and the file appender failed on it.
   */
  @Test
  void logFileLocationFallsBackToItsDirectory() {
    Path logFile = dir.resolve("fr.log");

    UserDataPathResolver.ResolvedUserDataPath resolved =
        UserDataPathResolver.resolve(
            new String[0], Map.of("FREEROUTING__LOGGING__FILE__LOCATION", logFile.toString()));

    assertEquals(dir.toAbsolutePath(), resolved.path());
  }

  @Test
  void userDataPathWinsOverTheLogLocationAndTheCommandLineWinsOverBoth() {
    Map<String, String> environment =
        Map.of(
            "FREEROUTING__USER_DATA_PATH", dir.resolve("env").toString(),
            "FREEROUTING__LOGGING__FILE__LOCATION", dir.resolve("logs").toString());

    assertEquals(
        dir.resolve("env"), UserDataPathResolver.resolve(new String[0], environment).path());
    assertEquals(
        dir.resolve("cli"),
        UserDataPathResolver.resolve(
                new String[] {"--user_data_path=" + dir.resolve("cli")}, environment)
            .path());
  }
}
