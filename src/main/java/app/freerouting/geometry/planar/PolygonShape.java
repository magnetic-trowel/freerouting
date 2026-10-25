package app.freerouting.geometry.planar;

import app.freerouting.logger.FRLogger;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Shape described bei a closed polygon of corner points. The corners are ordered in counterclock
 * sense around the border of the shape. The corners are normalised, so that the corner with the
 * lowest y-value comes first. In case of equal y-value the corner with the lowest x-value comes
 * first.
 */
public class PolygonShape extends PolylineShape {

  private static final int seed = 99;
  public final Point[] corners;

  /** Stores precalculated data for this polygon shape. */
  private transient IntBox precalculatedBoundingBox;

  private transient IntOctagon precalculatedBoundingOctagon;
  private transient TileShape[] precalculatedConvexPieces;

  /** Creates a new instance of PolygonShape. */
  public PolygonShape(Polygon polygon) {
    Polygon currentPolygon = polygon;
    if (polygon.windingNumberAfterClosing() < 0) {
      // the corners of the polygon are in clockwise sense
      currentPolygon = polygon.revertCorners();
    }
    Point[] currentCorners = currentPolygon.cornerArray();
    int lastCornerNo = currentCorners.length - 1;

    if (lastCornerNo > 0) {
      if (currentCorners[0].equals(currentCorners[lastCornerNo])) {
        // skip last point
        --lastCornerNo;
      }
    }

    boolean lastPointCollinear = false;

    if (lastCornerNo >= 2) {
      lastPointCollinear =
          currentCorners[lastCornerNo].sideOf(currentCorners[lastCornerNo - 1], currentCorners[0])
              == Side.COLLINEAR;
    }
    if (lastPointCollinear) {
      // skip last point
      --lastCornerNo;
    }

    int firstCornerNo = 0;
    boolean firstPointCollinear = false;

    if (lastCornerNo - firstCornerNo >= 2) {
      firstPointCollinear =
          currentCorners[0].sideOf(currentCorners[1], currentCorners[lastCornerNo])
              == Side.COLLINEAR;
    }

    if (firstPointCollinear) {
      // skip first point
      ++firstCornerNo;
    }
    Point[] cleaned = Arrays.copyOfRange(currentCorners, firstCornerNo, lastCornerNo + 1);
    if (cleaned.length < currentCorners.length
        && new Polygon(cleaned).windingNumberAfterClosing() < 0) {
      // A closing spike or duplicate corner changed the winding number, so the orientation taken
      // from the uncleaned corners was wrong.
      Collections.reverse(Arrays.asList(cleaned));
    }
    corners = startingAtLowestCorner(cleaned);
  }

  /** Creates a polygon shape from an array of corner points. */
  public PolygonShape(Point[] corners) {
    this(new Polygon(corners));
  }

  /** Returns the corners rotated to start with the corner with the lowest y, then lowest x. */
  private static Point[] startingAtLowestCorner(Point[] corners) {
    int startCornerNo = 0;
    for (int i = 1; i < corners.length; i++) {
      FloatPoint currentCorner = corners[i].toFloat();
      FloatPoint startCorner = corners[startCornerNo].toFloat();
      if (currentCorner.y < startCorner.y
          || currentCorner.y == startCorner.y && currentCorner.x < startCorner.x) {
        startCornerNo = i;
      }
    }
    Point[] result = new Point[corners.length];
    for (int i = 0; i < corners.length; i++) {
      result[i] = corners[(startCornerNo + i) % corners.length];
    }
    return result;
  }

  @Override
  public Point corner(int no) {
    if (no < 0 || no >= corners.length) {
      FRLogger.warn("PolygonShape.corner: no out of range");
      return null;
    }
    return corners[no];
  }

  @Override
  public int borderLineCount() {
    return corners.length;
  }

  @Override
  public boolean cornerIsBounded(int no) {
    return true;
  }

  @Override
  public boolean intersects(Shape shape) {
    if (shape instanceof PolygonShape other) {
      TileShape[] convexPieces = splitToConvex();
      TileShape[] otherPieces = other.splitToConvex();
      for (TileShape piece : convexPieces) {
        for (TileShape otherPiece : otherPieces) {
          if (piece.intersects(otherPiece)) {
            return true;
          }
        }
      }
      return false;
    }
    return shape.intersects(this);
  }

