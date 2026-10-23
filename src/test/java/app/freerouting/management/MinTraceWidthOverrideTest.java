package app.freerouting.management;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.core.RoutingJob;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.settings.sources.DefaultSettings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** HeadlessBoardManager makes router.min_trace_width_um the board's minimum trace half width. */
class MinTraceWidthOverrideTest {

  /** A synthetic board at 10 board units per micrometre. */
  private static final String DSN =
      """
      (pcb minwidth (resolution um 10) (unit um)
        (structure (layer top (type signal))
          (boundary (rect pcb 0 0 10000 10000))
          (via V) (rule (width 250) (clearance 200)))
        (placement (component P (place A 2000 5000 front 0) (place B 8000 5000 front 0)))
        (library (image P (pin S 1 0 0))
          (padstack S (shape (circle top 800)))
          (padstack V (shape (circle top 600)) (attach off)))
        (network (net SIG (pins A-1 B-1))))
      """;

  private RoutingBoard board;
  private RoutingJob job;
  private HeadlessBoardManager manager;

  @BeforeEach
  void setUp() {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(DSN.getBytes(StandardCharsets.UTF_8)), null, null, "test");
    assertInstanceOf(BoardReadResult.Success.class, result);
    board = (RoutingBoard) ((BoardReadResult.Success) result).board();
    job = new RoutingJob();
    job.routerSettings = new DefaultSettings().getSettings();
    manager = new HeadlessBoardManager(job);
    manager.board = board;
  }

  @Test
  void defaultLeavesNoMinimum() {
    manager.applyRouterSettingsForLoadedBoard();
    assertEquals(0, board.rules.getMinAllowedTraceHalfWidth());
  }

  @Test
  void settingBecomesTheBoardMinimumRoundedUp() {
    job.routerSettings.minTraceWidthUm = 200.0;
    manager.applyRouterSettingsForLoadedBoard();
    assertEquals(1000, board.rules.getMinAllowedTraceHalfWidth());

    // 1501 board units: a half width of 750 would make the trace 1 unit too narrow.
    job.routerSettings.minTraceWidthUm = 150.1;
    manager.applyRouterSettingsForLoadedBoard();
    assertEquals(751, board.rules.getMinAllowedTraceHalfWidth());
  }

  @Test
  void negativeValueIsIgnored() {
    job.routerSettings.minTraceWidthUm = 200.0;
    manager.applyRouterSettingsForLoadedBoard();
    job.routerSettings.minTraceWidthUm = -5.0;
    manager.applyRouterSettingsForLoadedBoard();
    assertEquals(1000, board.rules.getMinAllowedTraceHalfWidth());
  }
}
