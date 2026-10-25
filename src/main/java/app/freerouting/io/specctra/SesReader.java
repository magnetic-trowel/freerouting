package app.freerouting.io.specctra;

import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.core.library.Padstack;
import app.freerouting.geometry.planar.Point;
import app.freerouting.io.specctra.parser.IJFlexScanner;
import app.freerouting.io.specctra.parser.Keyword;
import app.freerouting.io.specctra.parser.LayerStructure;
import app.freerouting.io.specctra.parser.PolygonPath;
import app.freerouting.io.specctra.parser.ScopeKeyword;
import app.freerouting.io.specctra.parser.Shape;
import app.freerouting.io.specctra.parser.SpecctraDsnStreamReader;
import app.freerouting.io.specctra.parser.Wiring;
import app.freerouting.logger.FRLogger;
import app.freerouting.rules.Net;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a Specctra session (.ses) file and imports the routing data (wires and vias) into a {@link
 * BasicBoard}.
 *
 * <p>This class has no dependency on {@code BoardManager}, {@code RoutingJob}, or any GUI class.
 *
 * <p>This class is the public read entry point for Specctra session files.
 */
public final class SesReader {

  private final IJFlexScanner scanner;
  private final BasicBoard board;
  private final LayerStructure specctraLayerStructure;
  private final boolean reworkable;
  private double sessionToBoardScale;
  private int wiresImported;
  private int viasImported;
  private int errorsEncountered;

  /** Number of {@code (net ...)} scopes read so far per net name. */
  private final Map<String, Integer> netScopesRead = new HashMap<>();

  private SesReader(
      IJFlexScanner scanner, BasicBoard board, double scaleFactor, boolean reworkable) {
    this.scanner = scanner;
    this.board = board;
    this.specctraLayerStructure = new LayerStructure(board.layerStructure);
    this.sessionToBoardScale = scaleFactor;
    this.reworkable = reworkable;
  }

  /**
   * Reads a SES file and imports the routing data into {@code board}.
   *
   * <p>A session carries all routing, the design's own wiring included ({@link SesWriter} writes
   * every wire and via that is not system fixed). Its {@code network_out} scope therefore replaces
   * that copper on the board instead of adding a second copy of it.
   *
   * <p>The stream is always closed by this method on return (success or failure).
   *
   * @param in the SES input stream — closed by this method on completion
   * @param board the board to which wires and vias are added
   * @return a summary describing how many wires and vias were imported and how many errors were
   *     encountered; a non-zero {@link SesImportSummary#errorsEncountered()} value means some items
   *     were skipped but the board is otherwise intact
   * @throws IOException if {@code in} is {@code null} or {@code board} is {@code null}, or if the
   *     stream does not start with a valid Specctra session header, or if an I/O error occurs while
   *     reading the stream
   */
  public static SesImportSummary read(InputStream in, BasicBoard board) throws IOException {
    return read(in, board, false);
  }

  /**
   * Reads a SES file and imports the routing data into {@code board}, see {@link #read(InputStream,
   * BasicBoard)}.
   *
   * @param reworkable if false, every imported wire and via is protected ({@link
   *     FixedState#USER_FIXED}); if true, each gets the fixed state its {@code (type ...)} scope
   *     states, as the wiring of a DSN file does: none is routing the router may rip up and
   *     improve, {@code (type protect)} stays fixed
   */
  public static SesImportSummary read(InputStream in, BasicBoard board, boolean reworkable)
      throws IOException {
    if (in == null) {
      throw new IOException("SesReader.read: input stream must not be null");
    }
    if (board == null) {
      throw new IOException("SesReader.read: board must not be null");
    }

    IJFlexScanner scanner = new SpecctraDsnStreamReader(in);

    // SES files use the DSN-to-board scale factor: dsn_to_board(1) / resolution
    double scaleFactor =
        board.communication.coordinateTransform.dsnToBoard(1) / board.communication.resolution;

    SesReader reader = new SesReader(scanner, board, scaleFactor, reworkable);

    try {
      reader.processSessionScope();
      FRLogger.info(
          "SES file import complete: "
              + reader.wiresImported
              + " wires, "
              + reader.viasImported
              + " vias imported"
              + (reader.errorsEncountered > 0 ? " (" + reader.errorsEncountered + " errors)" : ""));
      return new SesImportSummary(
          reader.wiresImported, reader.viasImported, reader.errorsEncountered);
    } finally {
      closeQuietly(in);
    }
  }

