package app.freerouting.io.specctra;

import app.freerouting.geometry.planar.Direction;
import app.freerouting.geometry.planar.FloatPoint;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Limits;
import app.freerouting.geometry.planar.Line;
import app.freerouting.geometry.planar.Polyline;
import java.io.IOException;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.numbers.fraction.BigFraction;

/**
 * Restores the integer lines of a SES path from its corners.
 *
 * <p>A trace on the board is a sequence of integer lines, and its corners are their intersections,
 * which are rational. A SES path stores only the corners, as decimals. Each segment's line is the
 * integer line with the shortest direction that passes through both of its corners within their
 * serialization tolerance (a convergent of the segment's slope), and each interior corner is the
 * intersection of two such lines, exact however large its denominator. An end corner is a grid
 * point, or where another wire of the net meets the segment (the router splits a trace where a
 * branch leaves it, so each piece ends on the other's line), or else the simplest rational position
 * on the segment's line within the tolerance. Copper that no integer lines reproduce within the
 * tolerance is rejected, never moved.
 */
final class SesPathGeometry {
  /** An exact rational point. */
  private record Corner(BigFraction x, BigFraction y) {}

  /** A corner as read, in board units, and how far serialization may have moved it. */
  private record Sample(double x, double y, double tolerance) {
    BigFraction exactX() {
      return BigFraction.from(x);
    }

    BigFraction exactY() {
      return BigFraction.from(y);
    }

    Key key() {
      return new Key(x, y);
    }
  }

  /** The integer line {@code b * x - a * y = c} with the coprime direction {@code (a, b)}. */
  private record IntegerLine(BigInteger a, BigInteger b, BigInteger c) {
    boolean sameLine(IntegerLine other) {
      return a.equals(other.a) && b.equals(other.b) && c.equals(other.c)
          || a.equals(other.a.negate()) && b.equals(other.b.negate()) && c.equals(other.c.negate());
    }
  }

  /** Largest coefficient of the reduced lattice basis tried for an integer line. */
  private static final int MAX_BASIS_COEFFICIENT = 16;

  /** Directions and denominators beyond this cannot come from lines with board coordinates. */
  private static final BigInteger MAX_DENOMINATOR = BigInteger.ONE.shiftLeft(62);

  private SesPathGeometry() {}

  /** A wire's restored polyline, or why it cannot be restored. */
  record Restored(Polyline polyline, IOException failure) {}

  /** A wire's corners in board units and the integer lines of its segments. */
  private record Path(List<Sample> samples, List<IntegerLine> segments) {}

  /** The exact coordinates of a corner as read, to find the wires that meet there. */
  private record Key(double x, double y) {}

  /**
   * Restores the wires of one net. A wire usually ends where it meets another wire of its net (the
   * router splits a trace where a branch leaves it), so the line through such an end is the segment
   * line of a wire it meets.
   */
  static List<Restored> read(List<double[]> wires, double scale) {
    List<Object> paths = new ArrayList<>();
    Map<Key, List<IntegerLine>> linesAtCorner = new HashMap<>();
    for (double[] coordinates : wires) {
      try {
        Path path = path(coordinates, scale);
        for (int index = 0; index < path.segments().size(); index++) {
          IntegerLine segment = path.segments().get(index);
          for (Sample end : List.of(path.samples().get(index), path.samples().get(index + 1))) {
            linesAtCorner.computeIfAbsent(end.key(), _ -> new ArrayList<>()).add(segment);
          }
        }
        paths.add(path);
      } catch (IOException failure) {
        paths.add(failure);
      }
    }
    List<Restored> result = new ArrayList<>();
    for (Object path : paths) {
      try {
        if (path instanceof IOException failure) {
          throw failure;
        }
        result.add(new Restored(polyline((Path) path, linesAtCorner), null));
      } catch (IOException failure) {
        result.add(new Restored(null, failure));
      }
    }
    return result;
  }

  private static Path path(double[] coordinates, double scale) throws IOException {
    if (coordinates.length < 2 || coordinates.length % 2 != 0) {
      throw new IOException("A SES wire needs complete corners");
    }
    List<Sample> samples = new ArrayList<>();
    for (int index = 0; index < coordinates.length; index += 2) {
      Sample sample = sample(coordinates[index] * scale, coordinates[index + 1] * scale);
      // Point copper keeps two coincident endpoints; a repeated corner adds no copper.
      if (samples.isEmpty() || !samples.getLast().key().equals(sample.key())) {
        samples.add(sample);
      }
    }
    List<IntegerLine> segments = new ArrayList<>();
    for (int index = 1; index < samples.size(); index++) {
      IntegerLine segment = segmentLine(samples.get(index - 1), samples.get(index));
      if (!segments.isEmpty() && segments.getLast().sameLine(segment)) {
        // Collinear corners: going on keeps the same copper, turning back would lose some.
        Sample before = samples.get(index - 2);
        Sample current = samples.get(index - 1);
        Sample after = samples.get(index);
        if ((current.x() - before.x()) * (after.x() - current.x())
                + (current.y() - before.y()) * (after.y() - current.y())
            < 0) {
          throw new IOException(
              "A collinear SES reversal cannot be represented without losing copper");
        }
        samples.remove(index - 1);
        index--;
      } else {
        segments.add(segment);
      }
    }
    return new Path(samples, segments);
  }

