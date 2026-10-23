package app.freerouting.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AtomicFilesTest {

  @Test
  void replacesTheTargetAndLeavesNoTemporaryFile(@TempDir Path directory) throws Exception {
    Path target = directory.resolve("board.ses");
    Files.writeString(target, "(session old)");

    AtomicFiles.write(target, "(session new)".getBytes(StandardCharsets.UTF_8));

    assertEquals("(session new)", Files.readString(target));
    try (Stream<Path> files = Files.list(directory)) {
      assertEquals(1, files.count());
    }
  }

  @Test
  void createsMissingDirectories(@TempDir Path directory) throws Exception {
    Path target = directory.resolve("out/nested/board.drc.json");
    byte[] bytes = {1, 2, 3};
    AtomicFiles.write(target, bytes);
    assertArrayEquals(bytes, Files.readAllBytes(target));
  }
}
