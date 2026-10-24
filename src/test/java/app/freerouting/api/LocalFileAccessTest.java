package app.freerouting.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Client-chosen local paths: an allowlisted root, loopback-only default and a size cap. */
class LocalFileAccessTest {

  private static final String[] LOOPBACK = {"http://127.0.0.1:37864", "http://localhost:1"};
  private static final String[] NETWORK = {"http://0.0.0.0:37864"};

  @TempDir Path dir;

  @Test
  void withoutRootLocalPathsNeedLoopbackOnlyServer() throws IOException {
    Path file = Files.writeString(dir.resolve("board.dsn"), "(pcb)");

    assertArrayEquals(
        "(pcb)".getBytes(),
        LocalFileAccess.of(null, LOOPBACK, false, "api_server").read(file.toString()));
    IOException refused =
        assertThrows(
            IOException.class,
            () -> LocalFileAccess.of(null, NETWORK, false, "api_server").read(file.toString()));
    assertTrue(refused.getMessage().contains("api_server.local_file_root"));
    // The MCP stdio mode is local whatever its endpoints say.
    assertEquals(
        5, LocalFileAccess.of(null, NETWORK, true, "mcp_server").read(file.toString()).length);
  }

  @Test
  void rootAdmitsOnlyFilesBelowIt() throws IOException {
    Path root = Files.createDirectory(dir.resolve("root"));
    Path inside = Files.writeString(root.resolve("board.dsn"), "(pcb)");
    Path outside = Files.writeString(dir.resolve("secret.txt"), "secret");
    LocalFileAccess access = LocalFileAccess.of(root.toString(), NETWORK, false, "api_server");

    assertEquals(5, access.read(inside.toString()).length);
    assertThrows(IOException.class, () -> access.read(outside.toString()));
    assertThrows(IOException.class, () -> access.read(root.resolve("../secret.txt").toString()));
    // Outside the root, an absent file is refused like a present one: no existence oracle.
    IOException absent =
        assertThrows(IOException.class, () -> access.read(dir.resolve("absent").toString()));
    IOException present = assertThrows(IOException.class, () -> access.read(outside.toString()));
    assertEquals(
        absent.getMessage().replace("absent", "X"),
        present.getMessage().replace("secret.txt", "X"));

    assertEquals(root.toRealPath().resolve("out.ses"), access.resolveForWrite(root + "/out.ses"));
    assertThrows(IOException.class, () -> access.resolveForWrite(dir + "/out.ses"));
  }

  @Test
  void symbolicLinkCannotLeaveTheRoot() throws IOException {
    Path root = Files.createDirectory(dir.resolve("root"));
    Path outside = Files.writeString(dir.resolve("secret.txt"), "secret");
    Path link;
    try {
      link = Files.createSymbolicLink(root.resolve("link.dsn"), outside);
    } catch (UnsupportedOperationException | IOException e) {
      return; // No symbolic links on this file system.
    }
    LocalFileAccess access = LocalFileAccess.of(root.toString(), LOOPBACK, false, "api_server");

    assertThrows(IOException.class, () -> access.read(link.toString()));
  }

  @Test
  void largeFilesAreRefused() throws IOException {
    Path big = dir.resolve("big.dsn");
    try (RandomAccessFile file = new RandomAccessFile(big.toFile(), "rw")) {
      file.setLength(LocalFileAccess.MAX_FILE_BYTES + 1);
    }
    LocalFileAccess access = LocalFileAccess.of(null, LOOPBACK, false, "api_server");

    IOException error = assertThrows(IOException.class, () -> access.read(big.toString()));
    assertTrue(error.getMessage().contains("larger than"));
  }

  @Test
  void loopbackDetection() {
    assertTrue(LocalFileAccess.listensOnLoopbackOnly(new String[] {"http://[::1]:1"}));
    assertFalse(LocalFileAccess.listensOnLoopbackOnly(new String[] {"http://0.0.0.0:1"}));
    assertFalse(
        LocalFileAccess.listensOnLoopbackOnly(
            new String[] {"http://127.0.0.1:1", "http://10.0.0.1:1"}));
    assertFalse(LocalFileAccess.listensOnLoopbackOnly(new String[0]));
  }
}
