package app.freerouting.board.optimize;

import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.structure.AngleRestriction;
import app.freerouting.board.model.structure.ShapeEntrySide;
import app.freerouting.board.searchtree.SearchTreeChangeGrid;
import app.freerouting.datastructures.QueryRegionRecorder;
import app.freerouting.datastructures.QueryRegionRecorder.Region;
import app.freerouting.geometry.planar.Direction;
import app.freerouting.geometry.planar.FloatPoint;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.geometry.planar.IntDirection;
import app.freerouting.geometry.planar.IntOctagon;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Line;
import app.freerouting.geometry.planar.Simplex;
import app.freerouting.geometry.planar.TileShape;
import app.freerouting.rules.ClearanceMatrix;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * Exact memo of the trace shove check in {@link TraceShover}.
 *
 * <p>The maze search and optimizer ask the same shove question many times, on an unchanged board
 * and again after the board changed somewhere else. Each entry keeps the region its check read from
 * the search trees ({@link QueryRegionRecorder}); it stays valid while the board's {@link
 * SearchTreeChangeGrid} records no change in that region, no change that cannot be located, and no
 * change of the clearance matrix or the trace angle restriction. A hit replays the answer and all
 * shared-state mutations: temporary trace IDs from the ID generator, and writes to the board's
 * shove-failure record.
 */
public final class ShoveCheckCache {

  /** The entry count at which the cache starts over, bounding its memory. */
  public static final int MAX_ENTRIES = 1 << 16;

  /** The obstacle count at which the board's obstacle table starts over. */
  static final int MAX_OBSTACLES = 1 << 16;

  /** The most border lines a simplex key holds; a trace segment's tile has at most eight. */
  public static final int MAX_LINES = 8;

  /**
   * Whether entries outlive changes elsewhere on the board; {@code
   * -Dfreerouting.shove_cache.retain=false} restores a cache that any board change clears.
   */
  static final boolean RETAIN =
      !"false".equalsIgnoreCase(System.getProperty("freerouting.shove_cache.retain"));

  /**
   * Diagnostic: {@code -Dfreerouting.shove_cache.verify=true} recomputes every hit that was kept
   * across a board change and logs answers that differ.
   */
  static final boolean VERIFY = Boolean.getBoolean("freerouting.shove_cache.verify");

  /**
   * Diagnostic: with {@code -Dfreerouting.shove_cache.stats=true} (or verify) the process prints
   * the hit counts of all threads to standard error when it exits.
   */
  private static final boolean STATS =
      VERIFY || Boolean.getBoolean("freerouting.shove_cache.stats");

  private static final LongAdder[] TOTALS = new LongAdder[8];