  private static Polyline polyline(Path path, Map<Key, List<IntegerLine>> linesAtCorner)
      throws IOException {
    List<Sample> samples = path.samples();
    List<IntegerLine> segments = path.segments();
    if (samples.size() == 1) {
      Line[] crossing = crossingLines(pointCorner(samples.getFirst()));
      return new Polyline(new Line[] {crossing[0], crossing[1], crossing[0]});
    }
    Line[] lines = new Line[segments.size() + 2];
    for (int index = 0; index < segments.size(); index++) {
      lines[index + 1] = line(segments.get(index), samples.get(index));
    }
    Sample first = samples.getFirst();
    Sample last = samples.getLast();
    lines[0] = endpointLine(first, segments.getFirst(), lines[1], linesAtCorner.get(first.key()));
    lines[lines.length - 1] =
        endpointLine(
            last, segments.getLast(), lines[lines.length - 2], linesAtCorner.get(last.key()));
    Polyline polyline = new Polyline(lines);
    if (polyline.cornerCount() != samples.size()) {
      throw new IOException("A nonzero SES segment collapses at board coordinate precision");
    }
    for (int index = 1; index < samples.size(); index++) {
      if (polyline.corner(index).equals(polyline.corner(index - 1))) {
        throw new IOException("A nonzero SES segment collapses at board coordinate precision");
      }
    }
    for (int index = 0; index < samples.size(); index++) {
      FloatPoint corner = polyline.corner(index).toFloat();
      Sample sample = samples.get(index);
      if (Math.abs(corner.x - sample.x()) > 2 * sample.tolerance()
          || Math.abs(corner.y - sample.y()) > 2 * sample.tolerance()) {
        throw new IOException("The SES path cannot be represented on this board");
      }
    }
    return polyline;
  }

  static int halfWidth(double width, double scale) throws IOException {
    double value = width * scale / 2;
    double rounded = Math.rint(value);
    if (!Double.isFinite(value)
        || value <= 0
        || rounded < 1
        || rounded > Limits.CRIT_INT
        || Math.abs(value - rounded) > 4 * Math.ulp(value)) {
      throw new IOException("The SES wire width cannot be represented on this board");
    }
    return (int) rounded;
  }

  private static Sample sample(double x, double y) throws IOException {
    if (!Double.isFinite(x)
        || !Double.isFinite(y)
        || Math.abs(x) > Limits.CRIT_INT
        || Math.abs(y) > Limits.CRIT_INT) {
      throw new IOException("Non-finite or out-of-range SES wire coordinate");
    }
    // Writing and scaling round each coordinate a few times at double precision.
    return new Sample(x, y, 4 * Math.max(Math.ulp(x), Math.ulp(y)));
  }

  /**
   * Returns the integer line through both corners within their tolerance: the first convergent of
   * the segment's slope that has an integer line through the two corners.
   */
  private static IntegerLine segmentLine(Sample from, Sample to) throws IOException {
    BigFraction fromX = from.exactX();
    BigFraction fromY = from.exactY();
    BigFraction deltaX = to.exactX().subtract(fromX);
    BigFraction deltaY = to.exactY().subtract(fromY);
    BigFraction tolerance = BigFraction.from(Math.max(from.tolerance(), to.tolerance()));
    // Expand the flatter ratio, so that both a horizontal and a vertical segment end exactly.
    boolean steep = deltaY.abs().compareTo(deltaX.abs()) > 0;
    BigFraction ratio = steep ? deltaX.divide(deltaY).abs() : deltaY.divide(deltaX).abs();
    for (BigInteger[] convergent : convergents(ratio)) {
      BigInteger along = convergent[1];
      BigInteger across = convergent[0];
      BigInteger a = (steep ? across : along).multiply(BigInteger.valueOf(deltaX.signum()));
      BigInteger b = (steep ? along : across).multiply(BigInteger.valueOf(deltaY.signum()));
      if (a.abs().max(b.abs()).compareTo(MAX_DENOMINATOR) > 0) {
        break;
      }
      // b * x - a * y moves by at most (|a| + |b|) * tolerance when x and y move by tolerance.
      BigFraction slack = tolerance.multiply(a.abs().add(b.abs()));
      BigFraction offset = fromX.multiply(b).subtract(fromY.multiply(a));
      BigInteger c = offset.bigDecimalValue(0, RoundingMode.HALF_EVEN).toBigInteger();
      BigFraction toOffset = to.exactX().multiply(b).subtract(to.exactY().multiply(a));
      if (offset.subtract(c).abs().compareTo(slack) <= 0
          && toOffset.subtract(c).abs().compareTo(slack) <= 0) {
        return new IntegerLine(a, b, c);
      }
    }
    throw new IOException(
        "The SES segment has no integer supporting line; refusing to move copper");
  }

