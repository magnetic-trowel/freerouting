package app.freerouting.settings.sources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.io.specctra.DsnTestFixtures;
import app.freerouting.settings.GlobalSettings;
import app.freerouting.settings.RouterSettings;
import app.freerouting.settings.SettingsMerger;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DsnFileSettingsTest {

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  /**
   * Verifies that {@code DsnFileSettings} reads the layer count directly from the DSN file's {@code
   * (structure (layer ...))} entries, even when the file has no {@code (autorouteSettings ...)}
   * scope.
   *
   * <p>{@code Issue413-test.dsn} is a 2-layer board (F.Cu + B.Cu) <em>without</em> an {@code
   * (autorouteSettings)} block. The old {@code DefaultSettings} hard-coded {@code defaultLayerCount
   * = 2} for every board regardless of its actual layer stack. The fix made {@code DsnFileSettings}
   * extract the layer count from the DSN metadata and call {@code RouterSettings.setLayerCount(n)},
   * so the merged settings always reflect what the board file actually declares.
   *
   * <p>This test asserts {@code == 2} (not {@code != 2}) because the DSN file genuinely has 2
   * signal layers. Asserting the exact match is the strongest proof that the layer count comes from
   * the file, not from some coincidental hard-coded constant.
   */
  @Test
  void dsnFileSettingsReads2LayerCountFrom2LayerBoardWithoutAutorouteBlock() {
    // Issue413-test.dsn: 2 signal layers (F.Cu, B.Cu), no (autorouteSettings ...) block.
    InputStream in = DsnTestFixtures.openResource("Issue413-test.dsn");
    DsnFileSettings settings = new DsnFileSettings(in, "Issue413-test.dsn");

    assertEquals(
        2,
        settings.getSettings().getLayerCount(),
        """
        DsnFileSettings should read the layer count from the DSN structure section (2 layers:
        F.Cu and B.Cu) even when the file has no (autorouteSettings ...) block.
        """);
  }

  /**
   * Verifies that {@code DsnFileSettings} reads the correct layer count for a 4-layer board without
   * an {@code (autorouteSettings ...)} scope.
   *
   * <p>{@code Issue066-Project_GP8B.dsn} has 4 signal layers (F.Cu / In1.Cu / In2.Cu / B.Cu) and no
   * {@code (autorouteSettings)} block. This is the canonical multi-layer regression case: the old
   * hard-coded size-2 layer arrays would have been wrong here, while the correct implementation
   * must return 4.
   */
  @Test
  void dsnFileSettingsReads4LayerCountFrom4LayerBoardWithoutAutorouteBlock() {
    // Issue066-Project_GP8B.dsn: 4 signal layers (F.Cu, In1.Cu, In2.Cu, B.Cu),
    // no (autorouteSettings ...) block.
    InputStream in = DsnTestFixtures.openResource("Issue066-Project_GP8B.dsn");
    DsnFileSettings settings = new DsnFileSettings(in, "Issue066-Project_GP8B.dsn");

    assertEquals(
        4,
        settings.getSettings().getLayerCount(),
        """
        DsnFileSettings should read the layer count from the DSN structure section (4 layers:
        F.Cu, In1.Cu, In2.Cu, B.Cu) even when the file has no (autorouteSettings ...) block.
        """);
  }

  @Test
  void dsnFileSettingsReturnsNonNullSettings() {
    InputStream in = DsnTestFixtures.openResource("Issue143-rpi_splitter.dsn");
    DsnFileSettings settings = new DsnFileSettings(in, "Issue143-rpi_splitter.dsn");

    assertNotNull(settings.getSettings(), "getSettings() must never return null");
  }

  @Test
  void dsnFileSettingsPriorityIs20() {
    InputStream in = DsnTestFixtures.openResource("Issue143-rpi_splitter.dsn");
    DsnFileSettings settings = new DsnFileSettings(in, "Issue143-rpi_splitter.dsn");

    assertEquals(20, settings.getPriority());
  }

  @Test
  void dsnFileSettingsSourceNameContainsFilename() {
    InputStream in = DsnTestFixtures.openResource("Issue143-rpi_splitter.dsn");
    DsnFileSettings settings = new DsnFileSettings(in, "Issue143-rpi_splitter.dsn");

    assertTrue(settings.getSourceName().contains("Issue143-rpi_splitter.dsn"));
  }

  /**
   * An {@code (autoroute_settings ...)} scope sets only what it states. It used to force the
   * optimizer (and the autorouter) on when {@code (postroute ...)} (or {@code (autoroute ...)}) was
   * absent, which overrode {@code freerouting.json} at DSN priority.
   */
  @Test
  void autorouteSettingsScopeSetsOnlyTheKeywordsItContains(@TempDir Path tempDir)
      throws IOException {
    String dsn = withAutorouteSettings("(autoroute_settings (via_costs 60))");

    RouterSettings fromDsn = new DsnFileSettings(stream(dsn), "board.dsn").getSettings();
    assertEquals(60, fromDsn.getViaCosts());
    assertNull(fromDsn.optimizer.enabled, "no (postroute ...): the DSN has no optimizer opinion");
    assertNull(fromDsn.autorouter.enabled, "no (autoroute ...): the DSN has no router opinion");

    Path json = tempDir.resolve("freerouting.json");
    Files.writeString(json, "{\"router\": {\"optimizer\": {\"enabled\": false}}}");
    RouterSettings merged =
        new SettingsMerger(
                new DefaultSettings(),
                new JsonFileSettings(json),
                new DsnFileSettings(stream(dsn), "board.dsn"))
            .merge();
    assertFalse(merged.getRunOptimizer(), "freerouting.json disabled the optimizer");
    assertEquals(60, merged.getViaCosts());

    RouterSettings stated =
        new DsnFileSettings(
                stream(
                    withAutorouteSettings("(autoroute_settings (autoroute off) (postroute on))")),
                "board.dsn")
            .getSettings();
    assertFalse(stated.getRunRouter());
    assertTrue(stated.getRunOptimizer());
  }

  private static String withAutorouteSettings(String scope) throws IOException {
    try (InputStream in = DsnTestFixtures.openResource("Issue413-test.dsn")) {
      String dsn = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      return dsn.replaceFirst("\\(structure", "(structure\n    " + scope);
    }
  }

  private static InputStream stream(String text) {
    return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
  }
}
