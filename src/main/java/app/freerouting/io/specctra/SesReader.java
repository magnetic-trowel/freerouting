package app.freerouting.io.specctra;

import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.core.library.Padstack;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Point;
import app.freerouting.geometry.planar.Polyline;
import app.freerouting.geometry.planar.RationalPoint;
import app.freerouting.io.specctra.parser.IJFlexScanner;
import app.freerouting.io.specctra.parser.Keyword;
import app.freerouting.io.specctra.parser.LayerStructure;
import app.freerouting.io.specctra.parser.ScopeKeyword;
import app.freerouting.io.specctra.parser.SpecctraDsnStreamReader;
import app.freerouting.io.specctra.parser.Wiring;
import app.freerouting.logger.FRLogger;
import app.freerouting.rules.Net;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.numbers.fraction.BigFraction;

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
  private BigFraction sessionToBoardScale;
  private int wiresImported;
  private int viasImported;
  private int errorsEncountered;
  private final List<SessionWire> wires = new ArrayList<>();

  private record SessionWire(
      BigDecimal[] coordinates,
      BigFraction scale,
      double[] board,
      int layer,
      int halfWidth,
      int[] nets,
      int clearanceClass,
      FixedState fixed) {}

  private record LayerPoint(int layer, IntPoint at) {}

  /** Number of {@code (net ...)} scopes read so far per net name. */
  private final Map<String, Integer> netScopesRead = new HashMap<>();

  private SesReader(
      IJFlexScanner scanner, BasicBoard board, BigFraction scaleFactor, boolean reworkable) {
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

    SpecctraDsnStreamReader scanner = new SpecctraDsnStreamReader(in, true);

    // SES files use the DSN-to-board scale factor: dsn_to_board(1) / resolution
    BigFraction scaleFactor =
        SesNumbers.sessionScale(board, board.communication.unit, board.communication.resolution);

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
    try {
      readRoutesScope();
    } finally {
      insertWires();
    }
  }

  private void readRoutesScope() throws IOException {
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
    int resolution;
    try {
      resolution = exactNumber(resolutionToken).intValueExact();
    } catch (ArithmeticException invalid) {
      throw new IOException("SesReader: session resolution must be a positive integer", invalid);
    }
    if (!(unitToken instanceof String name)
        || app.freerouting.board.model.structure.Unit.fromString(name) == null
        || resolution <= 0
        || close != Keyword.CLOSED_BRACKET) {
      throw new IOException(
          "SesReader: invalid routes resolution; expected (resolution <unit> <positive integer>)");
    }
    app.freerouting.board.model.structure.Unit sessionUnit =
        app.freerouting.board.model.structure.Unit.fromString(name);
    this.sessionToBoardScale = SesNumbers.sessionScale(board, sessionUnit, resolution);
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

    for (; ; ) {
      final Object prevToken = nextToken;
      nextToken = this.scanner.nextToken();
      if (nextToken == null) {
        return;
      }
      if (nextToken == Keyword.CLOSED_BRACKET) {
        break;
      }

      if (prevToken == Keyword.OPEN_BRACKET) {
        if (nextToken == Keyword.WIRE) {
          if (!processWireScope(netNumbers)) {
            errorsEncountered++;
          }
        } else if (nextToken == Keyword.VIA) {
          ViaEntry via = readViaScope();
          if (via == null || !insertVia(via, netNumbers)) {
            errorsEncountered++;
          }
        } else {
          ScopeKeyword.skipScope(this.scanner);
        }
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

  /**
   * Processes a {@code (wire ...)} scope and queues the wire; {@link #insertWires} puts the
   * session's wires on the board together.
   *
   * @return {@code true} if the wire was queued or has no path (conduction areas); {@code false} on
   *     a parse or geometry error (the caller increments {@link #errorsEncountered})
   */
  private boolean processWireScope(int[] netNumbers) throws IOException {
    SessionPath wirePath = null;
    boolean sawPath = false;
    FixedState fixedState = FixedState.UNFIXED;
    Object nextToken = null;
    for (; ; ) {
      final Object prevToken = nextToken;
      nextToken = this.scanner.nextToken();
      if (nextToken == null) {
        FRLogger.warn(
            "SesReader.processWireScope: unexpected end of file at '"
                + this.scanner.getScopeIdentifier()
                + "'");
        return false;
      }
      if (nextToken == Keyword.CLOSED_BRACKET) {
        break;
      }
      if (prevToken == Keyword.OPEN_BRACKET) {
        if (nextToken == Keyword.POLYGON_PATH) {
          sawPath = true;
          wirePath = readExactPath();
        } else if (nextToken == Keyword.TYPE) {
          fixedState = Wiring.calcFixed(this.scanner);
        } else {
          ScopeKeyword.skipScope(this.scanner);
        }
      }
    }

    if (wirePath == null) {
      // conduction areas have no polygon_path — silently skip
      return !sawPath;
    }

    try {
      int layerIndex = wirePath.layer();
      if (layerIndex < 0 || layerIndex >= board.getLayerCount()) {
        throw new IOException("Invalid SES wire layer");
      }
      BigDecimal[] coordinates = wirePath.coordinates();
      BigFraction scale = sessionToBoardScale;
      double[] boardCoordinates = new double[coordinates.length];
      for (int i = 0; i < coordinates.length; i++) {
        boardCoordinates[i] = coordinates[i].doubleValue() * scale.doubleValue();
      }
      SesPathGeometry.check(boardCoordinates);
      int halfWidth = SesPathGeometry.halfWidth(wirePath.width(), scale);

      int clearanceClass =
          netClassOf(netNumbers)
              .defaultItemClearanceClasses
              .get(app.freerouting.rules.DefaultItemClearanceClasses.ItemClass.TRACE);

      wires.add(
          new SessionWire(
              coordinates,
              scale,
              boardCoordinates,
              layerIndex,
              halfWidth,
              netNumbers,
              clearanceClass,
              importedFixedState(fixedState)));
      return true;

    } catch (Exception e) {
      FRLogger.warn(
          "SesReader.processWireScope: failed to import wire on net #"
              + netNumbers[0]
              + " at "
              + wirePath.coordinates()[0]
              + ","
              + wirePath.coordinates()[1]
              + " — "
              + e.getMessage());
      return false;
    }
  }

  private record SessionPath(int layer, BigDecimal width, BigDecimal[] coordinates) {}

  /** A via as read from its scope; the fixed state is the one its file states. */
  private record ViaEntry(String padstackName, BigDecimal[] location, FixedState fixedState) {}

  private static BigDecimal exactNumber(Object token) throws IOException {
    if (token instanceof BigDecimal decimal) {
      return decimal;
    }
    throw new IOException("A decimal SES number was expected");
  }

  private SessionPath readExactPath() throws IOException {
    String layer = scanner.nextString();
    List<BigDecimal> numbers = new ArrayList<>();
    boolean invalid = false;
    for (; ; ) {
      Object token = scanner.nextToken();
      if (token == null) {
        throw new IOException("Unexpected end of SES path");
      }
      if (token == Keyword.CLOSED_BRACKET) {
        break;
      }
      if (token == Keyword.OPEN_BRACKET) {
        ScopeKeyword.skipScope(scanner);
        continue;
      }
      try {
        numbers.add(exactNumber(token));
      } catch (IOException invalidNumber) {
        invalid = true;
      }
    }
    if (invalid || numbers.size() < 3 || (numbers.size() - 1) % 2 != 0) {
      FRLogger.warn("SesReader: invalid wire path on layer " + layer);
      return null;
    }
    BigDecimal[] coordinates = numbers.subList(1, numbers.size()).toArray(BigDecimal[]::new);
    if (coordinates.length == 2) {
      // A single vertex represents point copper; keep two equal endpoints in the trace model.
      coordinates =
          new BigDecimal[] {coordinates[0], coordinates[1], coordinates[0], coordinates[1]};
    }
    return new SessionPath(specctraLayerStructure.getNo(layer), numbers.getFirst(), coordinates);
  }

  /**
   * Puts the session's wires on the board together, after its vias, so that a corner several wires
   * share reloads the same way in each. A wire that restores exactly is read on the grid too when
   * one of its rational corners rounds to a grid corner of a wire on its layer read on the grid.
   */
  private void insertWires() {
    int count = wires.size();
    Polyline[] exact = new Polyline[count];
    IOException[] failed = new IOException[count];
    boolean[] onGrid = new boolean[count];
    Set<LayerPoint> gridCorners = new HashSet<>();
    for (int i = 0; i < count; i++) {
      try {
        exact[i] = SesPathGeometry.exact(wires.get(i).coordinates(), wires.get(i).scale());
      } catch (IOException failure) {
        failed[i] = failure;
      }
    }
    for (int i = 0; i < count; i++) {
      if (exact[i] == null && failed[i] == null) {
        onGrid[i] = true;
        addGridCorners(wires.get(i), gridCorners);
      }
    }
    for (boolean again = !gridCorners.isEmpty(); again; ) {
      again = false;
      for (int i = 0; i < count; i++) {
        if (!onGrid[i]
            && exact[i] != null
            && rationalCornerOnGrid(exact[i], wires.get(i).layer, gridCorners)) {
          onGrid[i] = true;
          addGridCorners(wires.get(i), gridCorners);
          again = true;
        }
      }
    }
    for (int i = 0; i < count; i++) {
      SessionWire wire = wires.get(i);
      try {
        if (failed[i] != null) {
          throw failed[i];
        }
        Polyline polyline = onGrid[i] ? SesPathGeometry.grid(wire.board, null) : exact[i];
        // Import the copper exactly; normalization must not erase point copper or short segments.
        if (board.insertTraceWithoutCleaning(
                polyline, wire.layer, wire.halfWidth, wire.nets, wire.clearanceClass, wire.fixed)
            == null) {
          FRLogger.warn("SesReader.processWireScope: the wire makes no trace on the board");
          errorsEncountered++;
          continue;
        }
        wiresImported++;
      } catch (Exception e) {
        FRLogger.warn("SesReader.processWireScope: failed to import wire — " + e.getMessage());
        errorsEncountered++;
      }
    }
    wires.clear();
  }

  /**
   * Restores the integer lines of every wire that has no exact reading, with the other wires of its
   * net as partners: a wire usually ends where another wire of its net meets it. A wire restored
   * this way gets its polyline in {@code exact}; the others are left to the grid reading.
   */
  private void addGridCorners(SessionWire wire, Set<LayerPoint> gridCorners) {
    for (int i = 0; i + 1 < wire.board.length; i += 2) {
      gridCorners.add(
          new LayerPoint(
              wire.layer, SesPathGeometry.gridPoint(wire.board[i], wire.board[i + 1], null)));
    }
  }

  private static boolean rationalCornerOnGrid(
      Polyline polyline, int layer, Set<LayerPoint> gridCorners) {
    for (int i = 0; i < polyline.cornerCount(); i++) {
      if (polyline.corner(i) instanceof RationalPoint corner
          && gridCorners.contains(new LayerPoint(layer, corner.toFloat().round()))) {
        return true;
      }
    }
    return false;
  }

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

    BigDecimal[] location = new BigDecimal[2];
    for (int i = 0; i < 2; i++) {
      nextToken = this.scanner.nextToken();
      try {
        location[i] = exactNumber(nextToken);
      } catch (IOException invalidNumber) {
        FRLogger.warn("SesReader.readViaScope: " + invalidNumber.getMessage());
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
    BigDecimal[] location = via.location();
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

      BigFraction x = SesNumbers.recoverInteger(location[0], sessionToBoardScale);
      BigFraction y = SesNumbers.recoverInteger(location[1], sessionToBoardScale);
      if (!x.getDenominator().equals(java.math.BigInteger.ONE)
          || !y.getDenominator().equals(java.math.BigInteger.ONE)) {
        throw new IOException("The SES via center is not an integer board position");
      }
      Point viaLocation =
          Point.getInstance(x.getNumerator().intValueExact(), y.getNumerator().intValueExact())
              .toIntegerExact();

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