  /**
   * Returns a line that ends the trace at {@code end} on {@code segment}: through the grid point
   * there, else the segment line of another wire that meets this one there, else through the
   * simplest rational position on the segment within the corner's tolerance.
   */
  private static Line endpointLine(
      Sample end, IntegerLine segment, Line segmentLine, List<IntegerLine> linesHere)
      throws IOException {
    IntPoint base = (IntPoint) line(segment, end).a;
    BigFraction offsetX = end.exactX().subtract(base.x);
    BigFraction offsetY = end.exactY().subtract(base.y);
    BigFraction position =
        offsetX
            .multiply(segment.a())
            .add(offsetY.multiply(segment.b()))
            .divide(segment.a().pow(2).add(segment.b().pow(2)));
    // the simplest rational position whose point is within the tolerance
    Corner simplest = null;
    boolean onGrid = false;
    BigInteger sign = BigInteger.valueOf(position.signum());
    for (BigInteger[] convergent : convergents(position.abs())) {
      BigFraction steps = BigFraction.of(convergent[0].multiply(sign), convergent[1]);
      Corner candidate =
          new Corner(
              steps.multiply(segment.a()).add(base.x), steps.multiply(segment.b()).add(base.y));
      if (within(candidate, end)) {
        simplest = candidate;
        onGrid = convergent[1].equals(BigInteger.ONE);
        break;
      }
    }
    if (onGrid) {
      IntPoint point = integerPoint(simplest.x().getNumerator(), simplest.y().getNumerator());
      return Line.getInstance(point, segmentLine.direction().turn45Degree(2));
    }
    for (IntegerLine other : linesHere == null ? List.<IntegerLine>of() : linesHere) {
      Corner meeting = intersection(segment, other);
      if (meeting != null && within(meeting, end)) {
        return line(other, end);
      }
    }
    if (simplest == null) {
      throw new IOException("The SES path end cannot be restored at board coordinate precision");
    }
    Line[] crossing = crossingLines(simplest);
    return crossing[0].isParallel(segmentLine) ? crossing[1] : crossing[0];
  }

  /** Returns the intersection of two integer lines, or null if they are parallel. */
  private static Corner intersection(IntegerLine first, IntegerLine second) {
    BigInteger determinant =
        first.a().multiply(second.b()).subtract(second.a().multiply(first.b()));
    if (determinant.signum() == 0) {
      return null;
    }
    return new Corner(
        BigFraction.of(
            first.a().multiply(second.c()).subtract(second.a().multiply(first.c())), determinant),
        BigFraction.of(
            first.b().multiply(second.c()).subtract(second.b().multiply(first.c())), determinant));
  }

  private static boolean within(Corner corner, Sample sample) {
    BigFraction tolerance = BigFraction.from(sample.tolerance());
    return corner.x().subtract(sample.exactX()).abs().compareTo(tolerance) <= 0
        && corner.y().subtract(sample.exactY()).abs().compareTo(tolerance) <= 0;
  }

  /**
   * Restores point copper, which has no segment line. Its coordinates share the denominator of the
   * intersection they come from; the coordinate nearer zero carries it with more precision, so the
   * denominator found for one coordinate is tried for the other as well.
   */
  private static Corner pointCorner(Sample sample) throws IOException {
    double toleranceX = 4 * Math.ulp(sample.x());
    double toleranceY = 4 * Math.ulp(sample.y());
    BigFraction x = simplest(sample.exactX(), toleranceX);
    BigFraction y = simplest(sample.exactY(), toleranceY);
    if (x == null || y == null) {
      throw new IOException("The SES corner cannot be restored at board coordinate precision");
    }
    for (BigInteger denominator : List.of(x.getDenominator(), y.getDenominator())) {
      BigFraction sharedX = atDenominator(sample.exactX(), denominator);
      BigFraction sharedY = atDenominator(sample.exactY(), denominator);
      if (sharedX.subtract(sample.exactX()).abs().compareTo(BigFraction.from(toleranceX)) <= 0
          && sharedY.subtract(sample.exactY()).abs().compareTo(BigFraction.from(toleranceY)) <= 0) {
        return new Corner(sharedX, sharedY);
      }
    }
    return new Corner(x, y);
  }