  /**
   * Removes the copper that a Specctra session replaces: every wire and via with a net that is not
   * {@link FixedState#SYSTEM_FIXED}, which is exactly what {@link SesWriter} exports.
   *
   * @return the number of removed items
   */
  private static int removeSessionRouting(BasicBoard board) {
    java.util.List<app.freerouting.board.model.items.Item> sessionRouting =
        new java.util.ArrayList<>();
    for (app.freerouting.board.model.items.Item item : board.getItems()) {
      if ((item instanceof app.freerouting.board.trace.PolylineTrace
              || item instanceof app.freerouting.board.model.items.Via)
          && item.netCount() > 0
          && item.getFixedState() != FixedState.SYSTEM_FIXED) {
        sessionRouting.add(item);
      }
    }
    int removed = 0;
    for (app.freerouting.board.model.items.Item item : sessionRouting) {
      // DSN wiring loads as USER_FIXED, which removeItem refuses; the session carries its copy.
      item.unfix();
      if (!item.isDeletionForbidden()) {
        board.removeItem(item);
        removed++;
      }
    }
    return removed;
  }

  // ---------------------------------------------------------------------------
  // Private helpers for session-file parsing.
  // ---------------------------------------------------------------------------

  private static void closeQuietly(InputStream stream) {
    try {
      stream.close();
    } catch (IOException _) {
      // ignore — nothing useful to do here
    }
  }

  /** Transforms a Specctra session file into an Autodesk Fusion script file. */
  public static boolean saveSpecctraSessionSesAsFusionScriptScr(
      InputStream inputStream, OutputStream outputStream, BasicBoard board) {
    return app.freerouting.io.specctra.parser.SessionToFusion.getInstance(
        inputStream, outputStream, board);
  }

  /**
   * Processes the outermost scope of the session file.
   *
   * @throws IOException if the session header is malformed or an I/O error occurs
   */
  private void processSessionScope() throws IOException {
    // Validate the "(session <name>" header
    Object nextToken = null;
    for (int i = 0; i < 3; i++) {
      nextToken = this.scanner.nextToken();
      boolean keywordOk = true;
      if (i == 0) {
        keywordOk = nextToken == Keyword.OPEN_BRACKET;
      } else if (i == 1) {
        keywordOk = nextToken == Keyword.SESSION;
        this.scanner.yybegin(SpecctraDsnStreamReader.NAME); // consume the session name
      }
      if (!keywordOk) {
        throw new IOException(
            "SesReader: not a Specctra session file — expected '(session <name>' header, got: "
                + nextToken);
      }
    }

    // Read the direct subscopes of the session scope
    for (; ; ) {
      final Object prevToken = nextToken;
      nextToken = this.scanner.nextToken();
      if (nextToken == null) {
        // end of file
        return;
      }
      if (nextToken == Keyword.CLOSED_BRACKET) {
        // end of session scope
        break;
      }

      if (prevToken == Keyword.OPEN_BRACKET) {
        if (nextToken == Keyword.ROUTES) {
          processRoutesScope();
        } else {
          // skip placement, was_is, and any other scopes we don't need
          ScopeKeyword.skipScope(this.scanner);
        }
      }
    }
  }

  /** Processes the {@code (routes ...)} scope containing network data. */
  private void processRoutesScope() throws IOException {
    Object nextToken = null;
    for (; ; ) {
      final Object prevToken = nextToken;
      nextToken = this.scanner.nextToken();
      if (nextToken == null) {
        FRLogger.warn(
            "SesReader.processRoutesScope: unexpected end of file at '"
                + this.scanner.getScopeIdentifier()
                + "'");
        return;
      }
      if (nextToken == Keyword.CLOSED_BRACKET) {
        break;
      }

      if (prevToken == Keyword.OPEN_BRACKET) {
        if (nextToken == Keyword.NETWORK_OUT) {
          processNetworkScope();
        } else if (nextToken == Keyword.RESOLUTION_SCOPE) {
          readSessionResolution();
        } else {
          ScopeKeyword.skipScope(this.scanner);
        }
      }
    }
  }

