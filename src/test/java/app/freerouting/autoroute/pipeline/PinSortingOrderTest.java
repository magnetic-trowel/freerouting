package app.freerouting.autoroute.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.logger.FRLogger;
import app.freerouting.logger.LogEntry;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class PinSortingOrderTest {

  @Test
  void documentedSnakeCaseAndOlderCamelCaseNameTheSameOrder() {
    assertEquals(
        PinSortingOrder.DISTANCE_TO_CLOSEST_ON_NET,
        PinSortingOrder.fromSetting("distance_to_closest_on_net"));
    assertEquals(
        PinSortingOrder.DISTANCE_TO_CLOSEST_ON_NET,
        PinSortingOrder.fromSetting("distanceToClosestOnNet"));
    assertEquals(
        PinSortingOrder.SURROUNDINGS_DENSITY, PinSortingOrder.fromSetting("surroundings_density"));
    assertEquals(
        PinSortingOrder.SURROUNDINGS_DENSITY, PinSortingOrder.fromSetting("surroundingsDensity"));
    assertEquals(PinSortingOrder.INNER_FIRST, PinSortingOrder.fromSetting("inner_first"));
    assertEquals(PinSortingOrder.OUTER_FIRST, PinSortingOrder.fromSetting(" OUTER_FIRST "));
    assertEquals(PinSortingOrder.UNSORTED, PinSortingOrder.fromSetting("unsorted"));
    assertEquals(PinSortingOrder.OUTER_FIRST, PinSortingOrder.fromSetting(null));
  }

  @Test
  void unknownValueWarnsAndKeepsPinNumberOrder() {
    FRLogger.getLogEntries().clear();
    assertEquals(PinSortingOrder.UNSORTED, PinSortingOrder.fromSetting("closest_first"));
    assertTrue(
        Arrays.stream(FRLogger.getLogEntries().getEntries(null, null))
            .map(LogEntry::getMessage)
            .anyMatch(m -> m.contains("Unknown fanout.pin_sorting_order 'closest_first'")));
  }
}
