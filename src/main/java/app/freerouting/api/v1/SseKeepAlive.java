package app.freerouting.api.v1;

import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;

/**
 * Keep-alive comments for the job SSE streams.
 *
 * <p>A stream only notices that its client went away when a write fails. A stream of a job that
 * produces no events (for example one that is queued and never started) would otherwise keep its
 * scheduled poll and its job listener forever after the client disconnected, so every stream sends
 * a comment event at this interval while it has nothing else to send.
 */
final class SseKeepAlive {

  /** Interval between keep-alive comments; tests shorten it. */
  static volatile long intervalMillis = 15_000;

  private SseKeepAlive() {}

  static OutboundSseEvent event(Sse sse) {
    return sse.newEventBuilder().comment("keep-alive").build();
  }
}
