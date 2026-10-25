package app.freerouting.logger;

import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/**
 * Represents a single in-memory log entry.
 *
 * <p>The in-memory copy is bounded: the message is cut after {@link #MAX_MESSAGE_LENGTH}
 * characters, and the exception is kept as a snapshot with at most {@link #MAX_STACK_FRAMES} frames
 * per throwable and {@link #MAX_CAUSES} causes. The console and file logs receive the full message
 * and exception.
 */
public class LogEntry {

  /** Longest message kept in memory. */
  static final int MAX_MESSAGE_LENGTH = 2_000;

  /** Stack frames kept for each throwable of the snapshot. */
  static final int MAX_STACK_FRAMES = 32;

  /** Causes kept below the logged exception. */
  static final int MAX_CAUSES = 4;

  /** Timestamp of the log entry. */
  Instant timestamp;

  /** Type of the log entry. */
  LogEntryType type;

  /** Message of the log entry. */
  String message;

  /** Bounded snapshot of the exception of the log entry; dropped for older entries. */
  transient volatile Throwable exception;

  /** Topic of the log entry, usually the ID of the user, session, or job. */
  UUID topic;

  /**
   * Creates a log entry with the given type, message, optional exception, and topic.
   *
   * @param type the entry severity/type
   * @param message the log message text
   * @param exception optional associated exception
   * @param topic optional topic identifier
   */
  public LogEntry(LogEntryType type, String message, Throwable exception, UUID topic) {
    this.timestamp = Instant.now();
    this.type = type;
    this.message = truncate(message);
    this.exception = exception != null ? new ThrowableSnapshot(exception, 0) : null;
    this.topic = topic;
  }

  public LogEntryType getType() {
    return this.type;
  }

  public String getMessage() {
    return this.message;
  }

  public UUID getTopic() {
    return this.topic;
  }

  /** Returns the bounded snapshot of the logged exception, or {@code null}. */
  public Throwable getException() {
    return this.exception;
  }

  /** Forgets the exception snapshot; the store keeps it only for its newest error entries. */
  void dropException() {
    this.exception = null;
  }

  @Override
  public String toString() {
    return "%-7s".formatted(this.type.toString().toUpperCase(Locale.ROOT)) + " " + this.message;
  }

  static String truncate(String text) {
    if (text == null || text.length() <= MAX_MESSAGE_LENGTH) {
      return text;
    }
    return text.substring(0, MAX_MESSAGE_LENGTH)
        + " [... "
        + (text.length() - MAX_MESSAGE_LENGTH)
        + " characters omitted]";
  }

  /**
   * A copy of a logged exception with a bounded stack trace and cause chain. It prints like the
   * original but references none of its fields, so a kept entry cannot hold large objects alive.
   */
  private static final class ThrowableSnapshot extends Throwable {

    private static final long serialVersionUID = 1L;

    private final String description;

    private ThrowableSnapshot(Throwable original, int depth) {
      super(truncate(original.getMessage()), snapshotOfCause(original, depth), false, true);
      this.description = truncate(original.toString());
      StackTraceElement[] frames = original.getStackTrace();
      setStackTrace(
          frames.length > MAX_STACK_FRAMES ? Arrays.copyOf(frames, MAX_STACK_FRAMES) : frames);
    }

    private static Throwable snapshotOfCause(Throwable original, int depth) {
      Throwable cause = original.getCause();
      if (cause == null || cause == original || depth >= MAX_CAUSES) {
        return null;
      }
      return new ThrowableSnapshot(cause, depth + 1);
    }

    /** The stack trace is copied from the original instead. */
    @Override
    public synchronized Throwable fillInStackTrace() {
      return this;
    }

    @Override
    public String toString() {
      return description;
    }
  }
}
