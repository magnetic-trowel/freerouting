package app.freerouting.api.dto;

import app.freerouting.core.RoutingJob;
import app.freerouting.core.RoutingJobPriority;
import app.freerouting.settings.DesignRulesCheckerSettings;
import app.freerouting.settings.RouterSettings;
import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Request body of {@code POST /v1/jobs/enqueue}.
 *
 * <p>Only the fields a client may choose are accepted. The server owns the job's identifier, owner,
 * state, timestamps and its input, output, rules and session file details; reading the body as a
 * whole {@link RoutingJob} let a client pick another job's identifier or an output file name that
 * the job persistence then wrote to or deleted.
 */
@Schema(description = "Routing job to enqueue")
public class EnqueueJobRequest {

  @SerializedName("session_id")
  @Schema(
      name = "session_id",
      description = "Session that receives the job",
      example = "550e8400-e29b-41d4-a716-446655440000")
  public UUID sessionId;

  @SerializedName("name")
  @Schema(name = "name", description = "Display name of the job", example = "my-board")
  public String name;

  @SerializedName("priority")
  @Schema(name = "priority", description = "Scheduling priority of the job", example = "NORMAL")
  public RoutingJobPriority priority;

  @SerializedName("router_settings")
  @Schema(name = "router_settings", description = "Router configuration settings")
  public RouterSettings routerSettings;

  @SerializedName("drc_settings")
  @Schema(name = "drc_settings", description = "DRC configuration settings")
  public DesignRulesCheckerSettings drcSettings;

  /** Creates a new server-owned job in the given session from the client's choices. */
  public RoutingJob toRoutingJob() {
    RoutingJob job = new RoutingJob(sessionId);
    if (name != null && !name.isBlank()) {
      job.name = name;
    }
    if (priority != null) {
      job.priority = priority;
    }
    if (routerSettings != null) {
      job.routerSettings = routerSettings;
    }
    if (drcSettings != null) {
      job.drcSettings = drcSettings;
    }
    return job;
  }
}
