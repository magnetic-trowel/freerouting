package app.freerouting.autoroute.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.core.StoppableThread;
import app.freerouting.core.scoring.BoardStatistics;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Point;
import app.freerouting.geometry.planar.Polyline;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.settings.RouterSettings;
import app.freerouting.settings.SettingsMerger;
import app.freerouting.settings.sources.DefaultSettings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** The pass loop reuses full board statistics only while the board is unchanged. */
class BoardStatisticsReuseTest {

  private static final String DSN =
      """
      (pcb reuse (resolution um 1)
        (structure (layer top (type signal))
          (boundary (rect pcb 0 0 20000 10000))
          (via V) (rule (width 400) (clearance 200)))
        (placement (component C (place A 3000 3000 front 0) (place B 17000 3000 front 0)))
        (library (image C (pin S 1 0 0)) (padstack S (shape (circle top 800)))
          (padstack V (shape (circle top 600)) (attach off)))
        (network (net X (pins A-1 B-1))))
      """;

  @Test
  void statisticsAreReusedUntilTheBoardChanges() {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(DSN.getBytes(StandardCharsets.UTF_8)), null, null, "reuse");
    assertInstanceOf(BoardReadResult.Success.class, result);
    RoutingBoard board = (RoutingBoard) ((BoardReadResult.Success) result).board();
    RouterSettings settings = new SettingsMerger(new DefaultSettings()).merge();
    settings.setLayerCount(board.getLayerCount());
    BatchAutorouter router =
        new BatchAutorouter(
            new StoppableThread() {
              @Override
              protected void threadAction() {}
            },
            board,
            settings,
            true,
            true,
            settings.getStartRipupCosts(),
            500);

    BoardStatistics before = router.boardStatistics();
    assertSame(before, router.boardStatistics());
    assertEquals(1, before.connections.incompleteCount);

    Point[] pins = board.getPins().stream().map(Pin::getCenter).toArray(Point[]::new);
    int net = board.rules.nets.get("X").iterator().next().netNumber;
    assertNotNull(
        board.insertTraceWithoutCleaning(
            new Polyline(new Point[] {pins[0], new IntPoint(10000, 3000), pins[1]}),
            0,
            200,
            new int[] {net},
            1,
            FixedState.UNFIXED));

    BoardStatistics after = router.boardStatistics();
    assertNotSame(before, after);
    assertEquals(0, after.connections.incompleteCount);
    assertSame(after, router.boardStatistics());
  }
}
