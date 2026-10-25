package app.freerouting.io.specctra.parser;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Pin;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * A pad drawn as a polygon of two points with an aperture width ({@code (polygon 1 0.01 0 -3.5 0
 * 3.5)}, Issue179) was read as a two-corner polygon, whose convex piece was unbounded; its corners
 * fell outside the incomplete-count triangulation, which then threw and ended the routing job.
 */
class DegeneratePolygonPadTest {

  private static final String DSN =
      """
      (pcb degenerate (resolution um 1)
        (structure (layer top (type signal)) (layer bottom (type signal))
          (boundary (rect pcb 0 0 20000 20000)) (via V) (rule (width 200) (clearance 200)))
        (placement (component C (place C1 5000 10000 front 0) (place C2 15000 10000 front 0)))
        (library
          (image C (pin P7 1 0 0))
          (padstack P7 (shape (polygon top 10 0 -3500 0 3500)))
          (padstack V (shape (circle top 600)) (shape (circle bottom 600)) (attach off)))
        (network (net A (pins C1-1 C2-1))))
      """;

  @Test
  void twoPointPolygonPadIsTheLineItDraws() {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(DSN.getBytes(StandardCharsets.UTF_8)), null, null, "x");

    assertInstanceOf(BoardReadResult.Success.class, result);
    RoutingBoard board = (RoutingBoard) ((BoardReadResult.Success) result).board();
    for (Pin pin : board.getPins()) {
      IntBox box = pin.getShape(0).boundingBox();
      assertTrue(board.boundingBox.contains(box), box::toString);
      assertTrue(box.width() * 50 < box.height(), box::toString);
    }
  }
}
