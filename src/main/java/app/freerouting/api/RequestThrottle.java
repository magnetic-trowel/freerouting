package app.freerouting.api;

import app.freerouting.Freerouting;
import app.freerouting.settings.RateLimitSettings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import java.util.function.Supplier;

/**
 * The fixed-window request limit of one server (REST API or MCP), shared by its rate-limit filter
 * and its API-key filter.
 *
 * <p>The rate-limit filter counts the requests that passed authentication, by API key (or by
 * address when authentication is disabled). The API-key filter counts every failed attempt by
 * address under the same limit and refuses to check further keys from an address whose failures
 * fill the window, so keys cannot be guessed faster than the configured rate.
 */
public final class RequestThrottle {

  /** The REST API server's limit ({@code api_server.rate_limit}). */
  public static final RequestThrottle API =
      new RequestThrottle(
          () ->
              Freerouting.globalSettings != null
                      && Freerouting.globalSettings.apiServerSettings != null
                  ? Freerouting.globalSettings.apiServerSettings.rateLimit
                  : null,
          "Rate limit exceeded");

  /** The MCP server's limit ({@code mcp_server.rate_limit}). */
  public static final RequestThrottle MCP =
      new RequestThrottle(
          () ->
              Freerouting.globalSettings != null
                      && Freerouting.globalSettings.mcpServerSettings != null
                  ? Freerouting.globalSettings.mcpServerSettings.rateLimit
                  : null,
          "MCP rate limit exceeded");

  private final FixedWindowRateLimiter limiter = new FixedWindowRateLimiter();
  private final Supplier<RateLimitSettings> settings;
  private final String message;

  private RequestThrottle(Supplier<RateLimitSettings> settings, String message) {
    this.settings = settings;
    this.message = message;
  }

  /** Forgets all counts (servers restarted by tests in one JVM share the limits). */
  static void resetForTesting() {
    API.limiter.clear();
    MCP.limiter.clear();
  }

  /**
   * Counts one request of the caller and answers it with 429 when the caller's window is full.
   *
   * @return whether the request was aborted
   */
  public boolean countRequest(ContainerRequestContext request, HttpServletRequest servletRequest) {
    return apply(request, request.getMethod() + ":" + identity(request, servletRequest), true);
  }

  /**
   * Answers the request with 429, without checking its key, when the failed authentications from
   * its address fill the window.
   *
   * @return whether the request was aborted
   */
  public boolean rejectAfterFailedAuthentications(
      ContainerRequestContext request, HttpServletRequest servletRequest) {
    return apply(request, failureKey(servletRequest), false);
  }

  /** Counts one failed authentication of the request's address. */
  public void countFailedAuthentication(HttpServletRequest servletRequest) {
    RateLimitSettings cfg = enabledSettings();
    if (cfg != null) {
      limiter.check(failureKey(servletRequest), maxRequests(cfg), windowSeconds(cfg));
    }
  }

  private boolean apply(ContainerRequestContext request, String key, boolean count) {
    RateLimitSettings cfg = enabledSettings();
    if (cfg == null) {
      return false;
    }
    FixedWindowRateLimiter.Decision decision =
        count
            ? limiter.check(key, maxRequests(cfg), windowSeconds(cfg))
            : limiter.peek(key, maxRequests(cfg), windowSeconds(cfg));
    if (decision.allowed()) {
      return false;
    }
    request.abortWith(
        Response.status(429)
            .header("Retry-After", String.valueOf(decision.retryAfterSeconds()))
            .entity(
                "{\"error\":\""
                    + message
                    + "\",\"retry_after_seconds\":"
                    + decision.retryAfterSeconds()
                    + "}")
            .type("application/json")
            .build());
    return true;
  }

  private RateLimitSettings enabledSettings() {
    RateLimitSettings cfg = settings.get();
    return cfg != null && Boolean.TRUE.equals(cfg.enabled) ? cfg : null;
  }

  private static int maxRequests(RateLimitSettings cfg) {
    return cfg.requestsPerWindow == null ? 120 : cfg.requestsPerWindow;
  }

  private static int windowSeconds(RateLimitSettings cfg) {
    return cfg.windowSeconds == null ? 60 : cfg.windowSeconds;
  }

  private static String failureKey(HttpServletRequest servletRequest) {
    return "auth-failure:" + address(servletRequest);
  }

  /**
   * A request that passed authentication is counted by its API key; without authentication it is
   * counted by its address. The profile headers are chosen by the client, so keying on them let
   * every request use a fresh bucket.
   */
  private static String identity(
      ContainerRequestContext request, HttpServletRequest servletRequest) {
    var security = request.getSecurityContext();
    String principal =
        security != null && security.getUserPrincipal() != null
            ? security.getUserPrincipal().getName()
            : null;
    return principal != null && !principal.isBlank() ? "key:" + principal : address(servletRequest);
  }

  private static String address(HttpServletRequest servletRequest) {
    String address = null;
    try {
      address = servletRequest != null ? servletRequest.getRemoteAddr() : null;
    } catch (RuntimeException _) {
      // No servlet request outside of a container (unit tests).
    }
    return address != null && !address.isBlank() ? "addr:" + address : "anonymous";
  }
}
