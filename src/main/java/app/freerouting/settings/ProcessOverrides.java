package app.freerouting.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Remembers the persisted settings that command-line arguments and {@code FREEROUTING__}
 * environment variables changed in this process, so that saving {@code freerouting.json} does not
 * turn them into defaults for later runs.
 *
 * <p>Values are compared in their JSON form, keyed by the path of JSON member names (for example
 * {@code api_server / authentication / enabled}). A recorded path is written with the value the
 * file holds for it (or left out when the file has none) as long as its live value is still the one
 * the command line or environment set. Once the live value differs, something in this process
 * changed it, for example the user through the GUI, and from then on the live value is saved.
 */
final class ProcessOverrides {

  /** The value the command line or environment set, per JSON path. */
  private final Map<List<String>, JsonElement> overrideValues = new LinkedHashMap<>();

  /** The value before the first override, per JSON path; {@code null} when there was none. */
  private final Map<List<String>, JsonElement> originalValues = new HashMap<>();

  /**
   * Records every value that differs between two JSON snapshots of the settings, taken before and
   * after applying command-line arguments or environment variables.
   */
  synchronized void record(JsonObject before, JsonObject after) {
    recordDifferences(before, after, new ArrayList<>());
  }

  /** Returns whether nothing was overridden for this process. */
  synchronized boolean isEmpty() {
    return overrideValues.isEmpty();
  }

  /**
   * Replaces the overridden values in {@code live} with their persisted values.
   *
   * @param live JSON form of the current settings; it is modified in place
   * @param persisted JSON form of the settings currently saved in the file, or {@code null} when
   *     the file cannot be read, in which case the values from before the overrides are used
   */
  synchronized void restorePersistedValues(JsonObject live, JsonObject persisted) {
    Iterator<Map.Entry<List<String>, JsonElement>> iterator = overrideValues.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<List<String>, JsonElement> entry = iterator.next();
      List<String> path = entry.getKey();
      if (Objects.equals(get(live, path), entry.getValue())) {
        JsonElement persistedValue =
            persisted != null ? get(persisted, path) : originalValues.get(path);
        set(live, path, persistedValue);
      } else {
        // Changed after the override, so it is a value of this session that must be saved.
        iterator.remove();
        originalValues.remove(path);
      }
    }
  }

  private void recordDifferences(JsonObject before, JsonObject after, List<String> path) {
    Set<String> names = new TreeSet<>(before.keySet());
    names.addAll(after.keySet());
    for (String name : names) {
      JsonElement beforeValue = before.get(name);
      JsonElement afterValue = after.get(name);
      path.add(name);
      if (beforeValue != null
          && afterValue != null
          && beforeValue.isJsonObject()
          && afterValue.isJsonObject()) {
        recordDifferences(beforeValue.getAsJsonObject(), afterValue.getAsJsonObject(), path);
      } else if (!Objects.equals(beforeValue, afterValue)) {
        List<String> key = List.copyOf(path);
        if (!overrideValues.containsKey(key)) {
          originalValues.put(key, beforeValue == null ? null : beforeValue.deepCopy());
        }
        overrideValues.put(key, afterValue == null ? null : afterValue.deepCopy());
      }
      path.removeLast();
    }
  }

  private static JsonElement get(JsonObject root, List<String> path) {
    JsonElement current = root;
    for (String name : path) {
      if (current == null || !current.isJsonObject()) {
        return null;
      }
      current = current.getAsJsonObject().get(name);
    }
    return current;
  }

  private static void set(JsonObject root, List<String> path, JsonElement value) {
    JsonObject parent = root;
    for (String name : path.subList(0, path.size() - 1)) {
      JsonElement child = parent.get(name);
      if (child == null || !child.isJsonObject()) {
        if (value == null) {
          return;
        }
        child = new JsonObject();
        parent.add(name, child);
      }
      parent = child.getAsJsonObject();
    }
    String leaf = path.getLast();
    if (value == null) {
      parent.remove(leaf);
    } else {
      parent.add(leaf, value.deepCopy());
    }
  }
}
