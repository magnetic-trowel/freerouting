package app.freerouting.core.scoring;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/** Board-only lower bounds used by the V2 optimizer score. */
public class BoardStatisticsBounds implements Serializable {

  @SerializedName("min_trace_length_mm")
  public Float minTraceLengthMm;

  @SerializedName("min_via_count")
  public Integer minViaCount;

  @SerializedName("min_bend_count")
  public Integer minBendCount;

  /** Trace length in mm that uses as much routing space as one via (V3 score). */
  @SerializedName("via_equivalent_length_mm")
  public Float viaEquivalentLengthMm;

  /** Trace length in mm charged for one bend: one trace width plus clearance (V3 score). */
  @SerializedName("bend_equivalent_length_mm")
  public Float bendEquivalentLengthMm;
}
