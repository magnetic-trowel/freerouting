package app.freerouting.autoroute.pipeline;

import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.items.Via;
import app.freerouting.board.model.structure.Component;
import app.freerouting.core.library.Package;
import app.freerouting.drc.AirLine;
import app.freerouting.drc.DesignRulesChecker;
import app.freerouting.drc.PlaneFillConnectivity;
import app.freerouting.rules.Net;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Formats the diagnostic reports about connections the routed board leaves open. */
final class AutorouteUnroutedReport {

  private AutorouteUnroutedReport() {}

  static String build(RoutingBoard board) {
    DesignRulesChecker tempDrc = new DesignRulesChecker(board, null);
    tempDrc.calculateAllIncompletes();
    AirLine[] airlines = tempDrc.getAllAirlines();

    if (airlines == null || airlines.length == 0) {
      return "  (no unrouted connections found)";
    }

    Map<String, List<String>> byNet = new LinkedHashMap<>();
    for (AirLine airline : airlines) {
      String netName = airline.net != null ? airline.net.name : "(unknown net)";
      String fromDesc = describeItem(board, airline.fromItem);
      String toDesc = describeItem(board, airline.toItem);
      byNet
          .computeIfAbsent(netName, k -> new java.util.ArrayList<>())
          .add("    - " + fromDesc + "  ->  " + toDesc);
    }

    StringBuilder result = new StringBuilder();
    for (Map.Entry<String, List<String>> entry : byNet.entrySet()) {
      int count = entry.getValue().size();
      result
          .append("  Net '")
          .append(entry.getKey())
          .append("' (")
          .append(count)
          .append(" unrouted connection")
          .append(count == 1 ? "" : "s")
          .append("):\n");
      for (String line : entry.getValue()) {
        result.append(line).append('\n');
      }
    }
    return result.toString().stripTrailing();
  }

  /**
   * Returns a report of the pads and vias that the board counts as connected only through a pour
   * polygon, although foreign copper cuts them off from the pour's fill, or null if there are none.
   * The router does not see these open connections: it routes nothing to them, and the ratsnest and
   * the session file show the net as complete.
   */
  static String planeFillCutOffs(RoutingBoard board) {
    StringBuilder result = new StringBuilder();
    int itemCount = 0;
    for (int netNumber : PlaneFillConnectivity.pourNets(board)) {
      PlaneFillConnectivity.Result connectivity = PlaneFillConnectivity.analyze(board, netNumber);
      if (connectivity.cutOffGroups.isEmpty()) {
        continue;
      }
      Net net = board.rules.nets.get(netNumber);
      result.append("\n  Net '").append(net != null ? net.name : "#" + netNumber).append("':");
      for (Set<Item> group : connectivity.cutOffGroups) {
        result.append("\n    - ");
        String separator = "";
        for (Item item : group) {
          if (item instanceof Pin || item instanceof Via) {
            result.append(separator).append(describeItem(board, item));
            separator = ", ";
            itemCount++;
          }
        }
        if (separator.isEmpty()) {
          result.append(group.size()).append(" trace(s) from ").append(group.iterator().next());
        }
      }
    }
    if (result.isEmpty()) {
      return null;
    }
    return itemCount
        + " pin(s) or via(s) are cut off from the fill of their pour by foreign tracks and count as"
        + " connected only through the pour polygon; they stay unconnected when the pour is"
        + " refilled:"
        + result;
  }

  /**
   * Returns a short, user-friendly description of a board item suitable for the stagnation report.
   * For pins the format is {@code ComponentName-PinName}; all other item types use a generic form.
   */
  private static String describeItem(RoutingBoard board, Item item) {
    if (item instanceof Pin pin) {
      try {
        Component component = board.components.get(pin.getComponentId());
        if (component != null) {
          Package componentPackage = component.getPackage();
          if (componentPackage != null) {
            Package.Pin packagePin = componentPackage.getPin(pin.pinIndex);
            if (packagePin != null) {
              return component.name + "-" + packagePin.name;
            }
          }
          return component.name + " (pin #" + pin.pinIndex + ")";
        }
      } catch (Exception e) {
        // Fall through to the generic representation.
      }
    }
    return item != null ? item.toString() : "(unknown)";
  }
}
