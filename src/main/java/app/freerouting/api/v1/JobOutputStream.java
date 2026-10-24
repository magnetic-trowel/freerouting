package app.freerouting.api.v1;

import static app.freerouting.util.gson.GsonProvider.GSON;

import app.freerouting.api.dto.BoardFilePayload;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.core.BoardFileDetails;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.RoutingJobState;
import app.freerouting.core.events.RoutingJobLogEntryAddedEventListener;
import app.freerouting.io.FileFormat;
import app.freerouting.io.kicad.KiCadJsonWriter;
import app.freerouting.logger.FRLogger;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.zip.CRC32;

/**
 * Owns one subscriber's job stream (output snapshots or log entries): change detection, keep-alive
 * comments and connection cleanup.
 *
 * <p>All streams share two daemon threads. A stream is a scheduled poll, not a thread, so connected
 * clients cost no threads; a poll never blocks, and every send is asynchronous and bounded by
 * {@link #SEND_TIMEOUT_SECONDS}.
 */
final class JobOutputStream {
  // Bound a stalled asynchronous send so it cannot retain a job after the client stops reading.
  private static final int SEND_TIMEOUT_SECONDS = 10;
  private static final ScheduledThreadPoolExecutor EXECUTOR = newExecutor();
  private static final AtomicInteger OPEN_STREAMS = new AtomicInteger();
  private final RoutingJob job;
  private final SseEventSink sink;
  private final Sse sse;
  private volatile long nextKeepAliveAt = System.currentTimeMillis() + SseKeepAlive.intervalMillis;
  private final Supplier<OutboundSseEvent> nextEvent;
  private final Runnable onClose;
  private final AtomicBoolean closed = new AtomicBoolean();
  private final AtomicBoolean sending = new AtomicBoolean();
  // Set by wake() while a send is in flight, so the next event follows without waiting a period.
  private volatile boolean wakeRequested;
  private volatile ScheduledFuture<?> polling;
  private volatile CompletableFuture<?> pendingSend;

  private JobOutputStream(
      RoutingJob job,
      SseEventSink sink,
      Sse sse,
      Supplier<OutboundSseEvent> nextEvent,
      Runnable onClose) {
    this.job = job;
    this.sink = sink;
    this.sse = sse;
    this.nextEvent = nextEvent;
    this.onClose = onClose;
  }

  private static ScheduledThreadPoolExecutor newExecutor() {
    ScheduledThreadPoolExecutor executor =
        new ScheduledThreadPoolExecutor(
            2,
            runnable -> {
              Thread worker = new Thread(runnable, "job-stream-worker");
              worker.setDaemon(true);
              return worker;
            });
    executor.setRemoveOnCancelPolicy(true);
    return executor;
  }

  /** Returns the number of open streams (for tests). */
  static int openStreams() {
    return OPEN_STREAMS.get();
  }

  private void start(long periodMillis) {
    OPEN_STREAMS.incrementAndGet();
    polling = EXECUTOR.scheduleWithFixedDelay(this::poll, 0, periodMillis, TimeUnit.MILLISECONDS);
    if (closed.get()) {
      // The first poll closed the stream before the handle was stored.
      polling.cancel(false);
    }
  }

  /** Polls now instead of at the next period, because an event is waiting. */
  private void wake() {
    wakeRequested = true;
    if (!closed.get()) {
      EXECUTOR.execute(this::poll);
    }
  }

  /** Streams the job's log entries as they are added, one event per entry. */
  static void startLogs(RoutingJob job, SseEventSink sink, Sse sse) {
    Queue<Object> entries = new ConcurrentLinkedQueue<>();
    JobOutputStream[] stream = new JobOutputStream[1];
    // The listener runs on the routing thread: it only queues the entry.
    RoutingJobLogEntryAddedEventListener listener =
        event -> {
          entries.add(event.getLogEntry());
          stream[0].wake();
        };
    stream[0] =
        new JobOutputStream(
            job,
            sink,
            sse,
            () -> {
              Object entry = entries.poll();
              return entry == null
                  ? null
                  : sse.newEventBuilder()
                      .id(String.valueOf(System.currentTimeMillis()))
                      .data(GSON.toJson(entry))
                      .build();
            },
            () -> job.removeLogEntryAddedEventListener(listener));
    job.addLogEntryAddedEventListener(listener);
    stream[0].start(200);
  }

