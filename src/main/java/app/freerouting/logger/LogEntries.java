package app.freerouting.logger;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * In-memory collection of log entries for UI display and export.
 *
 * <p>The collection is bounded so that a long-running server does not accumulate every message it
 * ever logged: it keeps at most {@link #DEFAULT_MAX_ENTRIES} entries, of which at most {@link
 * #DEFAULT_MAX_ENTRIES_PER_TOPIC} share a topic (one routing job), and drops the oldest first. Only
 * the newest {@link #MAX_ENTRIES_WITH_EXCEPTION} error entries keep their exception snapshot. The
 * scheduler removes the entries of a job when it removes the job ({@link #removeTopic}). The
 * console and file logs are not affected by these limits.
 *
 * <p>All methods are safe to call from several threads. Listeners are notified on the thread that
 * logged, outside the collection's lock; a listener that throws does not reach that thread.
 */
public class LogEntries {

  /** Entries kept in total. */
  static final int DEFAULT_MAX_ENTRIES = 20_000;

  /** Entries kept for one topic, so that one chatty job cannot push out all others. */
  static final int DEFAULT_MAX_ENTRIES_PER_TOPIC = 2_000;

  /** Error entries that keep their exception snapshot. */
  static final int MAX_ENTRIES_WITH_EXCEPTION = 200;

  private final int maxEntries;
  private final int maxEntriesPerTopic;

  /** All kept entries, oldest first; entries compare by identity. Guards all state below. */
  private final LinkedHashSet<LogEntry> entries = new LinkedHashSet<>();

  /** The kept entries of each topic, oldest first, in the same order as {@link #entries}. */
  private final Map<UUID, ArrayDeque<LogEntry>> entriesByTopic = new HashMap<>();

  /** Error entries that still hold an exception snapshot, oldest first. */
  private final ArrayDeque<LogEntry> entriesWithException = new ArrayDeque<>();

  private int warningCount;
  private int errorCount;
  private final List<LogEntryAddedListener> listeners = new CopyOnWriteArrayList<>();

  /** Creates a collection with the default limits. */
  public LogEntries() {
    this(DEFAULT_MAX_ENTRIES, DEFAULT_MAX_ENTRIES_PER_TOPIC);
  }

  /**
   * Creates a collection with the given limits.
   *
   * @param maxEntries the number of entries kept in total
   * @param maxEntriesPerTopic the number of entries kept for one topic
   */
  LogEntries(int maxEntries, int maxEntriesPerTopic) {
    this.maxEntries = maxEntries;
    this.maxEntriesPerTopic = maxEntriesPerTopic;
  }

  /** Returns the number of warning entries currently stored. */
  public int getWarningCount() {
    synchronized (entries) {
      return warningCount;
    }
  }

  /** Returns the number of error entries currently stored. */
  public int getErrorCount() {
    synchronized (entries) {
      return errorCount;
    }
  }

  /** Returns the number of entries currently stored. */
  public int size() {
    synchronized (entries) {
      return entries.size();
    }
  }

  /** Removes all stored log entries. */
  public void clear() {
    synchronized (entries) {
      entries.clear();
      entriesByTopic.clear();
      entriesWithException.clear();
      warningCount = 0;
      errorCount = 0;
    }
  }

  /**
   * Removes the entries of one topic, for example of a routing job that the scheduler no longer
   * keeps.
   *
   * @param topic the topic whose entries are removed; {@code null} removes nothing
   */
  public void removeTopic(UUID topic) {
    if (topic == null) {
      return;
    }
    synchronized (entries) {
      ArrayDeque<LogEntry> ofTopic = entriesByTopic.remove(topic);
      if (ofTopic == null) {
        return;
      }
      for (LogEntry entry : ofTopic) {
        entries.remove(entry);
        count(entry, -1);
      }
      entriesWithException.removeIf(entry -> topic.equals(entry.topic));
    }
  }

  /** Returns all entries as a single newline-delimited string. */
  public String getAsString() {
    return Stream.of(getEntries(null, null))
        .map(LogEntry::toString)
        .collect(Collectors.joining("\n", "", "\n"));
  }

  /** Returns all entries as an array of formatted strings. */
  public String[] get() {
    return Stream.of(getEntries(null, null)).map(LogEntry::toString).toArray(String[]::new);
  }

  /** Returns entries filtered by timestamp and optional topic, oldest first. */
  public LogEntry[] getEntries(Instant entriesSince, UUID topic) {
    Predicate<LogEntry> since = e -> entriesSince == null || e.timestamp.isAfter(entriesSince);
    synchronized (entries) {
      if (topic == null) {
        return entries.stream().filter(since).toArray(LogEntry[]::new);
      }
      ArrayDeque<LogEntry> ofTopic = entriesByTopic.get(topic);
      return ofTopic == null
          ? new LogEntry[0]
          : ofTopic.stream().filter(since).toArray(LogEntry[]::new);
    }
  }

  /**
   * Returns the entries of any of {@code topics}, for example of all jobs of one session, oldest
   * first.
   *
   * @param entriesSince only entries after this instant, or {@code null} for all
   * @param topics the topics whose entries are returned
   */
  public LogEntry[] getEntriesOfTopics(Instant entriesSince, Collection<UUID> topics) {
    Set<UUID> wanted = Set.copyOf(topics);
    synchronized (entries) {
      return entries.stream()
          .filter(e -> e.topic != null && wanted.contains(e.topic))
          .filter(e -> entriesSince == null || e.timestamp.isAfter(entriesSince))
          .toArray(LogEntry[]::new);
    }
  }

  /** Adds a log entry without an associated exception. */
  public LogEntry add(LogEntryType type, String message, UUID topic) {
    return this.add(type, message, topic, null);
  }

  /** Adds a log entry, optionally with an associated exception. */
  public LogEntry add(LogEntryType type, String message, UUID topic, Throwable exception) {
    LogEntry logEntry = new LogEntry(type, message, exception, topic);

    synchronized (entries) {
      entries.add(logEntry);
      count(logEntry, 1);
      if (topic != null) {
        ArrayDeque<LogEntry> ofTopic =
            entriesByTopic.computeIfAbsent(topic, _ -> new ArrayDeque<>());
        ofTopic.addLast(logEntry);
        if (ofTopic.size() > maxEntriesPerTopic) {
          LogEntry oldestOfTopic = ofTopic.pollFirst();
          entries.remove(oldestOfTopic);
          count(oldestOfTopic, -1);
        }
      }
      if (logEntry.exception != null) {
        entriesWithException.addLast(logEntry);
        if (entriesWithException.size() > MAX_ENTRIES_WITH_EXCEPTION) {
          entriesWithException.pollFirst().dropException();
        }
      }
      Iterator<LogEntry> oldestFirst = entries.iterator();
      while (entries.size() > maxEntries) {
        LogEntry oldest = oldestFirst.next();
        oldestFirst.remove();
        count(oldest, -1);
        removeFromTopic(oldest);
      }
    }

    FRLogger.notifyListeners(listeners, listener -> listener.logEntryAdded(logEntry));

    return logEntry;
  }

  /** Registers a listener notified when a new entry is added. */
  public void addLogEntryAddedListener(LogEntryAddedListener listener) {
    listeners.add(listener);
  }

  /** Unregisters a previously added entry listener. */
  public void removeLogEntryAddedListener(LogEntryAddedListener listener) {
    listeners.remove(listener);
  }

  /** Removes the globally oldest entry from its topic queue. The caller holds the lock. */
  private void removeFromTopic(LogEntry entry) {
    if (entry.topic == null) {
      return;
    }
    ArrayDeque<LogEntry> ofTopic = entriesByTopic.get(entry.topic);
    if (ofTopic == null) {
      return;
    }
    // The topic queue follows the global order, so the globally oldest entry is its head.
    if (ofTopic.peekFirst() == entry) {
      ofTopic.pollFirst();
    } else {
      ofTopic.remove(entry);
    }
    if (ofTopic.isEmpty()) {
      entriesByTopic.remove(entry.topic);
    }
  }

  /** Updates the warning and error counts. The caller holds the lock. */
  private void count(LogEntry entry, int delta) {
    if (entry.type == LogEntryType.Warning) {
      warningCount += delta;
    } else if (entry.type == LogEntryType.Error) {
      errorCount += delta;
    }
  }

  /** Listener notified when a new log entry is added. */
  public interface LogEntryAddedListener {

    /** Called when a new log entry is stored. */
    void logEntryAdded(LogEntry logEntry);
  }
}