  @Override
  public boolean intersects(Circle circle) {
    TileShape[] convexPieces = splitToConvex();
    for (int i = 0; i < convexPieces.length; i++) {
      if (convexPieces[i].intersects(circle)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean intersects(Simplex simplex) {
    TileShape[] convexPieces = splitToConvex();
    for (int i = 0; i < convexPieces.length; i++) {
      if (convexPieces[i].intersects(simplex)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean intersects(IntOctagon oct) {
    TileShape[] convexPieces = splitToConvex();
    for (int i = 0; i < convexPieces.length; i++) {
      if (convexPieces[i].intersects(oct)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean intersects(IntBox box) {
    TileShape[] convexPieces = splitToConvex();
    for (int i = 0; i < convexPieces.length; i++) {
      if (convexPieces[i].intersects(box)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Polyline[] cutout(Polyline polyline) {
    FRLogger.warn("PolygonShape.cutout not yet implemented");
    return null;
  }

  @Override
  public PolygonShape enlarge(double offset) {
    if (offset == 0) {
      return this;
    }
    FRLogger.warn("PolygonShape.enlarge not yet implemented");
    return null;
  }

  @Override
  public double borderDistance(FloatPoint point) {
    if (corners.length == 0) {
      return 0;
    }
    FloatPoint prevCorner = corners[corners.length - 1].toFloat();
    double result = prevCorner.distance(point);
    for (Point corner : corners) {
      FloatPoint currentCorner = corner.toFloat();
      result = Math.min(result, new FloatLine(prevCorner, currentCorner).segmentDistance(point));
      prevCorner = currentCorner;
    }
    return result;
  }

  @Override
  public double smallestRadius() {
    return borderDistance(centreOfGravity());
  }

  @Override
  public boolean contains(FloatPoint point) {
    TileShape[] convexPieces = splitToConvex();
    for (int i = 0; i < convexPieces.length; i++) {
      if (convexPieces[i].contains(point)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean contains(Point point) {
    return !isOutside(point);
  }

  @Override
  public boolean containsInside(Point point) {
    if (containsOnBorder(point)) {
      return false;
    }
    return !isOutside(point);
  }

  @Override
  public boolean isOutside(Point point) {
    TileShape[] convexPieces = splitToConvex();
    for (int i = 0; i < convexPieces.length; i++) {
      if (!convexPieces[i].isOutside(point)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean containsOnBorder(Point point) {
    for (int i = 0; i < corners.length; i++) {
      if (liesOnSegment(point, corners[i], corners[(i + 1) % corners.length])) {
        return true;
      }
    }
    return false;
  }

  @Override
  public double distance(FloatPoint point) {
    FRLogger.warn("PolygonShape.distance not yet implemented");
    return 0;
  }

  @Override
  public PolygonShape translateBy(Vector vector) {
    if (vector.equals(Vector.ZERO)) {
      return this;
    }
    Point[] newCorners = new Point[corners.length];
    for (int i = 0; i < corners.length; i++) {
      newCorners[i] = corners[i].translateBy(vector);
    }
    return new PolygonShape(newCorners);
  }

  @Override
  public RegularTileShape boundingShape(ShapeBoundingDirections dirs) {
    return dirs.bounds(this);
  }

  @Override
  public IntBox boundingBox() {
    if (precalculatedBoundingBox == null) {
      double llx = Integer.MAX_VALUE;
      double lly = Integer.MAX_VALUE;
      double urx = Integer.MIN_VALUE;
      double ury = Integer.MIN_VALUE;
      for (int i = 0; i < corners.length; i++) {
        FloatPoint current = corners[i].toFloat();
        llx = Math.min(llx, current.x);
        lly = Math.min(lly, current.y);
        urx = Math.max(urx, current.x);
        ury = Math.max(ury, current.y);
      }
      IntPoint lowerLeft = new IntPoint((int) Math.floor(llx), (int) Math.floor(lly));
      IntPoint upperRight = new IntPoint((int) Math.ceil(urx), (int) Math.ceil(ury));
      precalculatedBoundingBox = new IntBox(lowerLeft, upperRight);
    }
    return precalculatedBoundingBox;
  }

  @Override
  public IntOctagon boundingOctagon() {
    if (precalculatedBoundingOctagon == null) {
      double lx = Integer.MAX_VALUE;
      double ly = Integer.MAX_VALUE;
      double rx = Integer.MIN_VALUE;
      double uy = Integer.MIN_VALUE;
      double ulx = Integer.MAX_VALUE;
      double lrx = Integer.MIN_VALUE;
      double llx = Integer.MAX_VALUE;
      double urx = Integer.MIN_VALUE;
      for (int i = 0; i < corners.length; i++) {
        FloatPoint current = corners[i].toFloat();
        lx = Math.min(lx, current.x);
        ly = Math.min(ly, current.y);
        rx = Math.max(rx, current.x);
        uy = Math.max(uy, current.y);

        double tmp = current.x - current.y;
        ulx = Math.min(ulx, tmp);
        lrx = Math.max(lrx, tmp);

        tmp = current.x + current.y;
        llx = Math.min(llx, tmp);
        urx = Math.max(urx, tmp);
      }
      precalculatedBoundingOctagon =
          new IntOctagon(
              (int) Math.floor(lx),
              (int) Math.floor(ly),
              (int) Math.ceil(rx),
              (int) Math.ceil(uy),
              (int) Math.floor(ulx),
              (int) Math.ceil(lrx),
              (int) Math.floor(llx),
              (int) Math.ceil(urx));
    }
    return precalculatedBoundingOctagon;
  }

  /**
   * Checks, if every line segment between 2 points of the shape is contained completely in the
   * shape.
   */
  public boolean isConvex() {
    if (corners.length <= 2) {
      return true;
    }
    Point prevPoint = corners[corners.length - 1];
    Point currentPoint = corners[0];
    Point nextPoint = corners[1];

    for (int ind = 0; ind < corners.length; ind++) {
      if (nextPoint.sideOf(prevPoint, currentPoint) == Side.ON_THE_RIGHT) {
        return false;
      }
      prevPoint = currentPoint;
      currentPoint = nextPoint;
      if (ind == corners.length - 2) {
        nextPoint = corners[0];
      } else if (ind == corners.length - 1) {
        nextPoint = corners[1];
      } else {
        nextPoint = corners[ind + 2];
      }
    }
    // check, if the sum of the interior angles is at most 2 * pi

    Line firstLine = new Line(corners[corners.length - 1], corners[0]);
    Line currentLine = new Line(corners[0], corners[1]);
    IntDirection firstDirection = (IntDirection) firstLine.direction();
    IntDirection currentDirection = (IntDirection) currentLine.direction();
    double lastDet = firstDirection.determinant(currentDirection);

    for (int ind2 = 2; ind2 < corners.length; ind2++) {
      currentLine = new Line(currentLine.b, corners[ind2]);
      currentDirection = (IntDirection) currentLine.direction();
      double currentDet = firstDirection.determinant(currentDirection);
      if (lastDet <= 0 && currentDet > 0) {
        return false;
      }
      lastDet = currentDet;
    }

    return true;
  }

  /** Returns the convex hull of this polygon shape. */
  public PolygonShape convexHull() {
    if (corners.length <= 2) {
      return this;
    }
    Point prevPoint = corners[corners.length - 1];
    Point currentPoint = corners[0];
    Point nextPoint;
    for (int ind = 0; ind < corners.length; ind++) {
      if (ind == corners.length - 1) {
        nextPoint = corners[0];
      } else {
        nextPoint = corners[ind + 1];
      }
      if (nextPoint.sideOf(prevPoint, currentPoint) != Side.ON_THE_LEFT) {
        // skip currentPoint;
        Point[] newCorners = new Point[corners.length - 1];
        System.arraycopy(corners, 0, newCorners, 0, ind);
        if (ind < newCorners.length) {
          // copy remaining elements if present
          System.arraycopy(corners, ind + 1, newCorners, ind, newCorners.length - ind);
        }
        PolygonShape result = new PolygonShape(newCorners);
        return result.convexHull();
      }
      prevPoint = currentPoint;
      currentPoint = nextPoint;
    }
    return this;
  }

  @Override
  public TileShape boundingTile() {
    PolygonShape hull = convexHull();
    Line[] boundingLines = new Line[hull.corners.length];
    for (int i = 0; i < boundingLines.length - 1; i++) {
      boundingLines[i] = new Line(hull.corners[i], hull.corners[i + 1]);
    }
    boundingLines[boundingLines.length - 1] =
        new Line(hull.corners[hull.corners.length - 1], hull.corners[0]);
    return TileShape.getInstance(boundingLines);
  }

  @Override
  public double area() {

    if (dimension() < 2) {
      return 0;
    }
    // calculate half of the absolute value of
    // x0 (y1 - yn-1) + x1 (y2 - y0) + x2 (y3 - y1) + ...+ xn-1( y0 - yn-2)
    // where xi, yi are the coordinates of the i-th corner of this polygon.

    double result = 0;
    FloatPoint prevCorner = corners[corners.length - 2].toFloat();
    FloatPoint currentCorner = corners[corners.length - 1].toFloat();
    for (int i = 0; i < corners.length; i++) {
      FloatPoint nextCorner = corners[i].toFloat();
      result += currentCorner.x * (nextCorner.y - prevCorner.y);
      prevCorner = currentCorner;
      currentCorner = nextCorner;
    }
    return 0.5 * Math.abs(result);
  }

  @Override
  public int dimension() {
    if (corners.length == 0) {
      return -1;
    }
    if (corners.length == 1) {
      return 0;
    }
    if (corners.length == 2) {
      return 1;
    }
    return 2;
  }

  @Override
  public boolean isBounded() {
    return true;
  }

  @Override
  public boolean isEmpty() {
    return corners.length == 0;
  }

  @Override
  public Line borderLine(int no) {
    if (no < 0 || no >= corners.length) {
      FRLogger.warn("PolygonShape.borderLine: no out of range");
      return null;
    }
    Point nextCorner;
    if (no == corners.length - 1) {
      nextCorner = corners[0];
    } else {
      nextCorner = corners[no + 1];
    }
    return new Line(corners[no], nextCorner);
  }

  @Override
  public FloatPoint nearestPointApprox(FloatPoint fromPoint) {
    double minDist = Double.MAX_VALUE;
    FloatPoint result = null;
    TileShape[] convexShapes = splitToConvex();
    for (int i = 0; i < convexShapes.length; i++) {
      FloatPoint currentNearestPoint = convexShapes[i].nearestPointApprox(fromPoint);
      double currentDistance = currentNearestPoint.distanceSquare(fromPoint);
      if (currentDistance < minDist) {
        minDist = currentDistance;
        result = currentNearestPoint;
      }
    }
    return result;
  }

  @Override
  public PolygonShape turn90Degree(int factor, IntPoint pole) {
    Point[] newCorners = new Point[corners.length];
    for (int i = 0; i < corners.length; i++) {
      newCorners[i] = corners[i].turn90Degree(factor, pole);
    }
    return new PolygonShape(newCorners);
  }

  @Override
  public PolygonShape rotateApprox(double angle, FloatPoint pole) {
    if (angle == 0) {
      return this;
    }
    Point[] newCorners = new Point[corners.length];
    for (int i = 0; i < corners.length; i++) {
      newCorners[i] = corners[i].toFloat().rotate(angle, pole).round();
    }
    return new PolygonShape(newCorners);
  }

  @Override
  public PolygonShape mirrorVertical(IntPoint pole) {
    Point[] newCorners = new Point[corners.length];
    for (int i = 0; i < corners.length; i++) {
      newCorners[i] = corners[i].mirrorVertical(pole);
    }
    return new PolygonShape(newCorners);
  }

  @Override
  public PolygonShape mirrorHorizontal(IntPoint pole) {
    Point[] newCorners = new Point[corners.length];
    for (int i = 0; i < corners.length; i++) {
      newCorners[i] = corners[i].mirrorHorizontal(pole);
    }
    return new PolygonShape(newCorners);
  }

  /**
   * Splits this polygon shape into convex pieces. The result is not exact, because rounded
   * intersections of lines are used in the result pieces. It can be made exact, if Polylines are
   * returned instead of Polygons, so that no intersection points are needed in the result.
   *
   * <p>The division-line split only works on a simple polygon: on a border that crosses or touches
   * itself it may fail, or worse, return pieces that do not cover the enclosed region. Such a
   * polygon is split as the region it encloses with the non-zero winding rule instead (see {@link
   * #splitEnclosedRegion}), so the result is never null.
   */
  @Override
  public TileShape[] splitToConvex() {
    if (this.precalculatedConvexPieces == null) {
      // a border with less than 3 corners encloses no region, it keeps its degenerate piece
      TileShape[] pieces = corners.length < 3 || borderIsSimple() ? splitSimple() : null;
      this.precalculatedConvexPieces = pieces != null ? pieces : splitEnclosedRegion();
    }
    return this.precalculatedConvexPieces;
  }

  /**
   * Splits this polygon with the division-line split, which requires a simple border. Returns null
   * if the split fails.
   */
  private TileShape[] splitSimple() {
    // a fixed seed per split gives a reproducible result, also across threads
    Collection<PolygonShape> convexPieces = splitToConvexRecu(new Random(seed));
    if (convexPieces == null) {
      return null;
    }
    List<TileShape> result = new ArrayList<>(convexPieces.size());
    for (PolygonShape piece : convexPieces) {
      // Rounding a division point onto a corner can leave a piece without area. As a tile its
      // degenerate border lines would bound a half-plane or a whole line instead of nothing.
      if (piece.dimension() == 2 || piece == this) {
        result.add(TileShape.getInstance(piece.corners));
      }
    }
    return result.toArray(new TileShape[0]);
  }

  /**
   * Returns true if the border of this polygon is simple: no two border lines cross or touch,
   * except consecutive lines in their common corner, and no border line runs back over the previous
   * one. The test is exact. Only border lines whose bounding boxes overlap are compared, found by a
   * sweep along one axis.
   */
  public boolean borderIsSimple() {
    int n = corners.length;
    for (int i = 0; i < n; i++) {
      Point prev = corners[(i + n - 1) % n];
      Point next = corners[(i + 1) % n];
      if (next.sideOf(prev, corners[i]) == Side.COLLINEAR
          && prev.compareX(corners[i]) == next.compareX(corners[i])
          && prev.compareY(corners[i]) == next.compareY(corners[i])) {
        // the border runs back over itself at this corner
        return false;
      }
    }
    if (n < 4) {
      return true;
    }
    // bounding boxes of the border lines from corner i to corner i + 1, widened by 1 so that the
    // rounding of rational corners cannot exclude a pair that touches
    double[] minX = new double[n];
    double[] maxX = new double[n];
    double[] minY = new double[n];
    double[] maxY = new double[n];
    double extentX = 0;
    double extentY = 0;
    for (int i = 0; i < n; i++) {
      FloatPoint a = corners[i].toFloat();
      FloatPoint b = corners[(i + 1) % n].toFloat();
      minX[i] = Math.min(a.x, b.x) - 1;
      maxX[i] = Math.max(a.x, b.x) + 1;
      minY[i] = Math.min(a.y, b.y) - 1;
      maxY[i] = Math.max(a.y, b.y) + 1;
      extentX += maxX[i] - minX[i];
      extentY += maxY[i] - minY[i];
    }
    // sweep along the axis on which the lines are shorter, so that fewer pairs overlap
    boolean sweepX = extentX <= extentY;
    double[] sweepMin = sweepX ? minX : minY;
    double[] sweepMax = sweepX ? maxX : maxY;
    double[] crossMin = sweepX ? minY : minX;
    double[] crossMax = sweepX ? maxY : maxX;
    Integer[] order = new Integer[n];
    for (int i = 0; i < n; i++) {
      order[i] = i;
    }
    Arrays.sort(order, Comparator.comparingDouble(i -> sweepMin[i]));
    for (int k = 0; k < n; k++) {
      int i = order[k];
      for (int m = k + 1; m < n && sweepMin[order[m]] <= sweepMax[i]; m++) {
        int j = order[m];
        int distance = Math.abs(i - j);
        if (distance == 1
            || distance == n - 1
            || crossMin[j] > crossMax[i]
            || crossMin[i] > crossMax[j]) {
          continue;
        }
        if (segmentsIntersect(corners[i], corners[(i + 1) % n], corners[j], corners[(j + 1) % n])) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Returns true if the closed line segments from a1 to a2 and from b1 to b2 have a common point.
   */
  private static boolean segmentsIntersect(Point a1, Point a2, Point b1, Point b2) {
    Side b1Side = b1.sideOf(a1, a2);
    Side b2Side = b2.sideOf(a1, a2);
    if (b1Side == b2Side && b1Side != Side.COLLINEAR) {
      return false;
    }
    Side a1Side = a1.sideOf(b1, b2);
    Side a2Side = a2.sideOf(b1, b2);
    if (a1Side == a2Side && a1Side != Side.COLLINEAR) {
      return false;
    }
    if (b1Side != Side.COLLINEAR || b2Side != Side.COLLINEAR) {
      return true;
    }
    // all four points are on one line
    return liesOnSegment(b1, a1, a2) || liesOnSegment(b2, a1, a2) || liesOnSegment(a1, b1, b2);
  }

  /** Returns true if point lies on the closed line segment from a to b. */
  private static boolean liesOnSegment(Point point, Point a, Point b) {
    return point.compareX(a) * point.compareX(b) <= 0
        && point.compareY(a) * point.compareY(b) <= 0
        && point.sideOf(a, b) == Side.COLLINEAR;
  }

  /**
   * Splits the region that this polygon encloses under the non-zero winding rule into convex
   * pieces, for a border that crosses or touches itself or that the division-line split fails on.
   * The region is resolved into simple outer borders and holes with {@link java.awt.geom.Area};
   * each outer border is split as a simple polygon, with the holes inside it cut out. Crossing
   * points are rounded to integer corners, as the division-line split also does. Loops that still
   * cannot be split are left out with a warning, so the result may be empty but is never null.
   */
  private TileShape[] splitEnclosedRegion() {
    Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
    for (int i = 0; i < corners.length; i++) {
      FloatPoint corner = corners[i].toFloat();
      if (i == 0) {
        path.moveTo(corner.x, corner.y);
      } else {
        path.lineTo(corner.x, corner.y);
      }
    }
    path.closePath();
    // java.awt.geom.Area orients holes against the outer borders, and the largest loop is an outer
    // border, so the sign of its area tells the two apart.
    List<List<Point>> loops = loopsOf(new java.awt.geom.Area(path));
    double outerSign = 0;
    double largestArea = 0;
    for (List<Point> loop : loops) {
      double area = signedArea(loop);
      if (Math.abs(area) > largestArea) {
        largestArea = Math.abs(area);
        outerSign = Math.signum(area);
      }
    }
    List<PolygonShape> outers = new ArrayList<>();
    List<PolygonShape> holes = new ArrayList<>();
    for (List<Point> loop : loops) {
      PolygonShape shape = new PolygonShape(loop.toArray(new Point[0]));
      if (shape.dimension() < 2) {
        continue;
      }
      if (Math.signum(signedArea(loop)) == outerSign) {
        outers.add(shape);
      } else {
        holes.add(shape);
      }
    }
    List<TileShape> result = new ArrayList<>();
    int skipped = 0;
    for (PolygonShape outer : outers) {
      // the corners are rounded, so an outer border is checked again before it is split
      TileShape[] outerPieces = outer.borderIsSimple() ? outer.splitSimple() : null;
      if (outerPieces == null) {
        ++skipped;
        continue;
      }
      outer.precalculatedConvexPieces = outerPieces;
      List<PolylineShape> holesInside = new ArrayList<>();
      for (PolygonShape hole : holes) {
        // a hole may touch its outer border in corners, but not in all of them
        if (Arrays.stream(hole.corners).anyMatch(outer::containsInside)) {
          holesInside.add(hole);
        }
      }
      TileShape[] pieces =
          holesInside.isEmpty()
              ? outerPieces
              : new PolylineArea(outer, holesInside.toArray(new PolylineShape[0])).splitToConvex();
      if (pieces == null) {
        ++skipped;
        continue;
      }
      result.addAll(Arrays.asList(pieces));
    }
    FRLogger.warn(
        "PolygonShape.split_to_convex: the border of the polygon with "
            + corners.length
            + " corners starting at "
            + corners[0]
            + " is not simple or could not be split directly; it was split as the region it"
            + " encloses"
            + (skipped > 0 ? " (" + skipped + " loop(s) left out)" : ""));
    return result.toArray(new TileShape[0]);
  }

  /** Twice the signed area of a closed loop (shoelace formula). */
  private static double signedArea(List<Point> loop) {
    double sum = 0;
    for (int i = 0; i < loop.size(); i++) {
      FloatPoint a = loop.get(i).toFloat();
      FloatPoint b = loop.get((i + 1) % loop.size()).toFloat();
      sum += a.x * b.y - b.x * a.y;
    }
    return sum;
  }

  /**
   * Returns the closed loops of the border of an area, with corners rounded to integers. A loop
   * that touches itself is split where it touches, so that the loops only meet in corners.
   */
  private static List<List<Point>> loopsOf(java.awt.geom.Area area) {
    List<List<Point>> loops = new ArrayList<>();
    List<IntPoint> loop = new ArrayList<>();
    double[] coords = new double[6];
    for (PathIterator it = area.getPathIterator(null); !it.isDone(); it.next()) {
      int type = it.currentSegment(coords);
      if (type == PathIterator.SEG_MOVETO) {
        addSplitWhereTouching(loop, loops);
        loop = new ArrayList<>();
      }
      if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
        IntPoint corner = new IntPoint((int) Math.round(coords[0]), (int) Math.round(coords[1]));
        if (loop.isEmpty() || !loop.getLast().equals(corner)) {
          loop.add(corner);
        }
      }
    }
    addSplitWhereTouching(loop, loops);
    return loops;
  }

  /**
   * Adds the closed loop through corners to loops, split into the parts between repeated visits of
   * the same point: a corner that lies on a border line of the loop is first inserted into that
   * line. Parts with less than 3 corners are left out.
   */
  private static void addSplitWhereTouching(List<IntPoint> corners, List<List<Point>> loops) {
    List<Point> open = new ArrayList<>();
    Map<Point, Integer> positions = new HashMap<>();
    int n = corners.size();
    if (n < 3) {
      return;
    }
    for (int i = 0; i <= n; i++) {
      // the closing corner is visited again at the end
      List<IntPoint> visits = new ArrayList<>();
      visits.add(corners.get(i % n));
      if (i < n) {
        visits.addAll(cornersOnSegment(corners.get(i), corners.get((i + 1) % n), corners));
      }
      for (IntPoint corner : visits) {
        Integer start = positions.putIfAbsent(corner, open.size());
        if (start == null) {
          open.add(corner);
          continue;
        }
        // the points since the last visit of this point close a loop of their own
        List<Point> closed = open.subList(start, open.size());
        if (closed.size() >= 3) {
          loops.add(new ArrayList<>(closed));
        }
        for (Point removed : closed.subList(1, closed.size())) {
          positions.remove(removed);
        }
        closed.subList(1, closed.size()).clear();
      }
    }
  }

  /**
   * Returns the corners, other than a and b, that lie on the line segment from a to b, ordered from
   * a. A corner closer than 1 to the segment counts as lying on it, because rounding the corners to
   * integers may have moved it off the segment, or across it.
   */
  private static List<IntPoint> cornersOnSegment(IntPoint a, IntPoint b, List<IntPoint> corners) {
    long dx = (long) b.x - a.x;
    long dy = (long) b.y - a.y;
    double lengthSquare = (double) dx * dx + (double) dy * dy;
    List<IntPoint> result = new ArrayList<>();
    for (IntPoint corner : corners) {
      long px = (long) corner.x - a.x;
      long py = (long) corner.y - a.y;
      double cross = (double) dx * py - (double) dy * px;
      double dot = (double) dx * px + (double) dy * py;
      if (dot > 0 && dot < lengthSquare && cross * cross < lengthSquare) {
        result.add(corner);
      }
    }
    result.sort(
        Comparator.comparingDouble(
            corner -> (double) dx * (corner.x - a.x) + (double) dy * (corner.y - a.y)));
    return result;
  }

  /** Private recursive part of split_to_convex. Returns a collection of polygon shape pieces. */
  private Collection<PolygonShape> splitToConvexRecu(Random random) {
    // start with a hashed corner and search the first concave corner
    int startCornerNo = random.nextInt(corners.length);
    Point currentCorner = corners[startCornerNo];
    Point prevCorner;
    if (startCornerNo != 0) {
      prevCorner = corners[startCornerNo - 1];
    } else {
      prevCorner = corners[corners.length - 1];
    }

    Point nextCorner;

    // search for the next concave corner from here
    int concaveCornerNo = -1;
    for (int i = 0; i < corners.length; i++) {
      if (startCornerNo < corners.length - 1) {
        nextCorner = corners[startCornerNo + 1];
      } else {
        nextCorner = corners[0];
      }
      if (nextCorner.sideOf(prevCorner, currentCorner) == Side.ON_THE_RIGHT) {
        // concave corner found
        concaveCornerNo = startCornerNo;
        break;
      }
      prevCorner = currentCorner;
      currentCorner = nextCorner;
      startCornerNo = (startCornerNo + 1) % corners.length;
    }
    Collection<PolygonShape> result = new LinkedList<>();
    if (concaveCornerNo < 0) {
      // no concave corner found, this shape is already convex
      result.add(this);
      return result;
    }
    DivisionPoint d = new DivisionPoint(concaveCornerNo);
    if (d.projection == null) {
      // projection not found, maybe polygon has selfintersections
      return null;
    }

    // construct the result pieces from polygon and the division point
    int cornerCount = d.cornerNoAfterProjection - concaveCornerNo;

    if (cornerCount < 0) {
      cornerCount += corners.length;
    }
    ++cornerCount;
    Point[] firstArr = new Point[cornerCount];
    int cornerInd = concaveCornerNo;

    for (int i = 0; i < cornerCount - 1; i++) {
      firstArr[i] = corners[cornerInd];
      cornerInd = (cornerInd + 1) % corners.length;
    }
    firstArr[cornerCount - 1] = d.roundedProjection;
    cornerCount = concaveCornerNo - d.cornerNoAfterProjection;
    if (cornerCount < 0) {
      cornerCount += corners.length;
    }
    cornerCount += 2;
    Point[] lastArr = new Point[cornerCount];
    lastArr[0] = d.roundedProjection;
    cornerInd = d.cornerNoAfterProjection;
    for (int i = 1; i < cornerCount; i++) {
      lastArr[i] = corners[cornerInd];
      cornerInd = (cornerInd + 1) % corners.length;
    }
    PolygonShape lastPiece = new PolygonShape(lastArr);
    PolygonShape firstPiece = new PolygonShape(firstArr);
    Collection<PolygonShape> c1 = firstPiece.splitToConvexRecu(random);
    if (c1 == null) {
      return null;
    }
    Collection<PolygonShape> c2 = lastPiece.splitToConvexRecu(random);
    if (c2 == null) {
      return null;
    }
    result.addAll(c1);
    result.addAll(c2);
    return result;
  }

  private class DivisionPoint {

    final int cornerNoAfterProjection;
    final FloatPoint projection;

    /**
     * The projection rounded towards the concave corner, so that the division segment stays on the
     * exact one, inside the polygon. Rounded to the nearest point it could cross the border next to
     * the projection, at a spike narrower than a unit, and leave a piece whose border runs back
     * over itself; such a piece has no concave corner and was kept as convex, which lost most of
     * its area.
     */
    final IntPoint roundedProjection;

    /**
     * At a concave corner of the closed polygon, a minimal axis parallel division line is
     * constructed, to divide the closed polygon into two.
     */
    DivisionPoint(int concaveCornerNo) {
      FloatPoint concaveCorner = corners[concaveCornerNo].toFloat();
      FloatPoint beforeConcaveCorner;

      if (concaveCornerNo != 0) {
        beforeConcaveCorner = corners[concaveCornerNo - 1].toFloat();
      } else {
        beforeConcaveCorner = corners[corners.length - 1].toFloat();
      }

      FloatPoint afterConcaveCorner;

      if (concaveCornerNo == corners.length - 1) {
        afterConcaveCorner = corners[0].toFloat();
      } else {
        afterConcaveCorner = corners[concaveCornerNo + 1].toFloat();
      }

      boolean searchRight =
          beforeConcaveCorner.y > concaveCorner.y || concaveCorner.y > afterConcaveCorner.y;

      boolean searchLeft =
          beforeConcaveCorner.y < concaveCorner.y || concaveCorner.y < afterConcaveCorner.y;

      boolean searchUp =
          beforeConcaveCorner.x < concaveCorner.x || concaveCorner.x < afterConcaveCorner.x;

      boolean searchDown =
          beforeConcaveCorner.x > concaveCorner.x || concaveCorner.x > afterConcaveCorner.x;

      double minProjectionDist = Integer.MAX_VALUE;
      FloatPoint minProjection = null;
      int cornerNoAfterMinProjection = 0;

      int cornerNoAfterCurrProjection = (concaveCornerNo + 2) % corners.length;

      Point cornerBeforeCurrProjection;
      if (cornerNoAfterCurrProjection != 0) {
        cornerBeforeCurrProjection = corners[cornerNoAfterCurrProjection - 1];
      } else {
        cornerBeforeCurrProjection = corners[corners.length - 1];
      }
      FloatPoint cornerBeforeProjectionApprox = cornerBeforeCurrProjection.toFloat();

      double currentDistance;
      int loopEnd = corners.length - 2;

      for (int i = 0; i < loopEnd; i++) {
        Point cornerAfterCurrProjection = corners[cornerNoAfterCurrProjection];
        FloatPoint cornerAfterProjectionApprox = cornerAfterCurrProjection.toFloat();
        if (cornerBeforeProjectionApprox.y != cornerAfterProjectionApprox.y) {
          // try a horizontal division
          double minY;
          double maxY;

          if (cornerAfterProjectionApprox.y > cornerBeforeProjectionApprox.y) {
            minY = cornerBeforeProjectionApprox.y;
            maxY = cornerAfterProjectionApprox.y;
          } else {
            minY = cornerAfterProjectionApprox.y;
            maxY = cornerBeforeProjectionApprox.y;
          }

          if (concaveCorner.y >= minY && concaveCorner.y <= maxY) {
            Line currentLine = new Line(cornerBeforeCurrProjection, cornerAfterCurrProjection);
            double xintersection = currentLine.functionInYValueApprox(concaveCorner.y);
            currentDistance = Math.abs(xintersection - concaveCorner.x);
            // Make sure, that the new shape will not be concave at the projection point.
            // That might happen, if the boundary curve runs back in itself.
            boolean projectionOk =
                currentDistance < minProjectionDist
                    && (searchRight
                            && xintersection > concaveCorner.x
                            && concaveCorner.y <= cornerAfterProjectionApprox.y
                        || searchLeft
                            && xintersection < concaveCorner.x
                            && concaveCorner.y >= cornerAfterProjectionApprox.y);
            if (projectionOk) {
              minProjectionDist = currentDistance;
              cornerNoAfterMinProjection = cornerNoAfterCurrProjection;
              minProjection = new FloatPoint(xintersection, concaveCorner.y);
            }
          }
        }

        if (cornerBeforeProjectionApprox.x != cornerAfterProjectionApprox.x) {
          // try a vertical division
          double minX;
          double maxX;
          if (cornerAfterProjectionApprox.x > cornerBeforeProjectionApprox.x) {
            minX = cornerBeforeProjectionApprox.x;
            maxX = cornerAfterProjectionApprox.x;
          } else {
            minX = cornerAfterProjectionApprox.x;
            maxX = cornerBeforeProjectionApprox.x;
          }
          if (concaveCorner.x >= minX && concaveCorner.x <= maxX) {
            Line currentLine = new Line(cornerBeforeCurrProjection, cornerAfterCurrProjection);
            double yintersection = currentLine.functionValueApprox(concaveCorner.x);
            currentDistance = Math.abs(yintersection - concaveCorner.y);
            // make sure, that the new shape will be convex at the projection point
            boolean projectionOk =
                currentDistance < minProjectionDist
                    && (searchUp
                            && yintersection > concaveCorner.y
                            && concaveCorner.x >= cornerAfterProjectionApprox.x
                        || searchDown
                            && yintersection < concaveCorner.y
                            && concaveCorner.x <= cornerAfterProjectionApprox.x);

            if (projectionOk) {
              minProjectionDist = currentDistance;
              cornerNoAfterMinProjection = cornerNoAfterCurrProjection;
              minProjection = new FloatPoint(concaveCorner.x, yintersection);
            }
          }
        }
        cornerBeforeCurrProjection = cornerAfterCurrProjection;
        cornerBeforeProjectionApprox = cornerAfterProjectionApprox;
        if (cornerNoAfterCurrProjection == corners.length - 1) {
          cornerNoAfterCurrProjection = 0;
        } else {
          ++cornerNoAfterCurrProjection;
        }
      }
      if (minProjectionDist == Integer.MAX_VALUE) {
        FRLogger.warn(
            String.format(
                "PolygonShape.DivisionPoint: projection not found for concave corner #%d at (%s, %s) in polygon with %d corners: %s",
                concaveCornerNo,
                concaveCorner.x,
                concaveCorner.y,
                corners.length,
                Arrays.toString(corners)));
      }

      projection = minProjection;
      cornerNoAfterProjection = cornerNoAfterMinProjection;
      roundedProjection =
          minProjection == null
              ? null
              : new IntPoint(
                  roundTowards(minProjection.x, concaveCorner.x),
                  roundTowards(minProjection.y, concaveCorner.y));
    }

    private static int roundTowards(double value, double target) {
      if (target > value) {
        return (int) Math.ceil(value);
      }
      if (target < value) {
        return (int) Math.floor(value);
      }
      return (int) Math.round(value);
    }
  }
}
