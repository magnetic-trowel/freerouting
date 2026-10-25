package app.freerouting.core.results;

import app.freerouting.constants.Constants;
import app.freerouting.core.RouterJobResourceUsage;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.RoutingJobState;
import app.freerouting.core.scoring.BoardStatistics;
import app.freerouting.core.scoring.BoardStatisticsBounds;
import app.freerouting.settings.RouterSettings;
import app.freerouting.util.gson.GsonProvider;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

/** Machine-readable summary of a headless CLI routing run for benchmark and autopilot harnesses. */
public final class RoutingResultManifest {

  public static final int SCHEMA_VERSION = 1;

  /** Creates an empty manifest for Gson deserialization. */
  public RoutingResultManifest() {}

  @SerializedName("schema_version")
  public int schemaVersion = SCHEMA_VERSION;

  @SerializedName("generated_at")
  public String generatedAt;

  @SerializedName("app_version")
  public String appVersion;

  @SerializedName("git_sha")
  public String gitSha;

  @SerializedName("fixture")
  public FixtureInfo fixture;

  @SerializedName("settings_snapshot")
  public RouterSettings settingsSnapshot;

  @SerializedName("phases")
  public PhaseMetrics phases = new PhaseMetrics();

  @SerializedName("board_statistics")
  public BoardStatistics boardStatistics;

  @SerializedName("bounds")
  public BoardStatisticsBounds bounds;

  @SerializedName("normalized_score")
  public Float normalizedScore;

  @SerializedName("optimizer_score")
  public Float optimizerScore;

  @SerializedName("resource_usage")
  public RouterJobResourceUsage resourceUsage;

  @SerializedName("final_state")
  public String finalState;

  @SerializedName("exit_code")
  public int exitCode;

  @SerializedName("output_written")
  public boolean outputWritten;

  /**
   * True when the written output is the last routing result the job published before it terminated
   * abnormally, rather than the result of a completed or timed-out job. The board statistics then
   * describe the board at termination, not necessarily the written output.
   */
  @SerializedName("output_partial")
  public boolean outputPartial;

  @SerializedName("cpu_score")
  public Integer cpuScore;

  /**
   * True when the routing thread was still running as the manifest was built: the job monitor
   * marked the job as timed out because the thread did not stop within the grace period. The
   * manifest then has no board statistics or scores, which only the ended thread provides.
   */
  @SerializedName("routing_thread_running")
  public boolean routingThreadRunning;

  /** Input fixture identity for the run. */
  public static class FixtureInfo {
    @SerializedName("filename")
    public String filename;

    @SerializedName("sha256")
    public String sha256;
  }

  /** Per-stage duration and pass counts. */
  public static class PhaseMetrics {
    @SerializedName("fanout")
    public PhaseDetail fanout = new PhaseDetail();

    @SerializedName("autorouter")
    public PhaseDetail autorouter = new PhaseDetail();

    @SerializedName("optimizer")
    public PhaseDetail optimizer = new PhaseDetail();

    @SerializedName("fallback")
    public FallbackDetail fallback;
  }

  /** Routing fallback attempts (default first) and final choice. */
  public static class FallbackDetail {
    @SerializedName("chosen")
    public String chosen;

    @SerializedName("attempts")
    public java.util.List<FallbackAttempt> attempts = new java.util.ArrayList<>();
  }

  /** One routing attempt of the fallback. */
  public static class FallbackAttempt {
    @SerializedName("strategy")
    public String strategy;

    @SerializedName("stop_reason")
    public String stopReason;

    @SerializedName("unrouted")
    public int unrouted;

    @SerializedName("new_or_changed_violations")
    public int newOrChangedViolations;

    @SerializedName("clearance_violations")
    public int clearanceViolations;

    @SerializedName("router_score")
    public float routerScore;

    @SerializedName("duration_seconds")
    public float durationSeconds;
  }

  /** Duration and pass count for one routing stage. */
  public static class PhaseDetail {
    /**
     * Why the stage stopped, in the lower-case form of {@link #stopReason(Enum)}: for the
     * autorouter a {@code BatchAutorouter.StopReason}, for the optimizer a {@code
     * BatchOptimizer.StopReason}. Null for a stage that did not run.
     */
    @SerializedName("stop_reason")
    public String stopReason;

    @SerializedName("before")
    public PhaseSnapshot before;

    @SerializedName("after")
    public PhaseSnapshot after;

    @SerializedName("duration_seconds")
    public Float durationSeconds;

    @SerializedName("cpu_seconds")
    public Float cpuSeconds;

    @SerializedName("passes_completed")
    public Integer passesCompleted;

    @SerializedName("total_allocated_gb")
    public Float totalAllocatedGb;

    @SerializedName("peak_heap_mb")
    public Float peakHeapMb;
  }

  /** Board metrics and calculated scores at a phase boundary. */
  public static class PhaseSnapshot {
    @SerializedName("board_statistics")
    public BoardStatistics boardStatistics;

    @SerializedName("score")
    public Float score;

    @SerializedName("router_score")
    public Float routerScore;

    @SerializedName("optimizer_score")
    public Float optimizerScore;

    @SerializedName("score_source")
    public String scoreSource;