  /**
   * Reads the resolution declaration in a session routes scope and calculates the session-to-board
   * coordinate scale factor.
   */
  private void readSessionResolution() throws IOException {
    Object unitToken = this.scanner.nextToken();
    Object resolutionToken = this.scanner.nextToken();
    Object close = this.scanner.nextToken();
    if (!(unitToken instanceof String name)
        || app.freerouting.board.model.structure.Unit.fromString(name) == null
        || !(resolutionToken instanceof Integer resolution)
        || resolution <= 0
        || close != Keyword.CLOSED_BRACKET) {
      throw new IOException(
          "SesReader: invalid routes resolution; expected (resolution <unit> <positive integer>)");
    }
    app.freerouting.board.model.structure.Unit sessionUnit =
        app.freerouting.board.model.structure.Unit.fromString(name);
    this.sessionToBoardScale = this.board.communication.getResolution(sessionUnit) / resolution;
  }

  /** Resolves the item's net class, falling back to the board default net class. */
  private app.freerouting.rules.NetClass netClassOf(int[] netNumbers) {
    Net net =
        netNumbers != null && netNumbers.length > 0
            ? this.board.rules.nets.get(netNumbers[0])
            : null;
    return net != null ? net.getNetClass() : this.board.rules.getDefaultNetClass();
  }

  /** Processes the {@code (network_out ...)} scope containing individual nets. */
  private void processNetworkScope() throws IOException {
    int replaced = removeSessionRouting(board);
    if (replaced > 0) {
      FRLogger.info("Removed " + replaced + " design wires/vias that the SES file replaces");
    }
    Object nextToken = null;
    for (; ; ) {
      final Object prevToken = nextToken;
      nextToken = this.scanner.nextToken();
      if (nextToken == null) {
        FRLogger.warn(
            "SesReader.processNetworkScope: unexpected end of file at '"
                + this.scanner.getScopeIdentifier()
                + "'");
        return;
      }
      if (nextToken == Keyword.CLOSED_BRACKET) {
        break;
      }

      if (prevToken == Keyword.OPEN_BRACKET) {
        if (nextToken == Keyword.NET) {
          processNetScope();
        } else {
          ScopeKeyword.skipScope(this.scanner);
        }
      }
    }
  }

  /**
   * Returns the net of the next {@code (net ...)} scope with this name. A net split into subnets
   * (DSN {@code fromto} or {@code order}) has one scope per subnet, in net number order (see {@link
   * SesWriter}); the n-th scope belongs to the n-th subnet.
   */
  private Net sessionNet(String netName) {
    List<Net> candidates = new ArrayList<>(board.rules.nets.get(netName));
    if (candidates.isEmpty()) {
      return null;
    }
    int scopeIndex = netScopesRead.merge(netName, 1, Integer::sum) - 1;
    return candidates.get(scopeIndex < candidates.size() ? scopeIndex : 0);
  }

  /** Processes a single {@code (net ...)} scope containing wires and vias. */
  private void processNetScope() throws IOException {
    Object nextToken = this.scanner.nextToken();
    if (!(nextToken instanceof String netName)) {
      FRLogger.warn(
          "SesReader.processNetScope: String expected at '"
              + this.scanner.getScopeIdentifier()
              + "'");
      errorsEncountered++;
      return;
    }
    this.scanner.setScopeIdentifier(netName);

    Net net = sessionNet(netName);
    if (net == null) {
      FRLogger.warn("SesReader: net not found: '" + netName + "' — skipping");
      errorsEncountered++;
      ScopeKeyword.skipScope(this.scanner);
      return;
    }
    int netNumber = net.netNumber;
    int[] netNumbers = new int[] {netNumber};

    // The wires of a net are restored together: a wire's end is often where another one meets it.
    List<Object> entries = new ArrayList<>();
    for (; ; ) {
      final Object prevToken = nextToken;
      nextToken = this.scanner.nextToken();
      if (nextToken == null || nextToken == Keyword.CLOSED_BRACKET) {
        break;
      }

      if (prevToken == Keyword.OPEN_BRACKET) {
        if (nextToken == Keyword.WIRE) {
          WireEntry wire = readWireScope();
          if (wire != null) {
            entries.add(wire);
          }
        } else if (nextToken == Keyword.VIA) {
          ViaEntry via = readViaScope();
          if (via != null) {
            entries.add(via);
          } else {
            errorsEncountered++;
          }
        } else {
          ScopeKeyword.skipScope(this.scanner);
        }
      }
    }
    importEntries(entries, netNumbers);
  }

