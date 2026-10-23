package app.freerouting.board.optimize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.board.model.structure.Layer;
import app.freerouting.board.model.structure.LayerStructure;
import app.freerouting.board.model.structure.ShapeEntrySide;
import app.freerouting.board.state.Communication;
import app.freerouting.board.trace.PolylineTrace;
import app.freerouting.datastructures.QueryRegionRecorder.Region;
import app.freerouting.geometry.planar.Direction;
import app.freerouting.geometry.planar.FloatPoint;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.geometry.planar.IntOctagon;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Polyline;
import app.freerouting.geometry.planar.PolylineShape;
import app.freerouting.geometry.planar.TileShape;
import app.freerouting.rules.BoardRules;
import app.freerouting.rules.ClearanceMatrix;
import org.junit.jupiter.api.Test;

class ShoveCheckCacheTest {

  private RoutingBoard createTestBoard() {
    Layer layer1 = new Layer("Top", true);
    Layer[] layers = new Layer[] {layer1};
    LayerStructure layerStructure = new LayerStructure(layers);
    ClearanceMatrix clearanceMatrix = ClearanceMatrix.getDefaultInstance(layerStructure, 10);
    BoardRules boardRules = new BoardRules(layerStructure, clearanceMatrix);
    boardRules.createDefaultNetClass();
    Communication communication = new Communication();

    return new RoutingBoard(
        new IntBox(0, 0, 2000000, 2000000),
        layerStructure,
        new PolylineShape[] {TileShape.getInstance(0, 0, 2000000, 2000000)},
        0,
        boardRules,
        communication);
  }

  private ShoveCheckCache.Key makeKey(TileShape s, ShapeEntrySide f, Direction d, int[] nets) {
    return new ShoveCheckCache.Key(s, f, d, 0, nets, 1, 5, 3, 3, true);
  }

  @Test
  void testKeysCompareByValue() {
    IntOctagon a = new IntBox(0, 0, 100, 50).toIntOctagon();
    IntOctagon same = new IntBox(0, 0, 100, 50).toIntOctagon();
    IntOctagon other = new IntBox(0, 0, 100, 51).toIntOctagon();
    ShapeEntrySide side1 = new ShapeEntrySide(2, new FloatPoint(50, 0));
    ShapeEntrySide side2 = new ShapeEntrySide(2, new FloatPoint(50, 0));

    ShoveCheckCache.Key key1 = makeKey(a, side1, Direction.RIGHT, new int[] {4});
    ShoveCheckCache.Key key2 = makeKey(same, side2, Direction.RIGHT, new int[] {4});
    ShoveCheckCache.Key keyOther = makeKey(other, side1, Direction.RIGHT, new int[] {4});

    assertEquals(key1, key2);
    assertEquals(key1.hashCode(), key2.hashCode());
    assertNotEquals(key1, keyOther);

    // Direction difference
    assertNotEquals(key1, makeKey(a, side1, Direction.UP, new int[] {4}));
    assertNotEquals(key1, makeKey(a, side1, null, new int[] {4}));

    // Side difference
    assertNotEquals(key1, makeKey(a, null, Direction.RIGHT, new int[] {4}));

    // Nets difference
    assertNotEquals(key1, makeKey(a, side1, Direction.RIGHT, new int[] {4, 5}));

    // Simplex through integer points
    assertEquals(
        makeKey(a.toSimplex(), side1, null, new int[] {4}),
        makeKey(same.toSimplex(), side2, null, new int[] {4}));
    assertNotEquals(
        makeKey(a.toSimplex(), side1, null, new int[] {4}),
        makeKey(other.toSimplex(), side1, null, new int[] {4}));
    assertNotEquals(
        makeKey(a.toSimplex(), side1, null, new int[] {4}), makeKey(a, side1, null, new int[] {4}));
  }

  @Test
  void testSupportedChecks() {
    IntOctagon a = new IntBox(0, 0, 100, 50).toIntOctagon();
    assertTrue(ShoveCheckCache.supported(a, Direction.RIGHT, new int[] {4, 5}));
    assertTrue(ShoveCheckCache.supported(a.toSimplex(), null, new int[] {4}));
    // More than 2 nets not supported
    assertFalse(ShoveCheckCache.supported(a, Direction.RIGHT, new int[] {4, 5, 6}));
  }

  private static ShoveCheckCache.Entry entry(RoutingBoard board, Region region) {
    return new ShoveCheckCache.Entry(
        true, 3, false, -1, false, -1, region, board.changeGrid().sequence());
  }

