package app.freerouting.settings.sources;

import app.freerouting.logger.FRLogger;
import app.freerouting.settings.RouterPropertyNames;
import app.freerouting.settings.RouterSettings;
import app.freerouting.settings.SettingsSource;
import app.freerouting.util.ReflectionUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provides router settings from command-line arguments. Only the settings specified via CLI will be
 * non-null.
 */
public class CliSettings implements SettingsSource {

  private static final int PRIORITY = 60;
  private final RouterSettings settings;
  private final Map<String, String> parsedArguments;

  /**
   * Creates a CliSettings source by parsing command-line arguments.
   *
   * @param args Command-line arguments array
   */
  public CliSettings(String[] args) {
    this.parsedArguments = new HashMap<>();
    this.settings = parseArguments(args);
  }

  private RouterSettings parseArguments(String[] args) {
    RouterSettings settings = new RouterSettings();
    boolean hasDesignInputArgument = false;
    boolean hasDesignOutputArgument = false;
    boolean hasExplicitRouterEnabledArgument = false;

    // Populate only the specified settings. This is the only reader of router options on the
    // command line; GlobalSettings reads the other sections.

    for (int i = 0; i < args.length; i++) {
      String arg = args[i];

      if (arg.startsWith("--")) {
        // Handle --property=value format
        if (arg.contains("=")) {
          String[] parts = arg.substring(2).split("=", 2);
          List<String> fieldPaths = RouterPropertyNames.fieldPaths(parts[0]);
          if (fieldPaths.contains("autorouter.enabled")) {
            hasExplicitRouterEnabledArgument = true;
          }
          applyRouterSetting(settings, parts[0], fieldPaths, parts[1]);
        }
      } else if (arg.startsWith("-")) {
        // Handle -flag value format
        String flag = arg.substring(1);
        String value = i + 1 < args.length && !args[i + 1].startsWith("-") ? args[++i] : "";

        if ("de".equals(flag)) {
          hasDesignInputArgument = true;
        } else if ("do".equals(flag)) {
          hasDesignOutputArgument = true;
        }

        if ("oit".equals(flag)) {
          FRLogger.warn(
              "The '-oit' command-line flag is deprecated; use"
                  + " '--router.optimizer.improvement_threshold' instead.");
        } else if ("inc".equals(flag)) {
          FRLogger.warn(
              "The '-inc' command-line flag is deprecated; use"
                  + " '--router.autorouter.ignore_net_classes' instead.");
        } else if ("is".equals(flag)) {
          applyItemSelectionStrategy(settings, value);
          continue;
        } else if ("us".equals(flag) || "hr".equals(flag)) {
          FRLogger.warn(
              "The '-"
                  + flag
                  + "' command-line flag no longer has any effect: the optimizer only has the"
                  + " global optimal board update strategy.");
          continue;
        } else if ("im".equals(flag)) {
          FRLogger.warn(
              "The '-im' command-line flag no longer has any effect: intermediate stages are not"
                  + " saved and routing cannot be resumed from them.");
          continue;
        }

        // "-mt 0" disables the optimizer, as it did in v1.9 and as the CLI docs say. Other values
        // set the thread count through router.max_threads.
        if ("mt".equals(flag) && isZeroOrNegativeInteger(value)) {
          applyRouterSetting(settings, "router.optimizer.enabled", "false");
          continue;
        }

        // Map short flags to router settings
        String propertyName = mapFlagToProperty(flag);
        if (propertyName != null) {
          applyRouterSetting(settings, propertyName, value);
        }
      }
    }

    // Legacy batch invocation (`-de ... -do ...`) is expected to route immediately.
    // Force router enabled unless the caller explicitly set --router.enabled=... .
    if (hasDesignInputArgument && hasDesignOutputArgument && !hasExplicitRouterEnabledArgument) {
      settings.autorouter.enabled = true;
      FRLogger.debug(
          "Applied CLI router setting: router.autorouter.enabled = true"
              + " (implicit from -de/-do batch mode)");
    }

    return settings;
  }

  private void applyRouterSetting(RouterSettings settings, String propertyName, String value) {
    applyRouterSetting(settings, propertyName, RouterPropertyNames.fieldPaths(propertyName), value);
  }

  private void applyRouterSetting(
      RouterSettings settings, String propertyName, List<String> fieldPaths, String value) {
    for (String fieldPath : fieldPaths) {
      String normalizedValue = RouterPropertyNames.normalizeValue(fieldPath, value);
      try {
        ReflectionUtil.setFieldValue(settings, fieldPath, normalizedValue);
        parsedArguments.put(propertyName, normalizedValue);
        FRLogger.debug("Applied CLI router setting: " + propertyName + " = " + normalizedValue);
      } catch (NoSuchFieldException e) {
        FRLogger.warn("Unknown settings property: " + propertyName);
      } catch (Exception e) {
        FRLogger.warn(
            "Failed to apply CLI router setting: " + propertyName + ": " + e.getMessage());
      }
    }
  }

  private static boolean isZeroOrNegativeInteger(String value) {
    try {
      return Integer.decode(value.trim()) <= 0;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /** Applies "-is sequential|prioritized". Other values (e.g. the removed "random") warn. */
  private void applyItemSelectionStrategy(RouterSettings settings, String value) {
    String v = value.trim().toLowerCase(Locale.ROOT);
    if (v.startsWith("prio")) {
      applyRouterSetting(settings, "router.optimizer.item_selection_strategy", "PRIORITIZED");
    } else if (v.startsWith("seq")) {
      applyRouterSetting(settings, "router.optimizer.item_selection_strategy", "SEQUENTIAL");
    } else {
      FRLogger.warn(
          "The '-is' value '"
              + value
              + "' is not supported; use 'sequential' or 'prioritized'. Keeping the default.");
    }
  }

  private String mapFlagToProperty(String flag) {
    // Map short flags to full property names
    return switch (flag) {
      case "mp" -> "router.max_passes";
      case "mt" -> "router.max_threads";
      case "inc" -> "router.autorouter.ignore_net_classes";
      case "oit" -> "router.optimizer.improvement_threshold";
      case "router-scoring-version", "optimizer-scoring-version", "scoring-version" -> flag;
      default -> null;
    };
  }

  @Override
  public RouterSettings getSettings() {
    return settings;
  }

  @Override
  public String getSourceName() {
    return "CLI Arguments";
  }

  @Override
  public int getPriority() {
    return PRIORITY;
  }

  /**
   * Gets the parsed arguments for debugging/logging.
   *
   * @return Map of property names to values
   */
  public Map<String, String> getParsedArguments() {
    return new HashMap<>(parsedArguments);
  }
}
