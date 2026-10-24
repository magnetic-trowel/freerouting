package app.freerouting.io.specctra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.structure.Unit;
import app.freerouting.io.BoardReadResult;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * Malformed DSN input found by fuzzing the reader. Each input must end in a typed {@link
 * BoardReadResult} (a usable board or a failure), never in an exception, a hang or runaway memory.
 * The comments name what the input caused before the fix.
 */
class DsnReaderMalformedInputTest {

  /** Start of a one-layer board; the structure scope is still open. */
  private static final String HEADER =
      """
      (pcb t
        (structure
          (layer F.Cu (type signal))
      """;

  private static final String BOUNDARY = "    (boundary (rect pcb 0 0 1000 1000))\n";

  /** A complete one-layer structure scope followed by an empty library scope. */
  private static final String STRUCTURE = HEADER + BOUNDARY + "  )\n  (library)\n";

  private static BoardReadResult read(String dsn) {
    return assertTimeoutPreemptively(
        Duration.ofSeconds(10),
        () ->
            DsnReader.readBoard(
                new ByteArrayInputStream(dsn.getBytes(StandardCharsets.UTF_8)), null, null));
  }

  private static RoutingBoard readBoard(String dsn) {
    BoardReadResult result = read(dsn);
    assertInstanceOf(BoardReadResult.Success.class, result);
    RoutingBoard board = (RoutingBoard) ((BoardReadResult.Success) result).board();
    assertNotNull(board);
    return board;
  }

  private static BoardReadResult.ParseError readParseError(String dsn) {
    BoardReadResult result = read(dsn);
    assertInstanceOf(BoardReadResult.ParseError.class, result);
    return (BoardReadResult.ParseError) result;
  }

  // ---------------------------------------------------------------------------------------------
  // Numbers
  // ---------------------------------------------------------------------------------------------

  @Test
  void integerBeyondIntRangeIsReadAsNumber() {
    // NumberFormatException from Integer.valueOf in the scanner
    RoutingBoard board =
        readBoard(HEADER + "    (boundary (path pcb 0 0 0 2147483648 0 1000 -1000 0 0))\n  )\n)\n");
    assertEquals(1, board.getLayerCount());
  }

  @Test
  void coordinateOverflowingTheBoardScaleIsParseError() {
    // 1e308 times the resolution is infinite: the scale reduction loop never ended
    BoardReadResult.ParseError error =
        readParseError(
            """
            (pcb t
              (resolution um 10)
              (structure
                (layer F.Cu (type signal))
                (boundary (path pcb 0 0 0 1e308 0 1000 -1000 0 0))
              )
            )
            """);
    assertTrue(error.detail().contains("out of range"), error.detail());
  }

  @Test
  void infiniteCoordinateIsNoNumber() {
    // 1e999 was scanned as Double.POSITIVE_INFINITY, which hung the board creation
    BoardReadResult result =
        read(HEADER + "    (boundary (path pcb 0 0 0 1e999 0 1000 -1000 0 0))\n  )\n)\n");
    assertInstanceOf(BoardReadResult.OutlineMissing.class, result);
  }

  @Test
  void nonPositiveResolutionIsParseError() {
    // was accepted, so that board units and the declared resolution disagreed
    BoardReadResult.ParseError error =
        readParseError(
            "(pcb t\n  (resolution mm 0)\n  (structure\n    (layer F.Cu (type signal))\n"
                + BOUNDARY
                + "  )\n)\n");
    assertTrue(error.detail().contains("resolution"), error.detail());
  }

  @Test
  void metadataReadKeepsTheUnitOfAnUnknownResolutionUnit() {
    // NullPointerException in Communication.physicalToBoard: readMetadata reads on after the
    // rejected resolution scope, which had already stored the unknown unit as null
    String dsn =
        """
        (pcb t
          (resolution foo 10
          (structure
            (layer F.Cu (type signal))
            (boundary (path pcb 0 0 0 1000 0 1000 -1000 0 -1000 0 0))
          )
        )
        """;
    BoardReadResult result =
        assertTimeoutPreemptively(
            Duration.ofSeconds(10),
            () ->
                DsnReader.readMetadata(
                    new ByteArrayInputStream(dsn.getBytes(StandardCharsets.UTF_8))));
    assertInstanceOf(BoardReadResult.Success.class, result);
    assertEquals(Unit.MIL, ((BoardReadResult.Success) result).metadata().unit());
    readParseError(dsn);
  }

  @Test
  void unparsableRotationDefaultsToZero() {
    // NumberFormatException from Double.parseDouble in Package.readRotation
    RoutingBoard board =
        readBoard(
            HEADER
                + BOUNDARY
                + """
                  )
                  (library
                    (image I (pin P (rotate) 1 0 0))
                    (padstack P (shape (circle F.Cu 100)))
                  )
                )
                """);
    assertEquals(1, board.library.packages.count());
  }

