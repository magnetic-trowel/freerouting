package app.freerouting.board.model.items;

import app.freerouting.autoroute.plane.FastPlaneConnectivityGrid;
import app.freerouting.board.actions.ItemInfoPrinter;
import app.freerouting.board.actions.ItemSelectionFilter;
import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.board.model.structure.Unit;
import app.freerouting.board.searchtree.SearchTreeObject;
import app.freerouting.board.searchtree.ShapeSearchTree;
import app.freerouting.datastructures.ShapeTree.TreeEntry;
import app.freerouting.geometry.planar.Area;
import app.freerouting.geometry.planar.FloatPoint;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.PlanarArea;
import app.freerouting.geometry.planar.Point;
import app.freerouting.geometry.planar.Shape;
import app.freerouting.geometry.planar.TileShape;
import app.freerouting.geometry.planar.Vector;
import app.freerouting.logger.FRLogger;
import app.freerouting.util.TextManager;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** A ObstacleArea, which can be electrically connected to other items. */
public class ConductionArea extends ObstacleArea implements Connectable {

  private static final double PLANE_HATCH_OPACITY = 0.85;

  private boolean isObstacle;
  private boolean isFilled = true;

  public boolean getIsFilled() {
    return this.isFilled;
  }

  /** SetIsFilled. */
  public void setIsFilled(boolean value) {
    this.isFilled = value;
    this.clearDerivedData();
  }

  private transient int cachedBoardRevision = -1;
  private transient PlanarArea cachedBoardFillArea;
  private transient int cachedFractureRevision = -1;
  private transient long cachedNetAssignmentStamp = -1;
  private transient FastPlaneConnectivityGrid.PlaneFractureResult cachedFractureResult;

  private transient int cachedIslandsRevision = -1;
  private transient List<PlanarArea> cachedFillIslands;
  // the bounds of each island as {minX, minY, width, height} ({@link PlanarArea#getBoundingBox})
  private transient List<double[]> cachedFillIslandBounds;
  private transient app.freerouting.drc.IslandIndex cachedFillIslandIndex;
  // the island found at each point asked about since the islands were computed (-1: none)
  private transient Map<PointKey, Integer> cachedIslandAtPoint;

  /** A point looked up in the fill islands. */
  private record PointKey(double x, double y) {}

  /** Creates a new instance of ConductionArea. */
  public ConductionArea(
      Area area,
      int layer,
      Vector translation,
      double rotationInDegree,
      boolean sideChanged,
      int[] netNumbers,
      int clearanceClassIndex,
      int id,
      int groupId,
      String name,
      boolean isObstacle,
      FixedState fixedState,
      BasicBoard board) {
    super(
        area,
        layer,
        translation,
        rotationInDegree,
        sideChanged,
        netNumbers,
        clearanceClassIndex,
        id,
        groupId,
        name,
        fixedState,
        board);
    this.isObstacle = isObstacle;
  }

  @Override
  public void clearDerivedData() {
    super.clearDerivedData();
    this.cachedBoardRevision = -1;
    this.cachedBoardFillArea = null;
    this.cachedIslandsRevision = -1;
    this.cachedFillIslands = null;
    this.cachedFillIslandBounds = null;
    this.cachedFillIslandIndex = null;
    this.cachedIslandAtPoint = null;
    this.cachedFractureRevision = -1;
    this.cachedNetAssignmentStamp = -1;
    this.cachedFractureResult = null;
  }

  /** Pre-computes detailed plane-fill geometry off the EDT so zoom-in paints stay responsive. */
  public void warmDetailedFillCache() {
    if (!this.isFilled) {
      return;
    }
    ensureDetailedFillCache(maxClearanceLookup(), this.getLayer());
  }

