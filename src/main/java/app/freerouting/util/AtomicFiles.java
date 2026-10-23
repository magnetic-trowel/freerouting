package app.freerouting.util;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Writes files so that a reader never sees a partly written one: the bytes go to a temporary file
 * beside the target, which then replaces the target in one move. A process killed while writing
 * leaves the previous file (or none) and a stray temporary file, never a truncated session file.
 */
public final class AtomicFiles {

  private AtomicFiles() {}

  /** Writes {@code bytes} to {@code target}, replacing it. */
  public static void write(Path target, byte[] bytes) throws IOException {
    Path absolute = target.toAbsolutePath();
    Path directory = absolute.getParent();
    if (directory != null) {
      Files.createDirectories(directory);
    }
    Path temporary =
        Files.createTempFile(directory, "." + absolute.getFileName().toString() + ".", ".tmp");
    try {
      Files.write(temporary, bytes);
      try {
        Files.move(
            temporary,
            absolute,
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }
}
