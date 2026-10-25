package app.freerouting.autoroute.drill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.geometry.planar.FloatPoint;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.geometry.planar.IntOctagon;
import app.freerouting.geometry.planar.PolylineArea;
import app.freerouting.geometry.planar.TileShape;
import java.util.Random;
import org.junit.jupiter.api.Test;

class DrillPageTest {

  @Test
  void mergedPiecesCoverTheSameRegionWithoutOverlap() {
    Random random = new Random(11);
    int mergedPages = 0;
    for (int page = 0; page < 200; page++) {
      IntBox pageBox = new IntBox(0, 0, 10000, 10000);
      TileShape[] holes = new TileShape[1 + random.nextInt(6)];
      for (int i = 0; i < holes.length; i++) {
        int x = random.nextInt(10000);
        int y = random.nextInt(10000);
        int r = 300 + random.nextInt(1500);
        int c = r * 4 / 10;
        holes[i] =
            new IntOctagon(
                    x - r,
                    y - r,
                    x + r,
                    y + r,
                    x - y - r - c,
                    x - y + r + c,
                    x + y - r - c,
                    x + y + r + c)
                .normalize()
                .intersection(pageBox);
      }
      TileShape[] pieces = new PolylineArea(pageBox, holes).splitToConvex();
      TileShape[] merged = DrillPage.mergeConvexPieces(pieces);

      TileShape[] again = DrillPage.mergeConvexPieces(pieces);
      assertEquals(merged.length, again.length, "deterministic");
      for (int i = 0; i < merged.length; i++) {
        assertEquals(merged[i].toString(), again[i].toString(), "deterministic");
      }
      assertTrue(merged.length <= pieces.length);
      if (merged.length < pieces.length) {
        mergedPages++;
      }
      for (int i = 0; i < merged.length; i++) {
        for (int j = i + 1; j < merged.length; j++) {
          assertFalse(
              merged[i].intersection(merged[j]).dimension() == 2
                  && merged[i].intersection(merged[j]).area() > 1,
              "merged pieces overlap");
        }
      }
      for (int k = 0; k < 400; k++) {
        FloatPoint point = new FloatPoint(random.nextInt(10000) + 0.5, random.nextInt(10000) + 0.5);
        assertEquals(covers(pieces, point), covers(merged, point), "coverage at " + point);
      }
    }
    assertTrue(mergedPages > 0, "some pieces are merged");
  }

  private static boolean covers(TileShape[] shapes, FloatPoint point) {
    for (TileShape shape : shapes) {
      if (shape.contains(point)) {
        return true;
      }
    }
    return false;
  }
}