  /** The distance around the area within which other items can reduce its fill. */
  private double maxClearanceLookup() {
    double maxClearanceLookupBoard = 2000.0 * this.board.communication.getResolution(Unit.UM);
    if (this.board.rules != null && this.board.rules.clearanceMatrix != null) {
      double maxMatrixClearance =
          this.board.rules.clearanceMatrix.maxValue(this.clearanceClassIndex(), this.getLayer());
      maxClearanceLookupBoard =
          Math.max(
              maxClearanceLookupBoard,
              maxMatrixClearance + 100.0 * this.board.communication.getResolution(Unit.UM));
    }
    return maxClearanceLookupBoard;
  }

  /**
   * Returns the copper of this area that connects the items of its net: {@link
   * #getDetailedFillArea()} without the clearances to the pours of other nets. The design file does
   * not give pour priorities, and with each of two overlapping pours cleared from the other, the
   * smaller one (a teardrop, or a local zone on a plane) has no copper left. Computed on every
   * call.
   */
  public PlanarArea getConnectingFill() {
    return computeFill(maxClearanceLookup(), this.getLayer(), false);
  }

  /** Returns true for a pour: a conduction area that does not belong to a component. */
  public boolean isPour() {
    return this.getComponentId() <= 0;
  }

  /**
   * Returns true if this area joins the two points through its copper: an area of a component
   * wherever they lie in it, a pour only if one island of its fill contains both ({@link
   * #fillIslandsMetBy}).
   */
  public boolean joins(Point first, Point second) {
    if (!isPour()) {
      return true;
    }
    BitSet firstIsland = new BitSet();
    setFillIslandAt(first, firstIsland);
    if (firstIsland.isEmpty()) {
      return false;
    }
    BitSet secondIsland = new BitSet();
    setFillIslandAt(second, secondIsland);
    return firstIsland.equals(secondIsland);
  }

  /**
   * Returns the indices of the islands of {@link #getConnectingFill()} that the item meets: those
   * that contain its drill centre or one of its trace ends. These are the points at which {@link
   * #getNormalContacts()} connects an item to this area, so the area joins two of its contacts
   * through its copper exactly if they meet a common island.
   */
  public BitSet fillIslandsMetBy(Item item) {
    BitSet result = new BitSet();
    if (!item.sharesLayer(this)) {
      return result;
    }
    if (item instanceof DrillItem drillItem && drillItem.getCenter() != null) {
      setFillIslandAt(drillItem.getCenter(), result);
    } else if (item instanceof Trace trace) {
      setFillIslandAt(trace.firstCorner(), result);
      setFillIslandAt(trace.lastCorner(), result);
    }
    return result;
  }

  /**
   * Sets the index of the island of {@link #getConnectingFill()} that contains the point, if one
   * does. The islands are computed again only after a change of the board that can change the fill:
   * the traces of the area's own net leave it unchanged.
   */
  void setFillIslandAt(Point point, BitSet islands) {
    int fillRevision =
        this.netCount() > 0
            ? this.board.revisionIgnoringTracesOf(this.getNetNumber(0))
            : this.board.getRevision();
    if (cachedFillIslands == null || cachedIslandsRevision != fillRevision) {
      cachedFillIslands = getConnectingFill().decomposeIntoIslands();
      cachedFillIslandBounds = new ArrayList<>(cachedFillIslands.size());
      for (PlanarArea island : cachedFillIslands) {
        cachedFillIslandBounds.add(island.getBoundingBox());
      }
      cachedFillIslandIndex = new app.freerouting.drc.IslandIndex(cachedFillIslandBounds);
      cachedIslandAtPoint = new java.util.HashMap<>();
      cachedIslandsRevision = fillRevision;
    }
    FloatPoint floatPoint = point.toFloat();
    // A point test on a fill island runs over all of its edges, thousands on a pour with many
    // holes, and the fill-cycle search asks about the same pads, vias and trace ends again and
    // again while the fill is unchanged: remember the answer per point.
    PointKey key = new PointKey(floatPoint.x, floatPoint.y);
    Integer known = cachedIslandAtPoint.get(key);
    if (known != null) {
      if (known >= 0) {
        islands.set(known);
      }
      return;
    }
    int found = -1;
    // the islands whose bounds contain the point, in ascending order: the first one that contains
    // it is the one a scan of all islands finds, since the islands do not overlap
    for (int i : cachedFillIslandIndex.candidates(floatPoint.x, floatPoint.y)) {
      if (app.freerouting.drc.IslandIndex.boundsContain(
              cachedFillIslandBounds.get(i), floatPoint.x, floatPoint.y)
          && cachedFillIslands.get(i).contains(floatPoint.x, floatPoint.y)) {
        found = i;
        break; // the islands do not overlap
      }
    }
    cachedIslandAtPoint.put(key, found);
    if (found >= 0) {
      islands.set(found);
    }
  }

