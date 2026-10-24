package app.freerouting.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.WebApplicationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class RequestBodyLimitFilterTest {

  @Test
  void analyticsEndpointsHaveTheLowerLimit() {
    assertEquals(
        RequestBodyLimitFilter.MAX_ANALYTICS_BODY_BYTES,
        RequestBodyLimitFilter.limitFor("/v1/analytics/track"));
    assertEquals(
        RequestBodyLimitFilter.MAX_ANALYTICS_BODY_BYTES,
        RequestBodyLimitFilter.limitFor("v1/analytics/identify"));
    assertEquals(
        RequestBodyLimitFilter.MAX_BODY_BYTES, RequestBodyLimitFilter.limitFor("v1/jobs/enqueue"));
    assertEquals(RequestBodyLimitFilter.MAX_BODY_BYTES, RequestBodyLimitFilter.limitFor(null));
  }

  @Test
  void bodyUpToTheLimitIsReadUnchanged() throws IOException {
    byte[] body = {1, 2, 3, 4};
    try (InputStream in =
        new RequestBodyLimitFilter.BoundedInputStream(new ByteArrayInputStream(body), 4)) {
      assertArrayEquals(body, in.readAllBytes());
    }
  }

  @Test
  void readingPastTheLimitFailsWithRequestEntityTooLarge() throws IOException {
    try (InputStream in =
        new RequestBodyLimitFilter.BoundedInputStream(new ByteArrayInputStream(new byte[10]), 4)) {
      WebApplicationException error = assertThrows(WebApplicationException.class, in::readAllBytes);
      assertEquals(413, error.getResponse().getStatus());
    }
    try (InputStream in =
        new RequestBodyLimitFilter.BoundedInputStream(new ByteArrayInputStream(new byte[10]), 4)) {
      in.skip(4);
      WebApplicationException error = assertThrows(WebApplicationException.class, in::read);
      assertEquals(413, error.getResponse().getStatus());
    }
  }
}
