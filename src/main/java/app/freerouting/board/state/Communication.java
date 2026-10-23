package app.freerouting.board.state;

import app.freerouting.board.actions.ItemIdGenerator;
import app.freerouting.board.model.structure.Unit;
import app.freerouting.datastructures.IdGenerator;
import app.freerouting.io.CoordinateTransform;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Communication information to host systems or host design formats. */
public class Communication implements Serializable {

  /** For coordinate transforms to a Specctra dsn file for example. */
  public final CoordinateTransform coordinateTransform;

  /** Mil, inch or mm. */
  public final Unit unit;

  /**
   * The resolution (1 / unitFactor) of the coordinate system, which is imported from the host
   * system.
   */
  public final int resolution;

  public final SpecctraParserInfo specctraParserInfo;
  public final IdGenerator idGenerator;
  public transient BoardObservers observers;

  /** Creates a new instance of BoardCommunication. */
  public Communication(
      Unit unit,
      int resolution,
      SpecctraParserInfo specctraParserInfo,
      CoordinateTransform coordinateTransform,
      IdGenerator idGenerator,
      BoardObservers observers) {
    this.coordinateTransform = coordinateTransform;
    this.unit = unit;
    this.resolution = resolution;
    this.specctraParserInfo = specctraParserInfo;
    this.idGenerator = idGenerator;
    this.observers = observers;
  }

  /** Creates a new instance of BoardCommunication. */
  public Communication() {
    this(
        Unit.MIL,
        1,
        new SpecctraParserInfo("\"", null, null, null, null, false),
        new CoordinateTransform(1, 0, 0),
        new ItemIdGenerator(),
        new BoardObserverAdaptor());
  }

  /** Checks whether the board's host CAD is Autodesk Fusion or legacy EAGLE. */
  public boolean hostCadIsFusion() {
    if (specctraParserInfo == null || specctraParserInfo.hostCad == null) {
      return false;
    }
    String cad = specctraParserInfo.hostCad.toLowerCase(Locale.ROOT);
    return cad.contains("fusion") || cad.contains("cadsoft") || cad.contains("eagle");
  }

  /** Checks whether the board's host CAD is KiCad. */
  public boolean hostCadIsKiCad() {
    if (specctraParserInfo == null || specctraParserInfo.hostCad == null) {
      return false;
    }
    return specctraParserInfo.hostCad.toLowerCase(Locale.ROOT).contains("kicad");
  }

  /** Host is old kicad. */
  public boolean hostIsOldKicad() {
    if ((specctraParserInfo == null)
        || (specctraParserInfo.hostCad == null)
        || (specctraParserInfo.hostVersion == null)) {
      return false;
    }

    if (specctraParserInfo.hostCad.toLowerCase(Locale.ROOT).contains("kicad")) {
      String versionString = specctraParserInfo.hostVersion;
      Matcher matcher = Pattern.compile("\\d+").matcher(versionString);
      if (matcher.find()) {
        int versionNumber = Integer.parseInt(matcher.group());
        return versionNumber <= 5;
      }
    }

    return false;
  }

  /** Host cad exists. */
  public boolean hostCadExists() {
    return specctraParserInfo != null && specctraParserInfo.hostCad != null;
  }

  /** Returns the number of internal board coordinates per input unit. */
  public double getResolution(Unit unit) {
    if (this.coordinateTransform != null) {
      return Unit.scale(this.coordinateTransform.dsnToBoard(1), unit, this.unit);
    }
    return Unit.scale(this.resolution, unit, this.unit);
  }

  /**
   * Returns the length of one internal board unit in {@code unit}. Uses the coordinate transform,
   * not the declared resolution: the DSN reader divides the scale by powers of ten when the
   * declared resolution would overflow int coordinates (for example LibrePCB's {@code resolution mm
   * 1000000}), so the resolution can overstate the board units per input unit.
   */
  public double boardUnitSize(Unit unit) {
    double boardUnitsPerHostUnit =
        this.coordinateTransform != null ? this.coordinateTransform.dsnToBoard(1) : this.resolution;
    return Unit.scale(1.0, this.unit, unit)
        / (boardUnitsPerHostUnit > 0 ? boardUnitsPerHostUnit : 1);
  }

  /** Converts a physical distance to internal board coordinates. */
  public double physicalToBoard(double value, Unit unit) {
    if (this.coordinateTransform != null) {
      return Unit.scale(value * this.coordinateTransform.dsnToBoard(1), unit, this.unit);
    }
    return Unit.scale(value * this.resolution, unit, this.unit);
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    observers = new BoardObserverAdaptor();
  }

  /**
   * Information from the parser scope in a Specctra-dsn-file. The fields are optional and may be
   * null.
   */
  public static class SpecctraParserInfo implements Serializable {

    /** Character for quoting strings in a dsn-File. */
    public final String stringQuote;

    public final String hostCad;
    public final String hostVersion;
    public final Collection<String[]> constants;
    public final WriteResolution writeResolution;
    public final boolean dsnFileGeneratedByHost;

    /** Creates parser metadata read from a Specctra DSN file. */
    public SpecctraParserInfo(
        String stringQuote,
        String hostCad,
        String hostVersion,
        Collection<String[]> constants,
        WriteResolution writeResolution,
        boolean dsnFileGeneratedByHost) {
      this.stringQuote = stringQuote;
      this.hostCad = hostCad;
      this.hostVersion = hostVersion;
      this.constants = constants;
      this.writeResolution = writeResolution;
      this.dsnFileGeneratedByHost = dsnFileGeneratedByHost;
    }

    /** Resolution metadata for Specctra DSN export. */
    public static class WriteResolution implements Serializable {

      public final String charName;
      public final int positiveInt;

      /** Creates a write-resolution descriptor. */
      public WriteResolution(String charName, int positiveInt) {
        this.charName = charName;
        this.positiveInt = positiveInt;
      }
    }
  }
}
