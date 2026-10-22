package app.freerouting.datastructures;

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
}
