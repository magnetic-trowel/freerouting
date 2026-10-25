package app.freerouting.management;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.library.Padstack;
import app.freerouting.geometry.planar.Circle;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.logger.FRLogger;
import app.freerouting.logger.LogEntry;
import app.freerouting.logger.LogEntryType;
import app.freerouting.rules.DefaultItemClearanceClasses.ItemClass;
import app.freerouting.rules.NetClass;
import app.freerouting.rules.ViaRule;
import app.freerouting.settings.GlobalSettings;
import app.freerouting.settings.sources.DefaultSettings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * KiCad exports the via of a net class whose via size is unset as "(circle F.Cu -0.001)". The DSN
 * reader does not build that padstack; router.fallback_via_diameter_um gives such classes a via.
 */
class FallbackViaTest {

  private static final String UNSET_VIA = "Via[0-1]_-0.001:-0.001_um";

  /** 10 board units per micrometre; the only via of the design has an unset size. */
  private static final String DSN =
      """
      (pcb fallback
        (parser (string_quote ") (host_cad "KiCad's Pcbnew") (host_version "9.0"))
        (resolution um 10) (unit um)
        (structure
          (layer F.Cu (type signal)) (layer B.Cu (type signal))
          (boundary (path pcb 0  0 0  20000 0  20000 10000  0 10000  0 0))
          (via "VIA")
          (rule (width 250) (clearance 200)))
        (placement
          (component P
            (place A 2000 3000 front 0) (place B 18000 3000 front 0)
            (place C 2000 7000 front 0) (place D 18000 7000 front 0)))
        (library (image P (pin S 1 0 0))
          (padstack S (shape (circle F.Cu 800)))
          (padstack "VIA" (shape (circle F.Cu DIA)) (shape (circle B.Cu DIA)) (attach off)))
        (network
          (net N1 (pins A-1 B-1))
          (net N2 (pins C-1 D-1))
          (class kicad_default N1 (circuit (use_via "VIA")) (rule (width 250) (clearance 200)))
          (class Wide N2 (circuit (use_via "VIA")) (rule (width 400) (clearance 300)))))
      """;

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  private static String design(String viaName, String diameter) {
    return DSN.replace("\"VIA\"", "\"" + viaName + "\"").replace("DIA", diameter);
  }

  private static RoutingBoard load(String dsn, Double diameterUm, Double drillUm) {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(dsn.getBytes(StandardCharsets.UTF_8)), null, null, "test");
    assertInstanceOf(BoardReadResult.Success.class, result);
    RoutingBoard board = (RoutingBoard) ((BoardReadResult.Success) result).board();
    RoutingJob job = new RoutingJob();
    job.routerSettings = new DefaultSettings().getSettings();
    job.routerSettings.fallbackViaDiameterUm = diameterUm;
    job.routerSettings.fallbackViaDrillUm = drillUm;
    HeadlessBoardManager manager = new HeadlessBoardManager(job);
    manager.board = board;
    manager.applyRouterSettingsForLoadedBoard();
    return board;
  }

  private static long fallbackWarnings(Instant mark) {
    return Arrays.stream(FRLogger.getLogEntries().getEntries(mark, null))
        .filter(e -> e.getType() == LogEntryType.Warning)
        .map(LogEntry::getMessage)
        .filter(m -> m.contains("--router.fallback_via_diameter_um"))
        .count();
  }

  private static String vias(RoutingBoard board) {
    StringBuilder result = new StringBuilder();
    for (Padstack padstack : board.library.getViaPadstacks()) {
      result.append(padstack.name).append(' ').append(padstack.getShape(0)).append('\n');
    }
    for (int i = 0; i < board.rules.netClasses.count(); i++) {
      NetClass netClass = board.rules.netClasses.get(i);
      ViaRule rule = netClass.getViaRule();
      result.append(netClass.getName()).append(':');
      for (int j = 0; rule != null && j < rule.viaCount(); j++) {
        result.append(' ').append(rule.getVia(j).getName());
      }
      result.append('\n');
    }
    return result.toString();
  }

  @Test
  void withoutTheSettingTheClassesHaveNoViaAndOneWarningNamesThem() {
    Instant mark = Instant.now().minusNanos(1);
    RoutingBoard board = load(design(UNSET_VIA, "-0.001"), null, null);
    assertEquals(0, board.library.viaPadstackCount());
    for (int i = 0; i < board.rules.netClasses.count(); i++) {
      ViaRule rule = board.rules.netClasses.get(i).getViaRule();
      assertTrue(rule == null || rule.viaCount() == 0);
    }
    assertEquals(1, fallbackWarnings(mark));
    String warning =
        Arrays.stream(FRLogger.getLogEntries().getEntries(mark, null))
            .map(LogEntry::getMessage)
            .filter(m -> m.contains("--router.fallback_via_diameter_um"))
            .findFirst()
            .orElseThrow();
    assertTrue(warning.contains(UNSET_VIA), warning);
    assertTrue(warning.contains("Wide"), warning);
  }

  @Test
  void theFallbackViaIsCreatedAndUsedByEveryClassWithoutVia() {
    RoutingBoard board = load(design(UNSET_VIA, "-0.001"), 500.0, 300.0);
    Padstack via = board.library.getViaPadstack("Via[0-1]_500:300_um");
    assertNotNull(via);
    assertEquals(0, via.fromLayer());
    assertEquals(1, via.toLayer());
    // 500 um at 10 board units per micrometre: radius 2500.
    assertEquals(2500, ((Circle) via.getShape(0)).radius);
    for (int i = 0; i < board.rules.netClasses.count(); i++) {
      NetClass netClass = board.rules.netClasses.get(i);
      ViaRule rule = netClass.getViaRule();
      assertEquals(1, rule.viaCount(), netClass.getName());
      assertEquals(via, rule.getVia(0).getPadstack());
      assertEquals(
          netClass.defaultItemClearanceClasses.get(ItemClass.VIA),
          rule.getVia(0).getClearanceClassIndex());
    }
  }

  @Test
  void theSettingChangesNothingWhenTheDesignHasUsableVias() {
    String dsn = design("Via[0-1]_800:400_um", "800");
    String without = vias(load(dsn, null, null));
    Instant mark = Instant.now().minusNanos(1);
    String with = vias(load(dsn, 500.0, 300.0));
    assertEquals(without, with);
    assertEquals(0, fallbackWarnings(mark));
  }
}