  /**
   * Returns the detailed fill geometry for this conduction area, computing clearances against all
   * overlapping items if needed.
   */
  public PlanarArea getDetailedFillArea() {
    warmDetailedFillCache();
    return cachedBoardFillArea;
  }

  /**
   * Returns the cached detailed fill geometry for renderer-owned painting.
   *
   * <p>The cache remains owned by the conduction-area model because it depends on board revision
   * and clearance geometry. The renderer owns the AWT paint operation that consumes this geometry.
   */
  public PlanarArea getDetailedFillArea(double maxClearanceLookupBoard, int layerIndex) {
    ensureDetailedFillCache(maxClearanceLookupBoard, layerIndex);
    return cachedBoardFillArea;
  }

  private void ensureDetailedFillCache(double maxClearanceLookupBoard, int layerIndex) {
    boolean boardChanged = this.board.getRevision() != cachedBoardRevision;
    if (cachedBoardFillArea != null && !boardChanged) {
      return;
    }
    cachedBoardFillArea = computeFill(maxClearanceLookupBoard, layerIndex, true);
    cachedBoardRevision = this.board.getRevision();
  }

  /**
   * Computes the fill on {@code layerIndex}: as drawn ({@link #getDetailedFillArea()}), or as it
   * connects ({@link #getConnectingFill()}), without clearances to other pours.
   */
  private PlanarArea computeFill(double maxClearanceLookupBoard, int layerIndex, boolean asDrawn) {
    PlanarArea fillArea = PlanarArea.fromArea(this.getArea());
    if (fillArea == null) {
      fillArea = PlanarArea.empty();
    }
    // Pours are filled only on the board. Zone outlines may extend past the board edge (KiCad
    // exports them that way); unclipped, the part outside the edge becomes an off-board frame.
    var outline = this.board.getOutline();
    if (!fillArea.isEmpty() && outline != null && outline.shapeCount() > 0) {
      PlanarArea boardArea = PlanarArea.empty();
      for (int i = 0; i < outline.shapeCount(); i++) {
        PlanarArea outlinePiece = PlanarArea.fromShape(outline.getShape(i));
        if (outlinePiece != null && !outlinePiece.isEmpty()) {
          // exclusive or: an outline piece inside another one is a cutout of the board
          boardArea =
              boardArea.union(outlinePiece).difference(boardArea.intersection(outlinePiece));
        }
      }
      if (!boardArea.isEmpty()) {
        fillArea = fillArea.intersection(boardArea);
      }
    }
    if (!fillArea.isEmpty()) {
      IntBox bbox = this.boundingBox();
      double spokeWidth = 400.0 * this.board.communication.getResolution(Unit.UM);
      int maxCl = (int) Math.round(maxClearanceLookupBoard);
      IntBox inflatedBbox =
          new IntBox(
              new IntPoint(bbox.ll.x - maxCl, bbox.ll.y - maxCl),
              new IntPoint(bbox.ur.x + maxCl, bbox.ur.y + maxCl));

      List<PlanarArea> foreignClearances = new ArrayList<>();
      List<PlanarArea> sameNetClearances = new ArrayList<>();

      Set<SearchTreeObject> overlaps = this.board.overlappingObjects(inflatedBbox, layerIndex);
      for (SearchTreeObject ob : overlaps) {
        if (!(ob instanceof Item currentItem) || currentItem == this) {
          continue;
        }
        if (!currentItem.sharesLayer(this)) {
          continue;
        }

        if (currentItem instanceof Trace || currentItem instanceof ConductionArea) {
          if (currentItem.sharesNet(this)) {
            continue;
          }
        }
        if (!asDrawn
            && currentItem instanceof ConductionArea
            && currentItem.getComponentId() <= 0) {
          continue;
        }

        int clClass1 = this.clearanceClassIndex();
        int clClass2 = currentItem.clearanceClassIndex();
        double clearanceDist = this.board.clearanceValue(clClass1, clClass2, layerIndex);

        if (currentItem.sharesNet(this)) {
          if (currentItem instanceof DrillItem drillItem) {
            FloatPoint center = drillItem.getCenter().toFloat();
            Shape shape = drillItem.getShapeOnLayer(layerIndex);
            if (shape == null) {
              continue;
            }

            Shape enlargedShape = shape.enlarge(clearanceDist);
            PlanarArea clearanceArea = PlanarArea.fromShape(enlargedShape);
            if (clearanceArea == null || clearanceArea.isEmpty()) {
              continue;
            }

            // The spokes must cross the whole clearance ring to reach the pour; along a diagonal
            // that is the distance to the farthest corner of the item, not its half width.
            IntBox itemBbox = drillItem.boundingBox();
            double reachX =
                Math.max(Math.abs(itemBbox.ll.x - center.x), Math.abs(itemBbox.ur.x - center.x));
            double reachY =
                Math.max(Math.abs(itemBbox.ll.y - center.y), Math.abs(itemBbox.ur.y - center.y));
            double expansionRadiusBoard = Math.hypot(reachX, reachY) + clearanceDist + spokeWidth;
            double spokeLength = 2 * expansionRadiusBoard;

            PlanarArea spoke1 =
                PlanarArea.createRotatedSpoke(
                    center.x, center.y, spokeWidth, spokeLength, Math.PI / 4.0);
            PlanarArea spoke2 =
                PlanarArea.createRotatedSpoke(
                    center.x, center.y, spokeWidth, spokeLength, -Math.PI / 4.0);
            PlanarArea spokes = spoke1.union(spoke2);

            PlanarArea thermalCutout = clearanceArea.difference(spokes);
            if (!thermalCutout.isEmpty()) {
              sameNetClearances.add(thermalCutout);
            }
          }
        } else {
          if (currentItem instanceof DrillItem drillItem) {
            Shape shape = drillItem.getShapeOnLayer(layerIndex);
            if (shape != null) {
              Shape enlargedShape = shape.enlarge(clearanceDist);
              PlanarArea clearanceArea = PlanarArea.fromShape(enlargedShape);
              if (clearanceArea != null && !clearanceArea.isEmpty()) {
                foreignClearances.add(clearanceArea);
              }
            }
          } else {
            int shapeCount = currentItem.tileShapeCount();
            for (int i = 0; i < shapeCount; i++) {
              if (currentItem.shapeLayer(i) == layerIndex) {
                TileShape tileShape = currentItem.getTileShape(i);
                if (tileShape != null) {
                  Shape enlargedShape = tileShape.enlarge(clearanceDist);
                  PlanarArea clearanceArea = PlanarArea.fromShape(enlargedShape);
                  if (clearanceArea != null && !clearanceArea.isEmpty()) {
                    foreignClearances.add(clearanceArea);
                  }
                }
              }
            }
          }
        }
      }

      // The spokes are left out of the clearance rings of the same-net items rather than added
      // back, so a spoke never bridges copper into the clearance of another net or past the pour
      // outline. Each list is merged first and applied once: one operation per item re-sweeps the
      // whole growing fill every time.
      fillArea = fillArea.subtractAll(foreignClearances);
      fillArea = fillArea.subtractAll(sameNetClearances);
    }
    return fillArea;
  }

