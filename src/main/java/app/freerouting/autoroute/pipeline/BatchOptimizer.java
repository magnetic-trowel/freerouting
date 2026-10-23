package app.freerouting.autoroute.pipeline;

import app.freerouting.autoroute.ItemRouteResult;
import app.freerouting.autoroute.ItemSelectionStrategy;
import app.freerouting.autoroute.events.TaskStateChangedEvent;
import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.facade.BoardImage;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.items.Pin;
import app.freerouting.board.model.items.Trace;
import app.freerouting.board.model.items.Via;
import app.freerouting.board.model.structure.Unit;
import app.freerouting.board.trace.PolylineTrace;
import app.freerouting.core.ProgressThrottler;
import app.freerouting.core.RouterCounters;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.StoppableThread;
import app.freerouting.core.results.RoutingResultManifest;
import app.freerouting.core.scoring.BoardStatistics;
import app.freerouting.datastructures.UndoableObjects;
import app.freerouting.geometry.planar.FloatPoint;
import app.freerouting.logger.FRLogger;
import app.freerouting.settings.RouterSettings;
import app.freerouting.settings.sources.DefaultSettings;
import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Optimizes routes using a deterministic thread pool pipeline on a board that has completed
 * auto-routing.
 */
public final class BatchOptimizer extends NamedAlgorithm {

  protected final ProgressThrottler progressThrottler = new ProgressThrottler(1000);
  protected ReadSortedRouteItems sortedRouteItems;
  // In the first passes the ripup costs are increased for better performance.
  protected boolean useIncreasedRipupCosts;
  // The minimum cumulative trace length that was reached during the optimization
  protected double minCumulativeTraceLength = 0.0;
  protected RoutingJob job;
  protected int totalItemsOptimized;
  protected Long deadlineMs;
  protected boolean isTimedOut;
  protected RoutingBoard bestBoard;
  protected float bestScore;
  protected int bestIncompleteCount;
  protected int bestClearanceViolationCount;
  protected final Map<Integer, ItemRouteResult> resultMap = new HashMap<>();
  protected final AtomicLong workerCpuNanos = new AtomicLong(0);
  protected final AtomicLong workerAllocBytes = new AtomicLong(0);
  protected volatile FloatPoint currentPosition = null;

  /** The commits after the best one in the last pass, in the order they were made. */
  private final List<ChangeSet> passCommits = new ArrayList<>();

  /**
   * Creates a new instance of BatchOptimizer, which is used to optimize the board.
   *
   * @param job the routing job to optimize.
   */
  public BatchOptimizer(RoutingJob job) {
    super(job.thread, job.board, job.routerSettings);
    this.job = job;
  }

  /** Creates the canonical optimizer for a routing job. */
  public static BatchOptimizer create(RoutingJob job) {
    BatchOptimizer optimizer = new BatchOptimizer(job);
    normalizeAlgorithm(job, optimizer);
    return optimizer;
  }

  /** Creates the optimizer used by headless jobs. */
  public static BatchOptimizer createForHeadless(RoutingJob job) {
    return create(job);
  }

  /** Creates the optimizer used by GUI jobs. */
  public static BatchOptimizer createForGui(RoutingJob job) {
    return create(job);
  }

  private static void normalizeAlgorithm(RoutingJob job, BatchOptimizer optimizer) {
    if (!optimizer.getId().equals(job.routerSettings.optimizer.algorithm)) {
      job.logWarning(
          "The algorithm '"
              + job.routerSettings.optimizer.algorithm
              + "' is not supported by the batch autorouter. The default algorithm '"
              + optimizer.getId()
              + "' will be used instead.");
      job.routerSettings.optimizer.algorithm = optimizer.getId();
    }
  }

  /** Returns true if timed out. */
  public boolean isTimedOut() {
    return this.isTimedOut;
  }

  static boolean containsOnlyUnfixedTraces(Collection<Item> itemList) {
    for (Item currentItem : itemList) {
      if (currentItem.isUserFixed() || !(currentItem instanceof Trace)) {
        return false;
      }
    }
    return true;
  }

  private static float sampleCurrentThreadCpuSeconds() {
    try {
      ThreadMXBean threadMxBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
      long cpuNanos = threadMxBean.getThreadCpuTime(Thread.currentThread().threadId());
      return cpuNanos < 0 ? -1f : cpuNanos / 1_000_000_000.0f;
    } catch (Throwable t) {
      return -1f;
    }
  }

