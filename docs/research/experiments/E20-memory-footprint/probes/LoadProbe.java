import app.freerouting.Freerouting;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.settings.GlobalSettings;
import java.io.FileInputStream;
import java.lang.management.ManagementFactory;

/** usage: LoadProbe <dsn> <repeat>: time, thread allocation and retained live heap of a DSN load. */
public class LoadProbe {
  public static void main(String[] a) throws Exception {
    Freerouting.globalSettings = new GlobalSettings();
    var tmx = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    var mem = ManagementFactory.getMemoryMXBean();
    for (int r = 0; r < Integer.parseInt(a[1]); r++) {
      System.gc(); long l0 = mem.getHeapMemoryUsage().getUsed();
      long al0 = tmx.getCurrentThreadAllocatedBytes(); long t0 = System.nanoTime();
      final Thread main = Thread.currentThread();
      final java.util.Map<String, Integer> histogram = new java.util.TreeMap<>();
      final boolean[] on = {true};
      Thread smp = new Thread(() -> { while (on[0]) { try { Thread.sleep(50); } catch (InterruptedException e) { return; }
        StackTraceElement[] st = main.getStackTrace(); StringBuilder k = new StringBuilder();
        int n = 0; for (StackTraceElement e : st) { if (!e.getClassName().startsWith("app.freerouting")) continue; k.append(e.getClassName().replace("app.freerouting.", "")).append('.').append(e.getMethodName()).append(':').append(e.getLineNumber()).append(" < "); if (++n == 6) break; }
        synchronized (histogram) { histogram.merge(k.toString(), 1, Integer::sum); } } });
      smp.setDaemon(true); smp.start();
      long tot0 = tmx.getTotalThreadAllocatedBytes();
      Object res; Object mgrBoard = null;
      if (r % 2 == 0) {
        res = DsnReader.readBoard(new FileInputStream(a[0]), null, null, null);
      } else {
        var mgr = new app.freerouting.management.HeadlessBoardManager(new app.freerouting.core.RoutingJob());
        res = mgr.loadFromSpecctraDsn(new FileInputStream(a[0]), new app.freerouting.board.state.BoardObserverAdaptor(), new app.freerouting.board.actions.ItemIdGenerator());
        mgrBoard = mgr.getRoutingBoard();
      }
      long t1 = System.nanoTime(); long al1 = tmx.getCurrentThreadAllocatedBytes();
      on[0] = false;
      if (r < 2) { synchronized (histogram) { histogram.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).limit(12).forEach(e -> System.out.println("LOADSTACK " + e.getValue() + " " + e.getKey())); } }
      Object board = res instanceof app.freerouting.io.BoardReadResult.Success s ? s.board() : null;
      // Post-load work may continue asynchronously.
      if (mgrBoard != null) board = mgrBoard;
      if (board instanceof app.freerouting.board.facade.BasicBoard bb && bb.postLoadFuture != null) bb.postLoadFuture.join();
      long t2 = System.nanoTime(); long tot1 = tmx.getTotalThreadAllocatedBytes();
      System.gc(); long l1 = mem.getHeapMemoryUsage().getUsed();
      System.out.printf("LOAD run=%d path=%s result=%s read_s=%.2f total_s=%.2f thread_alloc_GB=%.2f all_threads_alloc_GB=%.2f retained_MB=%.1f%n", r,
          r % 2 == 0 ? "DsnReader" : "HeadlessBoardManager+postLoad", res.getClass().getSimpleName(), (t1 - t0) / 1e9, (t2 - t0) / 1e9, (al1 - al0) / 1e9, (tot1 - tot0) / 1e9, (l1 - l0) / 1048576.0);
      board = null; res = null; mgrBoard = null;
    }
    System.exit(0);
  }
}
