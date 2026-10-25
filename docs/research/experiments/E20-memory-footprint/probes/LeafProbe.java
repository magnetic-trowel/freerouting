import app.freerouting.Freerouting;
import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.model.items.Item;
import app.freerouting.datastructures.ShapeTree;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.settings.GlobalSettings;
import java.io.FileInputStream;
import java.lang.management.ManagementFactory;
import java.util.*;
import javax.management.ObjectName;

/** usage: LeafProbe <dsn>: tree shapes per item class in the default tree, and a heap histogram. */
public class LeafProbe {
  public static void main(String[] a) throws Exception {
    Freerouting.globalSettings = new GlobalSettings();
    BasicBoard b = ((BoardReadResult.Success) DsnReader.readBoard(new FileInputStream(a[0]), null, null, null)).board();
    ShapeTree tree = b.searchTreeManager.getDefaultTree();
    Map<String, int[]> per = new TreeMap<>();
    for (var it = b.itemList.startReadObject(); ; ) {
      Item item = (Item) b.itemList.readObject(it);
      if (item == null) break;
      int[] c = per.computeIfAbsent(item.getClass().getSimpleName(), k -> new int[2]);
      c[0]++; c[1] += item.treeShapeCount((app.freerouting.board.searchtree.ShapeSearchTree) tree);
    }
    System.out.println("LEAF default_tree_leaves=" + tree.size() + " compensation=" + b.searchTreeManager.isClearanceCompensationUsed());
    per.forEach((k, v) -> System.out.println("LEAF " + k + " items=" + v[0] + " tree_shapes=" + v[1]));
    String h = (String) ManagementFactory.getPlatformMBeanServer().invoke(new ObjectName("com.sun.management:type=DiagnosticCommand"),
        "gcClassHistogram", new Object[] {new String[0]}, new String[] {String[].class.getName()});
    String[] ls = h.split("\n");
    for (int i = 0; i < 14; i++) System.out.println("LEAFH " + ls[i]);
    System.out.println("LEAFH " + ls[ls.length - 1]);
    System.out.println("LEAF board_alive=" + (b != null));
    System.exit(0);
  }
}