    @SerializedName("current_router_score")
    public Float currentRouterScore;

    @SerializedName("current_optimizer_score")
    public Float currentOptimizerScore;

    @SerializedName("current_score_source")
    public String currentScoreSource;

    public static PhaseSnapshot fromBoardStatistics(
        BoardStatistics statistics, RouterSettings settings, String scoreSource) {
      PhaseSnapshot snapshot = new PhaseSnapshot();
      snapshot.boardStatistics = statistics;
      snapshot.scoreSource = scoreSource;
      if (statistics != null && settings != null) {
        snapshot.routerScore = statistics.getRouterScore(settings);
        snapshot.optimizerScore = statistics.getOptimizerScore(settings);
      }
      return snapshot;
    }
  }

  /** Builds a manifest from a completed routing job without a CPU score. */
  public static RoutingResultManifest fromJob(
      RoutingJob job, String inputFilePath, boolean outputWritten, int exitCode) {
    return fromJob(job, inputFilePath, outputWritten, exitCode, null);
  }

  /**
   * Builds a manifest from a completed routing job.
   *
   * @param cpuScore single-thread {@code RuntimeEnvironment.cpuScore} for this process, or {@code
   *     null} if not measured
   */
  public static RoutingResultManifest fromJob(
      RoutingJob job, String inputFilePath, boolean outputWritten, int exitCode, Integer cpuScore) {
    RoutingResultManifest manifest = new RoutingResultManifest();
    manifest.generatedAt = Instant.now().toString();
    manifest.appVersion = Constants.FREEROUTING_VERSION;
    manifest.gitSha = resolveGitSha();
    manifest.fixture = new FixtureInfo();
    if (inputFilePath != null) {
      Path inputPath = Path.of(inputFilePath);
      manifest.fixture.filename = inputPath.getFileName().toString();
      manifest.fixture.sha256 = sha256Hex(inputPath);
    }
    manifest.settingsSnapshot = job.routerSettings;
    if (job.resultPhaseMetrics != null) {
      manifest.phases = job.resultPhaseMetrics;
    }
    manifest.finalState = job.state != null ? job.state.name() : RoutingJobState.INVALID.name();
    manifest.exitCode = exitCode;
    manifest.outputWritten = outputWritten;
    manifest.outputPartial =
        outputWritten
            && job.state != RoutingJobState.COMPLETED
            && job.state != RoutingJobState.TIMED_OUT;
    manifest.resourceUsage = job.resourceUsage;
    manifest.cpuScore = cpuScore;

    manifest.routingThreadRunning = !job.isRoutingThreadEnded();
    // The routing thread took them of the final board; reading the board again here would repeat
    // that work and, while the thread still runs, race with it.
    if (job.finalStatistics != null) {
      manifest.boardStatistics = job.finalStatistics;
      manifest.bounds = manifest.boardStatistics.bounds;
      if (job.routerSettings != null && job.routerSettings.scoring != null) {
        manifest.normalizedScore = manifest.boardStatistics.getRouterScore(job.routerSettings);
        if (manifest.phases.optimizer.before != null || manifest.phases.optimizer.after != null) {
          manifest.optimizerScore = manifest.boardStatistics.getOptimizerScore(job.routerSettings);
        }
      }
    }

    if (job.getCurrentPass() > 0 && manifest.phases.autorouter.passesCompleted == null) {
      manifest.phases.autorouter.passesCompleted = job.getCurrentPass();
    }

    if (manifest.phases.autorouter.durationSeconds == null
        && job.startedAt != null
        && job.finishedAt != null) {
      float totalSeconds =
          (float) (java.time.Duration.between(job.startedAt, job.finishedAt).toMillis() / 1000.0);
      manifest.phases.autorouter.durationSeconds = totalSeconds;
    }

    return manifest;
  }

  /** The manifest form of a stage's stop reason: its lower-case name, or null. */
  public static String stopReason(Enum<?> reason) {
    return reason != null ? reason.name().toLowerCase(Locale.ROOT) : null;
  }

  /** Writes the manifest as UTF-8 JSON to {@code targetPath}. */
  public static void write(Path targetPath, RoutingResultManifest manifest) throws IOException {
    if (targetPath.getParent() != null) {
      Files.createDirectories(targetPath.getParent());
    }
    String json = GsonProvider.GSON.toJson(manifest);
    Files.writeString(targetPath, json, StandardCharsets.UTF_8);
  }

  /** Resolves git SHA from harness environment or returns {@code unknown}. */
  public static String resolveGitSha() {
    String env = System.getenv("FREEROUTING_GIT_SHA");
    if (env != null && !env.isBlank()) {
      return env.trim();
    }
    String prop = System.getProperty("freerouting.git.sha");
    if (prop != null && !prop.isBlank()) {
      return prop.trim();
    }
    String legacyProp = System.getProperty("FREEROUTING_GIT_SHA");
    if (legacyProp != null && !legacyProp.isBlank()) {
      return legacyProp.trim();
    }
    return "unknown";
  }

  private static String sha256Hex(Path path) {
    try {
      byte[] bytes = Files.readAllBytes(path);
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(bytes));
    } catch (IOException | NoSuchAlgorithmException e) {
      return null;
    }
  }
}
