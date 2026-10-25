package app.freerouting.core;

import app.freerouting.management.jobs.RoutingJobScheduler;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** Represents a user session that contains the jobs that will be processed by the router. */
public class Session implements Serializable {

  private static final AtomicLong CREATION_COUNTER = new AtomicLong();

  @SerializedName("id")
  public final UUID id = UUID.randomUUID();

  @SerializedName("user_id")
  public final UUID userId;

  @SerializedName("host")
  public final String host;

  /**
   * Hashed API key used for internal telemetry attribution. Marked transient to avoid exposing in
   * API responses.
   */
  public final transient String apiKeyHash;

  public transient volatile boolean isPrimary;

  /** Position in creation order, used to evict the oldest sessions first. */
  public final transient long creationSequence = CREATION_COUNTER.incrementAndGet();

  /**
   * Creates a new session.
   *
   * @param userId The user ID that the session belongs to.
   * @param host The client host identifier.
   */
  public Session(UUID userId, String host) {
    this(userId, host, null);
  }

  /**
   * Creates a new session with an associated API key hash.
   *
   * @param userId The user ID that the session belongs to.
   * @param host The client host identifier.
   * @param apiKeyHash The SHA-256 hash of the caller's API key, or {@code null}.
   */
  public Session(UUID userId, String host, String apiKeyHash) {
    this.userId = userId;
    this.apiKeyHash = apiKeyHash;

    // Normalise: treat null or blank as the safe default
    if (host == null || host.isBlank()) {
      host = "Unknown/0.0";
    }
    this.host = host;

    // check if the host value is valid (it must contain the host name and version separated by "/")
    if (host.split("/").length != 2) {
      throw new IllegalArgumentException(
          "Invalid host value: '"
              + host
              + "'. It must contain the host name and version separated by '/'.");
    }
  }

  /**
   * Returns whether the caller owns this session: the same profile and, for an authenticated
   * caller, the API key that created the session. The profile comes from a request header the
   * client chooses, so without the key check any valid key could act as any profile.
   *
   * @param userId the caller's profile id
   * @param apiKeyHash the SHA-256 hash of the caller's validated API key, or {@code null} when
   *     authentication is disabled
   * @return whether the caller owns this session
   */
  public boolean isOwnedBy(UUID userId, String apiKeyHash) {
    return this.userId.equals(userId) && (apiKeyHash == null || apiKeyHash.equals(this.apiKeyHash));
  }

  /**
   * Adds a job to the session.
   *
   * @param routingJob The job to add.
   */
  public void addJob(RoutingJob routingJob) {
    RoutingJobScheduler.getInstance().enqueueJob(routingJob);
  }

  /**
   * Gets the unique identifier of the session.
   *
   * @return The session ID.
   */
  public UUID getId() {
    return this.id;
  }
}
