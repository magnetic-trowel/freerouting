package app.freerouting.settings;

/** Selects the optimizer-board score formula independently from router scoring. */
public enum OptimizerScoringVersion {
  /** Existing combined score retained for compatibility. */
  V1_LEGACY,

  /** Excess length, vias, and bends relative to stored lower bounds. */
  V2_LOWER_BOUND,

  /**
   * Excess length, vias, and bends relative to stored lower bounds, with vias and bends converted
   * to the trace length that uses the same routing space on the board.
   */
  V3_EQUIVALENT_LENGTH
}
