package app.freerouting.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.freerouting.util.gson.GsonProvider;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
 * Loading freerouting.json copies every primitive value of the parsed file onto the defaults, so a
 * value the file leaves out must parse as the field's default. That holds only while Gson builds
 * every settings class with its no-arg constructor; a class it had to allocate without one would
 * load as {@code false} and {@code 0} and overwrite the defaults.
 */
class SettingsFileDefaultsTest {

  @TempDir Path userDataDir;

  @BeforeEach
  void setUp() {
    GlobalSettings.resetForTesting();
    GlobalSettings.setUserDataPath(userDataDir);
  }

  @AfterEach
  void tearDown() {
    GlobalSettings.resetForTesting();
  }

  @Test
  void sectionsWithoutValuesLoadAsTheDefaults() throws IOException {
    JsonObject defaults = GsonProvider.GSON.toJsonTree(new GlobalSettings()).getAsJsonObject();
    JsonObject file = sectionsOnly(defaults);
    file.addProperty("version", GlobalSettings.getReleaseSafeVersion());
    Files.writeString(
        GlobalSettings.getConfigurationFilePath(),
        GsonProvider.GSON.toJson(file),
        StandardCharsets.UTF_8);

    GlobalSettings loaded = GlobalSettings.load();

    JsonObject actual = GsonProvider.GSON.toJsonTree(loaded).getAsJsonObject();
    for (String volatileSection : new String[] {"version", "profile", "statistics"}) {
      defaults.remove(volatileSection);
      actual.remove(volatileSection);
    }
    assertEquals(defaults, actual);
  }

  /** Returns {@code tree} with every object kept and every value and array left out. */
  private static JsonObject sectionsOnly(JsonObject tree) {
    JsonObject result = new JsonObject();
    for (Map.Entry<String, JsonElement> entry : tree.entrySet()) {
      if (entry.getValue().isJsonObject()) {
        result.add(entry.getKey(), sectionsOnly(entry.getValue().getAsJsonObject()));
      }
    }
    return result;
  }
}