  static void startSes(RoutingJob job, SseEventSink sink, Sse sse) {
    // A checksum belongs to this subscription, not to the shared job or other clients.
    AtomicLong previousChecksum = new AtomicLong(-1);
    JobOutputStream stream =
        new JobOutputStream(
            job,
            sink,
            sse,
            () -> {
              var output = job.output;
              if (output == null) {
                return null;
              }
              byte[] bytes = output.getData().readAllBytes();
              if (bytes.length == 0) {
                return null;
              }
              BoardFilePayload payload = new BoardFilePayload();
              payload.jobId = job.id;
              payload.setFilename(output.getFilename());
              payload.setData(bytes);
              if (payload.crc32 == previousChecksum.get()) {
                return null;
              }
              previousChecksum.set(payload.crc32);
              payload.dataBase64 = Base64.getEncoder().encodeToString(bytes);
              return sse.newEventBuilder()
                  .id(String.valueOf(System.currentTimeMillis()))
                  .data(GSON.toJson(payload))
                  .build();
            },
            () -> {});
    stream.start(200);
  }

  static void startJson(RoutingJob job, SseEventSink sink, Sse sse) {
    AtomicLong previousChecksum = new AtomicLong(-1);
    JobOutputStream stream =
        new JobOutputStream(
            job,
            sink,
            sse,
            () -> {
              String json = currentJson(job);
              if (json == null) {
                return null;
              }
              CRC32 checksum = new CRC32();
              checksum.update(json.getBytes(StandardCharsets.UTF_8));
              if (checksum.getValue() == previousChecksum.get()) {
                return null;
              }
              previousChecksum.set(checksum.getValue());
              return sse.newEventBuilder()
                  .id(String.valueOf(System.currentTimeMillis()))
                  .name("json-output")
                  .data(json)
                  .mediaType(MediaType.APPLICATION_JSON_TYPE)
                  .build();
            },
            () -> {});
    stream.start(500);
  }

  /**
   * Returns the KiCad JSON of the job's board. Once the scheduler released the board of a finished
   * job, a KiCad JSON output is the same document, written from the final board.
   */
  private static String currentJson(RoutingJob job) {
    RoutingBoard board = job.board;
    if (board != null) {
      return KiCadJsonWriter.write(board, job.name);
    }
    BoardFileDetails output = job.output;
    if (output == null || output.format != FileFormat.KICAD_SESSION_JSON) {
      return null;
    }
    byte[] bytes = output.getData().readAllBytes();
    return bytes.length > 0 ? new String(bytes, StandardCharsets.UTF_8) : null;
  }

  private void poll() {
    try {
      if (closed.get() || sink.isClosed() || failed()) {
        close();
        return;
      }
      if (!sending.compareAndSet(false, true)) {
        return;
      }
      wakeRequested = false;
      boolean finalSnapshot = job.state == RoutingJobState.COMPLETED;
      OutboundSseEvent event = nextEvent.get();
      if (failed()) {
        close();
        return;
      }
      if (event == null) {
        if (finalSnapshot) {
          sending.set(false);
          close();
          return;
        }
        if (System.currentTimeMillis() < nextKeepAliveAt) {
          sending.set(false);
          return;
        }
        // Nothing new to send: a keep-alive comment notices a client that went away.
        event = SseKeepAlive.event(sse);
      }
      nextKeepAliveAt = System.currentTimeMillis() + SseKeepAlive.intervalMillis;
      pendingSend = sink.send(event).toCompletableFuture();
      pendingSend
          .orTimeout(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .whenComplete(
              (ignored, error) -> {
                if (closed.get()) {
                  return;
                }
                if (error != null) {
                  // A failed or stalled send is how a stream learns that its client went away.
                  FRLogger.debug("Job stream closed, the client stopped receiving: " + error);
                  close();
                } else {
                  sending.set(false);
                  // A finished job closes once nothing is left to send (several log entries may
                  // be queued); a woken stream sends its next event without waiting a period.
                  if (finalSnapshot || wakeRequested) {
                    EXECUTOR.execute(this::poll);
                  }
                }
              });
    } catch (Exception error) {
      FRLogger.error("Error while streaming routing output", error);
      close();
    }
  }

  private boolean failed() {
    RoutingJobState state = job.state;
    return state != null && state.isTerminal() && state != RoutingJobState.COMPLETED;
  }

  private void close() {
    if (!closed.compareAndSet(false, true)) {
      return;
    }
    try {
      sink.close();
    } catch (Exception error) {
      FRLogger.error("Error closing SSE event sink", error);
    } finally {
      OPEN_STREAMS.decrementAndGet();
      var schedule = polling;
      if (schedule != null) {
        schedule.cancel(false);
      }
      onClose.run();
      var pending = pendingSend;
      if (pending != null) {
        pending.cancel(false);
      }
    }
  }
}
