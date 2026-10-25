package app.freerouting.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class UnreadRequestBodyDrainFilterTest {

  @Test
  void drainReadsTheBodyToItsEnd() {
    ByteArrayInputStream body = new ByteArrayInputStream(new byte[100_000]);

    assertEquals(100_000, UnreadRequestBodyDrainFilter.drain(body, 1_000_000));
    assertEquals(0, body.available());
  }

  @Test
  void drainStopsAtTheLimit() {
    ByteArrayInputStream body = new ByteArrayInputStream(new byte[100_000]);

    assertEquals(10_000, UnreadRequestBodyDrainFilter.drain(body, 10_000));
    assertEquals(90_000, body.available());
  }

  @Test
  void drainStopsQuietlyWhenTheClientGoesAway() {
    InputStream failing =
        new InputStream() {
          @Override
          public int read() throws IOException {
            throw new IOException("Connection reset");
          }

          @Override
          public int read(byte[] buffer, int offset, int length) throws IOException {
            throw new IOException("Connection reset");
          }
        };

    assertEquals(0, UnreadRequestBodyDrainFilter.drain(failing, 10_000));
  }
}
