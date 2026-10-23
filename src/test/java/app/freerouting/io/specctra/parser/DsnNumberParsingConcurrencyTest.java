package app.freerouting.io.specctra.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * The DSN reader parsed every width and clearance with one static NumberFormat. Jobs, API requests
 * and post-load checks parse concurrently, and DecimalFormat is not thread-safe, so concurrent
 * loads could read 0.1524 as 2.5401524.
 */
class DsnNumberParsingConcurrencyTest {

  private static final String[] VALUES = {"0.1524", "25.4", "12.7", "1000", "0.0005", "3.175"};

  @Test
  void concurrentReadersParseTheirOwnNumbers() throws Exception {
    StringBuilder text = new StringBuilder();
    for (int i = 0; i < 2000; i++) {
      text.append(VALUES[i % VALUES.length]).append(' ');
    }
    ExecutorService executor = Executors.newFixedThreadPool(8);
    try {
      List<Future<Integer>> results = new ArrayList<>();
      for (int t = 0; t < 8; t++) {
        results.add(
            executor.submit(
                () -> {
                  int wrong = 0;
                  for (int round = 0; round < 20; round++) {
                    SpecctraDsnStreamReader reader =
                        new SpecctraDsnStreamReader(new StringReader(text.toString()));
                    for (int i = 0; i < 2000; i++) {
                      Double value = reader.nextDouble();
                      if (value == null || value != Double.parseDouble(VALUES[i % VALUES.length])) {
                        wrong++;
                      }
                    }
                  }
                  return wrong;
                }));
      }
      for (Future<Integer> result : results) {
        assertEquals(0, result.get());
      }
    } finally {
      executor.shutdownNow();
    }
  }
}