  /** Inserts the wires and vias of a net in the order of the file. */
  private void importEntries(List<Object> entries, int[] netNumbers) {
    List<double[]> wireCoordinates = new ArrayList<>();
    for (Object entry : entries) {
      if (entry instanceof WireEntry wire) {
        wireCoordinates.add(wire.path().coordinateArr);
      }
    }
    List<SesPathGeometry.Restored> restored =
        SesPathGeometry.read(wireCoordinates, sessionToBoardScale);
    int wireIndex = 0;
    for (Object entry : entries) {
      boolean imported =
          entry instanceof WireEntry wire
              ? insertWire(wire, restored.get(wireIndex++), netNumbers)
              : insertVia((ViaEntry) entry, netNumbers);
      if (!imported) {
        errorsEncountered++;
      }
    }
  }

  // ---------------------------------------------------------------------------

  /**
   * Returns the fixed state of an imported wire or via whose {@code (type ...)} scope states the
   * given one ({@link FixedState#UNFIXED} without that scope).
   */
  private FixedState importedFixedState(FixedState written) {
    return this.reworkable ? written : FixedState.USER_FIXED;
  }

  /** A wire as read from its scope; the fixed state is the one its file states. */
  private record WireEntry(PolygonPath path, FixedState fixedState) {}

  /**
   * Reads a {@code (wire ...)} scope.
   *
   * @return the wire, or null if it has no path (conduction areas) or the file ends inside it,
   *     which counts as an error
   */
  private WireEntry readWireScope() throws IOException {
    PolygonPath wirePath = null;
    FixedState fixedState = FixedState.UNFIXED;
    Object nextToken = null;
    for (; ; ) {
      final Object prevToken = nextToken;
      nextToken = this.scanner.nextToken();
      if (nextToken == null) {
        FRLogger.warn(
            "SesReader.readWireScope: unexpected end of file at '"
                + this.scanner.getScopeIdentifier()
                + "'");
        errorsEncountered++;
        return null;
      }
      if (nextToken == Keyword.CLOSED_BRACKET) {
        return wirePath == null ? null : new WireEntry(wirePath, fixedState);
      }
      if (prevToken == Keyword.OPEN_BRACKET) {
        if (nextToken == Keyword.POLYGON_PATH) {
          wirePath = Shape.readPolygonPathScope(this.scanner, this.specctraLayerStructure, 1);
        } else if (nextToken == Keyword.TYPE) {
          fixedState = Wiring.calcFixed(this.scanner);
        } else {
          ScopeKeyword.skipScope(this.scanner);
        }
      }
    }
  }

  /**
   * Inserts a wire whose geometry {@link SesPathGeometry} restored.
   *
   * @return {@code true} if the wire was imported
   */
  private boolean insertWire(WireEntry wire, SesPathGeometry.Restored restored, int[] netNumbers) {
    PolygonPath wirePath = wire.path();
    try {
      int layerIndex = wirePath.layer.no;
      if (layerIndex < 0 || layerIndex >= board.getLayerCount()) {
        throw new IOException("Invalid SES wire layer");
      }
      if (restored.failure() != null) {
        throw restored.failure();
      }
      int halfWidth = SesPathGeometry.halfWidth(wirePath.width, sessionToBoardScale);

      int clearanceClass =
          netClassOf(netNumbers)
              .defaultItemClearanceClasses
              .get(app.freerouting.rules.DefaultItemClearanceClasses.ItemClass.TRACE);

      // Import the copper exactly; normalization must not erase point copper or short segments.
      var trace =
          board.insertTraceWithoutCleaning(
              restored.polyline(),
              layerIndex,
              halfWidth,
              netNumbers,
              clearanceClass,
              importedFixedState(wire.fixedState()));
      if (trace == null) {
        return false;
      }
      wiresImported++;
      return true;

    } catch (Exception e) {
      FRLogger.warn("SesReader.insertWire: failed to import wire — " + e.getMessage());
      return false;
    }
  }

