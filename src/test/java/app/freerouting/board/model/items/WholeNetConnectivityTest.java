package app.freerouting.board.model.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.io.specctra.DsnTestFixtures;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The once-per-net answer is the answer of each pin's own connectivity walk. */
class WholeNetConnectivityTest {

  @ParameterizedTest
  @ValueSource(
      strings = {"Issue103-Board-Routed.dsn", "Issue143-rpi_splitter.dsn", "Issue093-interf_u.dsn"})
  void agreesWithEveryPinsUnconnectedSet(String fixture) throws Exception {
    RoutingBoard board = DsnTestFixtures.loadBoard(fixture);
    WholeNetConnectivity connectivity = new WholeNetConnectivity();
    int pins = 0;
    for (Pin pin : board.getPins()) {
      if (pin.netCount() == 0) {
        continue;
      }
      int net = pin.getNetNumber(0);
      assertEquals(
          pin.getUnconnectedSet(net).isEmpty(),
          connectivity.isConnectedToWholeNet(pin, net),
          pin.toString());
      pins++;
    }
    assertTrue(pins > 0);
  }
}
