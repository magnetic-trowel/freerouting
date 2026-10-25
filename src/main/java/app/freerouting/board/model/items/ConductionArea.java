package app.freerouting.board.model.items;

import app.freerouting.board.actions.ItemInfoPrinter;
import app.freerouting.board.actions.ItemSelectionFilter;
import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.board.model.structure.Unit;
import app.freerouting.board.searchtree.SearchTreeObject;
import app.freerouting.board.searchtree.ShapeSearchTree;
import app.freerouting.drc.DesignRulesChecker;
import app.freerouting.geometry.planar.Area;
import app.freerouting.geometry.planar.AwtAreas;
import app.freerouting.geometry.planar.FloatPoint;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Point;
import app.freerouting.geometry.planar.Shape;
import app.freerouting.geometry.planar.TileShape;
import app.freerouting.geometry.planar.Vector;
import app.freerouting.logger.FRLogger;
import app.freerouting.util.TextManager;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
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
  private transient java.awt.geom.Area cachedBoardFillArea;

  private transient int cachedIslandsRevision = -1;
  private transient List<java.awt.geom.Area> cachedFillIslands;
  private transient List<java.awt.geom.Rectangle2D> cachedFillIslandBounds;

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
  public java.awt.geom.Area getConnectingFill() {
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
      cachedFillIslands = DesignRulesChecker.decomposeAreaIntoIslands(getConnectingFill());
      cachedFillIslandBounds = new ArrayList<>(cachedFillIslands.size());
      for (java.awt.geom.Area island : cachedFillIslands) {
        cachedFillIslandBounds.add(island.getBounds2D());
      }
      cachedIslandsRevision = fillRevision;
    }
    FloatPoint floatPoint = point.toFloat();
    for (int i = 0; i < cachedFillIslands.size(); i++) {
      if (cachedFillIslandBounds.get(i).contains(floatPoint.x, floatPoint.y)
          && cachedFillIslands.get(i).contains(floatPoint.x, floatPoint.y)) {
        islands.set(i);
        return; // the islands do not overlap
      }
    }
  }

  /**
   * Returns the detailed fill geometry for this conduction area, computing clearances against all
   * overlapping items if needed.
   */
  public java.awt.geom.Area getDetailedFillArea() {
    warmDetailedFillCache();
    return cachedBoardFillArea;
  }

  /**
   * Returns the cached detailed fill geometry for renderer-owned painting.
   *
   * <p>The cache remains owned by the conduction-area model because it depends on board revision
   * and clearance geometry. The renderer owns the AWT paint operation that consumes this geometry.
   */
  public java.awt.geom.Area getDetailedFillArea(double maxClearanceLookupBoard, int layerIndex) {
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
  private java.awt.geom.Area computeFill(
      double maxClearanceLookupBoard, int layerIndex, boolean asDrawn) {
    java.awt.geom.Area fillArea = getAwtAreaInBoardUnits(this.getArea());
    if (fillArea == null) {
      fillArea = new java.awt.geom.Area();
    }
    // Pours are filled only on the board. Zone outlines may extend past the board edge (KiCad
    // exports them that way); unclipped, the part outside the edge becomes an off-board frame.
    var outline = this.board.getOutline();
    if (!fillArea.isEmpty() && outline != null && outline.shapeCount() > 0) {
      java.awt.geom.Area boardArea = new java.awt.geom.Area();
      for (int i = 0; i < outline.shapeCount(); i++) {
        java.awt.geom.Area outlinePiece = getAwtAreaFromShapeInBoardUnits(outline.getShape(i));
        if (outlinePiece != null) {
          boardArea.exclusiveOr(outlinePiece);
        }
      }
      if (!boardArea.isEmpty()) {
        fillArea.intersect(boardArea);
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

      java.util.List<java.awt.geom.Area> foreignClearances = new java.util.ArrayList<>();
      java.util.List<java.awt.geom.Area> sameNetClearances = new java.util.ArrayList<>();
      java.util.List<java.awt.geom.Area> sameNetSpokesList = new java.util.ArrayList<>();

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
            java.awt.geom.Area clearanceAwt = getAwtAreaFromShapeInBoardUnits(enlargedShape);
            if (clearanceAwt == null) {
              continue;
            }

            // The spokes must cross the whole clearance ring to reach the pour; along a diagonal
            // that is the distance to the farthest corner of the item, not its half width.
            IntBox itemBbox = drillItem.boundingBox();
            double reachX =
                Math.max(Math.abs(itemBbox.ll.x - center.x), Math.abs(itemBbox.ur.x - center.x));
            double reachY =
                Math.max(Math.abs(itemBbox.ll.y - center.y), Math.abs(itemBbox.ur.y - center.y));
            double expansionRadiusBoard = Math.hypot(reachX, reachY) + clearanceDist + 1;

            double halfSpoke = spokeWidth / 2.0;
            java.awt.geom.Rectangle2D.Double baseSpoke =
                new java.awt.geom.Rectangle2D.Double(
                    center.x - halfSpoke,
                    center.y - expansionRadiusBoard,
                    spokeWidth,
                    2 * expansionRadiusBoard);

            java.awt.geom.AffineTransform rotP45 =
                java.awt.geom.AffineTransform.getRotateInstance(Math.PI / 4.0, center.x, center.y);
            java.awt.geom.AffineTransform rotM45 =
                java.awt.geom.AffineTransform.getRotateInstance(-Math.PI / 4.0, center.x, center.y);

            java.awt.geom.Area spokes =
                new java.awt.geom.Area(rotP45.createTransformedShape(baseSpoke));
            spokes.add(new java.awt.geom.Area(rotM45.createTransformedShape(baseSpoke)));

            spokes.intersect(clearanceAwt);

            sameNetClearances.add(clearanceAwt);
            sameNetSpokesList.add(spokes);
          }
        } else {
          if (currentItem instanceof DrillItem drillItem) {
            Shape shape = drillItem.getShapeOnLayer(layerIndex);
            if (shape != null) {
              Shape enlargedShape = shape.enlarge(clearanceDist);
              java.awt.geom.Area clearanceAwt = getAwtAreaFromShapeInBoardUnits(enlargedShape);
              if (clearanceAwt != null) {
                foreignClearances.add(clearanceAwt);
              }
            }
          } else {
            int shapeCount = currentItem.tileShapeCount();
            for (int i = 0; i < shapeCount; i++) {
              if (currentItem.shapeLayer(i) == layerIndex) {
                TileShape tileShape = currentItem.getTileShape(i);
                if (tileShape != null) {
                  Shape enlargedShape = tileShape.enlarge(clearanceDist);
                  java.awt.geom.Area clearanceAwt = getAwtAreaFromShapeInBoardUnits(enlargedShape);
                  if (clearanceAwt != null) {
                    foreignClearances.add(clearanceAwt);
                  }
                }
              }
            }
          }
        }
      }

      // Spokes go in before the foreign clearances come out, and stay inside the pour, so that a
      // spoke never bridges copper into the clearance of another net or past the pour outline.
      // Each list is merged first and applied once: one operation per item re-sweeps the whole
      // growing fill every time, which took minutes on pours with thousands of clearances.
      java.awt.geom.Area pourOutline =
          sameNetSpokesList.isEmpty() ? null : new java.awt.geom.Area(fillArea);
      AwtAreas.subtractAll(fillArea, sameNetClearances);
      if (pourOutline != null) {
        fillArea.add(AwtAreas.unionAll(sameNetSpokesList));
        fillArea.intersect(pourOutline);
      }
      AwtAreas.subtractAll(fillArea, foreignClearances);
    }
    return fillArea;
  }

  private static java.awt.geom.Area getAwtAreaInBoardUnits(Area area) {
    if (area == null || area.isEmpty()) {
      return null;
    }
    if (area instanceof app.freerouting.geometry.planar.Circle circle) {
      double radius = circle.radius;
      double diameter = 2 * radius;
      return new java.awt.geom.Area(
          new java.awt.geom.Ellipse2D.Double(
              circle.center.x - radius, circle.center.y - radius, diameter, diameter));
    }

    Shape borderShape = area.getBorder();
    if (!(borderShape instanceof app.freerouting.geometry.planar.PolylineShape border)
        || !border.isBounded()) {
      return null;
    }

    java.awt.geom.Path2D.Double borderPath = new java.awt.geom.Path2D.Double();
    int count = border.borderLineCount();
    if (count > 0) {
      FloatPoint p0 = border.cornerApprox(0);
      borderPath.moveTo(p0.x, p0.y);
      for (int i = 1; i < count; i++) {
        FloatPoint pi = border.cornerApprox(i);
        borderPath.lineTo(pi.x, pi.y);
      }
      borderPath.closePath();
    }
    java.awt.geom.Area awtArea = new java.awt.geom.Area(borderPath);

    Shape[] holes = area.getHoles();
    for (Shape hole : holes) {
      java.awt.geom.Area holeArea = getAwtAreaFromShapeInBoardUnits(hole);
      if (holeArea != null) {
        awtArea.subtract(holeArea);
      }
    }
    return awtArea;
  }

  private static java.awt.geom.Area getAwtAreaFromShapeInBoardUnits(Shape shape) {
    if (shape == null) {
      return null;
    }
    if (shape instanceof app.freerouting.geometry.planar.Circle circle) {
      double radius = circle.radius;
      double diameter = 2 * radius;
      return new java.awt.geom.Area(
          new java.awt.geom.Ellipse2D.Double(
              circle.center.x - radius, circle.center.y - radius, diameter, diameter));
    }
    if (shape instanceof IntBox box) {
      return new java.awt.geom.Area(
          new java.awt.geom.Rectangle2D.Double(box.ll.x, box.ll.y, box.width(), box.height()));
    }
    if (shape instanceof app.freerouting.geometry.planar.PolylineShape poly) {
      java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
      int count = poly.borderLineCount();
      if (count > 0) {
        FloatPoint p0 = poly.cornerApprox(0);
        path.moveTo(p0.x, p0.y);
        for (int i = 1; i < count; i++) {
          FloatPoint pi = poly.cornerApprox(i);
          path.lineTo(pi.x, pi.y);
        }
        path.closePath();
      }
      return new java.awt.geom.Area(path);
    }
    return null;
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

  @Override
  public Set<Item> getNormalContacts() {
    Set<Item> result = new TreeSet<>();
    for (int i = 0; i < tileShapeCount(); i++) {
      TileShape currentShape = getTileShape(i);
      Set<SearchTreeObject> overlaps = board.overlappingObjects(currentShape, getLayer());
      for (SearchTreeObject currentObject : overlaps) {
        if (!(currentObject instanceof Item currentItem)) {
          continue;
        }
        if (currentItem != this && currentItem.sharesNet(this) && currentItem.sharesLayer(this)) {
          if (currentItem instanceof Trace currentTrace) {
            if (currentShape.contains(currentTrace.firstCorner())
                || currentShape.contains(currentTrace.lastCorner())) {
              result.add(currentItem);
            }
          } else if (currentItem instanceof DrillItem currentDrillItem) {
            if (currentShape.contains(currentDrillItem.getCenter())) {
              result.add(currentItem);
            }
          }
        }
      }
    }
    return result;
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
    this.isObstacle = value;
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