  /** A via as read from its scope; the fixed state is the one its file states. */
  private record ViaEntry(String padstackName, double[] location, FixedState fixedState) {}

  /**
   * Reads a {@code (via ...)} scope.
   *
   * @return the via, or null on a parse error
   */
  private ViaEntry readViaScope() throws IOException {
    Object nextToken = this.scanner.nextToken();
    if (!(nextToken instanceof String padstackName)) {
      FRLogger.warn(
          "SesReader.readViaScope: padstack name expected at '"
              + this.scanner.getScopeIdentifier()
              + "'");
      return null;
    }
    this.scanner.setScopeIdentifier(padstackName);

    double[] location = new double[2];
    for (int i = 0; i < 2; i++) {
      nextToken = this.scanner.nextToken();
      if (nextToken instanceof Double d) {
        location[i] = d;
      } else if (nextToken instanceof Integer integer) {
        location[i] = integer;
      } else {
        FRLogger.warn(
            "SesReader.readViaScope: number expected at '"
                + this.scanner.getScopeIdentifier()
                + "'");
        return null;
      }
    }

    FixedState fixedState = FixedState.UNFIXED;
    nextToken = this.scanner.nextToken();
    while (nextToken == Keyword.OPEN_BRACKET) {
      if (this.scanner.nextToken() == Keyword.TYPE) {
        fixedState = Wiring.calcFixed(this.scanner);
      } else {
        ScopeKeyword.skipScope(this.scanner);
      }
      nextToken = this.scanner.nextToken();
    }

    if (nextToken != Keyword.CLOSED_BRACKET) {
      FRLogger.warn(
          "SesReader.readViaScope: closing bracket expected at '"
              + this.scanner.getScopeIdentifier()
              + "'");
      return null;
    }
    return new ViaEntry(padstackName, location, fixedState);
  }

  /**
   * Inserts a via into the board.
   *
   * @return {@code true} if the via was imported
   */
  private boolean insertVia(ViaEntry via, int[] netNumbers) {
    String padstackName = via.padstackName();
    double[] location = via.location();
    try {
      Padstack viaPadstack = this.board.library.padstacks.get(padstackName);
      if (viaPadstack == null && padstackName != null) {
        viaPadstack = this.board.library.padstacks.get(padstackName.replaceAll("\\.\\d+", ""));
      }
      // Older writers (v2.5.0) strip the decimals from padstack names (via-0.5:... -> via-0:...).
      for (int id = 1; viaPadstack == null && id <= this.board.library.padstacks.count(); id++) {
        Padstack candidate = this.board.library.padstacks.get(id);
        if (candidate != null
            && candidate.name != null
            && candidate.name.replaceAll("\\.\\d+", "").equals(padstackName)) {
          viaPadstack = candidate;
        }
      }
      if (viaPadstack == null) {
        FRLogger.warn("SesReader.insertVia: via padstack not found: " + padstackName);
        return false;
      }

      int x = (int) Math.round(location[0] * sessionToBoardScale);
      int y = (int) Math.round(location[1] * sessionToBoardScale);
      Point viaLocation = Point.getInstance(x, y);

      int clearanceClass =
          netClassOf(netNumbers)
              .defaultItemClearanceClasses
              .get(app.freerouting.rules.DefaultItemClearanceClasses.ItemClass.VIA);

      // Mirror the design rules: a via may sit on an SMD pad only where its rule allows it.
      boolean attachAllowed = viaPadstack.attachAllowed;
      boolean ruleFound = false;
      boolean ruleAllowsAttach = false;
      for (int i = 0; i < this.board.rules.viaInfos.count(); i++) {
        app.freerouting.rules.ViaInfo viaInfo = this.board.rules.viaInfos.get(i);
        if (viaInfo.getPadstack() == viaPadstack) {
          ruleFound = true;
          ruleAllowsAttach |= viaInfo.attachSmdAllowed();
        }
      }
      if (ruleFound) {
        attachAllowed &= ruleAllowsAttach;
      }

      board.insertVia(
          viaPadstack,
          viaLocation,
          netNumbers,
          clearanceClass,
          importedFixedState(via.fixedState()),
          attachAllowed);

      viasImported++;
      return true;

    } catch (Exception e) {
      FRLogger.warn("SesReader.insertVia: failed to import via — " + e.getMessage());
      return false;
    }
  }
}
