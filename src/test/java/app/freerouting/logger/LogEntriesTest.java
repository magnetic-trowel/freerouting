package app.freerouting.logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class LogEntriesTest {

  @Test
  void throwingListenerDoesNotReachTheLoggingThreadOrOtherListeners() {
    LogEntries logEntries = new LogEntries();
    AtomicInteger laterListenerCalls = new AtomicInteger(0);
    logEntries.addLogEntryAddedListener(
        _ -> {
          throw new IllegalStateException("listener failure");
        });
    logEntries.addLogEntryAddedListener(_ -> laterListenerCalls.incrementAndGet());

    LogEntry entry = logEntries.add(LogEntryType.Info, "routing progress", null);

    assertNotNull(entry, "add must return normally although a listener threw");
    assertEquals(1, laterListenerCalls.get(), "the next listener must still be notified");
    assertEquals(1, logEntries.size(), "the failure must not be recorded as another entry");
  }

  @Test
  void addAndRemoveLogEntryAddedListener() {
    LogEntries logEntries = new LogEntries();
    AtomicInteger listenerCallCount = new AtomicInteger(0);

    LogEntries.LogEntryAddedListener listener = _ -> listenerCallCount.incrementAndGet();

    // Add listener and verify it's called
    logEntries.addLogEntryAddedListener(listener);
    logEntries.add(LogEntryType.Info, "Test message 1", null);
    assertEquals(1, listenerCallCount.get(), "Listener should be called after being added");

    // Remove listener and verify it's no longer called
    logEntries.removeLogEntryAddedListener(listener);
    logEntries.add(LogEntryType.Info, "Test message 2", null);
    assertEquals(1, listenerCallCount.get(), "Listener should not be called after being removed");
  }

  @Test
  void fullCollectionDropsTheOldestEntries() {
    LogEntries logEntries = new LogEntries(5, 100);
    for (int i = 1; i <= 8; i++) {
      logEntries.add(i % 2 == 0 ? LogEntryType.Warning : LogEntryType.Info, "message " + i, null);
    }

    assertEquals(
        List.of("message 4", "message 5", "message 6", "message 7", "message 8"),
        messages(logEntries.getEntries(null, null)));
    assertEquals(5, logEntries.size());
    assertEquals(3, logEntries.getWarningCount(), "the counts follow the kept entries");
  }

  @Test
  void oneChattyTopicCannotPushOutTheOthers() {
    LogEntries logEntries = new LogEntries(100, 3);
    UUID chatty = UUID.randomUUID();
    UUID quiet = UUID.randomUUID();
    logEntries.add(LogEntryType.Info, "quiet 1", quiet);
    logEntries.add(LogEntryType.Info, "without topic", null);
    for (int i = 1; i <= 10; i++) {
      logEntries.add(LogEntryType.Info, "chatty " + i, chatty);
    }

    assertEquals(
        List.of("chatty 8", "chatty 9", "chatty 10"),
        messages(logEntries.getEntries(null, chatty)));
    assertEquals(List.of("quiet 1"), messages(logEntries.getEntries(null, quiet)));
    assertEquals(
        List.of("quiet 1", "without topic", "chatty 8", "chatty 9", "chatty 10"),
        messages(logEntries.getEntries(null, null)),
        "all entries stay in the order they were logged");
  }

  @Test
  void removeTopicDropsOnlyThatTopic() {
    LogEntries logEntries = new LogEntries();
    UUID finished = UUID.randomUUID();
    UUID running = UUID.randomUUID();
    logEntries.add(LogEntryType.Warning, "finished warning", finished);
    logEntries.add(LogEntryType.Error, "finished error", finished, new IllegalStateException());
    logEntries.add(LogEntryType.Info, "running info", running);
    logEntries.add(LogEntryType.Warning, "warning without topic", null);

    logEntries.removeTopic(finished);

    assertEquals(0, logEntries.getEntries(null, finished).length);
    assertEquals(
        List.of("running info", "warning without topic"),
        messages(logEntries.getEntries(null, null)));
    assertEquals(List.of("running info"), messages(logEntries.getEntries(null, running)));
    assertEquals(1, logEntries.getWarningCount());
    assertEquals(0, logEntries.getErrorCount());
  }

  @Test
  void longMessagesAndExceptionsAreKeptBounded() {
    LogEntries logEntries = new LogEntries();
    String longMessage = "x".repeat(LogEntry.MAX_MESSAGE_LENGTH * 3);
    Throwable logged = thrownAtDepth(200);
    for (int level = 0; level < 10; level++) {
      logged = new IllegalStateException("level " + level, logged);
    }

    LogEntry entry = logEntries.add(LogEntryType.Error, longMessage, null, logged);

    assertTrue(entry.getMessage().startsWith("x".repeat(LogEntry.MAX_MESSAGE_LENGTH)));
    assertTrue(entry.getMessage().length() < LogEntry.MAX_MESSAGE_LENGTH + 50);
    Throwable snapshot = entry.getException();
    assertNotSame(logged, snapshot, "the entry must not keep the logged exception alive");
    assertEquals(logged.toString(), snapshot.toString());
    int throwables = 0;
    for (Throwable t = snapshot; t != null; t = t.getCause()) {
      assertTrue(t.getStackTrace().length <= LogEntry.MAX_STACK_FRAMES);
      throwables++;
    }
    assertEquals(LogEntry.MAX_CAUSES + 1, throwables);
    StringWriter printed = new StringWriter();
    snapshot.printStackTrace(new PrintWriter(printed));
    assertTrue(printed.toString().startsWith(logged.toString()));
    assertTrue(printed.toString().contains("Caused by: " + logged.getCause()));
  }

  @Test
  void onlyTheNewestErrorsKeepTheirException() {
    LogEntries logEntries = new LogEntries();
    LogEntry first =
        logEntries.add(LogEntryType.Error, "first", null, new IllegalStateException("first"));
    LogEntry last = first;
    for (int i = 0; i < LogEntries.MAX_ENTRIES_WITH_EXCEPTION; i++) {
      last = logEntries.add(LogEntryType.Error, "next", null, new IllegalStateException("next"));
    }

    assertNull(first.getException());
    assertEquals("first", first.getMessage(), "the entry itself stays");
    assertNotNull(last.getException());
  }

  @Test
  void concurrentLoggingReadingAndListenersStayConsistent() throws Exception {
    int writers = 8;
    int entriesPerWriter = 5_000;
    LogEntries logEntries = new LogEntries(1_000, 100);
    List<UUID> topics = new ArrayList<>();
    AtomicInteger notifications = new AtomicInteger();
    AtomicBoolean writing = new AtomicBoolean(true);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(writers + 2);
    try {
      List<Future<?>> writes = new ArrayList<>();
      for (int w = 0; w < writers; w++) {
        UUID topic = UUID.randomUUID();
        topics.add(topic);
        writes.add(
            pool.submit(
                () -> {
                  start.await();
                  for (int i = 0; i < entriesPerWriter; i++) {
                    logEntries.add(
                        i % 10 == 0 ? LogEntryType.Warning : LogEntryType.Info,
                        "entry " + i,
                        i % 2 == 0 ? topic : null,
                        i % 500 == 0 ? new IllegalStateException("entry " + i) : null);
                  }
                  return null;
                }));
      }
      final Future<?> reads =
          pool.submit(
              () -> {
                start.await();
                while (writing.get()) {
                  logEntries.getEntries(null, null);
                  logEntries.getEntries(null, topics.getFirst());
                  logEntries.getAsString();
                  logEntries.getWarningCount();
                }
                return null;
              });
      final Future<?> listenerChanges =
          pool.submit(
              () -> {
                start.await();
                while (writing.get()) {
                  LogEntries.LogEntryAddedListener listener = _ -> notifications.incrementAndGet();
                  logEntries.addLogEntryAddedListener(listener);
                  logEntries.removeLogEntryAddedListener(listener);
                }
                return null;
              });

      start.countDown();
      for (Future<?> write : writes) {
        write.get(60, TimeUnit.SECONDS);
      }
      writing.set(false);
      reads.get(60, TimeUnit.SECONDS);
      listenerChanges.get(60, TimeUnit.SECONDS);
    } finally {
      writing.set(false);
      pool.shutdownNow();
    }

    LogEntry[] kept = logEntries.getEntries(null, null);
    assertEquals(1_000, kept.length);
    assertEquals(1_000, logEntries.size());
    assertEquals(
        Stream.of(kept).filter(e -> e.getType() == LogEntryType.Warning).count(),
        logEntries.getWarningCount());
    for (UUID topic : topics) {
      assertTrue(logEntries.getEntries(null, topic).length <= 100);
    }
  }

  private static List<String> messages(LogEntry[] entries) {
    return Stream.of(entries).map(LogEntry::getMessage).toList();
  }

  /** Returns an exception whose stack trace is at least {@code depth} frames deep. */
  private static Throwable thrownAtDepth(int depth) {
    return depth == 0 ? new IllegalArgumentException("deep") : thrownAtDepth(depth - 1);
  }
}
