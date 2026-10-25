package app.freerouting.api;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Bounds the size of every request body before a resource reads it into memory.
 *
 * <p>The resources read their bodies as one {@code String} (Base64 board files, JSON-RPC calls), so
 * without a limit a single request could allocate the whole heap. A request whose {@code
 * Content-Length} exceeds the limit is answered with 413 before anything is read; a body without a
 * length (chunked) fails with 413 as soon as it crosses the limit.
 *
 * <p>The analytics endpoints are reachable without an API key and only take small JSON objects, so
 * their limit is much lower.
 */
@Provider
@Priority(Priorities.AUTHENTICATION - 50)
public class RequestBodyLimitFilter implements ContainerRequestFilter {

  /** Largest accepted body: a 100 MB board file in Base64 plus its JSON envelope. */
  public static final long MAX_BODY_BYTES = 140L * 1024 * 1024;

  /** Largest accepted body for the unauthenticated analytics endpoints. */
  public static final long MAX_ANALYTICS_BODY_BYTES = 1024L * 1024;

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    long limit = limitFor(requestContext.getUriInfo().getPath());
    String contentLength = requestContext.getHeaderString(HttpHeaders.CONTENT_LENGTH);
    if (contentLength != null) {
      long declared;
      try {
        declared = Long.parseLong(contentLength.trim());
      } catch (NumberFormatException _) {
        declared = -1;
      }
      if (declared > limit) {
        requestContext.abortWith(tooLarge(limit));
        return;
      }
    }
    InputStream entity = requestContext.getEntityStream();
    if (entity != null) {
      requestContext.setEntityStream(new BoundedInputStream(entity, limit));
    }
  }

  static long limitFor(String path) {
    String normalized = path == null ? "" : (path.startsWith("/") ? path.substring(1) : path);
    return normalized.startsWith("v1/analytics/") ? MAX_ANALYTICS_BODY_BYTES : MAX_BODY_BYTES;
  }

  private static Response tooLarge(long limit) {
    return Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE)
        .entity("{\"error\":\"The request body exceeds the limit of " + limit + " bytes.\"}")
        .type("application/json")
        .build();
  }

  /** Fails the read once more than {@code limit} bytes have been consumed. */
  static final class BoundedInputStream extends FilterInputStream {
    private final long limit;
    private long consumed;

    BoundedInputStream(InputStream in, long limit) {
      super(in);
      this.limit = limit;
    }

    @Override
    public int read() throws IOException {
      int b = super.read();
      if (b >= 0) {
        count(1);
      }
      return b;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      int n = super.read(buffer, offset, length);
      if (n > 0) {
        count(n);
      }
      return n;
    }

    @Override
    public long skip(long n) throws IOException {
      long skipped = super.skip(n);
      if (skipped > 0) {
        count(skipped);
      }
      return skipped;
    }

    private void count(long n) {
      consumed += n;
      if (consumed > limit) {
        throw new WebApplicationException(tooLarge(limit));
      }
    }
  }
}
