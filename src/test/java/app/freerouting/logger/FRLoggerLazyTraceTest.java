package app.freerouting.logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.freerouting.geometry.planar.Point;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FRLoggerLazyTraceTest {

  @Test
  void messageIsNotBuiltWhenGranularTraceIsOff() {
    boolean granular = FRLogger.granularTraceEnabled;
    FRLogger.granularTraceEnabled = false;
    try {
      AtomicInteger built = new AtomicInteger();
      FRLogger.trace(
          "FRLoggerLazyTraceTest",
          "lazy_message",
          () -> {
            built.incrementAndGet();
            return "expensive";
          },
          "Clearance Check",
          new Point[0]);
      assertEquals(0, built.get());
    } finally {
      FRLogger.granularTraceEnabled = granular;
    }
  }
}
