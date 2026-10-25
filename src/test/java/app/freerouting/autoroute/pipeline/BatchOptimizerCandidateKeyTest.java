package app.freerouting.autoroute.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.items.Trace;
import app.freerouting.board.model.items.Via;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.io.specctra.DsnTestFixtures;
import app.freerouting.settings.GlobalSettings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The optimizer evaluates one candidate per key and drops the others, so the key must capture
 * exactly what the evaluation depends on.
 */
class BatchOptimizerCandidateKeyTest {

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  @Test
  void viasOfOneConnectionShareTheirKey() throws Exception {
    RoutingBoard board = loadUnfixedBoard();
    Map<String, List<Item>> byKey = new HashMap<>();
    for (Item item : board.getItems()) {
      if (item instanceof Via) {
        String key = BatchOptimizer.candidateKey(item);
        assertNotNull(key);
        byKey.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
      }
    }
    boolean anyShared = false;
    for (List<Item> group : byKey.values()) {
      Set<Item> connection =
          new TreeSet<>(group.getFirst().getConnectionItems(Item.StopConnectionOption.NONE));
      for (Item member : group) {
        assertEquals(
            connection,
            new TreeSet<>(member.getConnectionItems(Item.StopConnectionOption.NONE)),
            "candidates with one key must rip the same connection");
        assertEquals(group.getFirst().netCount(), member.netCount());
      }
      anyShared |= group.size() > 1;
    }
    assertTrue(anyShared, "the routed fixture should have connections with several vias");
  }

  @Test
  void candidatesHaveOneItemPerKey() throws Exception {
    RoutingBoard board = loadUnfixedBoard();
    List<Item> candidates = BatchOptimizer.candidates(board);
    Set<String> keys = new HashSet<>();
    for (Item candidate : candidates) {
      String key = BatchOptimizer.candidateKey(candidate);
      assertNotNull(key);
      assertTrue(keys.add(key), "two candidates share the key " + key);
    }
    List<Item> order = BatchOptimizer.optimizationOrder(board);
    assertTrue(candidates.size() < order.size(), "the fixture should have duplicate candidates");
    for (Item item : order) {
      assertTrue(keys.contains(BatchOptimizer.candidateKey(item)), "a key without a candidate");
    }
  }

  @Test
  void aFullyFixedBoardHasNoCandidates() throws Exception {
    RoutingBoard board = loadUnfixedBoard();
    for (Item item : board.getItems()) {
      if (item instanceof Trace || item instanceof Via) {
        item.setFixedState(FixedState.USER_FIXED);
      }
    }
    assertTrue(BatchOptimizer.candidates(board).isEmpty());
  }

  /** The routed fixture with all its wiring unfixed, since DSN wiring may be imported fixed. */
  private static RoutingBoard loadUnfixedBoard() throws Exception {
    RoutingBoard board = DsnTestFixtures.loadBoard("Issue103-Board-Routed.dsn");
    for (Item item : board.getItems()) {
      if (item instanceof Trace || item instanceof Via) {
        item.setFixedState(FixedState.UNFIXED);
      }
    }
    return board;
  }
}