  @Override
  public Item copy(int id) {
    return new ConductionArea(
        getRelativeArea(),
        getLayer(),
        getTranslation(),
        getRotationInDegree(),
        getSideChanged(),
        netNumbers,
        clearanceClassIndex(),
        id,
        getComponentId(),
        this.name,
        isObstacle,
        getFixedState(),
        board);
  }

  /**
   * Whether this area is dead copper: a piece of a fill smaller than 1 mm\u00b2 (the threshold of
   * {@code DesignRulesChecker.getZoneIslandViolations}), on a layer without traces, that touches
   * nothing of its net. A zone with no connection yet keeps all its islands when it is filled, so
   * an unrouted board can carry such pieces between the vias of another net: no via fits there, and
   * they are no incomplete connection.
   */
  public boolean isDeadCopper() {
    if (board == null || board.layerStructure.layers[getLayer()].isSignal) {
      return false;
    }
    double mm = board.communication.getResolution(Unit.MM);
    IntBox box = getArea().boundingBox();
    if ((double) box.width() * box.height() >= mm * mm) {
      return false;
    }
    return getNormalContacts().isEmpty();
  }

  @Override
  public Set<Item> getNormalContacts() {
    ShapeSearchTree defaultTree = board.searchTreeManager.getDefaultTree();
    // A plane spans most of its layer, often in thousands of tiles: rather than one tree query per
    // tile, look up the tiles that contain the ends of the traces and the centres of the vias, and
    // that overlap the pins of the net.
    Set<SearchTreeObject> overlaps =
        defaultTree.overlappingObjectsWithPointIn(
            this, getLayer(), this::contactPoints, this::isPinCandidate);
    Set<Item> result = new TreeSet<>();
    if (overlaps == null) {
      // Not in the tree: query tile by tile.
      for (int i = 0; i < tileShapeCount(); i++) {
        TileShape tile = getTileShape(i);
        for (SearchTreeObject currentObject : board.overlappingObjects(tile, getLayer())) {
          if (isContactInTile(currentObject, tile)) {
            result.add((Item) currentObject);
          }
        }
      }
      return result;
    }
    for (SearchTreeObject currentObject : overlaps) {
      result.add((Item) currentObject);
    }
    return result;
  }

