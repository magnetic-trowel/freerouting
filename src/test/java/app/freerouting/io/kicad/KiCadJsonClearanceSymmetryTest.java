package app.freerouting.io.kicad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.BasicBoard;
import app.freerouting.io.BoardReadResult;
import app.freerouting.rules.ClearanceMatrix;
import app.freerouting.settings.GlobalSettings;
import java.io.FileReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The KiCad JSON reader used to set net-class and custom-rule clearances in one direction only, so
 * a query from a power-class item near default copper used a different clearance than the DRC check
 * from the other item's side.
 */
class KiCadJsonClearanceSymmetryTest {

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "fixtures/Issue733-kicad_complex_hierarchy_input_design.json",
        "fixtures/Issue733-kicad_interf_u_input_design.json"
      })
  void clearanceMatrixIsSymmetric(String path) throws Exception {
    BasicBoard board;
    try (FileReader reader = new FileReader(path)) {
      BoardReadResult result = KiCadJsonReader.readBoard(reader, null, null);
      board =
          switch (result) {
            case BoardReadResult.Success success -> success.board();
            case BoardReadResult.OutlineMissing missing -> missing.board();
            default -> null;
          };
    }
    assertNotNull(board, "board of " + path);
    ClearanceMatrix matrix = board.rules.clearanceMatrix;
    int classCount = matrix.getClassCount();
    assertTrue(classCount > 2, "fixture should define net classes beyond default");
    for (int layer = 0; layer < board.getLayerCount(); layer++) {
      for (int i = 0; i < classCount; i++) {
        for (int j = i + 1; j < classCount; j++) {
          assertEquals(
              matrix.getValue(i, j, layer, false),
              matrix.getValue(j, i, layer, false),
              matrix.getName(i) + "/" + matrix.getName(j) + " on layer " + layer);
        }
      }
    }
  }
}
