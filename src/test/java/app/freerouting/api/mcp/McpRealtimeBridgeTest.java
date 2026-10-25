package app.freerouting.api.mcp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** MCP activity (tool names and statuses) reaches only the subscribers of the caller. */
class McpRealtimeBridgeTest {

  @Test
  void anEventReachesOnlyTheSubscribersOfItsCaller() {
    Sse sse = mock(Sse.class);
    OutboundSseEvent.Builder builder = mock(OutboundSseEvent.Builder.class, RETURNS_SELF);
    when(sse.newEventBuilder()).thenReturn(builder);
    when(builder.build()).thenReturn(mock(OutboundSseEvent.class));
    SseEventSink mine = mock(SseEventSink.class);
    SseEventSink theirs = mock(SseEventSink.class);
    SseEventSink sameProfileOtherKey = mock(SseEventSink.class);
    UUID profile = UUID.randomUUID();
    String owner = McpRealtimeBridge.owner(profile, "key-a");
    McpRealtimeBridge.registerSseClient(mine, sse, owner);
    McpRealtimeBridge.registerSseClient(
        theirs, sse, McpRealtimeBridge.owner(UUID.randomUUID(), null));
    McpRealtimeBridge.registerSseClient(
        sameProfileOtherKey, sse, McpRealtimeBridge.owner(profile, "key-b"));
    try {
      McpRealtimeBridge.broadcast(owner, "mcp.tool.called", new JsonObject());

      verify(mine).send(any(OutboundSseEvent.class));
      verify(theirs, never()).send(any(OutboundSseEvent.class));
      verify(sameProfileOtherKey, never()).send(any(OutboundSseEvent.class));
    } finally {
      McpRealtimeBridge.removeSseClient(mine);
      McpRealtimeBridge.removeSseClient(theirs);
      McpRealtimeBridge.removeSseClient(sameProfileOtherKey);
    }
  }
}