  private static BigFraction atDenominator(BigFraction value, BigInteger denominator) {
    BigInteger numerator =
        value.multiply(denominator).bigDecimalValue(0, RoundingMode.HALF_EVEN).toBigInteger();
    return BigFraction.of(numerator, denominator);
  }

  /**
   * Returns the rational with the smallest denominator within {@code tolerance} of {@code value}
   * (the first convergent that close), or null if its denominator would be too large.
   */
  private static BigFraction simplest(BigFraction value, double tolerance) {
    BigFraction slack = BigFraction.from(tolerance);
    BigInteger sign = BigInteger.valueOf(value.signum());
    for (BigInteger[] convergent : convergents(value.abs())) {
      BigFraction candidate = BigFraction.of(convergent[0].multiply(sign), convergent[1]);
      if (candidate.subtract(value).abs().compareTo(slack) <= 0) {
        return candidate;
      }
    }
    return null;
  }

  /**
   * Returns the convergents {@code {numerator, denominator}} of the continued fraction of a
   * non-negative rational, in order, up to the rational itself or the largest denominator.
   */
  private static List<BigInteger[]> convergents(BigFraction value) {
    List<BigInteger[]> result = new ArrayList<>();
    BigInteger numerator = value.getNumerator();
    BigInteger denominator = value.getDenominator();
    BigInteger previousH = BigInteger.ZERO;
    BigInteger previousK = BigInteger.ONE;
    BigInteger h = BigInteger.ONE;
    BigInteger k = BigInteger.ZERO;
    while (denominator.signum() != 0) {
      BigInteger[] quotient = numerator.divideAndRemainder(denominator);
      BigInteger nextH = quotient[0].multiply(h).add(previousH);
      BigInteger nextK = quotient[0].multiply(k).add(previousK);
      if (nextK.compareTo(MAX_DENOMINATOR) > 0) {
        break;
      }
      result.add(new BigInteger[] {nextH, nextK});
      previousH = h;
      previousK = k;
      h = nextH;
      k = nextK;
      numerator = denominator;
      denominator = quotient[1];
    }
    return result;
  }

  /** Returns the line through the integer points of {@code segment} nearest to {@code near}. */
  private static Line line(IntegerLine segment, Sample near) throws IOException {
    return integerLine(segment.c(), segment.a(), segment.b(), near.exactX(), near.exactY());
  }

  /** Returns an integer line through {@code corner} with direction {@code (dx, dy)}, if any. */
  private static Line cornerLine(Corner corner, BigInteger dx, BigInteger dy) throws IOException {
    BigInteger divisor = dx.gcd(dy);
    dx = dx.divide(divisor);
    dy = dy.divide(divisor);
    BigFraction intercept = corner.x.multiply(dy).subtract(corner.y.multiply(dx));
    if (!intercept.getDenominator().equals(BigInteger.ONE)) {
      throw new IOException("The SES corner has no integer line in this direction");
    }
    return integerLine(intercept.getNumerator(), dx, dy, corner.x, corner.y);
  }

  /**
   * Returns the line {@code dy * x - dx * y = constant} through its two integer points nearest to
   * {@code (nearX, nearY)}; {@code (dx, dy)} is coprime.
   */
  private static Line integerLine(
      BigInteger constant, BigInteger dx, BigInteger dy, BigFraction nearX, BigFraction nearY)
      throws IOException {
    BigInteger x;
    BigInteger y;
    if (dx.signum() == 0) {
      x = constant.divide(dy);
      y = nearY.bigDecimalValue(0, RoundingMode.HALF_UP).toBigInteger();
    } else if (dy.signum() == 0) {
      x = nearX.bigDecimalValue(0, RoundingMode.HALF_UP).toBigInteger();
      y = constant.negate().divide(dx);
    } else {
      // Coprime direction components define the line's integer lattice; the modular inverse
      // finds one of its points.
      x = constant.multiply(dy.mod(dx.abs()).modInverse(dx.abs())).mod(dx.abs());
      y = dy.multiply(x).subtract(constant).divide(dx);
      BigInteger steps =
          nearX.subtract(x).divide(dx).bigDecimalValue(0, RoundingMode.HALF_UP).toBigInteger();
      x = x.add(steps.multiply(dx));
      y = y.add(steps.multiply(dy));
    }
    return new Line(integerPoint(x, y), integerPoint(x.add(dx), y.add(dy)));
  }

