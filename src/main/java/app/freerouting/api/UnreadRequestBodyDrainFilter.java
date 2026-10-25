package app.freerouting.api;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Makes sure a response sent before the request body was read actually reaches the client.
 *
 * <p>Requests can be answered without reading their bodies, for example a 413 from {@link
 * RequestBodyLimitFilter} or a 401 from the API-key check. Jetty 12 then marks the connection as
 * not persistent and, as soon as more body bytes arrive, closes the socket while unread data is
 * still in its receive buffer. The kernel answers that close with a TCP reset, and a client that is
 * still uploading can lose the response before it reads it ({@code HttpClient} reports "header
 * parser received no bytes").
 *
 * <p>This filter runs around the Jersey servlet. When the request body is still unread after the
 * response was produced, it sends the response at once and then reads and discards the rest of the
 * body, up to {@link #MAX_DRAINED_BYTES}. A body within that bound is fully consumed, so the
 * connection ends with a normal close and the client always gets the response. A larger body is
 * answered with {@code Connection: close} and the connection is closed after the bound; by then the
 * response has been on the wire for the whole drain.
 */
public final class UnreadRequestBodyDrainFilter implements Filter {

  /** Most unread body bytes discarded after an early response before the connection is closed. */
  static final long MAX_DRAINED_BYTES = 16L * 1024 * 1024;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    chain.doFilter(request, response);

    if (request.isAsyncStarted()
        || !(request instanceof HttpServletRequest httpRequest)
        || !(response instanceof HttpServletResponse httpResponse)
        || expectsContinue(httpRequest)) {
      // A client waiting for 100 Continue does not send the body after a final response.
      return;
    }

    ServletInputStream input;
    try {
      input = request.getInputStream();
    } catch (IllegalStateException _) {
      // The body was read through getReader(), so it is not left unread.
      return;
    }
    if (input.isFinished()) {
      return;
    }

    long declared = request.getContentLengthLong();
    if (!response.isCommitted() && (declared < 0 || declared > MAX_DRAINED_BYTES)) {
      httpResponse.setHeader("Connection", "close");
    }
    response.flushBuffer();
    drain(input, MAX_DRAINED_BYTES);
  }

  private static boolean expectsContinue(HttpServletRequest request) {
    String expect = request.getHeader("Expect");
    return expect != null && expect.toLowerCase(Locale.ROOT).contains("100-continue");
  }

  /** Reads and discards up to {@code limit} bytes, stopping early at the end of the body. */
  static long drain(InputStream input, long limit) {
    byte[] buffer = new byte[8192];
    long drained = 0;
    try {
      while (drained < limit) {
        int n = input.read(buffer, 0, (int) Math.min(buffer.length, limit - drained));
        if (n < 0) {
          break;
        }
        drained += n;
      }
    } catch (IOException _) {
      // The client went away or stalled past the idle timeout; Jetty closes the connection.
    }
    return drained;
  }
}
