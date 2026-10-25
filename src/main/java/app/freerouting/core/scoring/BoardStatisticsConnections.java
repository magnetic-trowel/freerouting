package app.freerouting.core.scoring;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/** Statistics describing board connections. */
public class BoardStatisticsConnections implements Serializable {

  @SerializedName("maximum_count")
  public Integer maximumCount;

  /** Open connections; in result statistics, the pour cut-offs ({@link #pourCutOffCount}) too. */
  @SerializedName("incomplete_count")
  public Integer incompleteCount;

  /**
   * Groups of copper that a pour polygon connects to their net although foreign copper cuts them
   * off from the pour's fill ({@code drc.PlaneFillConnectivity}); each is one open connection. Set
   * only in result statistics ({@link BoardStatistics#ofResult}), null otherwise.
   */
  @SerializedName("pour_cut_off_count")
  public Integer pourCutOffCount;

  /** Open connections with an end blocked by the design (e.g. pins on foreign fixed copper). */
  @SerializedName("design_blocked_count")
  public Integer designBlockedCount;
}
