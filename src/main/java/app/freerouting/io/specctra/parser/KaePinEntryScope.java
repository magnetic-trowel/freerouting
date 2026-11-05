package app.freerouting.io.specctra.parser;

import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.structure.Component;
import app.freerouting.geometry.planar.FloatPoint;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.logger.FRLogger;
import app.freerouting.rules.KaePinEntries;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the DSN scope {@code kae_pin_entries} of kicad-auto-everything.
 *
 * <p>The scope comes at the end of the {@code pcb} scope (after the placement, network and wiring,
 * so the pins exist):
 *
 * <pre>
 * (kae_pin_entries (version 1) [(mode plan)]
 *   (pin REF PIN (layer L) (centre x y) (frame ux uy long short cx cy) [(radial)]
 *     (entry KIND (width W) [(neck N)] (approach x y) (end x y)) ...)
 *   (class_floor CLASS (width W) (layer_rule L (width W)) ...))
 * </pre>
 *
 * <p>Coordinates and widths are in DSN units. The table goes to {@code BoardRules.kaePinEntries}
 * and stays unused until {@code router.kae_pin_entries} is on. Builds without this class skip the
 * scope as an unknown one ({@link ScopeKeyword#readScope}). A pin, layer or class the board does
 * not know is skipped with a warning.
 */
public final class KaePinEntryScope {

  public static final String NAME = "kae_pin_entries";

  private KaePinEntryScope() {}

  /** Whether a scope's first token names this scope. */
  static boolean isScope(Object token) {
    return token instanceof String s && s.equalsIgnoreCase(NAME);
  }

  /** Reads the scope (its name already read). Returns false on a read error. */
  static boolean read(ReadScopeParameter p) {
    List<Object> body;
    try {
      body = readList(p.scanner);
    } catch (IOException e) {
      FRLogger.error("KaePinEntryScope.read: IO error scanning file", e);
      return false;
    }
    if (body == null) {
      return false;
    }
    BasicBoard board = p.getBoard();
    if (board == null || p.coordinateTransform == null) {
      FRLogger.warn("kae_pin_entries: no board yet, scope ignored");
      return true;
    }
    KaePinEntries table =
        interpret(
            body,
            board,
            p.coordinateTransform::dsnToBoard,
            v -> p.coordinateTransform.dsnToBoard(v));
    board.rules.kaePinEntries = table;
    FRLogger.info(
        "kae_pin_entries: read "
            + table.pinLayers().size()
            + " pin layers, "
            + table.classFloorCount()
            + " class floors");
    return true;
  }

  /** Point and length transforms from DSN units to board units. */
  interface PointTransform {
    FloatPoint apply(double[] tuple);
  }

  interface LengthTransform {
    double apply(double value);
  }

  /**
   * Builds the table from the scope's parsed body (lists of atoms; atoms are Strings or Numbers).
   */
  static KaePinEntries interpret(
      List<Object> body, BasicBoard board, PointTransform point, LengthTransform length) {
    KaePinEntries table = new KaePinEntries();
    Map<String, Pin> pins = new LinkedHashMap<>();
    for (Pin pin : board.getPins()) {
      Component c = board.components.get(pin.getComponentId());
      String name = pin.name();
      if (c != null && name != null) {
        pins.putIfAbsent(c.name + "\u0000" + name, pin);
      }
    }
    int skipped = 0;
    for (Object o : body) {
      if (!(o instanceof List<?> node) || node.isEmpty()) {
        continue;
      }
      String head = atom(node.get(0));
      if ("mode".equalsIgnoreCase(head) && node.size() >= 2) {
        table.setPlanned("plan".equalsIgnoreCase(atom(node.get(1))));
      } else if ("pin".equalsIgnoreCase(head) && node.size() >= 3) {
        Pin pin = pins.get(atom(node.get(1)) + "\u0000" + atom(node.get(2)));
        List<?> layerNode = sub(node, "layer");
        int layer = layerNode == null ? -1 : board.layerStructure.getNo(atom(layerNode.get(1)));
        List<?> frame = sub(node, "frame");
        if (pin == null || layer < 0 || frame == null || frame.size() < 5) {
          ++skipped;
          continue;
        }
        IntPoint centre =
            pin.getCenter() instanceof IntPoint ip ? ip : pin.getCenter().toFloat().round();
        List<KaePinEntries.Entry> entries = new ArrayList<>();
        for (Object e : node) {
          if (!(e instanceof List<?> en)
              || en.size() < 2
              || !"entry".equalsIgnoreCase(atom(en.get(0)))) {
            continue;
          }
          List<?> w = sub(en, "width");
          List<?> a = sub(en, "approach");
          List<?> end = sub(en, "end");
          List<?> neck = sub(en, "neck");
          if (w == null || a == null || end == null) {
            continue;
          }
          int halfWidth = (int) Math.round(length.apply(num(w.get(1))) / 2);
          int neckHalf = neck == null ? 0 : (int) Math.round(length.apply(num(neck.get(1))) / 2);
          entries.add(
              new KaePinEntries.Entry(
                  atom(en.get(1)),
                  point.apply(new double[] {num(a.get(1)), num(a.get(2))}).round(),
                  point.apply(new double[] {num(end.get(1)), num(end.get(2))}).round(),
                  halfWidth,
                  neckHalf));
        }
        if (entries.isEmpty()) {
          continue;
        }
        FloatPoint fc =
            frame.size() >= 7
                ? point.apply(new double[] {num(frame.get(5)), num(frame.get(6))})
                : centre.toFloat();
        // the DSN's y axis is the board's (both up), so the axis is taken as written
        table.add(
            new KaePinEntries.PinLayer(
                pin.getId(),
                layer,
                centre,
                fc.x,
                fc.y,
                num(frame.get(1)),
                num(frame.get(2)),
                length.apply(num(frame.get(3))),
                length.apply(num(frame.get(4))),
                sub(node, "radial") != null,
                entries));
      } else if ("class_floor".equalsIgnoreCase(head) && node.size() >= 3) {
        List<?> w = sub(node, "width");
        if (w == null) {
          continue;
        }
        int n = board.getLayerCount();
        int[] half = new int[n];
        java.util.Arrays.fill(half, (int) Math.ceil(length.apply(num(w.get(1))) / 2 - 1e-6));
        for (Object lr : node) {
          if (lr instanceof List<?> l
              && l.size() >= 3
              && "layer_rule".equalsIgnoreCase(atom(l.get(0)))) {
            int layer = board.layerStructure.getNo(atom(l.get(1)));
            List<?> lw = sub(l, "width");
            if (layer >= 0 && lw != null) {
              half[layer] = (int) Math.ceil(length.apply(num(lw.get(1))) / 2 - 1e-6);
            }
          }
        }
        table.setClassFloor(atom(node.get(1)), half);
      }
    }
    if (skipped > 0) {
      FRLogger.warn(
          "kae_pin_entries: " + skipped + " pins or layers not on the board were skipped");
    }
    return table;
  }

  /** Reads tokens up to the closing bracket of the current scope into nested lists. */
  static List<Object> readList(IJFlexScanner scanner) throws IOException {
    List<Object> out = new ArrayList<>();
    for (; ; ) {
      Object t = scanner.nextToken();
      if (t == null) {
        return null;
      }
      if (t == Keyword.OPEN_BRACKET) {
        List<Object> sub = readList(scanner);
        if (sub == null) {
          return null;
        }
        out.add(sub);
      } else if (t == Keyword.CLOSED_BRACKET) {
        return out;
      } else if (t instanceof Keyword k) {
        out.add(k.getName());
      } else {
        out.add(t);
      }
    }
  }

  private static String atom(Object o) {
    if (o instanceof Double d && d == Math.rint(d) && !Double.isInfinite(d)) {
      return Long.toString(d.longValue());
    }
    return String.valueOf(o);
  }

  private static double num(Object o) {
    if (o instanceof Number n) {
      return n.doubleValue();
    }
    return Double.parseDouble(String.valueOf(o));
  }

  private static List<?> sub(List<?> node, String head) {
    for (Object o : node) {
      if (o instanceof List<?> l && !l.isEmpty() && head.equalsIgnoreCase(atom(l.get(0)))) {
        return l;
      }
    }
    return null;
  }
}
