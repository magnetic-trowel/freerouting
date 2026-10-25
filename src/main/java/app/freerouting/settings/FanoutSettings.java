package app.freerouting.settings;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Configuration for the SMD-pin fanout pre-pass that runs before the main batch autorouter. During
 * fanout each single-layer (SMD) pin is given a short escape trace and a via so that the main
 * router can work pin-to-via rather than pin-to-pin, which dramatically improves routing quality
 * for dense SMD boards.
 *
 * <p>All fields follow the {@link RouterSettings} nullable-field contract: a {@code null} value
 * means "this source has no opinion" and is skipped by {@code SettingsMerger}. Hardcoded defaults
 * live exclusively in {@link app.freerouting.settings.sources.DefaultSettings}.
 */
public class FanoutSettings implements Serializable, Cloneable {

  /**
   * Whether to run the fanout pre-pass at all. When {@code false} the router skips fanout and
   * begins the standard auto-routing stage immediately.
   */
  @SerializedName("enabled")
  public Boolean enabled;

  /**
   * Maximum number of fanout passes. Each pass iterates over all SMD pins once; passes continue
   * until every pin is escaped <em>or</em> this limit is reached. Typical boards converge in 1–3
   * passes; the limit exists only as a safety cap.
   */
  @SerializedName("max_passes")
  public Integer maxPasses;

  /**
   * Maximum number of escape/fanout routing attempts allowed during the fanout stage. If this limit
   * is reached, the fanout stage will stop early.
   */
  @SerializedName("max_items")
  public Integer maxItems;

  /**
   * Whether the fanout router is allowed to rip up and re-route existing traces in order to make
   * room for a new escape via. When {@code false} fanout runs without any ripup, which is faster
   * but may leave more pins un-escaped on congested boards.
   */
  @SerializedName(
      value = "ripup_allowed",
      alternate = {"ripupAllowed"})
  public Boolean ripupAllowed;

  /**
   * The minimum physical escape wire length (in millimeters). Vias will not be placed closer than
   * this distance from the starting pin center during fanout.
   */
  @SerializedName("min_escape_length_mm")
  public Double minEscapeLengthMm;

  /**
   * The maximum physical escape wire length (in millimeters). The search tree expansion and via
   * placement during fanout will be restricted to this distance.
   */
  @SerializedName("max_escape_length_mm")
  public Double maxEscapeLengthMm;

  /**
   * The diameter of starting/escape vias used inside the pins during the fanout/escape stage (in
   * millimeters). Default is 0.250 mm.
   */
  @SerializedName("start_via_diameter_mm")
  public Double startViaDiameterMm;

  /**
   * The diameter of landing/end vias used at the end of escaping wires during the fanout/escape
   * stage (in millimeters). Default is 0.250 mm.
   */
  @SerializedName("end_via_diameter_mm")
  public Double endViaDiameterMm;

  /**
   * The sorting order for SMD pins within a component: "outer_first" (default), "inner_first" (v1.9
   * default), "distance_to_closest_on_net", "surroundings_density" or "unsorted" (pin number only).
   * Case, "_" and "-" are ignored, so the older camelCase spellings still work; see
   * docs/settings.md.
   */
  @SerializedName("pin_sorting_order")
  public String pinSortingOrder;

  /**
   * Whether to fallback to board-wide via rules if a net has no via rules defined or an empty via
   * list during fanout.
   */
  @SerializedName("fallback_to_board_vias")
  public Boolean fallbackToBoardVias;

  /**
   * Erik Gavriluk (freerouting.net): a pin whose escape fails inside the escape length window is
   * retried once without the window.
   */
  @SerializedName("retry_without_escape_window")
  public Boolean retryWithoutEscapeWindow;

  /**
   * Erik Gavriluk (freerouting.net): fanout strategy, "eager" or "direct_first". direct_first
   * routes same-layer pin connections before any escape via.
   */
  @SerializedName("strategy")
  public String strategy;

  /** Timeout for the fanout stage (e.g., "5m", "300s"). Default is null (no timeout). */
  @SerializedName("timeout")
  public String timeoutString;

  /** No-arg constructor required for deserialisation and {@link #clone()}. */
  public FanoutSettings() {}

  /**
   * Creates a deep copy of this {@code FanoutSettings} object. All fields are immutable wrappers or
   * primitives, so a shallow clone is sufficient.
   */
  @Override
  public FanoutSettings clone() {
    try {
      return (FanoutSettings) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError("Clone not supported", e);
    }
  }
}