  /**
   * The points of object of which one must lie in a tile of this area for a contact, or null if
   * object cannot be a contact or is a pin, looked up by its shape.
   */
  private Point[] contactPoints(SearchTreeObject object) {
    if (!(object instanceof Item item)
        || item == this
        || !item.sharesNet(this)
        || !item.sharesLayer(this)) {
      return null;
    }
    if (item instanceof Trace trace) {
      return new Point[] {trace.firstCorner(), trace.lastCorner()};
    }
    return item instanceof Via via ? new Point[] {via.getCenter()} : null;
  }

  /**
   * Whether object is a pin of the net of this area, which is a contact when its copper overlaps a
   * tile. Its centre need not be in the area: a thermal relief of a filled zone leaves the centre
   * of the pad in a hole, the spokes covering only its edge. A via, placed by the router with its
   * centre in the area, keeps that rule.
   */
  private boolean isPinCandidate(SearchTreeObject object) {
    return object instanceof Pin pin && pin.sharesNet(this) && pin.sharesLayer(this);
  }

  private boolean isContactInTile(SearchTreeObject object, TileShape tile) {
    if (isPinCandidate(object)) {
      return overlapsTile((Pin) object, tile);
    }
    Point[] points = contactPoints(object);
    if (points != null) {
      for (Point point : points) {
        if (tile.contains(point)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Whether the copper of drillItem on the layer of this area overlaps tile with a positive area.
   */
  private boolean overlapsTile(DrillItem drillItem, TileShape tile) {
    TileShape drillShape =
        drillItem.getTreeShapeOnLayer(board.searchTreeManager.getDefaultTree(), getLayer());
    return drillShape != null && tile.intersection(drillShape).dimension() == 2;
  }

  /**
   * Whether drillItem, a pin of the net of this area, overlaps a tile of this area with a positive
   * area: the counterpart of {@link #getNormalContacts()} for {@link
   * DrillItem#getNormalContacts()}.
   */
  boolean overlapsCopperOf(DrillItem drillItem) {
    if (!isPinCandidate(drillItem)) {
      return false;
    }
    ShapeSearchTree defaultTree = board.searchTreeManager.getDefaultTree();
    TileShape drillShape = drillItem.getTreeShapeOnLayer(defaultTree, getLayer());
    if (drillShape == null) {
      return false;
    }
    if (getSearchTreeEntries(defaultTree) == null) {
      for (int i = 0; i < tileShapeCount(); i++) {
        if (getTileShape(i).intersection(drillShape).dimension() == 2) {
          return true;
        }
      }
      return false;
    }
    Collection<TreeEntry> entries = new ArrayList<>();
    defaultTree.overlappingTreeEntries(drillShape, getLayer(), entries);
    for (TreeEntry entry : entries) {
      if (entry.object == this
          && getTreeShape(defaultTree, entry.shapeIndexInObject)
                  .intersection(drillShape)
                  .dimension()
              == 2) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if this conduction area is split into multiple isolated copper islands by
   * obstacles.
   */
  public boolean isFractured() {
    FastPlaneConnectivityGrid.PlaneFractureResult res = getFractureResult();
    return res != null && res.isFractured;
  }

  /** Returns the cached or freshly computed fracture analysis result for this conduction area. */
  public FastPlaneConnectivityGrid.PlaneFractureResult getFractureResult() {
    if (this.board == null) {
      return null;
    }
    int currentRevision = this.board.getRevision();
    long currentNetAssignmentStamp = this.board.getNetAssignmentChangeCount();
    if (this.cachedFractureResult != null
        && this.cachedFractureRevision == currentRevision
        && this.cachedNetAssignmentStamp == currentNetAssignmentStamp) {
      return this.cachedFractureResult;
    }
    FastPlaneConnectivityGrid connectivityGrid = new FastPlaneConnectivityGrid();
    Collection<Item> contacts = getNormalContacts();
    this.cachedFractureResult = connectivityGrid.evaluateFracture(this, this.board, contacts);
    this.cachedFractureRevision = currentRevision;
    this.cachedNetAssignmentStamp = currentNetAssignmentStamp;
    return this.cachedFractureResult;
  }

  @Override
  void getConnectedSetRecu(Set<Item> result, int netNumber, boolean stopAtPlane) {
    if (this.board == null) {
      super.getConnectedSetRecu(result, netNumber, stopAtPlane);
      return;
    }

    FastPlaneConnectivityGrid.PlaneFractureResult fractureResult = getFractureResult();
    if (fractureResult == null || !fractureResult.isFractured) {
      super.getConnectedSetRecu(result, netNumber, stopAtPlane);
      return;
    }

    // Determine which islands have already been contacted by items in result
    Set<Integer> reachedIslands = new HashSet<>();
    for (Item resItem : result) {
      if (resItem != this && resItem.sharesNet(this) && resItem.sharesLayer(this)) {
        for (Map.Entry<Integer, List<Item>> entry : fractureResult.islandItems.entrySet()) {
          if (entry.getValue().contains(resItem)) {
            reachedIslands.add(entry.getKey());
            break;
          }
        }
      }
    }

    if (reachedIslands.isEmpty()) {
      // If this conduction area itself is the start item, only visit island 0 so severed islands
      // are not falsely bridged
      reachedIslands.add(0);
    }

    for (int islandId : reachedIslands) {
      List<Item> reachableItems = fractureResult.islandItems.get(islandId);
      if (reachableItems != null) {
        for (Item item : reachableItems) {
          if (item != this && (netNumber <= 0 || item.containsNet(netNumber))) {
            if (result.add(item)) {
              item.getConnectedSetRecu(result, netNumber, stopAtPlane);
            }
          }
        }
      }
    }
  }

  @Override
  boolean isCycleRecu(
      Set<Item> visitedItems, Item searchItem, Item comeFromItem, boolean ignoreAreas) {
    if (ignoreAreas) {
      return false;
    }
    if (this.board == null) {
      return super.isCycleRecu(visitedItems, searchItem, comeFromItem, ignoreAreas);
    }
    FastPlaneConnectivityGrid.PlaneFractureResult fractureResult = getFractureResult();
    if (fractureResult == null || !fractureResult.isFractured) {
      return super.isCycleRecu(visitedItems, searchItem, comeFromItem, ignoreAreas);
    }

    Integer fromIsland = null;
    if (comeFromItem != null) {
      for (Map.Entry<Integer, List<Item>> entry : fractureResult.islandItems.entrySet()) {
        if (entry.getValue().contains(comeFromItem)) {
          fromIsland = entry.getKey();
          break;
        }
      }
    }

    Collection<Item> contactList = getNormalContacts();
    if (contactList == null) {
      return false;
    }
    for (Item currentContact : contactList) {
      if (currentContact == comeFromItem) {
        continue;
      }
      if (fromIsland != null) {
        boolean onSameIsland = false;
        List<Item> islandMembers = fractureResult.islandItems.get(fromIsland);
        if (islandMembers != null && islandMembers.contains(currentContact)) {
          onSameIsland = true;
        }
        if (!onSameIsland) {
          continue;
        }
      }
      if (currentContact == searchItem) {
        return true;
      }
      if (visitedItems.add(currentContact)) {
        if (currentContact.isCycleRecu(visitedItems, searchItem, this, ignoreAreas)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public TileShape getTraceConnectionShape(ShapeSearchTree searchTree, int index) {
    if (index < 0 || index >= this.treeShapeCount(searchTree)) {
      FRLogger.warn("ConductionArea.get_trace_connection_shape index out of range");
      return null;
    }
    return this.getTreeShape(searchTree, index);
  }

  @Override
  public Point[] getRatsnestCorners() {
    Point[] result;
    FloatPoint[] corners = this.getArea().cornerApproxArr();
    result = new Point[corners.length];
    for (int i = 0; i < corners.length; i++) {
      result[i] = corners[i].round();
    }

    return result;
  }

  @Override
  public boolean isObstacle(Item other) {
    if (this.isObstacle) {
      return super.isObstacle(other);
    }
    return false;
  }

  /** Returns if this conduction area is regarded as obstacle to traces of foreign nets. */
  public boolean getIsObstacle() {
    return this.isObstacle;
  }

  /** Sets, if this conduction area is regarded as obstacle to traces and vias of foreign nets. */
  public void setIsObstacle(boolean value) {
    if (this.isObstacle != value) {
      this.isObstacle = value;
      this.clearDerivedData();
    }
  }

  @Override
  public boolean isTraceObstacle(int netNumber) {
    return this.isObstacle && !this.containsNet(netNumber);
  }

  @Override
  public boolean isDrillable(int netNumber) {
    return !this.isObstacle || this.containsNet(netNumber);
  }

  @Override
  public boolean isSelectedByFilter(ItemSelectionFilter filter) {
    if (!this.isSelectedByFixedFilter(filter)) {
      return false;
    }
    return filter.isSelected(ItemSelectionFilter.SelectableChoices.CONDUCTION);
  }

  @Override
  public void printInfo(ItemInfoPrinter printer, Locale locale) {
    TextManager tm = new TextManager(this.getClass(), locale);

    printer.appendBold(tm.getText("conductionArea"));
    this.printShapeInfo(printer, locale);
    this.printConnectableItemInfo(printer, locale);
    printer.newline();
  }
}
