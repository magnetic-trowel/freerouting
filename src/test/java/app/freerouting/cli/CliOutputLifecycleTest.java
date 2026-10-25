package app.freerouting.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.core.BoardFileDetails;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.RoutingJobState;
import app.freerouting.core.StoppableThread;
import app.freerouting.core.results.RoutingResultManifest;
import app.freerouting.core.scoring.BoardStatistics;
import app.freerouting.settings.GlobalSettings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CliOutputLifecycleTest {
  @ParameterizedTest
  @EnumSource(RoutingJobState.class)
  void waitingEndsForEveryTerminalState(RoutingJobState state) {
    boolean terminal =
        switch (state) {
          case INVALID, COMPLETED, TIMED_OUT, CANCELLED, TERMINATED -> true;
          default -> false;
        };
    assertEquals(terminal, CliRunner.isCliTerminalState(state));
  }

  @Test
  void emptyOutputDoesNotOverwriteExistingFile(@TempDir Path directory) throws Exception {
    Path destination = directory.resolve("result.ses");
    Files.writeString(destination, "previous-user-file");
    GlobalSettings settings = new GlobalSettings();
    settings.initialOutputFile = destination.toString();
    RoutingJob job = new RoutingJob();
    job.state = RoutingJobState.TIMED_OUT;
    job.output = new BoardFileDetails();
    assertFalse(CliRunner.writeCliOutputIfAvailable(settings, job));
    assertEquals("previous-user-file", Files.readString(destination));
  }

  @Test
  void terminatedJobWritesItsLastPublishedOutputAsPartialFailure(@TempDir Path directory)
      throws Exception {
    Path destination = directory.resolve("result.ses");
    GlobalSettings settings = new GlobalSettings();
    settings.initialOutputFile = destination.toString();
    RoutingJob job = new RoutingJob();
    job.state = RoutingJobState.TERMINATED;
    job.output = new BoardFileDetails();
    job.output.setData("(session published)".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    boolean written = CliRunner.writeCliOutputIfAvailable(settings, job);

    assertTrue(written);
    assertEquals("(session published)", Files.readString(destination));
    assertEquals(1, CliRunner.computeCliExitCode(job, written));
    RoutingResultManifest manifest = RoutingResultManifest.fromJob(job, null, written, 1);
    assertTrue(manifest.outputWritten);
    assertTrue(manifest.outputPartial);
  }

  @Test
  void drcJsonAdditionalOutputIsDrcReport(@TempDir Path directory) throws Exception {
    app.freerouting.Freerouting.globalSettings = new GlobalSettings();
    Path report = directory.resolve("report.drc.json");
    GlobalSettings settings = new GlobalSettings();
    settings.initialOutputFile = directory.resolve("result.ses").toString();
    settings.additionalOutputFiles.add(report.toString());
    RoutingJob job = new RoutingJob();
    job.state = RoutingJobState.COMPLETED;
    job.board =
        app.freerouting.io.specctra.DsnTestFixtures.loadBoard("scoring-perfect-two-pin.dsn");
    job.output = new BoardFileDetails();
    job.output.setData("(session published)".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    assertTrue(CliRunner.writeCliOutputIfAvailable(settings, job));

    String written = Files.readString(report);
    assertTrue(written.contains("\"unconnectedItems\""), written);
    assertTrue(written.contains("schemas.kicad.org/drc"), written);
  }

  @Test
  void completedOutputIsNotPartial() {
    RoutingJob job = new RoutingJob();
    job.state = RoutingJobState.COMPLETED;
    assertFalse(RoutingResultManifest.fromJob(job, null, true, 0).outputPartial);
  }

  @Test
  void cancelledJobDoesNotWriteOutput(@TempDir Path directory) throws Exception {
    GlobalSettings settings = new GlobalSettings();
    settings.initialOutputFile = directory.resolve("result.ses").toString();
    RoutingJob job = new RoutingJob();
    job.state = RoutingJobState.CANCELLED;
    job.output = new BoardFileDetails();
    job.output.setData("(session published)".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    assertFalse(CliRunner.writeCliOutputIfAvailable(settings, job));
    assertFalse(Files.exists(directory.resolve("result.ses")));
  }

  @Test
  void routingThreadThatKeepsRunningIsNotWaitedFor() throws Exception {
    CountDownLatch release = new CountDownLatch(1);
    RoutingJob job = new RoutingJob();
    job.state = RoutingJobState.TIMED_OUT;
    job.thread = new BlockedThread(release);
    job.thread.start();
    try {
      long start = System.nanoTime();
      assertFalse(CliRunner.awaitRoutingThreadEnd(job, 200));
      assertTrue((System.nanoTime() - start) / 1_000_000L < 10_000L);
    } finally {
      release.countDown();
      job.thread.join();
    }
    assertTrue(CliRunner.awaitRoutingThreadEnd(job, 200));
  }

  @Test
  void routingThreadThatEndsWithinTheWaitIsJoined() throws Exception {
    RoutingJob job = new RoutingJob();
    job.state = RoutingJobState.TIMED_OUT;
    assertTrue(CliRunner.awaitRoutingThreadEnd(job, 0), "a job without a thread");
    CountDownLatch release = new CountDownLatch(1);
    job.thread = new BlockedThread(release);
    job.thread.start();
    release.countDown();
    assertTrue(CliRunner.awaitRoutingThreadEnd(job, 10_000));
  }

  @Test
  void manifestReportsTheStatisticsOfTheEndedRoutingThreadOnly() throws Exception {
    RoutingJob job = new RoutingJob();
    job.board =
        app.freerouting.io.specctra.DsnTestFixtures.loadBoard("scoring-perfect-two-pin.dsn");
    job.routerSettings = new app.freerouting.settings.sources.DefaultSettings().getSettings();
    job.state = RoutingJobState.TIMED_OUT;
    CountDownLatch release = new CountDownLatch(1);
    job.thread = new BlockedThread(release);
    job.thread.start();
    try {
      RoutingResultManifest running = RoutingResultManifest.fromJob(job, null, true, 0);
      assertTrue(running.routingThreadRunning);
      assertNull(running.boardStatistics, "the board of a running thread is not read");
      assertNull(running.normalizedScore);
    } finally {
      release.countDown();
      job.thread.join();
    }
    BoardStatistics finalStatistics = new BoardStatistics(job.board);
    job.finalStatistics = finalStatistics;
    RoutingResultManifest ended = RoutingResultManifest.fromJob(job, null, true, 0);
    assertFalse(ended.routingThreadRunning);
    assertSame(finalStatistics, ended.boardStatistics, "the thread's statistics, not new ones");
    assertTrue(ended.normalizedScore != null);
  }

  @Test
  void additionalOutputsAreNotWrittenFromABoardTheRoutingThreadStillChanges(@TempDir Path directory)
      throws Exception {
    app.freerouting.Freerouting.globalSettings = new GlobalSettings();
    Path report = directory.resolve("report.drc.json");
    GlobalSettings settings = new GlobalSettings();
    settings.initialOutputFile = directory.resolve("result.ses").toString();
    settings.additionalOutputFiles.add(report.toString());
    RoutingJob job = new RoutingJob();
    job.state = RoutingJobState.TIMED_OUT;
    job.board =
        app.freerouting.io.specctra.DsnTestFixtures.loadBoard("scoring-perfect-two-pin.dsn");
    job.output = new BoardFileDetails();
    job.output.setData("(session published)".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    CountDownLatch release = new CountDownLatch(1);
    job.thread = new BlockedThread(release);
    job.thread.start();
    try {
      assertTrue(CliRunner.writeCliOutputIfAvailable(settings, job), "the published output");
      assertFalse(Files.exists(report));
    } finally {
      release.countDown();
      job.thread.join();
    }
  }

  /** A routing thread that ignores the stop request until the test releases it. */
  private static final class BlockedThread extends StoppableThread {
    private final CountDownLatch release;

    BlockedThread(CountDownLatch release) {
      this.release = release;
      setDaemon(true);
    }

    @Override
    protected void threadAction() {
      try {
        release.await();
      } catch (InterruptedException _) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
