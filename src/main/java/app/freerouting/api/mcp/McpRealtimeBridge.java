package app.freerouting.api.mcp;

import com.google.gson.JsonObject;
import jakarta.websocket.Session;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pushes MCP activity events to the SSE and WebSocket subscribers of the caller that caused them.
 *
 * <p>Every subscriber is registered with its owner: the caller's profile id and, when the request
 * was authenticated, the hash of its API key (as for sessions, see {@code Session.isOwnedBy}). An
 * event reaches only the subscribers of the same owner, so one client does not see which tools
 * another client calls.
 */
public final class McpRealtimeBridge {

  private record SseClient(Sse sse, String owner) {}

  private static final Map<SseEventSink, SseClient> SSE_CLIENTS = new ConcurrentHashMap<>();
  private static final Map<Session, String> WS_CLIENTS = new ConcurrentHashMap<>();

  private McpRealtimeBridge() {}

  /** Returns the owner key of a caller. */
  static String owner(java.util.UUID profileId, String apiKeyHash) {
    return profileId + "/" + (apiKeyHash == null ? "" : apiKeyHash);
  }

  /** Registers an SSE client event sink of the given owner. */
  public static void registerSseClient(SseEventSink sink, Sse sse, String owner) {
    SSE_CLIENTS.put(sink, new SseClient(sse, owner));
  }

  /** Removes an SSE client event sink. */
  public static void removeSseClient(SseEventSink sink) {
    SSE_CLIENTS.remove(sink);
  }

  /** Registers a WebSocket session client of the given owner. */
  public static void registerWsClient(Session session, String owner) {
    WS_CLIENTS.put(session, owner);
  }

  /** Removes a WebSocket session client. */
  public static void removeWsClient(Session session) {
    WS_CLIENTS.remove(session);
  }

  /** Sends an event name and payload JSON to the connected clients of the given owner. */
  public static void broadcast(String owner, String eventName, JsonObject payload) {
    JsonObject envelope = new JsonObject();
    envelope.addProperty("event", eventName);
    envelope.add("payload", payload);
    String json = envelope.toString();

    SSE_CLIENTS.forEach(
        (sink, client) -> {
          if (sink.isClosed()) {
            SSE_CLIENTS.remove(sink);
            return;
          }
          if (!client.owner().equals(owner)) {
            return;
          }

          OutboundSseEvent event =
              client
                  .sse()
                  .newEventBuilder()
                  .name(eventName)
                  .mediaType(jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE)
                  .data(json)
                  .build();
          sink.send(event);
        });

    WS_CLIENTS.forEach(
        (session, sessionOwner) -> {
          if (!session.isOpen()) {
            WS_CLIENTS.remove(session);
            return;
          }
          if (sessionOwner.equals(owner)) {
            session.getAsyncRemote().sendText(json);
          }
        });
  }
}
