import app.freerouting.Freerouting;
import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.RoutingJobState;
import app.freerouting.core.Session;
import app.freerouting.management.jobs.RoutingJobScheduler;
import app.freerouting.management.sessions.SessionManager;
import app.freerouting.settings.GlobalSettings;
import app.freerouting.settings.SettingsMerger;
import app.freerouting.settings.sources.CliSettings;
import app.freerouting.settings.sources.DefaultSettings;
import app.freerouting.settings.sources.DsnFileSettings;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.reflect.Field;
import java.util.*;
import java.util.zip.Deflater;
import javax.management.ObjectName;

/** usage: MemProbe <histoIntervalMs> <dsn> <repeat> [cli flags...] */
public class MemProbe {
  static final MemoryMXBean MEM = ManagementFactory.getMemoryMXBean();
  static volatile long peak = 0;
  static volatile String peakHisto = "";
  static volatile boolean sampling = false;

  static String histo() throws Exception {
    return (String) ManagementFactory.getPlatformMBeanServer().invoke(
        new ObjectName("com.sun.management:type=DiagnosticCommand"), "gcClassHistogram",
        new Object[] {new String[0]}, new String[] {String[].class.getName()});
  }
  static long histoTotal(String h) {
    for (String l : h.split("\n")) if (l.startsWith("Total")) return Long.parseLong(l.trim().split("\\s+")[2]);
    return -1;
  }
  static long live() { System.gc(); System.gc(); return MEM.getHeapMemoryUsage().getUsed(); }
  static String top(String h, int n) {
    String[] ls = h.split("\n"); StringBuilder sb = new StringBuilder();
    for (int i = 0; i < Math.min(ls.length, n + 2); i++) sb.append(ls[i]).append('\n');
    sb.append(ls[ls.length - 1]).append('\n'); return sb.toString();
  }
  static Object field(Object o, String name) throws Exception {
    Class<?> c = o.getClass();
    while (c != null) { try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f.get(o); } catch (NoSuchFieldException e) { c = c.getSuperclass(); } }
    throw new NoSuchFieldException(name);
  }

  public static void main(String[] a) throws Exception {
    int iv = Integer.parseInt(a[0]); String dsn = a[1]; int repeat = Integer.parseInt(a[2]);
    String[] flags = Arrays.copyOfRange(a, 3, a.length);
    Freerouting.globalSettings = new GlobalSettings();
    long base = live();
    System.out.printf("PROBE baseline_live_MB=%.1f%n", base / 1048576.0);
    Thread sampler = new Thread(() -> {
      while (true) {
        try { Thread.sleep(iv); if (!sampling) continue;
          String h = histo(); long t = histoTotal(h);
          if (t > peak) { peak = t; peakHisto = h; }
        } catch (InterruptedException e) { return; } catch (Exception e) { e.printStackTrace(); }
      }
    });
    sampler.setDaemon(true); sampler.start();
    for (int r = 1; r <= repeat; r++) {
      RoutingJobScheduler scheduler = RoutingJobScheduler.getInstance();
      Session session = SessionManager.getInstance().createSession(UUID.randomUUID(), "Freerouting/probe");
      RoutingJob job = new RoutingJob(session.id);
      job.setInput(new File(dsn));
      SettingsMerger merger = new SettingsMerger(new DefaultSettings(),
          new DsnFileSettings(job.input.getData(), job.input.getFilename()));
      merger.addOrReplaceSources(new CliSettings(flags));
      job.routerSettings = merger.merge();
      peak = 0; peakHisto = ""; sampling = true;
      long t0 = System.nanoTime();
      scheduler.enqueueJob(job);
      job.state = RoutingJobState.READY_TO_START;
      while (job.state != RoutingJobState.COMPLETED && job.state != RoutingJobState.CANCELLED
          && job.state != RoutingJobState.TERMINATED && job.state != RoutingJobState.TIMED_OUT) Thread.sleep(100);
      sampling = false;
      double wall = (System.nanoTime() - t0) / 1e9;
      RoutingBoard b = job.board;
      var st = b.getStatistics();
      System.out.printf("PROBE run=%d state=%s wall=%.1f unrouted=%d viol=%d peak_live_MB=%.1f hash=%s%n", r, job.state, wall,
          st.connections.incompleteCount, st.clearanceViolations.totalCount, peak / 1048576.0, b.getHash());
      System.out.print("PROBE peak histogram (top 25):\n" + top(peakHisto, 25));
      // attribution on final board
      byte[] image = b.serialize(false);
      Deflater d1 = new Deflater(1); d1.setInput(image); d1.finish(); byte[] buf = new byte[image.length + 1024];
      long c0 = System.nanoTime(); int z1 = d1.deflate(buf); long c1 = System.nanoTime(); d1.end();
      Collection<?> trees = (Collection<?>) field(b.searchTreeManager, "compensatedSearchTrees");
      StringBuilder ts = new StringBuilder();
      for (Object t : trees) ts.append(t.getClass().getSimpleName()).append("(cl=").append(field(t, "compensatedClearanceClassNo")).append(",leaves=").append(((app.freerouting.datastructures.ShapeTree) t).size()).append(") ");
      int items = 0; for (var it = b.itemList.startReadObject(); b.itemList.readObject(it) != null; ) items++;
      System.out.printf("PROBE board items=%d serialized_MB=%.2f deflate1_MB=%.2f (%.1fx, %.0f ms) angle=%s trees=%d %s%n", items,
          image.length / 1048576.0, z1 / 1048576.0, image.length / (double) z1, (c1 - c0) / 1e6, b.rules.getTraceAngleRestriction(), trees.size(), ts);
      // live cost of the board itself and of a deserialized copy
      long l0 = live();
      Object copy = BasicBoard.deserialize(image);
      long l1 = live();
      RoutingBoard rc = (RoutingBoard) copy;
      Collection<?> ctrees = (Collection<?>) field(rc.searchTreeManager, "compensatedSearchTrees");
      System.out.printf("PROBE deserialized_copy_live_MB=%.1f copy_trees=%d (job-retained live now %.1f MB)%n", (l1 - l0) / 1048576.0, ctrees.size(), l0 / 1048576.0);
      rc.searchTreeManager.getAutorouteTree(1);
      long l2 = live();
      System.out.printf("PROBE copy_plus_autoroute_tree1_MB=%.1f%n", (l2 - l0) / 1048576.0);
      copy = null; rc = null; image = null; b = null; st = null; trees = null; ctrees = null; merger = null;
      long withJob = live();
      synchronized (scheduler.jobs) { scheduler.jobs.clear(); }
      SessionManager.getInstance().removeSession(session.id.toString());
      job = null; session = null;
      long cleared = live();
      String h = histo();
      System.out.printf("PROBE after_run=%d retained_with_scheduler_MB=%.1f after_clear_MB=%.1f (baseline %.1f)%n", r, withJob / 1048576.0, cleared / 1048576.0, base / 1048576.0);
      System.out.print("PROBE retained histogram (top 15):\n" + top(h, 15));
    }
    System.exit(0);
  }
}
