package app.freerouting.rules;

import app.freerouting.geometry.planar.IntPoint;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pad entries planned outside Freerouting (kicad-auto-everything's rule D), read from the DSN scope
 * {@code kae_pin_entries}, and per-class minimum trace widths.
 *
 * <p>For each pin and layer the table lists where a trace should arrive (the approach point, a stub
 * outside the pad outline) and where the planner ends it (on the pad's long centre line).
 * Freerouting's connection model needs the trace end at the pin centre, so the tail it draws is
 * approach, end, centre: a fixed piece the tightener tightens up to. The router never re-derives
 * these points; the table is the one copy of the rule.
 *
 * <p>Used only when {@code router.kae_pin_entries} is on ({@link #isEnabled()}). Lookups are by pin
 * item id and layer; nothing iterates a hash map in a routing decision, so results stay
 * deterministic: entries keep their DSN order.
 */
public final class KaePinEntries implements Serializable {

  /** One planned entry: the approach point, the end, the tail half width and its neck. */
  public static final class Entry implements Serializable {
    public final String kind;
    public final IntPoint approach;
    public final IntPoint end;

    /** Half width of the trace the entry was planned for, in board units. */
    public final int halfWidth;

    /** Half width of the tail when it is necked, 0 when it is not. */
    public final int neckHalfWidth;

    public Entry(String kind, IntPoint approach, IntPoint end, int halfWidth, int neckHalfWidth) {
      this.kind = kind;
      this.approach = approach;
      this.end = end;
      this.halfWidth = halfWidth;
      this.neckHalfWidth = neckHalfWidth;
    }
  }

  /** The entries of one pin on one layer, with the pad's frame (its smallest rectangle). */
  public static final class PinLayer implements Serializable {
    public final int pinId;
    public final int layer;
    public final IntPoint centre;

    /** Centre of the pad's frame (its smallest rotated rectangle), in board units. */
    public final double frameX;

    public final double frameY;

    /** Unit vector of the pad's long axis. */
    public final double ux;

    public final double uy;

    /** Long and short side of the pad's frame, in board units. */
    public final double frameLong;

    public final double frameShort;

    /** A round land: any centre-aimed approach is normal to its outline. */
    public final boolean radial;

    public final List<Entry> entries;

    public PinLayer(
        int pinId,
        int layer,
        IntPoint centre,
        double frameX,
        double frameY,
        double ux,
        double uy,
        double frameLong,
        double frameShort,
        boolean radial,
        List<Entry> entries) {
      this.pinId = pinId;
      this.layer = layer;
      this.centre = centre;
      this.frameX = frameX;
      this.frameY = frameY;
      this.ux = ux;
      this.uy = uy;
      this.frameLong = frameLong;
      this.frameShort = frameShort;
      this.radial = radial;
      this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }
  }

  /**
   * Ends whose tail could not be made, by trace id and end, with a hash of the trace's geometry:
   * pull-tight does not try the same geometry again. Looked up, never iterated.
   */
  public final Map<Long, Integer> failedTails = new HashMap<>();

  private boolean enabled;
  private final List<PinLayer> pinLayers = new ArrayList<>();
  private final Map<Long, PinLayer> byPinLayer = new HashMap<>();
  private final Map<String, int[]> classFloors = new LinkedHashMap<>();

  private static long key(int pinId, int layer) {
    return ((long) pinId << 8) | (layer & 0xff);
  }

  /** Adds the entries of a pin on a layer; a second table for the same pin and layer is ignored. */
  public void add(PinLayer pinLayer) {
    long k = key(pinLayer.pinId, pinLayer.layer);
    if (!byPinLayer.containsKey(k)) {
      byPinLayer.put(k, pinLayer);
      pinLayers.add(pinLayer);
    }
  }

  /** The entries of a pin on a layer, or null. */
  public PinLayer get(int pinId, int layer) {
    return byPinLayer.get(key(pinId, layer));
  }

  /** All pin layers, in DSN order. */
  public List<PinLayer> pinLayers() {
    return Collections.unmodifiableList(pinLayers);
  }

  /** Sets the minimum trace half width per layer of a net class (by class name). */
  public void setClassFloor(String className, int[] halfWidthPerLayer) {
    classFloors.put(className, halfWidthPerLayer.clone());
  }

  /** The minimum trace half width of a net class on a layer, 0 when it has none. */
  public int classFloor(String className, int layer) {
    int[] f = className == null ? null : classFloors.get(className);
    if (f == null || layer < 0 || layer >= f.length) {
      return 0;
    }
    return f[layer];
  }

  public int classFloorCount() {
    return classFloors.size();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /** The table of a board when it is in use, else null. */
  public static KaePinEntries active(BoardRules rules) {
    KaePinEntries t = rules == null ? null : rules.kaePinEntries;
    return t != null && t.enabled ? t : null;
  }

  /** Whether the table governs this pin's ends on this layer (upstream exit rules stay off). */
  public static boolean covers(BoardRules rules, int pinId, int layer) {
    KaePinEntries t = active(rules);
    return t != null && t.get(pinId, layer) != null;
  }

  /** The class floor of a net on a layer, 0 when the table is off or the class has none. */
  public static int floorHalfWidth(BoardRules rules, int netNumber, int layer) {
    KaePinEntries t = active(rules);
    if (t == null || t.classFloors.isEmpty() || rules.nets == null) {
      return 0;
    }
    Net net = rules.nets.get(netNumber);
    if (net == null || net.getNetClass() == null) {
      return 0;
    }
    return t.classFloor(net.getNetClass().getName(), layer);
  }
}
