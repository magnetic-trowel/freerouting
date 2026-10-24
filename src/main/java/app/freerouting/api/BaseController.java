package app.freerouting.api;

import app.freerouting.api.security.ApiKeyPrincipal;
import app.freerouting.api.security.ProfileMismatchException;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.Session;
import app.freerouting.management.jobs.RoutingJobScheduler;
import app.freerouting.management.sessions.SessionManager;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

/**
 * Base class for all Freerouting API controllers.
 *
 * <p>Provides shared authentication logic via {@link #authenticateUser()}, which resolves the
 * caller's UUID from the standard HTTP request headers. All protected controller methods must call
 * this method before performing any business logic.
 *
 * <h2>Authentication headers</h2>
 *
 * <ul>
 *   <li>{@code Freerouting-Profile-ID} — preferred; must be a valid RFC 4122 UUID string.
 *   <li>{@code Freerouting-Profile-Email} — fallback; email-to-UUID resolution is not yet
 *       implemented (see TODO in {@link #authenticateUser()}).
 * </ul>
 *
 * <p>Note: the method name intentionally uses PascalCase to match the original naming convention of
 * this code-base; a rename to camelCase is planned as a separate clean-up.
 */
public class BaseController {

  /**
   * Thrown by {@link #authenticateUser()} when the caller's profile headers are missing or cannot
   * be resolved. It is a client error: {@link ApiExceptionMapper} answers it with 400 Bad Request.
   */
  public static final class MissingProfileException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    MissingProfileException(String message) {
      super(message);
    }
  }

  @Context private HttpHeaders httpHeaders;
  @Context private jakarta.ws.rs.core.SecurityContext securityContext;
  private UUID userIdOverride;

  /** Default constructor for BaseController. */
  public BaseController() {}

  /** Sets a user ID override (for testing without full HTTP context). */
  public void setUserIdOverride(UUID userIdOverride) {
    this.userIdOverride = userIdOverride;
  }

  /** Sets the HTTP headers (for testing). */
  public void setHttpHeaders(HttpHeaders httpHeaders) {
    this.httpHeaders = httpHeaders;
  }

  /** Sets the security context (for testing). */
  public void setSecurityContext(jakarta.ws.rs.core.SecurityContext securityContext) {
    this.securityContext = securityContext;
  }

  /**
   * Returns the authenticated principal from the request's {@link
   * jakarta.ws.rs.core.SecurityContext}, or {@code null} if unauthenticated.
   *
   * @return the caller's Principal or null
   */
  public java.security.Principal getAuthenticatedPrincipal() {
    return securityContext != null ? securityContext.getUserPrincipal() : null;
  }

  /**
   * Resolves and returns the authenticated caller's {@link UUID}.
   *
   * <p>Resolution order:
   *
   * <ol>
   *   <li>Use {@code userIdOverride} if set (e.g. in unit tests).
   *   <li>When the API key is issued to a profile, use that profile; a {@code
   *       Freerouting-Profile-ID} header that names another profile is refused with {@link
   *       ProfileMismatchException} (403). The email header cannot be checked and is ignored then.
   *   <li>Parse {@code Freerouting-Profile-ID} header as a UUID.
   *   <li>If that header is absent or unparsable, fall back to {@code Freerouting-Profile-Email}
   *       (email-to-UUID look-up is not yet implemented).
   * </ol>
   *
   * @return the caller's UUID — never {@code null}.
   * @throws MissingProfileException if both headers are missing/empty, or if neither yields a
   *     resolvable UUID, and the API key is not issued to a profile.
   * @throws ProfileMismatchException if the API key is issued to another profile than the header
   *     names.
   */
  protected UUID authenticateUser() {
    if (userIdOverride != null) {
      return userIdOverride;
    }

    String userIdString =
        httpHeaders != null ? httpHeaders.getHeaderString("Freerouting-Profile-ID") : null;
    String userEmailString =
        httpHeaders != null ? httpHeaders.getHeaderString("Freerouting-Profile-Email") : null;

    // A key issued to a profile acts only as that profile (see ApiKeyPrincipal).
    if (getAuthenticatedPrincipal() instanceof ApiKeyPrincipal key && key.profileId() != null) {
      return key.resolveProfile(userIdString);
    }

    if (((userIdString == null) || (userIdString.isEmpty()))
        && ((userEmailString == null) || (userEmailString.isEmpty()))) {
      throw new MissingProfileException(
          "Freerouting-Profile-ID or Freerouting-Profile-Email HTTP request header must be set in"
              + " order to get authenticated.");
    }

    UUID userId = null;

    // We need to get the userId from the e-mail address first
    if ((userIdString != null) && (!userIdString.isEmpty())) {
      try {
        userId = UUID.fromString(userIdString);
      } catch (IllegalArgumentException _) {
        // We couldn't parse the userId, so we fall back to e-mail address
      }
    }

    if ((userEmailString != null) && (!userEmailString.isEmpty())) {
      // TODO: get userId from e-mail address
    }

    if (userId == null) {
      throw new MissingProfileException(
          "The user couldn't be authenticated based on the Freerouting-Profile-ID or"
              + " Freerouting-Profile-Email HTTP request header values.");
    }

    // TODO: authenticate the user by calling the auth endpoint

    return userId;
  }

  /**
   * Returns the SHA-256 hash of the caller's validated API key, or {@code null} when the request is
   * not authenticated (authentication disabled). Sessions are bound to the key that created them,
   * so a valid key cannot reach another key's sessions by sending their profile id.
   */
  protected String callerApiKeyHash() {
    java.security.Principal principal = getAuthenticatedPrincipal();
    return principal != null ? ApiUsageFilter.sha256Hex(principal.getName()) : null;
  }

  /**
   * Returns the session when it exists and belongs to the caller, or {@code null}. Another caller's
   * session is answered like an absent one.
   */
  protected Session findOwnedSession(String sessionId, UUID userId) {
    return SessionManager.getInstance().getSession(sessionId, userId, callerApiKeyHash());
  }

  /**
   * Returns the job when it exists and belongs to the caller, or {@code null}. Another caller's job
   * is answered like an absent one ({@link #jobNotFound()}), so a response neither shows a foreign
   * session id nor tells whether a job id exists.
   */
  protected RoutingJob findOwnedJob(String jobId, UUID userId) {
    RoutingJob job = RoutingJobScheduler.getInstance().getJob(jobId);
    if (job == null || job.sessionId == null) {
      return null;
    }
    return findOwnedSession(job.sessionId.toString(), userId) != null ? job : null;
  }

  /** The response for a job that does not exist or belongs to another caller. */
  protected static Response jobNotFound() {
    return Response.status(Response.Status.NOT_FOUND)
        .entity("{\"error\":\"Job not found.\"}")
        .type(MediaType.APPLICATION_JSON)
        .build();
  }
}
