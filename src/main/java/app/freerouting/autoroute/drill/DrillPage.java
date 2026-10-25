package app.freerouting.autoroute.drill;

import app.freerouting.autoroute.AutorouteDiagnostic;
import app.freerouting.autoroute.expansion.CompleteExpansionRoom;
import app.freerouting.autoroute.expansion.ExpandableObject;
import app.freerouting.autoroute.maze.AutorouteEngine;
import app.freerouting.autoroute.maze.MazeSearchElement;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.searchtree.ShapeSearchTree;
import app.freerouting.datastructures.ShapeTree.TreeEntry;
import app.freerouting.geometry.planar.IntBox;
import app.freerouting.geometry.planar.Point;
import app.freerouting.geometry.planar.PolylineArea;
import app.freerouting.geometry.planar.RegularTileShape;
import app.freerouting.geometry.planar.TileShape;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class DrillPage implements ExpandableObject {

  /** The shape of the page. */
  public final IntBox shape;

  private final MazeSearchElement[] mazeSearchElements;
  private final RoutingBoard board;

  /** The list of expansion drills on this page. Null, if not yet calculated. */
  private Collection<ExpansionDrill> drills;

  /** The number of the net, for which the drills are calculated. */
  private int netNumber = -1;

  /** Creates a new instance of DrillPage. */
  public DrillPage(IntBox shape, RoutingBoard board) {
    this.shape = shape;
    this.board = board;
    mazeSearchElements = new MazeSearchElement[board.getLayerCount()];
    for (int i = 0; i < mazeSearchElements.length; i++) {
      mazeSearchElements[i] = new MazeSearchElement();
    }
  }

  /**
   * Looks if drillShape contains the center of a drillable Pin on layer. Returns null if no. such
   * Pin was found.
   */
  private static Point calcPinCenterInDrill(TileShape drillShape, int layer, RoutingBoard board) {
    Collection<Item> overlappingItems = board.overlappingItems(drillShape, layer);
    Point result = null;
    for (Item currentItem : overlappingItems) {
      if (currentItem instanceof Pin currentPin) {
        if (currentPin.drillAllowed() && drillShape.containsInside(currentPin.getCenter())) {
          result = currentPin.getCenter();
        }
      }
    }
    return result;
  }

  /** Returns the drills on this page. If attachSmd, drilling to smd pins is allowed. */
  public Collection<ExpansionDrill> getDrills(AutorouteEngine autorouteEngine, boolean attachSmd) {
    if (this.drills == null || autorouteEngine.getNetNumber() != this.netNumber) {
      this.netNumber = autorouteEngine.getNetNumber();
      this.drills = new LinkedList<>();
      ShapeSearchTree searchTree = autorouteEngine.autorouteSearchTree;
      Collection<TreeEntry> overlaps = new LinkedList<>();
      searchTree.overlappingTreeEntries(this.shape, -1, overlaps);
      Collection<TileShape> cutoutShapes = new LinkedList<>();
      // drills on top of existing vias are used in the ripup algorithm
      TileShape prevObstacleShape = IntBox.EMPTY;
      for (TreeEntry currentEntry : overlaps) {
        if (!(currentEntry.object instanceof Item currentItem)) {
          continue;
        }
        if (currentItem.isDrillable(this.netNumber)) {
          continue;
        }
        if (currentItem instanceof Pin pin) {
          if (attachSmd && pin.drillAllowed() && pin.containsNet(this.netNumber)) {
            continue;
          }
        }
        TileShape currentObstacleShape =
            currentItem.getTreeShape(searchTree, currentEntry.shapeIndexInObject);
        if (!prevObstacleShape.contains(currentObstacleShape)) {
          // Checked to avoid multiple cutout for example for vias with the same shape on all
          // layers.
          TileShape currentCutoutShape = currentObstacleShape.intersection(this.shape);
          if (currentCutoutShape.dimension() == 2) {
            cutoutShapes.add(currentCutoutShape);
          }
        }
        prevObstacleShape = currentObstacleShape;
      }
      TileShape[] holes = new TileShape[cutoutShapes.size()];
      Iterator<TileShape> it = cutoutShapes.iterator();
      for (int i = 0; i < holes.length; i++) {
        holes[i] = it.next();
      }
      PolylineArea shapeWithHoles = new PolylineArea(this.shape, holes);
      TileShape[] drillShapes = shapeWithHoles.splitToConvex(autorouteEngine.stoppableThread);
      if (drillShapes == null) {
        return this.drills;
      }
      drillShapes = mergeConvexPieces(drillShapes);

      // Use the center points of these drill shapes to try making a via.
      int drillFirstLayer = 0;
      int drillLastLayer = this.board.getLayerCount() - 1;
      for (int i = 0; i < drillShapes.length; i++) {
        TileShape currentDrillShape = drillShapes[i];
        Point pinCenter = null;
        if (attachSmd) {
          pinCenter =
              calcPinCenterInDrill(currentDrillShape, drillFirstLayer, autorouteEngine.board);
          if (pinCenter == null) {
            pinCenter =
                calcPinCenterInDrill(currentDrillShape, drillLastLayer, autorouteEngine.board);
          }
        }
        Point currentDrillLocation = pinCenter;
        if (currentDrillLocation == null) {
          currentDrillLocation = currentDrillShape.centreOfGravity().round();
        }
        ExpansionDrill newDrill =
            new ExpansionDrill(
                currentDrillShape, currentDrillLocation, drillFirstLayer, drillLastLayer);
        if (newDrill.calculateExpansionRooms(autorouteEngine)) {
          this.drills.add(newDrill);
        }
        if (pinCenter != null) {
          Point centreOfGravity = currentDrillShape.centreOfGravity().round();
          if (!centreOfGravity.equals(pinCenter)) {
            ExpansionDrill cogDrill =
                new ExpansionDrill(
                    currentDrillShape, centreOfGravity, drillFirstLayer, drillLastLayer);
            if (cogDrill.calculateExpansionRooms(autorouteEngine)) {
              this.drills.add(cogDrill);
            }
          }
        }
      }
    }
    return this.drills;
  }

  /**
   * Merges pieces whose union is convex. Cutting the holes out of the page one after another leaves
   * many small pieces whose layout depends on the order of the holes; a via candidate is taken in
   * every piece, so merging them gives fewer candidates, further away from the obstacles. The
   * pieces are scanned in their given order, so the result is deterministic.
   */
  static TileShape[] mergeConvexPieces(TileShape[] pieces) {
    List<TileShape> result = new ArrayList<>(Arrays.asList(pieces));
    for (int i = 0; i < result.size(); i++) {
      for (int j = i + 1; j < result.size(); j++) {
        TileShape union = convexUnion(result.get(i), result.get(j));
        if (union != null) {
          result.set(i, union);
          result.remove(j);
          j = i;
        }
      }
    }
    return result.toArray(new TileShape[0]);
  }

  /** Returns the union of first and second if it is convex, otherwise null. */
  private static TileShape convexUnion(TileShape first, TileShape second) {
    if (!(first instanceof RegularTileShape regularFirst)
        || !(second instanceof RegularTileShape regularSecond)) {
      return null;
    }
    TileShape overlap = first.intersection(second);
    if (overlap.dimension() < 1) {
      // disjoint or touching only at a corner
      return null;
    }
    double overlapArea = overlap.dimension() == 2 ? overlap.area() : 0;
    TileShape hull = regularFirst.union(regularSecond);
    // The bounding octagon of two octagons is their union exactly when no area is added.
    if (Math.abs(hull.area() - (first.area() + second.area() - overlapArea)) > 1e-3) {
      return null;
    }
    return hull.simplify();
  }

  @Override
  public TileShape getShape() {
    return this.shape;
  }

  @Override
  public int getDimension() {
    return 2;
  }

  @Override
  public int mazeSearchElementCount() {
    return this.mazeSearchElements.length;
  }

  @Override
  public MazeSearchElement getMazeSearchElement(int index) {
    return this.mazeSearchElements[index];
  }

  /** Resets all drills of this page for autorouting the next connection. */
  @Override
  public void reset() {
    if (this.drills != null) {
      for (ExpansionDrill currentDrill : this.drills) {
        currentDrill.reset();
      }
    }
    for (MazeSearchElement currentInfo : mazeSearchElements) {
      currentInfo.reset();
    }
  }

  /**
   * Invalidates the drills of this page so that they are recalculated at the next call of
   * getDrills().
   */
  public void invalidate() {
    this.drills = null;
  }

  /** Emits optional diagnostics for the drills on this page. */
  public void emitDiagnostics(AutorouteDiagnostic.Sink sink, double intensity) {
    if (sink == null || intensity <= 0 || drills == null) {
      return;
    }
    for (ExpansionDrill currentDrill : drills) {
      currentDrill.emitDiagnostic(sink, intensity);
    }
  }

  @Override
  public CompleteExpansionRoom otherRoom(CompleteExpansionRoom room) {
    return null;
  }

  @Override
  public int getId() {
    // Stable hash of shape and netNumber
    return 31 * shape.getId() + netNumber;
  }
}
