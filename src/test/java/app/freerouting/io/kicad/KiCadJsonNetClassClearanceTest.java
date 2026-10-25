package app.freerouting.io.kicad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.model.structure.Unit;
import app.freerouting.io.BoardReadResult;
import app.freerouting.rules.ClearanceMatrix;
import app.freerouting.settings.GlobalSettings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * KiCad keeps the larger of two net-class clearances between their items. The JSON reader left
 * pairs of non-default classes at the 0.2 mm fallback and used a class's own clearance against the
 * default class even when it was the smaller one.
 */
class KiCadJsonNetClassClearanceTest {

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  @Test
  void netClassPairsUseTheLargerClearance() throws Exception {
    JsonObject design =
        JsonParser.parseString(
                Files.readString(
                    Path.of("fixtures/Issue733-kicad_complex_hierarchy_input_design.json")))
            .getAsJsonObject();
    JsonArray netClasses = design.getAsJsonArray("netClasses");
    // The fixture defines "power" (0.3 mm); add the default class and two more classes.
    netClasses.add(netClass("Default", 0.25));
    netClasses.add(netClass("hv", 0.5));
    netClasses.add(netClass("fine", 0.1));

    BoardReadResult result =
        KiCadJsonReader.readBoard(new StringReader(design.toString()), null, null);
    BasicBoard board =
        switch (result) {
          case BoardReadResult.Success success -> success.board();
          case BoardReadResult.OutlineMissing missing -> missing.board();
          default -> null;
        };
    assertNotNull(board);
    ClearanceMatrix matrix = board.rules.clearanceMatrix;
    int defaultClass = matrix.getNo("default");
    int power = matrix.getNo("power");
    int hv = matrix.getNo("hv");
    int fine = matrix.getNo("fine");
    double unitsPerMm = board.communication.physicalToBoard(1.0, Unit.MM);
    for (int layer = 0; layer < board.getLayerCount(); layer++) {
      assertEquals(mm(0.5, unitsPerMm), matrix.getValue(power, hv, layer, false), "power/hv");
      assertEquals(mm(0.5, unitsPerMm), matrix.getValue(hv, power, layer, false), "hv/power");
      assertEquals(mm(0.3, unitsPerMm), matrix.getValue(fine, power, layer, false), "fine/power");
      assertEquals(
          mm(0.25, unitsPerMm), matrix.getValue(defaultClass, fine, layer, false), "default/fine");
      assertEquals(mm(0.1, unitsPerMm), matrix.getValue(fine, fine, layer, false), "fine/fine");
      assertEquals(mm(0.5, unitsPerMm), matrix.getValue(defaultClass, hv, layer, false), "def/hv");
    }
  }

  private static int mm(double value, double unitsPerMm) {
    int units = (int) Math.round(value * unitsPerMm);
    // The matrix stores even values.
    return units % 2 == 0 ? units : units + 1;
  }

  private static JsonObject netClass(String name, double clearance) {
    JsonObject result = new JsonObject();
    result.addProperty("name", name);
    result.addProperty("clearance", clearance);
    result.addProperty("traceWidth", 0.25);
    result.addProperty("viaDiameter", 0.8);
    result.addProperty("viaDrill", 0.4);
    result.addProperty("uviaDiameter", 0.3);
    result.addProperty("uviaDrill", 0.1);
    return result;
  }
}
