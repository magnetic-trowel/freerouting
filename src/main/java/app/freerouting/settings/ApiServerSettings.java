package app.freerouting.settings;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/** Network and lifecycle configuration for the REST API server. */
public class ApiServerSettings implements Serializable {

  /** Whether the API server should be started. */
  @SerializedName("enabled")
  public Boolean isEnabled = false;

  /** Whether the API server is currently running. */
  @SerializedName("running")
  public transient Boolean isRunning = false;

  /** Whether plain HTTP endpoints are permitted. */
  @SerializedName("http_allowed")
  public Boolean isHttpAllowed = true;

  /** Endpoint URLs on which the API server listens. */
  @SerializedName("endpoints")
  public String[] endpoints = {"http://127.0.0.1:37864"};

  /** Authentication configuration for API requests. */
  @SerializedName("authentication")
  public ApiAuthenticationSettings authentication = new ApiAuthenticationSettings();

  /** Comma-separated origins allowed for cross-origin requests. */
  @SerializedName(
      value = "cors_origins",
      alternate = {"corsOrigins"})
  public String corsOrigins = "";

  /** Fixed-window request-rate limiting configuration. */
  @SerializedName("rate_limit")
  public RateLimitSettings rateLimit = new RateLimitSettings();

  /**
   * Directory whose files clients may name by path (local-file inputs and outputs). Unset: local
   * paths are allowed only while every endpoint is a loopback address. See {@code
   * app.freerouting.api.LocalFileAccess}.
   */
  @SerializedName("local_file_root")
  public String localFileRoot;

  /**
   * Maximum number of routing jobs the scheduler runs concurrently. Defaults to null (resolved at
   * runtime dynamically as {@code max(1, CPU cores - 1)}).
   */
  @SerializedName("max_parallel_jobs")
  public Integer maxParallelJobs;

  /**
   * Maximum number of finished routing jobs (completed, cancelled, timed out, terminated or
   * invalid) the scheduler keeps; the oldest are removed first. Queued and running jobs never
   * count. Defaults to null (resolved at runtime to 200); values below 1 also use the default.
   */
  @SerializedName("max_finished_jobs")
  public Integer maxFinishedJobs;

  /**
   * Number of the most recently finished routing jobs that keep their routed board in memory, for
   * DRC reports and KiCad JSON output generated from the board. Older finished jobs keep their
   * output file and statistics only. Defaults to null (resolved at runtime to 4); {@code 0} keeps
   * no finished board, negative values use the default.
   */
  @SerializedName("max_finished_job_boards")
  public Integer maxFinishedJobBoards;
}
