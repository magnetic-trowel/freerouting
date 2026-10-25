package app.freerouting.gui.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.board.actions.ItemIdGenerator;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.state.BoardObserverAdaptor;
import app.freerouting.core.RoutingJob;
import app.freerouting.gui.workspace.WorkspaceSettings;
import app.freerouting.management.HeadlessBoardManager;
import app.freerouting.settings.SettingsMerger;
import app.freerouting.settings.sources.DefaultSettings;
import app.freerouting.settings.sources.DsnFileSettings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Replays the settings steps of opening a second design in the same GUI frame (File / Open or
 * drag-and-drop): {@code GuiBoardManager.setupGuiAfterBoardLoad} resets the {@link
 * WorkspaceSettings} singleton with the job's settings and registers it, then {@code
 * BoardFrame.attachParsedBoard} registers the new DSN file settings and merges.
 */
class BoardFrameSettingsMergeTest {

  private static final Path FIXTURE = Path.of("fixtures/Issue103-Board-Routed.dsn");

  @AfterEach
  void tearDown() {
    WorkspaceSettings.setInstance(null);
  }

  @Test
  void secondBoardUsesItsOwnDsnSettingsAndGuiEditsSurviveTheRoutingMerge() throws Exception {
    String boardA = Files.readString(FIXTURE, StandardCharsets.UTF_8);
    assertTrue(boardA.contains("(via_costs 50)") && boardA.contains("(postroute off)"));
    String boardB =
        boardA
            .replace("(via_costs 50)", "(via_costs 77)")
            .replace("(postroute off)", "(postroute on)");

    SettingsMerger merger = new SettingsMerger(new DefaultSettings());
    RoutingJob job = new RoutingJob();

    // First board.
    WorkspaceSettings workspaceA = attach(merger, job, boardA, "a.dsn");
    assertEquals(50, job.routerSettings.getViaCosts());
    assertFalse(job.routerSettings.getRunOptimizer());

    // A parameter-window edit writes to the job's settings; the merge done by the Autoroute
    // button must keep it instead of reverting it to the value captured at load time.
    job.routerSettings.setViaCosts(80);
    assertEquals(80, merger.merge().getViaCosts(), "GUI edit lost by the routing-start merge");
    assertNotNull(workspaceA);

    // Second board: its own DSN settings must win over the previous board's merged settings.
    attach(merger, job, boardB, "b.dsn");
    assertEquals(77, job.routerSettings.getViaCosts(), "previous board's via costs leaked");
    assertTrue(job.routerSettings.getRunOptimizer(), "previous board's postroute flag leaked");
  }

  private static WorkspaceSettings attach(
      SettingsMerger merger, RoutingJob job, String dsn, String filename) throws Exception {
    byte[] content = dsn.getBytes(StandardCharsets.UTF_8);
    HeadlessBoardManager manager = new HeadlessBoardManager(new RoutingJob());
    manager.loadFromSpecctraDsn(
        new ByteArrayInputStream(content), new BoardObserverAdaptor(), new ItemIdGenerator());
    RoutingBoard board = manager.getRoutingBoard();
    assertNotNull(board);

    // GuiBoardManager.setupGuiAfterBoardLoad
    WorkspaceSettings workspaceSettings = WorkspaceSettings.reset(board, job.routerSettings);
    merger.addOrReplaceSources(workspaceSettings);
    // BoardFrame.attachParsedBoard
    merger.addOrReplaceSources(new DsnFileSettings(new ByteArrayInputStream(content), filename));
    BoardFrame.mergeSettingsForLoadedBoard(merger, job, workspaceSettings, board);
    return workspaceSettings;
  }
}
