package app.freerouting.settings;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Configuration for console and file logging, from {@code freerouting.json}, {@code
 * FREEROUTING__LOGGING__*} environment variables and {@code --logging.*} command-line arguments
 * like every other section. A value stays {@code null} unless one of them sets it; the startup
 * ({@code LoggingBootstrap}) applies the defaults: console on at {@code INFO}, file on at {@code
 * DEBUG} in the default log directory.
 */
public class LoggingSettings implements Serializable {

  /** Console logging configuration. */
  @SerializedName("console")
  public final ConsoleLoggingSettings console = new ConsoleLoggingSettings();

  /** File logging configuration. */
  @SerializedName("file")
  public final FileLoggingSettings file = new FileLoggingSettings();

  /** Forgets every value, so that the defaults apply. */
  void clear() {
    console.enabled = null;
    console.level = null;
    file.enabled = null;
    file.level = null;
    file.location = null;
    file.pattern = null;
  }

  /** Settings controlling console log output. */
  public static class ConsoleLoggingSettings implements Serializable {
    /** Whether console logging is enabled. */
    @SerializedName("enabled")
    public Boolean enabled;

    /** Minimum console log level. */
    @SerializedName("level")
    public String level;
  }

  /** Settings controlling file log output. */
  public static class FileLoggingSettings implements Serializable {
    /** Whether file logging is enabled. */
    @SerializedName("enabled")
    public Boolean enabled;

    /** Minimum file log level. */
    @SerializedName("level")
    public String level;

    /** The log file, or the directory of {@code freerouting.log}. */
    @SerializedName("location")
    public String location;

    /** Log4j pattern of the file log lines. */
    @SerializedName("pattern")
    public String pattern;
  }
}
