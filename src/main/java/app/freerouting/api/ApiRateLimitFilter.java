package app.freerouting.api;

import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * API request rate limiting filter. It runs after authentication and counts each request by its API
 * key; failed authentications are counted by the API-key filter under the same limit (see {@link
 * RequestThrottle}).
 */
@Provider
@Priority(Priorities.AUTHENTICATION + 10)
public class ApiRateLimitFilter implements ContainerRequestFilter {

  @Context private HttpServletRequest servletRequest;

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    RequestThrottle.API.countRequest(requestContext, servletRequest);
  }
}