  @Test
  void hugeComponentRotationIsReducedModulo360() {
    // the board subtracted 360 degrees in a loop that never ended for 1e20
    RoutingBoard board =
        readBoard(
            HEADER
                + BOUNDARY
                + """
                  )
                  (placement (component I (place C1 500 500 front 1e20)))
                  (library
                    (image I (pin P 1 0 0))
                    (padstack P (shape (circle F.Cu 100)))
                  )
                  (network)
                )
                """);
    // 1e20 is exactly representable and 1e20 mod 360 = 280
    assertEquals(280.0, board.components.get("C1").getRotationInDegree());
  }

  // ---------------------------------------------------------------------------------------------
  // Truncated files: every loop that waits for a closing bracket must stop at the end of the file
  // ---------------------------------------------------------------------------------------------

  @Test
  void fileEndingInsidePolygonPathIsParseError() {
    // null tokens were added to a list forever (hang, then OutOfMemoryError)
    readParseError(HEADER + "    (boundary (path pcb 0 0 0 1000");
  }

  @Test
  void fileEndingInsidePolylinePathIsParseError() {
    // hang, then OutOfMemoryError
    readParseError(HEADER + "    (boundary (polyline_path pcb 0 0 0 1000");
  }

  @Test
  void fileEndingInsideBoundaryIsParseError() {
    // hang
    readParseError(HEADER + "    (boundary (rect pcb 0 0 1000 1000)");
  }

  @Test
  void fileEndingInsideLayerNetListIsParseError() {
    // hang
    readParseError("(pcb t\n  (structure\n    (layer F.Cu (use_net A B");
  }

  @Test
  void fileEndingAfterLayerTypeIsParseError() {
    // NullPointerException
    readParseError("(pcb t\n  (structure\n    (layer F.Cu (type");
  }

  @Test
  void fileEndingInsideStructureRuleIsParseError() {
    // NullPointerException: Rule.readScope returned null to Collection.addAll
    readParseError(HEADER + BOUNDARY + "    (rule (width 100)");
  }

  @Test
  void fileEndingInsideNetClassRuleIsParseError() {
    // NullPointerException
    readParseError(STRUCTURE + "  (network (class c A (rule (width 100)");
  }

  @Test
  void fileEndingInsideNetClassViaListIsParseError() {
    // NullPointerException: Structure.readViaPadstacks returned null to Collection.addAll
    readParseError(STRUCTURE + "  (network (class c (circuit (use_via V");
  }

  // ---------------------------------------------------------------------------------------------
  // Unreadable parts are skipped, as the reader already skips similar ones
  // ---------------------------------------------------------------------------------------------

  @Test
  void unsupportedOutlineShapesBesideRectangleAreSkipped() {
    // NullPointerException for the polyline path (it has no board shape), ClassCastException for
    // the circle
    readBoard(HEADER + BOUNDARY + "    (boundary (polyline_path pcb 0 0 0 1000 1000))\n  )\n)\n");
    readBoard(HEADER + BOUNDARY + "    (boundary (circle signal 500 0 0))\n  )\n)\n");
  }

  @Test
  void polylinePathOutlineAloneIsMissingOutline() {
    // NullPointerException: a polyline path has no bounding box
    BoardReadResult result =
        read(HEADER + "    (boundary (polyline_path pcb 0 0 0 1000 1000))\n  )\n)\n");
    assertInstanceOf(BoardReadResult.OutlineMissing.class, result);
  }

  @Test
  void circularOutlineIsParseError() {
    // ClassCastException: a circle is no polygonal board outline
    BoardReadResult.ParseError error =
        readParseError(HEADER + "    (boundary (circle pcb 1000))\n  )\n)\n");
    assertTrue(error.detail().contains("circle"), error.detail());
  }

  @Test
  void keepoutWithoutShapeIsSkipped() {
    // NullPointerException in Structure.insertKeepout
    readBoard(HEADER + BOUNDARY + "    (keepout \"k\" (foo))\n  )\n)\n");
  }

  @Test
  void keepoutWithEmptyPolygonIsSkipped() {
    // ArrayIndexOutOfBoundsException in PolygonShape for a polygon without corners
    readBoard(HEADER + BOUNDARY + "    (keepout \"k\" (polygon F.Cu 0))\n  )\n)\n");
  }

  @Test
  void keepoutWindowWithoutShapeIsSkipped() {
    // NullPointerException in Shape.transformAreaToBoard for the unreadable window
    readBoard(
        HEADER + BOUNDARY + "    (keepout k (rect F.Cu 0 0 100 100) (window (foo)))\n  )\n)\n");
  }

  @Test
  void wireWindowWithoutShapeIsSkipped() {
    // NullPointerException in Shape.transformAreaToBoard for the unreadable window
    readBoard(
        HEADER + BOUNDARY + "  )\n  (wiring (wire (rect F.Cu 0 0 100 100) (window (foo))))\n)\n");
  }