  private record LatticeVector(BigInteger x, BigInteger y) {
    BigInteger dot(LatticeVector other) {
      return x.multiply(other.x).add(y.multiply(other.y));
    }

    LatticeVector times(long factor) {
      return new LatticeVector(
          x.multiply(BigInteger.valueOf(factor)), y.multiply(BigInteger.valueOf(factor)));
    }

    LatticeVector subtract(LatticeVector other, BigInteger multiple) {
      return new LatticeVector(
          x.subtract(other.x.multiply(multiple)), y.subtract(other.y.multiply(multiple)));
    }
  }

  private static Line[] crossingLines(Corner corner) throws IOException {
    BigInteger denominator = commonDenominator(corner.x, corner.y);
    BigInteger x = corner.x.multiply(denominator).getNumerator();
    BigInteger y = corner.y.multiply(denominator).getNumerator();
    if (denominator.equals(BigInteger.ONE)) {
      IntPoint point = integerPoint(x, y);
      return new Line[] {
        Line.getInstance(point, Direction.RIGHT), Line.getInstance(point, Direction.UP)
      };
    }
    // Integer supporting directions satisfy y*dx - x*dy = 0 (mod denominator).
    // A basis of that congruence lattice avoids treating the intersection denominator as a
    // coordinate bound. Reduce only this SES representation, never the router's geometry.
    BigInteger divisor = y.gcd(denominator);
    BigInteger horizontal = denominator.divide(divisor);
    BigInteger residue =
        x.multiply(y.divide(divisor).mod(horizontal).modInverse(horizontal)).mod(horizontal);
    LatticeVector first = new LatticeVector(horizontal, BigInteger.ZERO);
    LatticeVector second = new LatticeVector(residue, divisor);
    boolean reduced = false;
    for (int iteration = 0; iteration < 128; iteration++) {
      if (first.dot(first).compareTo(second.dot(second)) > 0) {
        LatticeVector swap = first;
        first = second;
        second = swap;
      }
      BigInteger multiple =
          BigFraction.of(first.dot(second), first.dot(first))
              .bigDecimalValue(0, RoundingMode.HALF_EVEN)
              .toBigInteger();
      if (multiple.signum() == 0) {
        reduced = true;
        break;
      }
      second = second.subtract(first, multiple);
    }
    if (!reduced) {
      throw new IOException("SES supporting-line reduction did not converge");
    }
    Line firstLine = null;
    IOException lastFailure = null;
    // A lattice vector is an integer line through the corner only if its components are coprime:
    // a multiple of a direction that is not in the lattice reduces to that direction, whose line
    // has no integer point (the vertical line through x=1/3). With a composite denominator every
    // short basis combination can be such a multiple (denominator 3522 = 2 * 3 * 587 and the basis
    // (1, 1), (1761, -1761)), so try the combinations in order of their coefficients' size.
    for (int size = 1; size <= MAX_BASIS_COEFFICIENT; size++) {
      for (int i = -size; i <= size; i++) {
        for (int j = 0; j <= size; j++) {
          if (Math.max(Math.abs(i), j) != size || (j == 0 && i < 0)) {
            continue; // each direction once, up to its sign
          }
          LatticeVector direction = first.times(i).subtract(second, BigInteger.valueOf(-j));
          try {
            Line candidate = cornerLine(corner, direction.x, direction.y);
            if (firstLine == null) {
              firstLine = candidate;
            } else if (!firstLine.isParallel(candidate)) {
              return new Line[] {firstLine, candidate};
            }
          } catch (IOException unsupportedDirection) {
            lastFailure = unsupportedDirection;
          }
        }
      }
    }
    throw new IOException("The SES corner has no supported pair of integer lines", lastFailure);
  }

  private static BigInteger commonDenominator(BigFraction x, BigFraction y) {
    BigInteger dx = x.getDenominator();
    BigInteger dy = y.getDenominator();
    return dx.divide(dx.gcd(dy)).multiply(dy);
  }

  private static IntPoint integerPoint(BigInteger x, BigInteger y) throws IOException {
    if (x.abs().compareTo(Limits.CRIT_INT_BIG) > 0 || y.abs().compareTo(Limits.CRIT_INT_BIG) > 0) {
      throw new IOException(
          "SES supporting-line coordinates exceed the board's exact integer range");
    }
    return new IntPoint(x.intValueExact(), y.intValueExact());
  }
}
