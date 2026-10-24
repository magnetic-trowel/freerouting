package app.freerouting.api;

import app.freerouting.Freerouting;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Decides which local files a client may name by path: the {@code file_path}, {@code rules_path}
 * and {@code session_path} fields of {@code POST /v1/autoroute} and the MCP local-file tools.
 *
 * <p>With a configured root ({@code api_server.local_file_root} or {@code
 * mcp_server.local_file_root}) only files under that directory are allowed; the path is resolved
 * with {@link Path#toRealPath} so that neither {@code ..} nor a symbolic link leads out of it.
 * Without a root, local paths are allowed only while the server listens on loopback addresses only,
 * where the client runs on the same machine as the server and could read the files itself; a server
 * reachable from the network refuses them. A file larger than {@link #MAX_FILE_BYTES} is refused.
 */
public final class LocalFileAccess {

  /** Largest file read for a client: the board size the request-body limit admits. */
  public static final long MAX_FILE_BYTES = 100L * 1024 * 1024;

  // The root as configured and as resolved: a client may name it either way.
  private final Path root;
  private final Path realRoot;
  private final String refusal;

  private LocalFileAccess(Path root, Path realRoot, String refusal) {
    this.root = root;
    this.realRoot = realRoot;
    this.refusal = refusal;
  }

  /** The policy of the REST API server. */
  public static LocalFileAccess forApiServer() {
    var settings =
        Freerouting.globalSettings != null ? Freerouting.globalSettings.apiServerSettings : null;
    return settings == null
        ? of(null, new String[0], false, "api_server")
        : of(settings.localFileRoot, settings.endpoints, false, "api_server");
  }

  /** The policy of the MCP server. */
  public static LocalFileAccess forMcpServer() {
    var settings =
        Freerouting.globalSettings != null ? Freerouting.globalSettings.mcpServerSettings : null;
    return settings == null
        ? of(null, new String[0], false, "mcp_server")
        : of(
            settings.localFileRoot,
            settings.endpoints,
            Boolean.TRUE.equals(settings.isStdioMode),
            "mcp_server");
  }

  static LocalFileAccess of(
      String configuredRoot, String[] endpoints, boolean localOnly, String settingsPrefix) {
    if (configuredRoot != null && !configuredRoot.isBlank()) {
      try {
        Path root = Path.of(configuredRoot).toAbsolutePath().normalize();
        return new LocalFileAccess(root, root.toRealPath(), null);
      } catch (IOException | RuntimeException e) {
        return new LocalFileAccess(
            null,
            null,
            settingsPrefix + ".local_file_root '" + configuredRoot + "' is not a directory.");
      }
    }
    if (localOnly || listensOnLoopbackOnly(endpoints)) {
      return new LocalFileAccess(null, null, null);
    }
    return new LocalFileAccess(
        null,
        null,
        "Local file paths are disabled because the server is reachable from the network. Send"
            + " the file content instead, or set "
            + settingsPrefix
            + ".local_file_root to a directory whose files clients may use.");
  }

  static boolean listensOnLoopbackOnly(String[] endpoints) {
    if (endpoints == null || endpoints.length == 0) {
      return false;
    }
    for (String endpoint : endpoints) {
      try {
        String host = URI.create(endpoint.trim()).getHost();
        if (host == null
            || !("localhost".equalsIgnoreCase(host)
                || InetAddress.getByName(host).isLoopbackAddress())) {
          return false;
        }
      } catch (IOException | RuntimeException e) {
        return false;
      }
    }
    return true;
  }

  /**
   * Reads a file the client named.
   *
   * @throws IOException if the file is not allowed, does not exist, is not a regular file or is
   *     larger than {@link #MAX_FILE_BYTES}
   */
  public byte[] read(String clientPath) throws IOException {
    Path path = allowed(clientPath).toRealPath();
    requireInsideRealRoot(path, clientPath);
    if (!Files.isRegularFile(path)) {
      throw new IOException("Not a regular file: " + clientPath);
    }
    if (Files.size(path) > MAX_FILE_BYTES) {
      throw new IOException(
          "The file " + clientPath + " is larger than " + MAX_FILE_BYTES + " bytes.");
    }
    return Files.readAllBytes(path);
  }

  /**
   * Returns where to write a file the client named. Its directory must exist; the file itself is
   * replaced.
   *
   * @throws IOException if the path is not allowed or its directory does not exist
   */
  public Path resolveForWrite(String clientPath) throws IOException {
    Path path = allowed(clientPath);
    Path directory = path.getParent();
    if (directory == null || path.getFileName() == null) {
      throw new IOException("Not a file path: " + clientPath);
    }
    Path target = directory.toRealPath().resolve(path.getFileName());
    requireInsideRealRoot(target, clientPath);
    return target;
  }

  /**
   * Checks the policy and the path as written, before touching the file system: an answer for a
   * path outside the root does not tell whether that file exists.
   */
  private Path allowed(String clientPath) throws IOException {
    if (refusal != null) {
      throw new IOException(refusal);
    }
    if (clientPath == null || clientPath.isBlank()) {
      throw new IOException("The file path is empty.");
    }
    Path path;
    try {
      path = Path.of(clientPath).toAbsolutePath().normalize();
    } catch (RuntimeException e) {
      throw new IOException("Invalid file path: " + clientPath);
    }
    if (root != null && !path.startsWith(root) && !path.startsWith(realRoot)) {
      throw outside(clientPath);
    }
    return path;
  }

  /** Checks the resolved path, after {@code ..} and symbolic links. */
  private void requireInsideRealRoot(Path realPath, String clientPath) throws IOException {
    if (realRoot != null && !realPath.startsWith(realRoot)) {
      throw outside(clientPath);
    }
  }

  private static IOException outside(String clientPath) {
    return new IOException("The file " + clientPath + " is outside the allowed directory.");
  }
}