  private static PolylineTrace insertTrace(RoutingBoard board, int x0, int y0, int x1, int y1) {
    return board.insertTraceWithoutCleaning(
        new Polyline(new IntPoint(x0, y0), new IntPoint(x1, y1)),
        0,
        1000,
        new int[] {1},
        1,
        FixedState.UNFIXED);
  }

  private static final Region REGION = new Region(100_000, 100_000, 110_000, 110_000, 1L);

  private static ShoveCheckCache.Key regionKey() {
    return new ShoveCheckCache.Key(
        new IntBox(100_000, 100_000, 110_000, 110_000).toIntOctagon(),
        null,
        Direction.RIGHT,
        0,
        new int[] {1},
        1,
        5,
        3,
        3,
        false);
  }

  @Test
  void testCachePutGetAndResetOnUnlocatedChange() {
    RoutingBoard board = createTestBoard();
    ShoveCheckCache cache = ShoveCheckCache.forThread();
    ShoveCheckCache.Key key = regionKey();
    ShoveCheckCache.Entry entry = entry(board, REGION);
    cache.put(board, key, entry);
    assertSame(entry, cache.tryGet(board, key));

    // A change that cannot be located invalidates every entry.
    board.itemList.noteContentChange();
    assertNull(cache.tryGet(board, key));
  }

  @Test
  void entryOutlivesChangesOutsideItsRegion() {
    RoutingBoard board = createTestBoard();
    ShoveCheckCache cache = ShoveCheckCache.forThread();
    ShoveCheckCache.Key key = regionKey();
    ShoveCheckCache.Entry entry = entry(board, REGION);
    cache.put(board, key, entry);

    PolylineTrace far = insertTrace(board, 1_500_000, 1_500_000, 1_600_000, 1_500_000);
    assertSame(entry, cache.tryGet(board, key));
    board.removeItem(far);
    assertSame(entry, cache.tryGet(board, key));

    insertTrace(board, 90_000, 105_000, 120_000, 105_000);
    assertNull(cache.tryGet(board, key));
  }

  @Test
  void fixedStateChangeInsideTheRegionDropsTheEntry() {
    RoutingBoard board = createTestBoard();
    PolylineTrace trace = insertTrace(board, 90_000, 105_000, 120_000, 105_000);
    ShoveCheckCache cache = ShoveCheckCache.forThread();
    ShoveCheckCache.Key key = regionKey();
    cache.put(board, key, entry(board, REGION));
    assertNotNull(cache.tryGet(board, key));

    // The fixed state is not counted by the content version, but shove checks read it.
    trace.setFixedState(FixedState.SHOVE_FIXED);
    assertNull(cache.tryGet(board, key));
  }

  @Test
  void clearanceMatrixChangeDropsAllEntries() {
    RoutingBoard board = createTestBoard();
    ShoveCheckCache cache = ShoveCheckCache.forThread();
    ShoveCheckCache.Key key = regionKey();
    cache.put(board, key, entry(board, REGION));
    assertNotNull(cache.tryGet(board, key));

    board.rules.clearanceMatrix.setValue(1, 1, 0, 400);
    assertNull(cache.tryGet(board, key));
  }

  @Test
  void entryOnLayerIgnoresChangesOnOtherLayers() {
    RoutingBoard board = createTwoLayerBoard();
    ShoveCheckCache cache = ShoveCheckCache.forThread();
    ShoveCheckCache.Key key = regionKey();
    ShoveCheckCache.Entry entry = entry(board, REGION);
    cache.put(board, key, entry);

    board.insertTraceWithoutCleaning(
        new Polyline(new IntPoint(90_000, 105_000), new IntPoint(120_000, 105_000)),
        1,
        1000,
        new int[] {1},
        1,
        FixedState.UNFIXED);
    assertSame(entry, cache.tryGet(board, key));
  }

  private RoutingBoard createTwoLayerBoard() {
    LayerStructure layerStructure =
        new LayerStructure(new Layer[] {new Layer("Top", true), new Layer("Bottom", true)});
    ClearanceMatrix clearanceMatrix = ClearanceMatrix.getDefaultInstance(layerStructure, 10);
    BoardRules boardRules = new BoardRules(layerStructure, clearanceMatrix);
    boardRules.createDefaultNetClass();
    return new RoutingBoard(
        new IntBox(0, 0, 2000000, 2000000),
        layerStructure,
        new PolylineShape[] {TileShape.getInstance(0, 0, 2000000, 2000000)},
        0,
        boardRules,
        new Communication());
  }
}
