package app.freerouting.api.mcp;

import app.freerouting.api.RequestThrottle;
import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/** MCP request rate-limiting filter; the MCP counterpart of {@code ApiRateLimitFilter}. */
@Provider
@Priority(Priorities.AUTHENTICATION + 10)
public class McpRateLimitFilter implements ContainerRequestFilter {

  @Context private HttpServletRequest servletRequest;

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    String path = requestContext.getUriInfo().getPath();
    if (!isMcpPath(path) || isExcludedPath(path)) {
      return;
    }

    RequestThrottle.MCP.countRequest(requestContext, servletRequest);
  }

  private static boolean isMcpPath(String path) {
    if (path == null) {
      return false;
    }
    String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
    return normalizedPath.startsWith("v1/mcp") || normalizedPath.startsWith(".well-known/");
  }

  private static boolean isExcludedPath(String path) {
    if (path == null) {
      return false;
    }
    String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
    return normalizedPath.startsWith(".well-known/");
  }
}