  @Test
  void circularKeepoutWithWindowIsSkipped() {
    // NullPointerException: an area with holes needs a polygonal border
    readBoard(
        HEADER
            + BOUNDARY
            + "    (keepout k (circle F.Cu 100 50 50) (window (rect F.Cu 40 40 60 60)))\n  )\n)\n");
  }

  @Test
  void keepoutBeyondTheBoardCoordinateRangeIsSkipped() {
    // OutOfMemoryError: the wrapped-around int coordinates needed billions of tree sections
    readBoard(HEADER + BOUNDARY + "    (keepout k (circle F.Cu 1e10 0 0))\n  )\n)\n");
    readBoard(
        HEADER
            + BOUNDARY
            + "    (keepout k (rect F.Cu -30000000 -30000000 30000000 30000000))\n  )\n)\n");
  }

  @Test
  void imageKeepoutBeyondTheBoardCoordinateRangeIsSkipped() {
    // OutOfMemoryError, as for a structure keepout
    RoutingBoard board =
        readBoard(
            HEADER
                + BOUNDARY
                + """
                  )
                  (placement (component I (place C1 500 500 front 0)))
                  (library
                    (image I (pin P 1 0 0) (keepout "" (circle F.Cu 1e10)))
                    (padstack P (shape (circle F.Cu 100)))
                  )
                  (network)
                )
                """);
    assertEquals(1, board.components.count());
  }

  @Test
  void planeWithoutShapeIsSkipped() {
    // NullPointerException in Structure.promotePowerLayersWithoutPlane
    readBoard(HEADER + BOUNDARY + "    (plane GND (foo))\n  )\n)\n");
  }

  @Test
  void wireOnUnknownLayerIsSkipped() {
    // NullPointerException in Wiring.readWireScope
    RoutingBoard board =
        readBoard(
            HEADER + BOUNDARY + "  )\n  (wiring (wire (polyline_path X.Cu 100 0 0 500 500)))\n)\n");
    assertTrue(board.getTraces().isEmpty());
  }

  @Test
  void unclosedLayerListInNetClassCircuitIsSkipped() {
    // NullPointerException: DsnFile.readStringListScope returned null to Arrays.stream
    readBoard(STRUCTURE + "  (network (class c (circuit (use_layer F.Cu (x)))))\n)\n");
  }

  @Test
  void unclosedClassListInClassClassIsParseError() {
    // NullPointerException
    readParseError(STRUCTURE + "  (network (class_class (classes a b (x))))\n)\n");
  }

  // Without a (library ...) scope the board has no padstack and package lists.

  @Test
  void networkWithoutLibraryLoads() {
    // NullPointerException: the structure's via padstacks were looked up in the missing list
    RoutingBoard board =
        readBoard(HEADER + BOUNDARY + "    (via V)\n  )\n  (network (net A))\n)\n");
    assertNull(board.library.padstacks);
  }

  @Test
  void placedComponentWithoutLibraryIsSkipped() {
    // NullPointerException in Network.insertComponent
    RoutingBoard board =
        readBoard(
            HEADER
                + BOUNDARY
                + "  )\n  (placement (component I (place C1 500 500 front 0)))\n  (network)\n)\n");
    assertEquals(0, board.components.count());
  }

  @Test
  void networkViaWithoutLibraryIsParseError() {
    // NullPointerException in Network.readViaInfo; an unknown via padstack fails the read
    readParseError(HEADER + BOUNDARY + "  )\n  (network (via V P default))\n)\n");
  }

  @Test
  void wiringViaWithoutLibraryIsParseError() {
    // NullPointerException in Wiring.readViaScope; an unknown via padstack fails the read
    readParseError(HEADER + BOUNDARY + "  )\n  (wiring (via V 500 500))\n)\n");
  }

  // ---------------------------------------------------------------------------------------------
  // Scopes that need the board, which only a preceding (structure ...) scope creates
  // ---------------------------------------------------------------------------------------------

  @Test
  void libraryBeforeStructureIsParseError() {
    // NullPointerException
    BoardReadResult.ParseError error =
        readParseError("(pcb t\n  (library (padstack p (shape (circle F.Cu 100))))\n)\n");
    assertTrue(error.detail().contains("(library ...)"), error.detail());
  }

  @Test
  void placeControlBeforeStructureIsParseError() {
    // NullPointerException
    readParseError("(pcb t\n  (place_control (flip_style rotate_first))\n)\n");
  }

  @Test
  void wiringBeforeStructureIsParseError() {
    // Success with a null board
    readParseError("(pcb t\n  (wiring)\n)\n");
  }

  @Test
  void fileWithoutStructureIsParseError() {
    // Success with a null board
    readParseError("(pcb t\n  (parser (host_cad x))\n)\n");
    readParseError("(pcb t");
  }
}