  private static float sampleCurrentThreadAllocatedMb() {
    try {
      ThreadMXBean threadMxBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
      threadMxBean.setThreadAllocatedMemoryEnabled(true);
      long allocatedBytes = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().threadId());
      return allocatedBytes < 0 ? -1f : allocatedBytes / (1024.0f * 1024.0f);
    } catch (Throwable t) {
      return -1f;
    }
  }

  private static float sampleHeapUsageMb() {
    try {
      long heapUsed = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
      return heapUsed / (1024.0f * 1024.0f);
    } catch (Throwable t) {
      return 0f;
    }
  }

  /**
   * Evaluates whether optimization can be safely bypassed before starting the batch loop.
   *
   * @param stats current board statistics
   * @return reason string explaining why optimization is bypassed, or {@code null} if guards pass
   */
  public String evaluatePreFlightGuards(BoardStatistics stats) {
    if (this.settings.optimizer != null
        && Boolean.FALSE.equals(this.settings.optimizer.enablePreflightGuards)) {
      return null;
    }

    // Guard 1: Incomplete connections
    if (stats.connections.incompleteCount > 0) {
      return String.format(
          Locale.US,
          "the board has %d unrouted connection(s) (optimizer only runs on completely routed boards)",
          stats.connections.incompleteCount);
    }

    float initialScore = stats.getOptimizerScore(this.job.routerSettings);

    // Guard 2: Zero vias and optimal trace length
    if (stats.vias.totalCount == 0) {
      if (initialScore >= 950.0f) {
        return String.format(
            Locale.US,
            "the board has no vias to eliminate and initial optimizer score (%.2f) is already >= 950.00",
            initialScore);
      }
      if (stats.bounds != null
          && stats.bounds.minTraceLengthMm != null
          && stats.bounds.minTraceLengthMm > 0
          && stats.traces.totalLengthMm != null
          && stats.traces.totalLengthMm <= stats.bounds.minTraceLengthMm * 1.05f) {
        return String.format(
            Locale.US,
            "the board has no vias to eliminate and trace length (%.2f mm) is within 5%% of theoretical minimum (%.2f mm)",
            stats.traces.totalLengthMm,
            stats.bounds.minTraceLengthMm);
      }
    }

    // Guard 3: Score ceiling / theoretical optimum
    if (initialScore >= 995.0f) {
      return String.format(
          Locale.US,
          "the initial optimizer score (%.2f) is already at or near theoretical maximum (995.00)",
          initialScore);
    }
    if (stats.bounds != null
        && stats.bounds.minTraceLengthMm != null
        && stats.bounds.minTraceLengthMm > 0
        && stats.traces.totalLengthMm != null
        && stats.traces.totalLengthMm <= stats.bounds.minTraceLengthMm * 1.02f
        && (stats.bounds.minViaCount == null
            || stats.vias.totalCount <= stats.bounds.minViaCount)) {
      return String.format(
          Locale.US,
          "total trace length (%.2f mm) is already within 2%% of the theoretical minimum (%.2f mm)",
          stats.traces.totalLengthMm,
          stats.bounds.minTraceLengthMm);
    }

    // Guard 4: All vias mandatory layer transitions
    if (this.board != null && areAllViasMandatoryLayerTransitions(this.board)) {
      return "all vias on the board are mandatory layer transitions between SMD pins that cannot be eliminated";
    }

    return null;
  }

  /**
   * Checks if all vias on the board are mandatory layer transitions between SMD pins on different
   * layers, with no alternative routing possible.
   */
  public static boolean areAllViasMandatoryLayerTransitions(RoutingBoard routingBoard) {
    Collection<Via> vias = routingBoard.getVias();
    if (vias.isEmpty()) {
      return false;
    }
    for (Via via : vias) {
      if (via.isUserFixed()) {
        continue;
      }
      if (via.netCount() == 0) {
        return false;
      }
      int netNo = via.getNetNumber(0);
      Set<Item> connected = via.getConnectedSet(netNo, true);
      List<Pin> smdPins = new ArrayList<>();
      boolean onlySmdPinsAndTraces = true;
      for (Item item : connected) {
        if (item instanceof Pin pin) {
          if (pin.firstLayer() == pin.lastLayer()) {
            smdPins.add(pin);
          } else {
            onlySmdPinsAndTraces = false;
            break;
          }
        } else if (item instanceof Via v) {
          if (v != via) {
            onlySmdPinsAndTraces = false;
            break;
          }
        } else if (!(item instanceof Trace)) {
          onlySmdPinsAndTraces = false;
          break;
        }
      }
      if (!onlySmdPinsAndTraces || smdPins.size() < 2) {
        return false;
      }
      int firstLayer = smdPins.get(0).firstLayer();
      boolean crossesLayers = false;
      for (Pin pin : smdPins) {
        if (pin.firstLayer() != firstLayer) {
          crossesLayers = true;
          break;
        }
      }
      if (!crossesLayers) {
        return false;
      }
    }
    return true;
  }

  /** Optimize the route on the board. */
  public void runBatchLoop() {
    if (this.job != null && this.job.board != null) {
      this.board = this.job.board;
    }
    if (this.thread != null) {
      this.thread.resetStopAutoRouterRequest();
    }

    job.logDebug(
        "Before optimization: Via count: "
            + board.getViaCount()
            + ", trace length: "
            + Math.round(board.cumulativeTraceLength()));

    useIncreasedRipupCosts = true;

    // Capture initial board state for baseline and session summary
    BoardStatistics initialStats = board.getStatistics();
    float initialRouterScore = initialStats.getRouterScore(job.routerSettings);
    float initialOptimizerScore = initialStats.getOptimizerScore(job.routerSettings);
    RoutingResultManifest.PhaseDetail phase = job.resultPhaseMetrics.optimizer;
    phase.before =
        RoutingResultManifest.PhaseSnapshot.fromBoardStatistics(
            initialStats, job.routerSettings, "current");
    phase.before.score = phase.before.optimizerScore;
    int initialIncomplete = initialStats.connections.incompleteCount;
    int initialViolations = initialStats.clearanceViolations.totalCount;

    String bypassReason = evaluatePreFlightGuards(initialStats);
    if (bypassReason != null) {
      job.logInfo("Skipping optimization stage: " + bypassReason + ".");
      phase.durationSeconds = 0.0f;
      phase.cpuSeconds = 0.0f;
      phase.totalAllocatedGb = 0.0f;
      phase.peakHeapMb = sampleHeapUsageMb();
      phase.after =
          RoutingResultManifest.PhaseSnapshot.fromBoardStatistics(
              initialStats, job.routerSettings, "current");
      phase.after.score = phase.after.optimizerScore;
      phase.passesCompleted = 0;
      this.fireTaskStateChangedEvent(
          new TaskStateChangedEvent(this, TaskState.FINISHED, 0, this.board.getHash()));
      return;
    }

    this.bestBoard = this.board.deepCopy();
    this.bestScore = initialOptimizerScore;
    this.bestIncompleteCount = initialIncomplete;
    this.bestClearanceViolationCount = initialViolations;

    job.logInfo(
        String.format(
            Locale.US,
            "Optimization stage started on board '%s'. Baseline router score: %.2f, "
                + "optimizer score: %.2f, incomplete connections: %d, "
                + "clearance violations: %d.",
            this.board.getHash(),
            initialRouterScore,
            initialOptimizerScore,
            initialIncomplete,
            initialViolations));

    if (initialStats.bounds != null && initialStats.bounds.viaEquivalentLengthMm != null) {
      job.logInfo(
          String.format(
              Locale.US,
              "Optimizer score bounds: minimum trace length %.2f mm, minimum vias %d, minimum bends"
                  + " %d; one via is worth %.3f mm and one bend %.3f mm of trace.",
              initialStats.bounds.minTraceLengthMm,
              initialStats.bounds.minViaCount,
              initialStats.bounds.minBendCount,
              initialStats.bounds.viaEquivalentLengthMm,
              initialStats.bounds.bendEquivalentLengthMm));
    }

    // Capture start-of-session resource usage baselines
    long sessionStartMs = System.currentTimeMillis();
    final float cpuSecondsStart = sampleCurrentThreadCpuSeconds();
    final float allocMbStart = sampleCurrentThreadAllocatedMb();
    float peakHeapMb = sampleHeapUsageMb();
    workerCpuNanos.set(0);
    workerAllocBytes.set(0);

    if (this.settings.optimizer != null && this.settings.optimizer.timeoutString != null) {
      Long timeoutSeconds =
          app.freerouting.util.TextManager.parseTimespanString(
              this.settings.optimizer.timeoutString);
      if (timeoutSeconds != null) {
        this.deadlineMs = sessionStartMs + timeoutSeconds * 1000;
      }
    }

    if (this.settings.optimizer != null
        && this.settings.optimizer.optimizationImprovementThreshold != null) {
      float threshold = this.settings.optimizer.optimizationImprovementThreshold;
      if (Float.isNaN(threshold) || Float.isInfinite(threshold) || threshold < 0.0f) {
        job.logWarning(
            String.format(
                Locale.US,
                "Invalid optimizer improvement threshold: %.4f. Resetting to default %.2f%%.",
                threshold,
                DefaultSettings.DEFAULT_OPTIMIZER_IMPROVEMENT_THRESHOLD));
        this.settings.optimizer.optimizationImprovementThreshold =
            DefaultSettings.DEFAULT_OPTIMIZER_IMPROVEMENT_THRESHOLD;
      }
    }

    this.fireTaskStateChangedEvent(
        new TaskStateChangedEvent(this, TaskState.STARTED, 0, this.board.getHash()));

    double scoreImprovement = -1;
    int currentPass = 0;
    while ((this.settings.optimizer.maxPasses == null
            || currentPass < this.settings.optimizer.maxPasses)
        && (this.settings.optimizer.maxItems == null
            || this.totalItemsOptimized < this.settings.optimizer.maxItems)
        && (!this.thread.isStopRequested())) {
      if (this.deadlineMs != null && System.currentTimeMillis() >= this.deadlineMs) {
        this.isTimedOut = true;
        job.logInfo("Optimizer stage timed out before starting pass #" + (currentPass + 1));
        break;
      }
      ++currentPass;

      float scoreBeforePass = board.getStatistics().getOptimizerScore(job.routerSettings);

      String currentBoardHash = this.board.getHash();
      this.fireTaskStateChangedEvent(
          new TaskStateChangedEvent(this, TaskState.RUNNING, currentPass, currentBoardHash));

      boolean withPreferredDirections = currentPass % 2 != 0; // to create more variations
      try {
        optRoutePass(currentPass, withPreferredDirections);
      } catch (OutOfMemoryError outOfMemory) {
        // The pass's candidate boards are unreachable once it unwinds, which frees the heap. The
        // live board may be half-way through a commit, so continue from the best accepted board
        // and stop optimizing instead of letting the error end the routing job.
        this.passCommits.clear();
        this.board = this.bestBoard;
        this.job.board = this.board;
        job.logWarning(
            String.format(
                Locale.US,
                "Optimizer pass #%d ran out of memory; stopping the optimizer and keeping the best"
                    + " accepted board (optimizer score %.2f).",
                currentPass,
                this.bestScore));
        this.fireBoardUpdatedEvent(new BoardStatistics(this.board), null, this.board);
        break;
      }
      peakHeapMb = Math.max(peakHeapMb, sampleHeapUsageMb());

      if (this.isTimedOut) {
        break;
      }

      BoardStatistics passStats = board.getStatistics();
      float scoreAfterPass = passStats.getOptimizerScore(job.routerSettings);
      String rejectionReason =
          optimizerCandidateRejectionReason(
              this.bestIncompleteCount,
              this.bestClearanceViolationCount,
              this.bestScore,
              passStats,
              scoreAfterPass);
      // Commits were made best first, so the last one is the least valuable. Drop commits from the
      // end until the board beats the incumbent; the best commit alone is the last board tried.
      int droppedCommits = 0;
      while (rejectionReason != null && !this.passCommits.isEmpty()) {
        ChangeSet last = this.passCommits.remove(this.passCommits.size() - 1);
        if (!last.revert(this.board)) {
          break;
        }
        droppedCommits++;
        passStats = board.getStatistics();
        scoreAfterPass = passStats.getOptimizerScore(job.routerSettings);
        rejectionReason =
            optimizerCandidateRejectionReason(
                this.bestIncompleteCount,
                this.bestClearanceViolationCount,
                this.bestScore,
                passStats,
                scoreAfterPass);
      }
      if (droppedCommits > 0) {
        job.logInfo(
            String.format(
                Locale.US,
                "Optimizer pass #%d dropped its %d lowest-ranked commits: %s.",
                currentPass,
                droppedCommits,
                rejectionReason == null ? "accepted" : "still " + rejectionReason));
      }
      if (rejectionReason == null) {
        this.bestScore = scoreAfterPass;
        this.bestIncompleteCount = passStats.connections.incompleteCount;
        this.bestClearanceViolationCount = passStats.clearanceViolations.totalCount;
        this.bestBoard = this.board.deepCopy();
      } else {
        job.logInfo(
            String.format(
                Locale.US,
                "Optimizer pass #%d candidate rejected: %s. Restoring incumbent "
                    + "(optimizer score %.2f, incomplete connections: %d, clearance violations: %d).",
                currentPass,
                rejectionReason,
                this.bestScore,
                this.bestIncompleteCount,
                this.bestClearanceViolationCount));
        restoreIncumbentBoard();
        // The pass made no progress. Measuring it by the rejected candidate would repeat the
        // same rejected pass until max_passes.
        scoreAfterPass = this.bestScore;
      }

      double passImprovementFraction =
          scoreBeforePass > 0 ? (double) (scoreAfterPass - scoreBeforePass) / scoreBeforePass : 0;
      double passImprovementPercent = passImprovementFraction * 100.0;
      String passOutcome =
          scoreAfterPass > scoreBeforePass
              ? "IMPROVED"
              : (scoreAfterPass < scoreBeforePass ? "REGRESSED" : "UNCHANGED");
      String passImprovementPercentStr =
          scoreBeforePass > 0
              ? String.format(Locale.US, "%.4f%%", passImprovementPercent)
              : "n/a (baseline was 0.00)";
      job.logInfo(
          String.format(
              Locale.US,
              "Optimizer pass #%d: optimizer score %.2f -> %.2f (%s, %s), router score: %.2f, "
                  + "incomplete connections: %d, clearance violations: %d.",
              currentPass,
              scoreBeforePass,
              scoreAfterPass,
              passOutcome,
              passImprovementPercentStr,
              passStats.getRouterScore(job.routerSettings),
              passStats.connections.incompleteCount,
              passStats.clearanceViolations.totalCount));

      if (this.useIncreasedRipupCosts
          && passImprovementPercent <= this.settings.optimizer.optimizationImprovementThreshold) {
        this.useIncreasedRipupCosts = false;
        // Increased ripup costs stopped paying off. Keep the optimizer going to try with normal
        // ripup costs before the threshold may stop it.
        scoreImprovement = -1;
      } else {
        scoreImprovement = passImprovementPercent;
      }

      // At or below: a threshold of 0 stops at the first pass that gains nothing instead of
      // repeating unchanged passes until max_passes.
      if (scoreImprovement != -1
          && scoreImprovement <= this.settings.optimizer.optimizationImprovementThreshold) {
        job.logInfo(
            String.format(
                Locale.US,
                "Stopping optimizer because the improvement in this pass (%.4f%%) is at or below "
                    + "the threshold (%.2f%%).",
                scoreImprovement,
                this.settings.optimizer.optimizationImprovementThreshold));
        break;
      }
    }

    this.currentPosition = null;

    // Restore best board achieved if final state regressed below best score or added
    // violations/opens
    BoardStatistics endStats = this.board.getStatistics();
    float finalBoardScore = endStats.getOptimizerScore(job.routerSettings);
    int finalIncomplete = endStats.connections.incompleteCount;
    int finalViolations = endStats.clearanceViolations.totalCount;

    boolean regressed =
        finalBoardRegressed(
            this.bestScore,
            this.bestIncompleteCount,
            this.bestClearanceViolationCount,
            finalBoardScore,
            finalIncomplete,
            finalViolations);

    if (regressed && this.bestBoard != null) {
      job.logInfo(
          String.format(
              Locale.US,
              "Restoring best board achieved (score %.2f vs final %.2f, incomplete connections %d vs %d, clearance violations %d vs %d).",
              this.bestScore,
              finalBoardScore,
              this.bestIncompleteCount,
              finalIncomplete,
              this.bestClearanceViolationCount,
              finalViolations));
      restoreIncumbentBoard();
    }
    this.bestBoard = null;

    this.fireTaskStateChangedEvent(
        new TaskStateChangedEvent(this, TaskState.FINISHED, currentPass, this.board.getHash()));

    // Session summary
    double sessionDurationSeconds = (System.currentTimeMillis() - sessionStartMs) / 1000.0;
    float cpuSecondsEnd = sampleCurrentThreadCpuSeconds();
    float allocMbEnd = sampleCurrentThreadAllocatedMb();
    float cpuSecondsMain =
        cpuSecondsStart >= 0f && cpuSecondsEnd >= cpuSecondsStart
            ? cpuSecondsEnd - cpuSecondsStart
            : Math.max(0f, cpuSecondsEnd);
    float allocMbMain =
        allocMbStart >= 0f && allocMbEnd >= allocMbStart
            ? allocMbEnd - allocMbStart
            : Math.max(0f, allocMbEnd);
    float cpuSecondsUsed = cpuSecondsMain + (workerCpuNanos.get() / 1_000_000_000.0f);
    float allocMbUsed = allocMbMain + (workerAllocBytes.get() / (1024.0f * 1024.0f));
    peakHeapMb = Math.max(peakHeapMb, sampleHeapUsageMb());
    phase.durationSeconds = (float) sessionDurationSeconds;
    phase.cpuSeconds = cpuSecondsUsed;
    phase.totalAllocatedGb = allocMbUsed / 1024.0f;
    phase.peakHeapMb = peakHeapMb;

    BoardStatistics finalStats = new BoardStatistics(this.board);
    float finalRouterScore = finalStats.getRouterScore(job.routerSettings);
    float finalOptimizerScore = finalStats.getOptimizerScore(job.routerSettings);
    phase.after =
        RoutingResultManifest.PhaseSnapshot.fromBoardStatistics(
            finalStats, job.routerSettings, "current");
    phase.after.score = phase.after.optimizerScore;
    phase.passesCompleted = currentPass;
    String completionStatus =
        this.isTimedOut
            ? "completed with timeout:"
            : (this.thread.isStopRequested() ? "interrupted:" : "completed:");
    job.logInfo(
        String.format(
            Locale.US,
            "Optimization stage %s. Baseline router score: %.2f, baseline optimizer score: %.2f, "
                + "final router score: %.2f, final optimizer score: %.2f, completed in %.2f "
                + "seconds, using %.2f total CPU seconds, %.2f GB total allocated, and %.1f MB "
                + "peak heap usage.",
            completionStatus,
            initialRouterScore,
            initialOptimizerScore,
            finalRouterScore,
            finalOptimizerScore,
            sessionDurationSeconds,
            cpuSecondsUsed,
            allocMbUsed / 1024.0f,
            peakHeapMb));
  }

  /**
   * Restores a working copy of the incumbent snapshot. The live board is mutated in place by later
   * passes, so assigning {@code this.board = this.bestBoard} would alias and destroy the snapshot.
   */
  private void restoreIncumbentBoard() {
    this.board = this.bestBoard.deepCopy();
    this.job.board = this.board;
    this.fireBoardUpdatedEvent(new BoardStatistics(this.board), null, this.board);
  }

  /**
   * Returns the explicit reason a candidate must be rejected, or {@code null} when it is accepted.
   *
   * <p>Completeness and DRC count are vetoes; optimizer score ranks candidates that pass both
   * vetoes. In particular, a more-complete but uglier candidate is not an automatic win.
   */
  static String optimizerCandidateRejectionReason(
      int incumbentIncompleteCount,
      int incumbentClearanceViolationCount,
      float incumbentOptimizerScore,
      BoardStatistics candidate,
      float candidateOptimizerScore) {
    if (candidate.connections.incompleteCount > incumbentIncompleteCount) {
      return "CONNECTIVITY_REGRESSION";
    }
    if (candidate.clearanceViolations.totalCount > incumbentClearanceViolationCount) {
      return "DRC_COUNT_REGRESSION";
    }
    if (!(candidateOptimizerScore > incumbentOptimizerScore)) {
      return "OPTIMIZER_SCORE_NOT_IMPROVED";
    }
    return null;
  }

  /**
   * Checks whether the final board at the end of the optimizer regressed compared to the best
   * accepted incumbent board. Replaced if score is lower, or if incomplete connections or clearance
   * violations increased (FR-077).
   */
  public static boolean finalBoardRegressed(
      float bestScore,
      int bestIncomplete,
      int bestViolations,
      float finalScore,
      int finalIncomplete,
      int finalViolations) {
    return finalScore < bestScore
        || finalIncomplete > bestIncomplete
        || finalViolations > bestViolations;
  }

  private List<Integer> prepareCandidateItems() {
    List<Integer> itemIds = new ArrayList<>();
    this.sortedRouteItems = new ReadSortedRouteItems();

    ItemSelectionStrategy strategy =
        this.settings.optimizer != null ? this.settings.optimizer.itemSelectionStrategy : null;
    if (strategy == null) {
      strategy = ItemSelectionStrategy.SEQUENTIAL;
    }

    if (strategy == ItemSelectionStrategy.PRIORITIZED && !resultMap.isEmpty()) {
      List<Integer> nonPrioritized = new ArrayList<>();
      PriorityQueue<ItemRouteResult> pq = new PriorityQueue<>();

      for (Item item = sortedRouteItems.next(); item != null; item = sortedRouteItems.next()) {
        ItemRouteResult r = resultMap.get(item.getId());
        if (r != null) {
          pq.add(r);
        } else {
          nonPrioritized.add(item.getId());
        }
      }

      while (!pq.isEmpty()) {
        itemIds.add(pq.poll().itemId());
      }
      itemIds.addAll(nonPrioritized);
    } else {
      for (Item item = sortedRouteItems.next(); item != null; item = sortedRouteItems.next()) {
        itemIds.add(item.getId());
      }
    }

    this.sortedRouteItems = null;
    resultMap.clear();
    return itemIds;
  }

  /**
   * Tries to reduce the number of vias and the trace length of a completely routed board. Returns
   * the amount of improvements made in percentage (expressed between 0.0 and 1.0), or -1 if the
   * routing must go on no matter how much it improved.
   */
  protected float optRoutePass(int passNo, boolean withPreferredDirections) {
    this.passCommits.clear();
    BoardStatistics boardStatisticsBefore = board.getStatistics();
    RouterCounters routerCounters = new RouterCounters();
    routerCounters.passCount = passNo;
    progressThrottler.reset();
    this.fireBoardUpdatedEvent(boardStatisticsBefore, routerCounters, this.board);

    this.minCumulativeTraceLength = boardStatisticsBefore.traces.totalWeightedLength;
    CommitSelection selection =
        CommitSelection.of(
            this.settings.optimizer != null ? this.settings.optimizer.commitSelection : null);
    ScoreTracker scoreTracker =
        selection == CommitSelection.ALL
            ? null
            : ScoreTracker.of(boardStatisticsBefore, this.board, job.routerSettings);
    if (scoreTracker == null && selection != CommitSelection.ALL) {
      job.logWarning(
          "Optimizer commit selection '"
              + selection
              + "' needs the V2 optimizer score; using 'all' instead.");
      selection = CommitSelection.ALL;
    }
    List<Integer> candidateItemIds = prepareCandidateItems();

    if (this.settings.optimizer != null
        && this.settings.optimizer.maxItems != null
        && this.settings.optimizer.maxItems > 0) {
      int remaining = this.settings.optimizer.maxItems - this.totalItemsOptimized;
      if (remaining <= 0) {
        job.logInfo(
            "Max items limit reached ("
                + this.settings.optimizer.maxItems
                + "). Stopping optimizer.");
        return 0.0f;
      }
      if (candidateItemIds.size() > remaining) {
        candidateItemIds = new ArrayList<>(candidateItemIds.subList(0, remaining));
      }
    }

    if (candidateItemIds.isEmpty()) {
      return 0.0f;
    }

    int configuredThreads =
        (this.settings.optimizer != null && this.settings.optimizer.maxThreads != null)
            ? this.settings.optimizer.maxThreads
            : 1;
    int threadPoolSize = Math.max(1, configuredThreads);

    String optimizationPassId =
        "BatchOptRoute.opt_route_pass #"
            + passNo
            + " with "
            + candidateItemIds.size()
            + " items, "
            + boardStatisticsBefore.items.viaCount
            + " vias and "
            + "%(,.2f".formatted(boardStatisticsBefore.traces.totalLength)
            + " trace length running on "
            + threadPoolSize
            + " thread(s).";

    FRLogger.traceEntry(optimizationPassId);

    ExecutorService pool =
        Executors.newFixedThreadPool(
            threadPoolSize,
            r -> {
              Thread t = new Thread(r, "batch-optimizer-worker");
              t.setDaemon(true);
              t.setUncaughtExceptionHandler(
                  (t1, e) -> job.logError("Exception in thread pool worker thread: " + t1, e));
              return t;
            });

    CandidateResult winningCandidate = null;
    List<RankedCandidate> improvedResults = new ArrayList<>();
    BoardImage baselineImage = this.board.image();
    BoardStatistics.OptimizerMetrics baselineMetrics =
        BoardStatistics.OptimizerMetrics.of(this.board);
    int chunkSize = Math.max(threadPoolSize * 4, 8);
    boolean stoppedOrTimedOut = false;
    boolean earlyStopDueToFailures = false;
    int consecutiveFailures = 0;
    int maxConsecutiveFailures =
        passNo == 1
            ? (this.settings.optimizer != null
                    && this.settings.optimizer.maxConsecutiveFailuresPass1 != null
                ? this.settings.optimizer.maxConsecutiveFailuresPass1
                : 12)
            : (this.settings.optimizer != null
                    && this.settings.optimizer.maxConsecutiveFailures != null
                ? this.settings.optimizer.maxConsecutiveFailures
                : 50);

    // Candidates with the same key route identically; evaluate the first and reuse its result.
    int[] representative = new int[candidateItemIds.size()];
    int[] lastDuplicate = new int[candidateItemIds.size()];
    Map<String, Integer> firstIndexByKey = new HashMap<>();
    for (int k = 0; k < candidateItemIds.size(); k++) {
      String key = candidateKey(this.board, candidateItemIds.get(k));
      Integer first = key == null ? null : firstIndexByKey.putIfAbsent(key, k);
      representative[k] = first == null ? k : first;
      lastDuplicate[representative[k]] = k;
    }
    Map<Integer, Future<CandidateResult>> futureByIndex = new HashMap<>();
    int nextToSubmit = 0;

    try {
      for (int i = 0; i < candidateItemIds.size(); i += chunkSize) {
        if (this.deadlineMs != null && System.currentTimeMillis() >= this.deadlineMs) {
          job.logInfo("Optimizer stage timed out.");
          this.isTimedOut = true;
          stoppedOrTimedOut = true;
          break;
        }
        if (this.thread != null && this.thread.isStopRequested()) {
          stoppedOrTimedOut = true;
          break;
        }

        int end = Math.min(i + chunkSize, candidateItemIds.size());
        for (int k = i; k < end; k++) {
          // Keep chunkSize candidates in flight ahead of the one consumed next. Results are still
          // consumed in candidate order, so every decision is the same as with a barrier per
          // chunk, but a slow candidate no longer leaves the other workers idle.
          int submitLimit = Math.min(candidateItemIds.size(), k + chunkSize);
          while (nextToSubmit < submitLimit) {
            int toSubmit = nextToSubmit++;
            if (representative[toSubmit] != toSubmit) {
              continue;
            }
            int itemId = candidateItemIds.get(toSubmit);
            futureByIndex.put(
                toSubmit,
                pool.submit(
                    new OptimizeCandidateTask(
                        job,
                        baselineImage,
                        itemId,
                        this.minCumulativeTraceLength,
                        withPreferredDirections,
                        this.useIncreasedRipupCosts,
                        this.thread,
                        this.deadlineMs,
                        baselineMetrics,
                        selection != CommitSelection.ALL,
                        selection == CommitSelection.SCORE_RANKED ? scoreTracker : null)));
          }
          try {
            int rep = representative[k];
            CandidateResult res = futureByIndex.get(rep).get();
            if (k == lastDuplicate[rep]) {
              futureByIndex.remove(rep);
            }
            if (res != null && rep != k) {
              int itemId = candidateItemIds.get(k);
              res = res.forDuplicate(itemId, getItemPosition(this.board.getItem(itemId)));
            }
            this.totalItemsOptimized++;
            if (res != null) {
              this.workerCpuNanos.addAndGet(res.cpuUsedNanos);
              this.workerAllocBytes.addAndGet(res.allocUsedBytes);
              this.resultMap.put(res.result.itemId(), res.result);

              if (res.itemPosition != null) {
                this.currentPosition = res.itemPosition;
              }

              if (res.result.improved()) {
                consecutiveFailures = 0;
                improvedResults.add(new RankedCandidate(res.result, res.scoreGain));
                if (winningCandidate == null || res.ranksBefore(winningCandidate, selection)) {
                  winningCandidate = res;
                }
              } else {
                consecutiveFailures++;
                if (consecutiveFailures >= maxConsecutiveFailures) {
                  job.logInfo(
                      String.format(
                          Locale.US,
                          "Stopping optimization pass #%d early after %d consecutive items could "
                              + "not be improved.",
                          passNo,
                          consecutiveFailures));
                  earlyStopDueToFailures = true;
                  break;
                }
              }
            }
          } catch (InterruptedException e) {
            stoppedOrTimedOut = true;
            Thread.currentThread().interrupt();
            break;
          } catch (ExecutionException e) {
            if (e.getCause() instanceof OutOfMemoryError outOfMemory) {
              throw outOfMemory;
            }
            job.logError("Error in candidate optimization task", e.getCause());
          }
        }

        if (stoppedOrTimedOut || earlyStopDueToFailures) {
          break;
        }
      }
    } finally {
      pool.shutdownNow();
    }

    float routeImproved = 0.0f;

    if (!stoppedOrTimedOut && winningCandidate != null && winningCandidate.result.improved()) {
      this.board = winningCandidate.board;
      this.job.board = this.board;
      ScoreTracker liveScore = scoreTracker == null ? null : scoreTracker.copy();
      if (liveScore != null) {
        liveScore.apply(winningCandidate.changes);
      }
      int runnersUp =
          commitRunnersUp(
              improvedResults,
              winningCandidate.result,
              withPreferredDirections,
              selection,
              liveScore);
      job.logInfo(
          String.format(
              Locale.US,
              "Optimizer pass #%d committed %d of %d improved candidates (selection %s%s).",
              passNo,
              runnersUp + 1,
              improvedResults.size(),
              selection.settingValue(),
              liveScore == null
                  ? ""
                  : String.format(
                      Locale.US,
                      ", optimizer score %.2f -> %.2f",
                      scoreTracker.score(),
                      liveScore.score())));
      this.minCumulativeTraceLength = this.board.getStatistics().traces.totalWeightedLength;

      BoardStatistics boardStatisticsAfter = this.board.getStatistics();
      this.fireBoardUpdatedEvent(boardStatisticsAfter, routerCounters, this.board);
      routeImproved = winningCandidate.result.improvementPercentage();
    }

    double routeoptimizerPassDuration = FRLogger.traceExit(optimizationPassId);
    BoardStatistics boardStatisticsAfter = new BoardStatistics(this.board);
    this.fireBoardUpdatedEvent(boardStatisticsAfter, routerCounters, this.board);
    job.logInfo(
        String.format(
            Locale.US,
            "Optimizer pass #%d on board '%s' was completed in %.2f seconds with the score of %s.",
            passNo,
            this.board.getHash(),
            routeoptimizerPassDuration,
            FRLogger.formatScore(
                boardStatisticsAfter.getOptimizerScore(job.routerSettings),
                boardStatisticsAfter.connections.incompleteCount,
                boardStatisticsAfter.clearanceViolations.totalCount)));
    return routeImproved;
  }

  /**
   * Commits the pass's other improved candidates after its best one, up to {@code
   * optimizer.max_commits_per_pass} in total (a value below 1 means no limit). All candidates were
   * evaluated against the same board, so each one is routed again on the board that the earlier
   * commits produced and is kept only if it still improves that board in the sense of the commit
   * selection. Candidates are committed best first, so a smaller improvement never takes the space
   * a larger one needs. Returns the number of candidates committed in addition to the best one.
   */
  private int commitRunnersUp(
      List<RankedCandidate> improvedResults,
      ItemRouteResult best,
      boolean withPreferredDirections,
      CommitSelection selection,
      ScoreTracker liveScore) {
    Integer configured =
        this.settings.optimizer != null ? this.settings.optimizer.maxCommitsPerPass : null;
    int maxCommits = configured == null ? 1 : (configured < 1 ? Integer.MAX_VALUE : configured);
    if (selection == CommitSelection.SCORE_RANKED) {
      improvedResults.sort(RankedCandidate.BY_SCORE_GAIN);
    } else {
      improvedResults.sort((a, b) -> a.result().compareTo(b.result()));
    }
    List<Float> prefixScores = new ArrayList<>();
    if (liveScore != null) {
      prefixScores.add(liveScore.score());
    }
    int committed = 0;
    for (RankedCandidate candidate : improvedResults) {
      if (committed + 1 >= maxCommits
          || (this.thread != null && this.thread.isStopRequested())
          || (this.deadlineMs != null && System.currentTimeMillis() >= this.deadlineMs)) {
        break;
      }
      if (candidate.result() != best
          && recommit(candidate.result().itemId(), withPreferredDirections, selection, liveScore)) {
        committed++;
        if (liveScore != null) {
          prefixScores.add(liveScore.score());
        }
      }
    }
    if (selection == CommitSelection.BEST_PREFIX && committed > 0) {
      int bestPrefix = 0;
      for (int i = 1; i < prefixScores.size(); i++) {
        if (prefixScores.get(i) > prefixScores.get(bestPrefix)) {
          bestPrefix = i;
        }
      }
      while (committed > bestPrefix) {
        ChangeSet last = this.passCommits.get(this.passCommits.size() - 1);
        if (!last.revert(this.board)) {
          break;
        }
        this.passCommits.remove(this.passCommits.size() - 1);
        liveScore.unapply(last);
        committed--;
      }
    }
    return committed;
  }

  /**
   * Routes the connections of the item again on the live board. Keeps the result, and records its
   * changes for {@link #passCommits}, if it improves the board in the sense of the commit
   * selection; undoes it otherwise. Returns false if an earlier commit removed the item.
   */
  private boolean recommit(
      int itemId,
      boolean withPreferredDirections,
      CommitSelection selection,
      ScoreTracker liveScore) {
    Item item = this.board.getItem(itemId);
    if (item == null) {
      return false;
    }
    this.board.generateSnapshot();
    ItemRouteResult result =
        optRouteItemOnBoard(
            job,
            this.board,
            item,
            0,
            withPreferredDirections,
            this.useIncreasedRipupCosts,
            this.thread,
            this.deadlineMs);
    ChangeSet changes = null;
    boolean keep;
    if (selection == CommitSelection.SCORE_RANKED) {
      changes = ChangeSet.since(this.board);
      keep =
          isRunning(this.thread, this.deadlineMs)
              && result.incompleteCount() <= result.incompleteCountBefore()
              && liveScore.scoreWith(changes) > liveScore.score();
    } else {
      keep = result.improved();
      if (keep) {
        changes = ChangeSet.since(this.board);
        keep =
            selection != CommitSelection.SCORE_GATED
                || liveScore.scoreWith(changes) > liveScore.score();
      }
    }
    if (keep) {
      this.passCommits.add(changes);
      if (liveScore != null) {
        liveScore.apply(changes);
      }
      this.board.popSnapshot();
    } else {
      this.board.undo(null);
    }
    this.board.clearTransientAutorouteState();
    return keep;
  }

  private static boolean isRunning(StoppableThread thread, Long deadlineMs) {
    return (thread == null || !thread.isStopRequested())
        && (deadlineMs == null || System.currentTimeMillis() < deadlineMs);
  }

  /** How a pass chooses the improvements it commits; see {@code optimizer.commit_selection}. */
  enum CommitSelection {
    ALL,
    BEST_PREFIX,
    SCORE_GATED,
    SCORE_RANKED;

    static CommitSelection of(String value) {
      if (value != null) {
        for (CommitSelection selection : values()) {
          if (selection.settingValue().equalsIgnoreCase(value.trim())) {
            return selection;
          }
        }
      }
      return ALL;
    }

    String settingValue() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /** An improved candidate of a pass and the optimizer score it gained on the pass's board. */
  private record RankedCandidate(ItemRouteResult result, double scoreGain) {
    static final java.util.Comparator<RankedCandidate> BY_SCORE_GAIN =
        (a, b) -> {
          int byGain = Double.compare(b.scoreGain, a.scoreGain);
          return byGain != 0 ? byGain : Integer.compare(a.result.itemId(), b.result.itemId());
        };
  }

  /**
   * The V2 optimizer score of a board, kept up to date from the trace length, via and bend changes
   * of the commits made on it, so that a commit can be judged without new board statistics.
   */
  static final class ScoreTracker {
    private final BoardStatistics statistics;
    private final RouterSettings routerSettings;
    private final double boardUnitToMm;
    private double traceLengthMm;
    private int viaCount;
    private int bendCount;

    private ScoreTracker(
        BoardStatistics statistics,
        RouterSettings routerSettings,
        double boardUnitToMm,
        double traceLengthMm,
        int viaCount,
        int bendCount) {
      this.statistics = statistics;
      this.routerSettings = routerSettings;
      this.boardUnitToMm = boardUnitToMm;
      this.traceLengthMm = traceLengthMm;
      this.viaCount = viaCount;
      this.bendCount = bendCount;
    }

    /** Returns a tracker for the board the statistics were taken of, or null without V2 scoring. */
    static ScoreTracker of(
        BoardStatistics statistics, RoutingBoard board, RouterSettings routerSettings) {
      if (statistics.getOptimizerScore(routerSettings, 0, 0, 0) == null) {
        return null;
      }
      double boardUnitToMm = board.communication.boardUnitSize(Unit.MM);
      return new ScoreTracker(
          statistics,
          routerSettings,
          boardUnitToMm,
          statistics.traces.totalLengthMm != null ? statistics.traces.totalLengthMm : 0.0,
          statistics.vias.totalCount != null ? statistics.vias.totalCount : 0,
          statistics.bends.totalCount != null ? statistics.bends.totalCount : 0);
    }

    ScoreTracker copy() {
      return new ScoreTracker(
          statistics, routerSettings, boardUnitToMm, traceLengthMm, viaCount, bendCount);
    }

    float score() {
      return statistics.getOptimizerScore(routerSettings, traceLengthMm, viaCount, bendCount);
    }

    float scoreWith(ChangeSet changes) {
      return statistics.getOptimizerScore(
          routerSettings,
          traceLengthMm + changes.lengthDelta * boardUnitToMm,
          viaCount + changes.viaDelta,
          bendCount + changes.bendDelta);
    }

    void apply(ChangeSet changes) {
      traceLengthMm += changes.lengthDelta * boardUnitToMm;
      viaCount += changes.viaDelta;
      bendCount += changes.bendDelta;
    }

    void unapply(ChangeSet changes) {
      traceLengthMm -= changes.lengthDelta * boardUnitToMm;
      viaCount -= changes.viaDelta;
      bendCount -= changes.bendDelta;
    }
  }

  /**
   * Try to improve the route by re-routing the connections containing item.
   *
   * @param item the item to be re-routed
   * @param withPreferredDirections if true, the preferred directions are used for the traces
   */
  protected ItemRouteResult optRouteItem(Item item, boolean withPreferredDirections) {
    if (!(item.board instanceof RoutingBoard routingBoard)) {
      job.logWarning("The item to be optimized is not on a RoutingBoard.");
      return new ItemRouteResult(item.getId());
    }

    double baseline =
        this.minCumulativeTraceLength > 0
            ? this.minCumulativeTraceLength
            : routingBoard.getStatistics().traces.totalWeightedLength;

    return optRouteItemOnBoard(
        job,
        routingBoard,
        item,
        baseline,
        withPreferredDirections,
        this.useIncreasedRipupCosts,
        this.thread,
        this.deadlineMs);
  }

  static ItemRouteResult optRouteItemOnBoard(
      RoutingJob job,
      RoutingBoard routingBoard,
      Item item,
      double baselineTraceLength,
      boolean withPreferredDirections,
      boolean useIncreasedRipupCosts,
      StoppableThread thread,
      Long deadlineMs) {
    return optRouteItemOnBoard(
        job,
        routingBoard,
        item,
        baselineTraceLength,
        withPreferredDirections,
        useIncreasedRipupCosts,
        thread,
        deadlineMs,
        null);
  }

  static ItemRouteResult optRouteItemOnBoard(
      RoutingJob job,
      RoutingBoard routingBoard,
      Item item,
      double baselineTraceLength,
      boolean withPreferredDirections,
      boolean useIncreasedRipupCosts,
      StoppableThread thread,
      Long deadlineMs,
      BoardStatistics.OptimizerMetrics baselineMetrics) {
    BoardStatistics.OptimizerMetrics metricsBefore =
        baselineMetrics != null
            ? baselineMetrics
            : BoardStatistics.OptimizerMetrics.of(routingBoard);
    double baseline =
        baselineTraceLength > 0 ? baselineTraceLength : metricsBefore.totalWeightedLength();
    RouterCounters routerCountersBefore = new RouterCounters();
    if (item.hasIgnoredNets()) {
      return new ItemRouteResult(item.getId());
    }
    routerCountersBefore.incompleteCount = calculateIncompleteCount(routingBoard);

    Set<Item> rippedConnections = rippedConnectionsOf(item);

    for (Item currentItem : rippedConnections) {
      if (currentItem.isUserFixed() || currentItem.hasIgnoredNets()) {
        return new ItemRouteResult(item.getId());
      }
    }

    routingBoard.removeItems(rippedConnections);
    for (int i = 0; i < item.netCount(); i++) {
      routingBoard.combineTraces(item.getNetNumber(i));
    }

    int ripupCosts = job.routerSettings.getStartRipupCosts();
    if (useIncreasedRipupCosts && job.routerSettings.optimizer != null) {
      ripupCosts *= job.routerSettings.optimizer.additionalRipupCostFactorAtStart;
    }

    if (item instanceof Trace && job.routerSettings.optimizer != null) {
      ripupCosts =
          (int) Math.round(job.routerSettings.optimizer.traceRipupCostFactor * (double) ripupCosts);
    }

    int maxAutoroutePasses =
        job.routerSettings.optimizer != null ? job.routerSettings.optimizer.maxAutoroutePasses : 1;

    BatchAutorouter.autoroutePassesForOptimizingItem(
        job,
        maxAutoroutePasses,
        ripupCosts,
        job.routerSettings.tracePullTightAccuracy,
        withPreferredDirections,
        routingBoard,
        job.routerSettings);

    BoardStatistics.OptimizerMetrics metricsAfter =
        BoardStatistics.OptimizerMetrics.of(routingBoard);
    RouterCounters routerCountersAfter = new RouterCounters();
    routerCountersAfter.incompleteCount = calculateIncompleteCount(routingBoard);

    ItemRouteResult result =
        new ItemRouteResult(
            item.getId(),
            metricsBefore.viaCount(),
            metricsAfter.viaCount(),
            baseline,
            metricsAfter.totalWeightedLength(),
            routerCountersBefore.incompleteCount,
            routerCountersAfter.incompleteCount);
    boolean routeImproved =
        (thread == null || !thread.isStopRequested())
            && (deadlineMs == null || System.currentTimeMillis() < deadlineMs)
            && result.improved();
    result.updateImproved(routeImproved);
    return result;
  }

  /** The connections a candidate rips: its own and, for a trace, those of unfixed neighbours. */
  private static Set<Item> rippedConnectionsOf(Item item) {
    Set<Item> rippedItems = new TreeSet<>();
    rippedItems.add(item);

    if (item instanceof Trace currentTrace) {
      Set<Item> currentContactList = currentTrace.getStartContacts();
      for (int i = 0; i < 2; i++) {
        if (containsOnlyUnfixedTraces(currentContactList)) {
          rippedItems.addAll(currentContactList);
        }
        currentContactList = currentTrace.getEndContacts();
      }
    }

    Set<Item> rippedConnections = new TreeSet<>();
    for (Item currentItem : rippedItems) {
      rippedConnections.addAll(currentItem.getConnectionItems(Item.StopConnectionOption.NONE));
    }
    return rippedConnections;
  }

  /**
   * Returns everything {@link #optRouteItemOnBoard} reads of a candidate besides the pass's board
   * image and flags: the connections it rips, the nets whose traces it combines and whether it is a
   * trace (which scales the rip-up cost). Candidates with equal keys, such as the vias of one
   * connection or the branches beside a fork, produce the same result. Returns null for candidates
   * that are not shared.
   */
  static String candidateKey(RoutingBoard board, int itemId) {
    Item item = board.getItem(itemId);
    if (item == null || item.hasIgnoredNets()) {
      return null;
    }
    StringBuilder key = new StringBuilder(item.getClass().getName());
    for (int i = 0; i < item.netCount(); i++) {
      key.append(':').append(item.getNetNumber(i));
    }
    key.append('|');
    for (Item ripped : rippedConnectionsOf(item)) {
      key.append(ripped.getId()).append(',');
    }
    return key.toString();
  }

  private static FloatPoint getItemPosition(Item item) {
    if (item instanceof Via via) {
      return via.getCenter().toFloat();
    } else if (item instanceof Trace trace) {
      return trace.firstCorner().toFloat();
    }
    return null;
  }

  private static final class CandidateResult {
    final ItemRouteResult result;
    final RoutingBoard board;
    final ChangeSet changes;
    final double scoreGain;
    final FloatPoint itemPosition;
    final long cpuUsedNanos;
    final long allocUsedBytes;

    CandidateResult(
        ItemRouteResult result,
        RoutingBoard board,
        ChangeSet changes,
        double scoreGain,
        FloatPoint itemPosition,
        long cpuUsedNanos,
        long allocUsedBytes) {
      this.result = result;
      this.board = board;
      this.changes = changes;
      this.scoreGain = scoreGain;
      this.itemPosition = itemPosition;
      this.cpuUsedNanos = cpuUsedNanos;
      this.allocUsedBytes = allocUsedBytes;
    }

    /** This result for a duplicate candidate: same routing, board and changes, its own item. */
    CandidateResult forDuplicate(int itemId, FloatPoint position) {
      return new CandidateResult(
          result.withItemId(itemId), board, changes, scoreGain, position, 0, 0);
    }

    /** True if this candidate is a better pick than the other under the commit selection. */
    boolean ranksBefore(CandidateResult other, CommitSelection selection) {
      if (selection == CommitSelection.SCORE_RANKED) {
        return RankedCandidate.BY_SCORE_GAIN.compare(
                new RankedCandidate(result, scoreGain),
                new RankedCandidate(other.result, other.scoreGain))
            < 0;
      }
      return result.improvedOver(other.result);
    }
  }

  /**
   * Routes the connections of one item again on a private copy of the pass's board image. An
   * improved candidate keeps its board; any other candidate board is dropped.
   */
  private static final class OptimizeCandidateTask implements Callable<CandidateResult> {
    private final RoutingJob job;
    private final BoardImage image;
    private final int itemId;
    private final double baselineTraceLength;
    private final boolean withPreferredDirections;
    private final boolean useIncreasedRipupCosts;
    private final StoppableThread thread;
    private final Long deadlineMs;
    private final BoardStatistics.OptimizerMetrics baselineMetrics;
    private final boolean recordChanges;
    private final ScoreTracker scoreRanking;

    OptimizeCandidateTask(
        RoutingJob job,
        BoardImage image,
        int itemId,
        double baselineTraceLength,
        boolean withPreferredDirections,
        boolean useIncreasedRipupCosts,
        StoppableThread thread,
        Long deadlineMs,
        BoardStatistics.OptimizerMetrics baselineMetrics,
        boolean recordChanges,
        ScoreTracker scoreRanking) {
      this.job = job;
      this.image = image;
      this.itemId = itemId;
      this.baselineTraceLength = baselineTraceLength;
      this.withPreferredDirections = withPreferredDirections;
      this.useIncreasedRipupCosts = useIncreasedRipupCosts;
      this.thread = thread;
      this.deadlineMs = deadlineMs;
      this.baselineMetrics = baselineMetrics;
      this.recordChanges = recordChanges;
      this.scoreRanking = scoreRanking;
    }

    @Override
    public CandidateResult call() {
      if ((thread != null && thread.isStopRequested())
          || (deadlineMs != null && System.currentTimeMillis() >= deadlineMs)) {
        return new CandidateResult(new ItemRouteResult(itemId), null, null, 0, null, 0, 0);
      }

      long cpuStart = -1;
      long allocStart = -1;
      ThreadMXBean mxBean = null;
      try {
        mxBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        cpuStart = mxBean.getThreadCpuTime(Thread.currentThread().threadId());
        allocStart = mxBean.getThreadAllocatedBytes(Thread.currentThread().threadId());
      } catch (Throwable ignored) {
      }

      RoutingBoard workerBoard = image == null ? null : image.copy();
      Item item = workerBoard == null ? null : workerBoard.getItem(itemId);
      if (item == null) {
        return new CandidateResult(new ItemRouteResult(itemId), null, null, 0, null, 0, 0);
      }
      workerBoard.generateSnapshot();

      FloatPoint position = getItemPosition(item);

      RoutingJob candidateJob = new RoutingJob();
      candidateJob.name = job.name;
      candidateJob.shortName = job.shortName;
      candidateJob.routerSettings = job.routerSettings.clone();
      candidateJob.thread = thread;
      candidateJob.board = workerBoard;
      candidateJob.timeoutAt = job.timeoutAt;

      ItemRouteResult result =
          optRouteItemOnBoard(
              candidateJob,
              workerBoard,
              item,
              baselineTraceLength,
              withPreferredDirections,
              useIncreasedRipupCosts,
              thread,
              deadlineMs,
              baselineMetrics);

      long cpuUsed = 0;
      long allocUsed = 0;
      if (mxBean != null) {
        try {
          long cpuEnd = mxBean.getThreadCpuTime(Thread.currentThread().threadId());
          if (cpuStart >= 0 && cpuEnd >= cpuStart) {
            cpuUsed = cpuEnd - cpuStart;
          }
          long allocEnd = mxBean.getThreadAllocatedBytes(Thread.currentThread().threadId());
          if (allocStart >= 0 && allocEnd >= allocStart) {
            allocUsed = allocEnd - allocStart;
          }
        } catch (Throwable ignored) {
        }
      }

      ChangeSet changes = null;
      double scoreGain = 0;
      if (scoreRanking != null) {
        changes = ChangeSet.since(workerBoard);
        scoreGain = scoreRanking.scoreWith(changes) - scoreRanking.score();
        result.updateImproved(
            isRunning(thread, deadlineMs)
                && result.incompleteCount() <= result.incompleteCountBefore()
                && scoreGain > 0);
      }
      if (!result.improved()) {
        return new CandidateResult(result, null, null, 0, position, cpuUsed, allocUsed);
      }
      if (changes == null && recordChanges) {
        changes = ChangeSet.since(workerBoard);
      }
      if (!workerBoard.popSnapshot()) {
        FRLogger.warn("BatchOptimizer: failed to remove the candidate-board snapshot");
      }
      workerBoard.clearTransientAutorouteState();
      return new CandidateResult(
          result, workerBoard, changes, scoreGain, position, cpuUsed, allocUsed);
    }
  }

  /**
   * The traces and vias one commit changed on the live board: the ids of the items it inserted or
   * changed and detached copies of what they replaced, so that the commit can be reverted.
   */
  static final class ChangeSet {
    private final List<Integer> insertedIds;
    private final List<Item> removed;
    private final boolean tracesAndViasOnly;

    /** Change of the total trace length in board units. */
    final double lengthDelta;

    final int viaDelta;
    final int bendDelta;

    private ChangeSet(
        List<Integer> insertedIds,
        List<Item> removed,
        boolean tracesAndViasOnly,
        double lengthDelta,
        int viaDelta,
        int bendDelta) {
      this.insertedIds = insertedIds;
      this.removed = removed;
      this.tracesAndViasOnly = tracesAndViasOnly;
      this.lengthDelta = lengthDelta;
      this.viaDelta = viaDelta;
      this.bendDelta = bendDelta;
    }

    /** Returns the changes since the board's last snapshot. */
    static ChangeSet since(RoutingBoard board) {
      List<UndoableObjects.Storable> before = new ArrayList<>();
      List<UndoableObjects.Storable> after = new ArrayList<>();
      board.itemList.changesSinceSnapshot(before, after);
      List<Integer> insertedIds = new ArrayList<>(after.size());
      List<Item> removed = new ArrayList<>(before.size());
      boolean tracesAndViasOnly = true;
      double lengthDelta = 0;
      int viaDelta = 0;
      int bendDelta = 0;
      for (UndoableObjects.Storable storable : after) {
        Item item = (Item) storable;
        insertedIds.add(item.getId());
        tracesAndViasOnly &= item instanceof Trace || item instanceof Via;
        lengthDelta += traceLength(item);
        viaDelta += item instanceof Via ? 1 : 0;
        bendDelta += bendCount(item);
      }
      for (UndoableObjects.Storable storable : before) {
        Item item = (Item) storable;
        Item detached = item.copy(item.getId());
        detached.board = null;
        removed.add(detached);
        tracesAndViasOnly &= item instanceof Trace || item instanceof Via;
        lengthDelta -= traceLength(item);
        viaDelta -= item instanceof Via ? 1 : 0;
        bendDelta -= bendCount(item);
      }
      return new ChangeSet(
          insertedIds, removed, tracesAndViasOnly, lengthDelta, viaDelta, bendDelta);
    }

    private static double traceLength(Item item) {
      return item instanceof Trace trace ? trace.getLength() : 0;
    }

    /** Counts bends as {@link BoardStatistics} does: the inner corners of a polyline trace. */
    private static int bendCount(Item item) {
      if (item instanceof PolylineTrace trace) {
        int cornerCount = trace.polyline().cornerCount();
        return cornerCount >= 3 ? cornerCount - 2 : 0;
      }
      return 0;
    }

    /**
     * Reverts the change: removes the inserted items and inserts copies of the replaced ones.
     * Returns false, without changing the board, if an inserted item is missing or the change
     * involved other items than traces and vias.
     */
    boolean revert(RoutingBoard board) {
      if (!tracesAndViasOnly) {
        return false;
      }
      List<Item> current = new ArrayList<>(insertedIds.size());
      for (int id : insertedIds) {
        Item item = board.getItem(id);
        if (item == null) {
          return false;
        }
        current.add(item);
      }
      for (Item item : current) {
        board.removeItem(item);
      }
      for (Item item : removed) {
        Item copy = item.copy(item.getId());
        if (copy instanceof Via via) {
          via.setPadstack(board.library.padstacks.get(via.getPadstack().id));
        }
        board.insertItem(copy);
      }
      return true;
    }
  }

  /**
   * Returns the current position of the item, which will be rerouted or null, if the optimizer is
   * not active.
   */
  public FloatPoint getCurrentPosition() {
    return this.currentPosition;
  }

  @Override
  public String getId() {
    return "freerouting-optimizer";
  }

  @Override
  protected String getName() {
    return "Freerouting Optimizer";
  }

  @Override
  protected String getVersion() {
    return "1.0";
  }

  @Override
  protected String getDescription() {
    return "Freerouting Optimizer v1.0";
  }

  @Override
  protected NamedAlgorithmType getType() {
    return NamedAlgorithmType.OPTIMIZER;
  }

  private static int calculateIncompleteCount(RoutingBoard board) {
    return board.routingLedger().incompleteCount();
  }

  /** Reads the vias and traces on the board in ascending x order. */
  /**
   * Returns the optimizer's candidate order: one item per distinct (x, y, first layer) key in
   * ascending order, where a via is keyed by its center and a trace by the larger of its end
   * corners. A via wins over a trace with the same key, and among items of one kind the first in
   * the item list wins. Unfixed vias and traces without ignored nets are eligible; a trace touching
   * an unfixed via is not, because the via is optimized instead.
   *
   * <p>This is the order the former selection loop produced by scanning the whole item list once
   * per candidate for the smallest key above the previous one; it is now built with one sort.
   */
  static List<Item> optimizationOrder(BasicBoard board) {
    record Entry(double x, double y, int layer, int rank, int index, Item item) {}
    List<Entry> entries = new ArrayList<>();
    int index = 0;
    Iterator<UndoableObjects.UndoableObjectNode> it = board.itemList.startReadObject();
    for (UndoableObjects.Storable current = board.itemList.readObject(it);
        current != null;
        current = board.itemList.readObject(it)) {
      if (current instanceof Via via && !via.isUserFixed() && !via.hasIgnoredNets()) {
        FloatPoint center = via.getCenter().toFloat();
        entries.add(new Entry(center.x, center.y, via.firstLayer(), 0, index++, via));
      }
    }
    // Traces after vias, so that a via is preferred to a trace at the same location.
    it = board.itemList.startReadObject();
    for (UndoableObjects.Storable current = board.itemList.readObject(it);
        current != null;
        current = board.itemList.readObject(it)) {
      if (current instanceof Trace trace
          && !trace.isShoveFixed()
          && !trace.hasIgnoredNets()
          && !touchesUnfixedVia(trace)) {
        FloatPoint first = trace.firstCorner().toFloat();
        FloatPoint last = trace.lastCorner().toFloat();
        FloatPoint key = first.x < last.x || first.x == last.x && first.y < last.y ? last : first;
        entries.add(new Entry(key.x, key.y, trace.getLayer(), 1, index++, trace));
      }
    }
    java.util.Comparator<Entry> byKey =
        (a, b) -> {
          if (a.x() != b.x()) {
            return a.x() < b.x() ? -1 : 1;
          }
          if (a.y() != b.y()) {
            return a.y() < b.y() ? -1 : 1;
          }
          return Integer.compare(a.layer(), b.layer());
        };
    entries.sort(byKey.thenComparingInt(Entry::rank).thenComparingInt(Entry::index));
    List<Item> result = new ArrayList<>();
    Entry previous = null;
    for (Entry entry : entries) {
      if (previous == null || byKey.compare(previous, entry) != 0) {
        result.add(entry.item());
        previous = entry;
      }
    }
    return result;
  }

  private static boolean touchesUnfixedVia(Trace trace) {
    for (Item contact : trace.getNormalContacts()) {
      if (contact instanceof Via && !contact.isUserFixed()) {
        return true;
      }
    }
    return false;
  }

  protected class ReadSortedRouteItems {

    private final Iterator<Item> order = optimizationOrder(board).iterator();

    Item next() {
      return order.hasNext() ? order.next() : null;
    }
  }
}
