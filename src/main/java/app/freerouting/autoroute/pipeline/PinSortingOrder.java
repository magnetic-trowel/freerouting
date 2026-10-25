package app.freerouting.autoroute.pipeline;

import app.freerouting.logger.FRLogger;
import java.util.Locale;

/**
 * The order in which fanout escapes the SMD pins of a component ({@code fanout.pin_sorting_order}).
 */
enum PinSortingOrder {
  /** Pins further from the component center first. */
  OUTER_FIRST,
  /** Pins closer to the component center first. */
  INNER_FIRST,
  /** Pins with the closest pin on the same net first. */
  DISTANCE_TO_CLOSEST_ON_NET,
  /** Pins with the most SMD pins within 20 mm first. */
  SURROUNDINGS_DENSITY,
  /** Pin-number order. */
  UNSORTED;

  /** The order used when the setting is not given. */
  static final PinSortingOrder DEFAULT = OUTER_FIRST;

  /**
   * Parses a setting value, ignoring case, {@code _} and {@code -}, so that the documented {@code
   * distance_to_closest_on_net} and the older {@code distanceToClosestOnNet} name the same order.
   * An unknown value warns and falls back to pin-number order, which it always produced.
   *
   * @param value the setting value, or {@code null} for the default
   */
  static PinSortingOrder fromSetting(String value) {
    if (value == null) {
      return DEFAULT;
    }
    String key = normalize(value);
    for (PinSortingOrder order : values()) {
      if (normalize(order.name()).equals(key)) {
        return order;
      }
    }
    FRLogger.warn(
        "Unknown fanout.pin_sorting_order '"
            + value
            + "'; expected outer_first, inner_first, distance_to_closest_on_net,"
            + " surroundings_density or unsorted. Using pin-number order (unsorted).");
    return UNSORTED;
  }

  private static String normalize(String value) {
    return value.trim().replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
  }
}
