package app.freerouting.settings;

import java.util.List;
import java.util.Locale;

/**
 * The one table of router property names: command-line arguments ({@link
 * app.freerouting.settings.sources.CliSettings}), {@code FREEROUTING__ROUTER__*} environment
 * variables ({@link app.freerouting.settings.sources.EnvironmentVariablesSource}) and {@link
 * GlobalSettings#setValue} all map a name to {@link RouterSettings} field paths here, so they
 * accept the same names and values.
 *
 * <p>Accepted names: {@code router.<path>}, with {@code .}, {@code :} or {@code -} between levels
 * (for example {@code router-autorouter-max_passes}); {@code optimizer.<path>} as a short form of
 * {@code router.optimizer.<path>}; {@code router.scoring.version} (also {@code
 * router-scoring-version}) for {@code router.router_scoring.version}; {@code
 * optimizer.scoring.version} (also {@code optimizer-scoring-version}) for {@code
 * router.optimizer_scoring.version}; {@code scoring-version} for both; and the deprecated flat
 * autorouter keys such as {@code router.max_passes}, which warn.
 */
public final class RouterPropertyNames {

  private static final String ROUTER_SCORING_VERSION = "router_scoring.version";
  private static final String OPTIMIZER_SCORING_VERSION = "optimizer_scoring.version";

  private RouterPropertyNames() {}

  /** Returns whether {@code propertyName} names a router setting. */
  public static boolean isRouterProperty(String propertyName) {
    return relativePath(propertyName) != null;
  }

  /**
   * Returns the {@link RouterSettings} field paths that {@code propertyName} sets, or an empty list
   * when it is not a router property. A deprecated flat autorouter key logs a warning.
   *
   * @param propertyName a property name such as {@code router.autorouter.max_passes}
   */
  public static List<String> fieldPaths(String propertyName) {
    String relative = relativePath(propertyName);
    if (relative == null) {
      return List.of();
    }
    switch (relative) {
      case "scoring-version" -> {
        return List.of(ROUTER_SCORING_VERSION, OPTIMIZER_SCORING_VERSION);
      }
      case "scoring.version" -> {
        return List.of(ROUTER_SCORING_VERSION);
      }
      case "optimizer.scoring.version" -> {
        return List.of(OPTIMIZER_SCORING_VERSION);
      }
      default -> {
        if (LegacyRouterSettingsBridge.isDeprecatedFlatAutorouterPath(relative)) {
          LegacyRouterSettingsBridge.warnDeprecatedPath(
              "router." + relative, "router.autorouter." + relative);
          return List.of("autorouter." + relative);
        }
        return List.of(relative);
      }
    }
  }

  /**
   * Maps the short scoring-version names ({@code v1}, {@code v2}, {@code v3}, {@code legacy},
   * {@code continuous}, {@code lower_bound}, {@code equivalent_length}) to their enum constants for
   * a {@code *.version} field path from {@link #fieldPaths}; returns any other value unchanged.
   */
  public static String normalizeValue(String fieldPath, String value) {
    if (value == null || !fieldPath.endsWith(".version")) {
      return value;
    }
    return switch (value.trim().toLowerCase(Locale.ROOT)) {
      case "v1", "legacy" -> "V1_LEGACY";
      case "v2", "continuous" ->
          fieldPath.startsWith("optimizer") ? "V2_LOWER_BOUND" : "V2_CONTINUOUS";
      case "lower_bound", "lower-bound" -> "V2_LOWER_BOUND";
      case "v3", "equivalent_length", "equivalent-length" -> "V3_EQUIVALENT_LENGTH";
      default -> value;
    };
  }

  /**
   * Returns the lower-case path relative to {@code router.} with {@code .} between levels, {@code
   * scoring-version} for that alias, or {@code null} when the name is not a router property.
   */
  private static String relativePath(String propertyName) {
    if (propertyName == null) {
      return null;
    }
    String name = propertyName.trim().toLowerCase(Locale.ROOT);
    if ("scoring-version".equals(name)) {
      return name;
    }
    name = name.replace(':', '.').replace('-', '.');
    if (name.startsWith("router.")) {
      return name.substring("router.".length());
    }
    // optimizer.<path> is short for router.optimizer.<path>.
    return name.startsWith("optimizer.") ? name : null;
  }
}