  static {
    for (int i = 0; i < TOTALS.length; i++) {
      TOTALS[i] = new LongAdder();
    }
    if (STATS) {
      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () ->
                      System.err.println(
                          "Shove check cache: hits="
                              + TOTALS[0].sum()
                              + " retained_hits="
                              + TOTALS[1].sum()
                              + " misses="
                              + TOTALS[2].sum()
                              + " verified="
                              + TOTALS[3].sum()
                              + " verify_mismatches="
                              + TOTALS[4].sum()
                              + " dropped_changed="
                              + TOTALS[5].sum()
                              + " resets="
                              + TOTALS[6].sum()
                              + " overflow_clears="
                              + TOTALS[7].sum())));
    }
  }

  private static final ThreadLocal<ShoveCheckCache> CURRENT =
      ThreadLocal.withInitial(ShoveCheckCache::new);

  /** Least recently used entries go first once the cache is full. */
  private final Map<Key, Entry> entries =
      new LinkedHashMap<>(1024, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, Entry> eldest) {
          return size() > MAX_ENTRIES;
        }
      };

  private WeakReference<RoutingBoard> owner;
  private int generation;
  private ClearanceMatrix clearanceMatrix;
  private int clearanceMatrixModifications;
  private AngleRestriction angleRestriction;
  private boolean lastHitRetained;
  private long hits;
  private long misses;
  private long retainedHits;
  private long verifiedHits;
  private long verifyMismatches;

  /** Returns this thread's shove check cache instance. */
  public static ShoveCheckCache forThread() {
    return CURRENT.get();
  }

  public long getHits() {
    return hits;
  }

  public long getMisses() {
    return misses;
  }

  /** Hits whose entry was computed before a board change elsewhere. */
  public long getRetainedHits() {
    return retainedHits;
  }

  /** Retained hits that the verify diagnostic recomputed. */
  public long getVerifiedHits() {
    return verifiedHits;
  }

  /** Recomputed retained hits whose answer differed. */
  public long getVerifyMismatches() {
    return verifyMismatches;
  }

  void countHit() {
    hits++;
    if (lastHitRetained) {
      retainedHits++;
    }
    if (STATS) {
      TOTALS[0].increment();
      if (lastHitRetained) {
        TOTALS[1].increment();
      }
    }
  }

  void countMiss() {
    misses++;
    if (STATS) {
      TOTALS[2].increment();
    }
  }

  void countVerified(boolean same) {
    verifiedHits++;
    if (!same) {
      verifyMismatches++;
    }
    TOTALS[3].increment();
    if (!same) {
      TOTALS[4].increment();
    }
  }

  /** Whether the entry returned by the last {@link #tryGet} was kept across a board change. */
  boolean lastHitRetained() {
    return lastHitRetained;
  }

  /** Looks up a valid entry for {@code key} on {@code board}. */
  public Entry tryGet(RoutingBoard board, Key key) {
    SearchTreeChangeGrid grid = attach(board);
    lastHitRetained = false;
    Entry entry = entries.get(key);
    if (entry == null) {
      return null;
    }
    int now = grid.sequence();
    if (entry.sequence == now) {
      return entry;
    }
    if (RETAIN && grid.unchangedSince(entry.region, entry.sequence)) {
      // Valid at the current sequence too; later lookups before the next change are direct.
      entry.sequence = now;
      lastHitRetained = true;
      return entry;
    }
    entries.remove(key);
    if (STATS) {
      TOTALS[5].increment();
    }
    return null;
  }

  /** Puts an entry for {@code key} on {@code board}. */
  public void put(RoutingBoard board, Key key, Entry entry) {
    attach(board);
    entries.put(key, entry);
  }

  /**
   * The index of {@code obstacle} in the board's obstacle table, adding it when new. A table that
   * outgrows the cache starts over together with the entries that index it.
   */
  public int obstacleIndex(RoutingBoard board, Item obstacle) {
    List<Item> table = board.getShoveCacheObstacles();
    Map<Item, Integer> index = board.getShoveCacheObstacleIndex();
    Integer known = index.get(obstacle);
    if (known != null) {
      return known;
    }
    if (table.size() >= MAX_OBSTACLES) {
      entries.clear();
      board.clearShoveCacheObstacles();
      if (STATS) {
        TOTALS[7].increment();
      }
    }
    table.add(obstacle);
    index.put(obstacle, table.size() - 1);
    return table.size() - 1;
  }

  /**
   * Binds the cache to {@code board}, dropping entries that no longer hold, and returns its grid.
   */
  private SearchTreeChangeGrid attach(RoutingBoard board) {
    SearchTreeChangeGrid grid = board.changeGrid();
    ClearanceMatrix matrix = board.rules.clearanceMatrix;
    AngleRestriction angles = board.rules.getTraceAngleRestriction();
    if (owner == null
        || owner.get() != board
        || grid.generation() != generation
        || matrix != clearanceMatrix
        || matrix.getModificationCount() != clearanceMatrixModifications
        || angles != angleRestriction) {
      if (!entries.isEmpty()) {
        entries.clear();
        if (STATS) {
          TOTALS[6].increment();
        }
      }
      board.clearShoveCacheObstacles();
      owner = new WeakReference<>(board);
      generation = grid.generation();
      clearanceMatrix = matrix;
      clearanceMatrixModifications = matrix.getModificationCount();
      angleRestriction = angles;
    }
    return grid;
  }

  /** True for a check whose key can be held strictly by value. */
  public static boolean supported(TileShape shape, Direction dir, int[] nets) {
    return (shape instanceof IntOctagon || shape instanceof IntBox || integerSimplex(shape))
        && (dir == null || dir instanceof IntDirection)
        && (nets == null || nets.length <= 2);
  }

  private static boolean integerSimplex(TileShape shape) {
    if (!(shape instanceof Simplex s) || shape.getClass() != Simplex.class) {
      return false;
    }
    int n = s.borderLineCount();
    if (n == 0 || n > MAX_LINES) {
      return false;
    }
    for (int i = 0; i < n; i++) {
      Line line = s.borderLine(i);
      if (!(line.a instanceof IntPoint) || !(line.b instanceof IntPoint)) {
        return false;
      }
    }
    return true;
  }

  private static int[] primitive(int x, int y) {
    int g = (int) gcd(Math.abs((long) x), Math.abs((long) y));
    return g == 0 ? new int[] {0, 0} : new int[] {x / g, y / g};
  }

  private static long gcd(long a, long b) {
    while (b != 0) {
      long t = b;
      b = a % b;
      a = t;
    }
    return a;
  }

  /** Inputs of one shove check, held strictly by value with no object references kept. */
  public static final class Key {
    private final int kind; // 1 octagon, 2 box, 3 simplex
    private final int lineCount;
    private final int[] coords;
    private final int sideNo;
    private final long sideX;
    private final long sideY;
    private final boolean sideHasPoint;
    private final int dirX;
    private final int dirY;
    private final int netCount;
    private final int net0;
    private final int net1;
    private final int layer;
    private final int clearanceClass;
    private final int depth;
    private final int viaDepth;
    private final int springOverDepth;
    private final boolean timed;
    private final int hash;

    public Key(
        TileShape shape,
        ShapeEntrySide fromSide,
        Direction dir,
        int layer,
        int[] nets,
        int clearanceClass,
        int depth,
        int viaDepth,
        int springOverDepth,
        boolean timed) {
      if (shape instanceof IntOctagon o) {
        this.kind = 1;
        this.lineCount = 0;
        this.coords =
            new int[] {
              o.leftX,
              o.bottomY,
              o.rightX,
              o.topY,
              o.upperLeftDiagonalX,
              o.lowerRightDiagonalX,
              o.lowerLeftDiagonalX,
              o.upperRightDiagonalX
            };
      } else if (shape instanceof IntBox b) {
        this.kind = 2;
        this.lineCount = 0;
        this.coords = new int[] {b.ll.x, b.ll.y, b.ur.x, b.ur.y};
      } else {
        Simplex x = (Simplex) shape;
        this.kind = 3;
        this.lineCount = x.borderLineCount();
        this.coords = new int[4 * this.lineCount];
        for (int i = 0; i < this.lineCount; i++) {
          Line line = x.borderLine(i);
          IntPoint a = (IntPoint) line.a;
          IntPoint b = (IntPoint) line.b;
          this.coords[4 * i] = a.x;
          this.coords[4 * i + 1] = a.y;
          this.coords[4 * i + 2] = b.x;
          this.coords[4 * i + 3] = b.y;
        }
      }

      if (fromSide == null) {
        this.sideNo = Integer.MIN_VALUE;
        this.sideX = 0;
        this.sideY = 0;
        this.sideHasPoint = false;
      } else {
        this.sideNo = fromSide.no;
        FloatPoint p = fromSide.borderIntersection;
        this.sideHasPoint = (p != null);
        this.sideX = p == null ? 0 : Double.doubleToLongBits(p.x);
        this.sideY = p == null ? 0 : Double.doubleToLongBits(p.y);
      }

      if (dir instanceof IntDirection d) {
        int[] prim = primitive(d.x, d.y);
        this.dirX = prim[0];
        this.dirY = prim[1];
      } else {
        this.dirX = 0;
        this.dirY = 0;
      }

      this.netCount = (nets == null) ? 0 : nets.length;
      this.net0 = (nets != null && nets.length > 0) ? nets[0] : 0;
      this.net1 = (nets != null && nets.length > 1) ? nets[1] : 0;
      this.layer = layer;
      this.clearanceClass = clearanceClass;
      this.depth = depth;
      this.viaDepth = viaDepth;
      this.springOverDepth = springOverDepth;
      this.timed = timed;

      int h = 17;
      h = 31 * h + kind;
      h = 31 * h + lineCount;
      h = 31 * h + Arrays.hashCode(coords);
      h = 31 * h + sideNo;
      h = 31 * h + Long.hashCode(sideX);
      h = 31 * h + Long.hashCode(sideY);
      h = 31 * h + (sideHasPoint ? 1 : 0);
      h = 31 * h + dirX;
      h = 31 * h + dirY;
      h = 31 * h + netCount;
      h = 31 * h + net0;
      h = 31 * h + net1;
      h = 31 * h + layer;
      h = 31 * h + clearanceClass;
      h = 31 * h + depth;
      h = 31 * h + viaDepth;
      h = 31 * h + springOverDepth;
      h = 31 * h + (timed ? 1 : 0);
      this.hash = h;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof Key o)) {
        return false;
      }
      return hash == o.hash
          && kind == o.kind
          && lineCount == o.lineCount
          && Arrays.equals(coords, o.coords)
          && sideNo == o.sideNo
          && sideX == o.sideX
          && sideY == o.sideY
          && sideHasPoint == o.sideHasPoint
          && dirX == o.dirX
          && dirY == o.dirY
          && netCount == o.netCount
          && net0 == o.net0
          && net1 == o.net1
          && layer == o.layer
          && clearanceClass == o.clearanceClass
          && depth == o.depth
          && viaDepth == o.viaDepth
          && springOverDepth == o.springOverDepth
          && timed == o.timed;
    }

    @Override
    public int hashCode() {
      return hash;
    }
  }

  /**
   * A check's outcome, its recorded side-effects on shared board state, the region it read and the
   * change sequence of the board at which the outcome is known to hold.
   */
  public static final class Entry {
    public final boolean result;
    public final int ids;
    public final boolean wroteObstacle;
    public final int obstacle;
    public final boolean wroteLayer;
    public final int layer;
    public final Region region;
    int sequence;

    public Entry(
        boolean result,
        int ids,
        boolean wroteObstacle,
        int obstacle,
        boolean wroteLayer,
        int layer,
        Region region,
        int sequence) {
      this.result = result;
      this.ids = ids;
      this.wroteObstacle = wroteObstacle;
      this.obstacle = obstacle;
      this.wroteLayer = wroteLayer;
      this.layer = layer;
      this.region = region;
      this.sequence = sequence;
    }
  }
}
