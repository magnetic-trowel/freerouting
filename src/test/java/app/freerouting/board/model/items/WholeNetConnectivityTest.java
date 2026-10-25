package app.freerouting.board.model.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.io.specctra.DsnTestFixtures;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Every answer is the one of the item's own connectivity walk on the current board. */
class WholeNetConnectivityTest {

  @ParameterizedTest
  @ValueSource(
      strings = {"Issue103-Board-Routed.dsn", "Issue143-rpi_splitter.dsn", "Issue093-interf_u.dsn"})
  void agreesWithEveryItemsOwnWalk(String fixture) throws Exception {
    RoutingBoard board = DsnTestFixtures.loadBoard(fixture);
    WholeNetConnectivity connectivity = new WholeNetConnectivity();
    assertTrue(assertAgrees(board, connectivity) > 0);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Issue103-Board-Routed.dsn", "Issue093-interf_u.dsn"})
  void answersFollowBoardChanges(String fixture) throws Exception {
    RoutingBoard board = DsnTestFixtures.loadBoard(fixture);
    WholeNetConnectivity connectivity = new WholeNetConnectivity();
    assertAgrees(board, connectivity);
    List<Trace> traces = new ArrayList<>(board.getTraces());
    assertTrue(!traces.isEmpty());
    for (int i = 0; i < traces.size(); i += 3) {
      board.removeItem(traces.get(i));
    }
    assertAgrees(board, connectivity);
  }

  private static int assertAgrees(RoutingBoard board, WholeNetConnectivity connectivity) {
    int checked = 0;
    for (Item item : board.getItems()) {
      if (!(item instanceof Connectable) || item.netCount() == 0) {
        continue;
      }
      int net = item.getNetNumber(0);
      assertEquals(
          item.getConnectedSet(net), connectivity.connectedSet(item, net), item.toString());
      assertEquals(
          item.getUnconnectedSet(net), connectivity.unconnectedSet(item, net), item.toString());
      assertEquals(
          item.getUnconnectedSet(net).isEmpty(),
          connectivity.isConnectedToWholeNet(item, net),
          item.toString());
      checked++;
    }
    return checked;
  }
}
