package app.freerouting.drc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.freerouting.TestFixtures;
import app.freerouting.board.actions.ItemIdGenerator;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Every load of a design has new identity hash codes; its airlines must not depend on them. */
class NetIncompletesDeterminismTest {

  private static List<String> airlines() throws Exception {
    var result =
        DsnReader.readBoard(
            new FileInputStream(TestFixtures.resolveFile("Issue508-DAC2020_bm06.dsn")),
            null,
            new ItemIdGenerator());
    var board = ((BoardReadResult.Success) result).board();
    return Arrays.stream(new DesignRulesChecker(board, null).getAllAirlines())
        .map(a -> a.fromItem.getId() + "-" + a.toItem.getId() + " " + a.fromCorner + a.toCorner)
        .toList();
  }

  @Test
  void airlinesAreIdenticalForEveryLoadOfTheSameDesign() throws Exception {
    List<String> first = airlines();
    for (int i = 0; i < 4; ++i) {
      assertEquals(first, airlines());
    }
  }
}
