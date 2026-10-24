package app.freerouting.autoroute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The profiler's reset at the end of a routing session also drops the pass history. */
class PerformanceProfilerResetTest {

  @Test
  void resetClearsPassHistory() {
    PerformanceProfiler.reset();
    PerformanceProfiler.recordPass(1, 10, 100, 50);
    PerformanceProfiler.recordPass(2, 5, 80, 100);
    assertEquals(2, PerformanceProfiler.passHistorySize());

    PerformanceProfiler.reset();

    assertEquals(0, PerformanceProfiler.passHistorySize());
  }
}
