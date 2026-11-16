package app.freerouting.settings;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * Execution knobs for the batch autorouter stage (fanout and optimizer stay in their own objects).
 * All fields are nullable so {@code SettingsMerger} can tell "this source sets the field" from
 * "this source has no opinion".
 */
@Schema(name = "AutorouterSettings", description = "Execution settings for the autorouter stage")
public class AutorouterSettings implements Serializable, Cloneable {

  /** Whether the autorouter stage runs after fanout. */
  @SerializedName("enabled")
  @Schema(description = "Whether the autorouter stage runs after fanout")
  public Boolean enabled;

  /** Algorithm identifier (for example {@link RouterSettings#ALGORITHM_CURRENT}). */
  @SerializedName("algorithm")
  @Schema(description = "Algorithm identifier (e.g. 'freerouting-router')")
  public String algorithm;

  /** Maximum autorouter passes. {@code 0} means no limit. */
  @SerializedName("max_passes")
  @Schema(description = "Maximum autorouter passes (0 means unlimited)")
  public Integer maxPasses;

  /**
   * Work budget of the pass loop: the router stops before a pass once the passes so far have used
   * it ({@code StopReason.WORK_BUDGET}, a clean stop like the pass limit). Work is counted, not
   * timed, so the stop is the same on any machine and under any load: the search steps (polls of the
   * searches' step limits, {@link app.freerouting.datastructures.TimeLimit#threadSteps()}) plus, per
   * pass started, {@link #workPassWeight} x the board's pins and {@link #workUnroutedWeight} x its
   * incomplete connections x its pins (the per-pass work no step limit polls: board statistics,
   * item insertion). Unset or {@code 0}: no budget.
   */
  @SerializedName("max_work")
  @Schema(description = "Work budget of the pass loop in work units (0 or unset: none)")
  public Long maxWork;

  /** Work units per pass started, per board pin (see {@link #maxWork}). */
  @SerializedName("work_pass_weight")
  @Schema(description = "Work units per pass started and board pin")
  public Double workPassWeight;

  /** Work units per pass started, per incomplete connection and board pin (see {@link #maxWork}). */
  @SerializedName("work_unrouted_weight")
  @Schema(description = "Work units per pass started, incomplete connection and board pin")
  public Double workUnroutedWeight;

  /** Maximum items attempted in the autorouter stage. */
  @SerializedName("max_items")
  @Schema(description = "Maximum items attempted in the autorouter stage")
  public Integer maxItems;

  /**
   * Kept for the canonical CLI {@code --router.autorouter.max_threads}. The autorouter pass does
   * not read it. The optimizer pool uses {@code router.optimizer.max_threads}.
   */
  @SerializedName("max_threads")
  @Schema(description = "Maximum worker threads for the autorouter stage")
  public Integer maxThreads;

  /** Net class names the autorouter should skip. */
  @SerializedName("ignore_net_classes")
  @Schema(
      description = "Array of exact net class names to skip/ignore during autorouting",
      example = "[\"kicad_default\"]")
  public String[] ignoreNetClasses;

  /**
   * Erik Gavriluk (freerouting.net): whether nets that required necking on earlier connections
   * route at the neck width directly on later connections (skipping redundant class-width
   * searches).
   */
  @SerializedName("start_necked")
  @Schema(description = "Whether nets requiring necking route at neck width directly")
  public Boolean startNecked;

  /**
   * Erik Gavriluk (freerouting.net): comma-separated routing fallback strategies, tried in order
   * when the pass loop gives up with unrouted connections or new clearance violations.
   */
  @SerializedName("fallback_strategies")
  @Schema(description = "Comma-separated routing fallback strategies")
  public String fallbackStrategies;

  /**
   * Fallback strategies for a job whose fanout does not run, used when none of {@code
   * fallback_strategies} applies to it (the default list holds only fanout strategies). {@code
   * none} disables them.
   */
  @SerializedName("no_fanout_fallback_strategies")
  @Schema(
      description = "Fallback strategies when fanout does not run and no fallback_strategies apply")
  public String noFanoutFallbackStrategies;

  /**
   * Fallback only: the input board is taken as the default attempt's result (no pass loop of its
   * own), and the fallback ladder runs on it at once. For a caller that has already routed and
   * repaired the board and wants the ladder's alternatives last, inside its own time box. The
   * board then counts as stopped by stagnation, so the ladder may start. Unset or {@code false}:
   * the pass loop runs first.
   */
  @SerializedName("fallback_only")
  @Schema(description = "Skip the pass loop and run the fallback ladder on the input board")
  public Boolean fallbackOnly;

  /** Erik Gavriluk (freerouting.net): fallback alternatives an exploration round runs. */
  @SerializedName("explore_fallback_strategies")
  @Schema(description = "Fallback alternatives an exploration round runs")
  public String exploreFallbackStrategies;

  /**
   * Erik Gavriluk (freerouting.net): factor applied to the via cost of nets whose pins are all SMD.
   */
  @SerializedName("smd_net_via_cost_factor")
  @Schema(description = "Via cost factor for pure SMD nets")
  public Double smdNetViaCostFactor;

  /**
   * Connection attempts at most in the repair after routing that joins copper cut off from the fill
   * of its pour (BatchAutorouter.connectPourCutOffs). A count of work, not of time, so the repair
   * stops at the same point on every machine. Negative: automatic, twice the cut-off groups at its
   * start plus 10. {@code 0} disables the repair.
   */
  @SerializedName("pour_repair_attempts")
  @Schema(description = "Connection attempts at most in the pour cut-off repair (negative: automatic)")
  public Integer pourRepairAttempts;

  /**
   * Erik Gavriluk (freerouting.net): a board with at most this many open connections is continued
   * from its best routing before the other fallback attempts. {@code 0} disables that continuation.
   */
  @SerializedName("residual_repair_max_open")
  @Schema(description = "Open connections at which the best board is continued before alternatives")
  public Integer residualRepairMaxOpen;

  /**
   * Erik Gavriluk (freerouting.net): exploration attempts after the opening fallback. {@code 0}
   * disables them. Later tries of a kind raise via costs and widen the ripped neighbourhood.
   * Attempts are not cut short by elapsed time.
   */
  @SerializedName("explore_patience")
  @Schema(description = "Exploration attempts in a row without improvement before stopping")
  public Integer explorePatience;

  /**
   * Erik Gavriluk (freerouting.net): extra seed mixed into the autorouter item order. Null keeps
   * the pass loop's own order.
   */
  @SerializedName("item_order_seed")
  @Schema(description = "Extra seed for the autorouter item order")
  public Integer itemOrderSeed;

  @Override
  public AutorouterSettings clone() {
    try {
      AutorouterSettings copy = (AutorouterSettings) super.clone();
      if (this.ignoreNetClasses != null) {
        copy.ignoreNetClasses = this.ignoreNetClasses.clone();
      }
      return copy;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError("Clone not supported", e);
    }
  }
}
