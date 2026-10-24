package app.freerouting.gui.board;

import app.freerouting.analytics.FRAnalytics;
import app.freerouting.gui.windows.board.WindowMessage;
import app.freerouting.io.FileFormat;
import app.freerouting.logger.FRLogger;
import app.freerouting.management.jobs.RoutingJobScheduler;
import app.freerouting.management.sessions.SessionManager;
import app.freerouting.settings.GlobalSettings;
import java.io.ByteArrayInputStream;
import java.io.File;
import javax.swing.JOptionPane;

/** Installs file-menu actions while keeping {@link BoardFrame} as the public GUI façade. */
public class BoardFrameFileActions {

  private final BoardFrame boardFrame;
  private final GlobalSettings globalSettings;

  public BoardFrameFileActions(BoardFrame boardFrame, GlobalSettings globalSettings) {
    this.boardFrame = boardFrame;
    this.globalSettings = globalSettings;
  }

  public void install() {
    boardFrame.menubar.fileMenu.addOpenEventListener(this::open);
    boardFrame.menubar.fileMenu.addSaveAsEventListener(this::saveAs);
  }

  private void open(File selectedFile) {
    if (selectedFile == null) {
      return;
    }
    // Opening a file replaces the current board, so offer to save unsaved changes first (the
    // drag-and-drop path already asks; the menu path silently discarded them).
    if (!boardFrame.confirmSaveBeforeLoad(boardFrame)) {
      return;
    }

    try {
      boardFrame.routingJob.setInput(selectedFile);
      if (boardFrame.routingJob.input.format == FileFormat.UNKNOWN) {
        FRLogger.warn("The input file format was not recognised.");
        return;
      }
    } catch (Exception e) {
      FRLogger.error("There was an error while reading the input file.", e);
      return;
    }

    if (boardFrame.routingJob.input.getFile() == null) {
      return;
    }

    try {
      byte[] fileContent = boardFrame.routingJob.input.getData().readAllBytes();
      FileFormat inputFormat = boardFrame.routingJob.input.format;
      javax.swing.SwingUtilities.invokeLater(
          () -> {
            String sessionId = SessionManager.getInstance().getPrimarySession().id.toString();
            RoutingJobScheduler.getInstance().clearJobs(sessionId);
            RoutingJobScheduler.getInstance().enqueueJob(boardFrame.routingJob);

            globalSettings.setDefaultValue(
                "gui.input_directory", boardFrame.routingJob.input.getDirectoryPath());
          });

      if (boardFrame.boardPanel != null && boardFrame.boardPanel.boardHandling != null) {
        switch (inputFormat) {
          case DSN ->
              boardFrame.loadFromBytesAsync(fileContent, FileFormat.DSN, boardFrame.routingJob);
          case KICAD_DESIGN_JSON ->
              boardFrame.loadFromBytesAsync(
                  fileContent, FileFormat.KICAD_DESIGN_JSON, boardFrame.routingJob);
          case FRB -> {
            if (!boardFrame.load(
                new ByteArrayInputStream(fileContent),
                FileFormat.FRB,
                null,
                boardFrame.routingJob)) {
              boardFrame.restoreTutorialBoardAfterFailedLoad(null);
            }
          }
          default ->
              FRLogger.warn(
                  "Loading the board failed, because the selected file format is not supported.");
        }
        if (inputFormat == FileFormat.DSN) {
          FRAnalytics.buttonClicked("fileio_loaddsn", boardFrame.routingJob.getInputFileDetails());
        } else if (inputFormat == FileFormat.KICAD_DESIGN_JSON) {
          FRAnalytics.buttonClicked("fileio_loadjson", boardFrame.routingJob.getInputFileDetails());
        } else if (inputFormat == FileFormat.FRB) {
          FRAnalytics.buttonClicked("fileio_loadfrb", boardFrame.routingJob.getInputFileDetails());
        }
      }
    } catch (Exception e) {
      FRLogger.error("There was an error while reading the input file.", e);
    }
  }

  @SuppressWarnings("deprecation")
  private void saveAs(File selectedFile) {
    if (selectedFile == null) {
      return;
    }
    if (!boardFrame.routingJob.tryToSetOutputFile(selectedFile)) {
      return;
    }

    switch (boardFrame.routingJob.output.format) {
      case SES -> {
        boolean saved =
            boardFrame.saveAsSpecctraSessionSes(
                boardFrame.routingJob.output.getFile(), boardFrame.routingJob.input.getFilename());
        if (saved
            && WindowMessage.confirm(
                boardFrame.tm.getText("confirm_rules_save"), JOptionPane.NO_OPTION)) {
          boardFrame.saveRulesAs(
              boardFrame.routingJob.getRulesFile(),
              boardFrame.routingJob.input.getFilename(),
              boardFrame.boardPanel.boardHandling);
        }
        FRAnalytics.fileSaved("SES", boardFrame.routingJob.getOutputFileDetails());
        FRAnalytics.buttonClicked("fileio_saveses", boardFrame.routingJob.getOutputFileDetails());
      }
      case KICAD_SESSION_JSON -> {
        boardFrame.saveAsKiCadJson(
            boardFrame.routingJob.output.getFile(), boardFrame.routingJob.input.getFilename());
        FRAnalytics.fileSaved("KICAD_SESSION_JSON", boardFrame.routingJob.getOutputFileDetails());
        FRAnalytics.buttonClicked(
            "fileio_savekicadjson", boardFrame.routingJob.getOutputFileDetails());
      }
      case DSN -> {
        boardFrame.saveAsSpecctraDesignDsn(
            boardFrame.routingJob.output.getFile(),
            boardFrame.routingJob.input.getFilename(),
            false);
        FRAnalytics.fileSaved("DSN", boardFrame.routingJob.getOutputFileDetails());
        FRAnalytics.buttonClicked("fileio_savedsn", boardFrame.routingJob.getOutputFileDetails());
      }
      case FRB -> {
        boardFrame.saveAsBinary(boardFrame.routingJob.output.getFile());
        FRAnalytics.fileSaved("FRB", boardFrame.routingJob.getOutputFileDetails());
        FRAnalytics.buttonClicked("fileio_savefrb", boardFrame.routingJob.getOutputFileDetails());
      }
      case SCR -> {
        boardFrame.saveAsFusionScriptScr(
            boardFrame.routingJob.getFusionScriptFile(), boardFrame.routingJob.input.getFilename());
        FRAnalytics.fileSaved("SCR", boardFrame.routingJob.input.getFilename());
        FRAnalytics.buttonClicked("fileio_savescr", "");
      }
      default ->
          FRLogger.warn(
              "Saving the board failed, because the selected file format is not supported.");
    }
  }
}
