package app.freerouting.io.kicad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.io.specctra.DsnTestFixtures;
import app.freerouting.settings.GlobalSettings;
import app.freerouting.util.gson.GsonProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * KiCad JSON coordinates are in the declared unit, and the resolution is the number of board units
 * per unit. A fixed factor per unit only fits one input resolution.
 */
class KiCadJsonWriterUnitsTest {

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  @Test
  void milBoardCoordinatesAreWrittenInMil() throws Exception {
    // (resolution mil 1000), boundary from (0 0) to (325 -273.5)
    RoutingBoard board = DsnTestFixtures.loadBoard("Issue070-Autorouter_FQ101_PCB_2022-05-13.dsn");
    KiCadBoardJson json =
        GsonProvider.GSON.fromJson(KiCadJsonWriter.write(board, "units"), KiCadBoardJson.class);

    assertEquals(KiCadBoardJson.UnitJson.MIL, json.unit);
    assertEquals(1000.0, json.resolution, 1e-9);
    double maxX = json.outline.corners.stream().mapToDouble(p -> p.x).max().orElseThrow();
    double maxY = json.outline.corners.stream().mapToDouble(p -> p.y).max().orElseThrow();
    assertEquals(325.0, maxX, 1e-6);
    assertEquals(273.5, maxY, 1e-6);
  }

  @Test
  void viaDrillComesFromThePadstackName() throws Exception {
    // the wiring vias use "Via[0-1]_1600:600_um": 1.6 mm pad, 0.6 mm drill
    RoutingBoard board = DsnTestFixtures.loadBoard("Issue163-pic_programmer.dsn");
    KiCadBoardJson json =
        GsonProvider.GSON.fromJson(KiCadJsonWriter.write(board, "drill"), KiCadBoardJson.class);

    assertEquals(KiCadBoardJson.UnitJson.UM, json.unit);
    assertFalse(json.vias.isEmpty());
    for (KiCadBoardJson.ViaJson via : json.vias) {
      assertEquals(1600.0, via.diameter, 1e-6);
      assertEquals(600.0, via.drill, 1e-6);
    }
  }
}
