package app.freerouting.datastructures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TimeLimitTest {

  @Test
  void limitCountsStepsInsteadOfClockTime() throws InterruptedException {
    TimeLimit limit = new TimeLimit(1);
    Thread.sleep(20);
    for (int i = 0; i < 250_000; ++i) {
      assertFalse(limit.limitExceeded());
    }
    assertTrue(limit.limitExceeded());
  }

  @Test
  void meterSumsTheStepsOfEveryLimitThatReportsToIt() {
    TimeLimit.Meter meter = new TimeLimit.Meter();
    TimeLimit first = new TimeLimit(1, meter);
    TimeLimit second = new TimeLimit(1, meter);
    for (int i = 0; i < 3; ++i) {
      first.limitExceeded();
    }
    second.limitExceeded();
    new TimeLimit(1).limitExceeded();
    assertEquals(4, meter.steps());
  }
}
