package app.freerouting.io.kicad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.io.BoardReadResult;
import com.google.gson.Gson;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The routed board goes back to KiCad as JSON in millimetres. Its coordinates and widths must keep
 * the board's precision: two decimals put every trace corner and via on a 10 um grid and turned a
 * 0.127 mm trace into 0.13 mm.
 */
class KiCadJsonWriterPrecisionTest {

  private static final String BOARD =
      """
      {
        "designName": "Precision",
        "unit": "MM",
        "resolution": 10000.0,
        "layers": [
          {"index": 0, "name": "F.Cu", "type": "signal"},
          {"index": 1, "name": "B.Cu", "type": "signal"}
        ],
        "nets": [{"id": 1, "name": "N1", "className": "default"}],
        "outline": {
          "corners": [
            {"x": 0.0, "y": 0.0}, {"x": 20.0, "y": 0.0}, {"x": 20.0, "y": 20.0}, {"x": 0.0, "y": 20.0}
          ]
        },
        "traces": [
          {"id": 1, "netName": "N1", "layerIndex": 0, "width": 0.127,
           "points": [{"x": 1.2345, "y": 2.3456}, {"x": 8.7654, "y": 2.3456}]}
        ],
        "vias": [
          {"id": 1, "netName": "N1", "position": {"x": 5.0005, "y": 7.1235},
           "diameter": 0.45, "drill": 0.2, "startLayerIndex": 0, "endLayerIndex": 1}
        ]
      }
      """;

  @Test
  void tracesAndViasKeepTheBoardPrecision() {
    BoardReadResult result = KiCadJsonReader.readBoard(new StringReader(BOARD), null, null);
    RoutingBoard board =
        (RoutingBoard) assertInstanceOf(BoardReadResult.Success.class, result).board();

    // a plain Gson reads the numbers exactly as written
    KiCadBoardJson written =
        new Gson().fromJson(KiCadJsonWriter.write(board, "Precision"), KiCadBoardJson.class);

    KiCadBoardJson.TraceJson trace = written.traces.getFirst();
    assertEquals(0.127, trace.width, 1e-12);
    assertEquals(
        List.of(1.2345, 2.3456, 8.7654, 2.3456),
        trace.points.stream().flatMap(p -> java.util.stream.Stream.of(p.x, p.y)).toList());
    KiCadBoardJson.ViaJson via = written.vias.getFirst();
    assertEquals(5.0005, via.position.x, 1e-12);
    assertEquals(7.1235, via.position.y, 1e-12);
    assertEquals(0.45, via.diameter, 1e-12);
  }
}
