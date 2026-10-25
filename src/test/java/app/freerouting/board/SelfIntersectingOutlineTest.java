package app.freerouting.board;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.board.actions.ItemIdGenerator;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.structure.BoardOutline;
import app.freerouting.core.RoutingJob;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.io.BoardReadResult;
import app.freerouting.management.HeadlessBoardManager;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * A board whose outline crosses itself (seen on a large benchmark board) made the post-load outline
 * checks throw a NullPointerException in PolygonShape.contains, so the job failed before routing;
 * v2.5.0 loaded it.
 */
class SelfIntersectingOutlineTest {

  private static final String DSN =
      """
      (pcb selfx (resolution um 1)
        (structure (layer top (type signal)) (layer bottom (type signal))
          (boundary (path pcb 0 20000 4000 12000 8000 10000 14000 20000 20000 4000 20000 20000 4000))
          (via V) (rule (width 200) (clearance 200)))
        (placement (component C (place C1 14000 17000 front 0) (place C2 17000 17000 front 0)))
        (library
          (image C (pin P 1 0 0))
          (padstack P (shape (rect top -500 -500 500 500)))
          (padstack V (shape (circle top 600)) (shape (circle bottom 600)) (attach off)))
        (network (net A (pins C1-1 C2-1))))
      """;

  @Test
  void boardWithACrossingOutlineLoads() {
    HeadlessBoardManager manager = new HeadlessBoardManager(new RoutingJob());

    BoardReadResult result =
        manager.loadFromSpecctraDsn(
            new ByteArrayInputStream(DSN.getBytes(StandardCharsets.UTF_8)),
            null,
            new ItemIdGenerator());

    assertInstanceOf(BoardReadResult.Success.class, result);
    RoutingBoard board = manager.getRoutingBoard();
    assertNotNull(board);
    BoardOutline outline = board.getOutline();
    assertTrue(outline.contains(new IntPoint(14000, 17000)));
  }
}
