package app.freerouting.autoroute.pipeline;

import app.freerouting.autoroute.events.BoardUpdatedEventListener;
import app.freerouting.autoroute.events.TaskStateChangedEventListener;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.RoutingStage;
import app.freerouting.settings.RouterSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Sequences the shared fanout, autoroute, and optimization stages for a routing job. */
public final class RoutingPipeline {

  /** Receives lifecycle callbacks between pipeline stages. */
  public interface StageListener {
    /** Invoked after fanout and autorouting have completed. */
    default void afterRouting(BatchAutorouter autorouter) {}

    /** Invoked immediately before optimization starts. */
    default void beforeOptimization(BatchOptimizer optimizer) {}

    /** Invoked after optimization has completed. */
    default void afterOptimization(BatchOptimizer optimizer) {}
  }

  private final RoutingJob job;
  private BatchAutorouter autorouter;
  private final BatchOptimizer optimizer;
  private final List<StageListener> stageListeners = new ArrayList<>();

  private RoutingPipeline(RoutingJob job, Function<RoutingJob, BatchOptimizer> optimizerFactory) {
    this.job = job;
    normalizeRouterAlgorithm(job);
    this.autorouter = new BatchAutorouter(job);
    this.optimizer = job.routerSettings.getRunOptimizer() ? optimizerFactory.apply(job) : null;
  }

  /** Creates the canonical routing pipeline for a routing job. */
  public static RoutingPipeline create(RoutingJob job) {
    return new RoutingPipeline(job, BatchOptimizer::create);
  }

  /** Creates a pipeline using the GUI optimizer policy. */
  public static RoutingPipeline createForGui(RoutingJob job) {
    return create(job);
  }

  /** Creates a pipeline using the headless optimizer policy. */
  public static RoutingPipeline createForHeadless(RoutingJob job) {
    return create(job);
  }

  /** Returns the shared autorouter stage. */
  public BatchAutorouter getAutorouter() {
    return this.autorouter;
  }

  /** Returns the configured optimizer stage, or {@code null} when optimization is disabled. */
  public BatchOptimizer getOptimizer() {
    return this.optimizer;
  }

  /** Registers a listener for stage transitions. */
  public void addStageListener(StageListener listener) {
    this.stageListeners.add(listener);
  }

  /** Forwards board updates from both algorithm stages to the supplied listener. */
  public void addBoardUpdatedEventListener(BoardUpdatedEventListener listener) {
    this.autorouter.addBoardUpdatedEventListener(listener);
    if (this.optimizer != null) {
      this.optimizer.addBoardUpdatedEventListener(listener);
    }
  }

  /** Forwards task-state updates from both algorithm stages to the supplied listener. */
  public void addTaskStateChangedEventListener(TaskStateChangedEventListener listener) {
    this.autorouter.addTaskStateChangedEventListener(listener);
    if (this.optimizer != null) {
      this.optimizer.addTaskStateChangedEventListener(listener);
    }
  }

  /** Runs the configured stages in order. */
  public void run() {
    if (this.job != null && this.job.board != null) {
      this.job.board.awaitPostLoad();
    }
    runRoutingStage();
    runOptimizationStage();
    connectPlaneFillCutOffs();
    reportPlaneFillCutOffs();
    this.job.stage = RoutingStage.IDLE;
  }

  /**
   * Routes the copper that foreign tracks cut off from the fill of its pour to the rest of its net,
   * after the router and the optimizer, whose connectivity follows pour polygons and so sees none
   * of it ({@link BatchAutorouter#connectPourCutOffs}).
   */
  private void connectPlaneFillCutOffs() {
    if (this.job.board == null
        || this.job.thread.isStopRequested()
        || !this.job.routerSettings.getRunRouter()) {
      return;
    }
    this.autorouter.board = this.job.board;
    int connected = this.autorouter.connectPourCutOffs();
    if (connected > 0) {
      this.job.logInfo(
          "Connected "
              + connected
              + " group(s) of copper that foreign tracks had cut off from the fill of their pour.");
    }
  }

  /**
   * Warns about pads and vias that the final board connects only through a pour polygon, while
   * foreign tracks cut them off from the pour's fill. The board's connectivity cannot see them (it
   * follows pour polygons, not fills), so this is the only report of them besides the DRC.
   */
  private void reportPlaneFillCutOffs() {
    if (this.job.board == null || this.job.thread.isStopRequested()) {
      return;
    }
    String report = AutorouteUnroutedReport.planeFillCutOffs(this.job.board);
    if (report != null) {
      this.job.logWarning(report);
    }
  }

  private void runRoutingStage() {
    boolean routerEnabled =
        this.job.routerSettings.getRunRouter()
            && (this.job.routerSettings.autorouter.maxPasses == null
                || this.job.routerSettings.autorouter.maxPasses >= 0);

    if (routerEnabled || this.job.routerSettings.isFanoutEnabled()) {
      this.job.stage = RoutingStage.ROUTING;
    }

    if (routerEnabled && !this.job.thread.isStopAutoRouterRequested()) {
      List<RoutingFallback.Strategy> strategies =
          RoutingFallback.parse(
              this.job.routerSettings,
              name ->
                  this.job.logWarning(
                      "Unknown routing fallback strategy '"
                          + name
                          + "' ignored; known strategies: "
                          + knownStrategyNames()
                          + "."));
      byte[] input = null;
      if (!strategies.isEmpty() && this.job.board != null) {
        try {
          input = this.job.board.serialize(false);
        } catch (RuntimeException e) {
          this.job.logWarning(
              "Routing fallback disabled; the input board could not be saved: " + e.getMessage());
        }
      }
      this.autorouter.runBatchLoop();
      RoutingFallbackRunner fallback = new RoutingFallbackRunner(this.autorouter);
      if (input != null) {
        this.autorouter = fallback.run(this.job, this.autorouter, strategies, input);
      }
      if (RoutingSupervisor.explores(this.job.routerSettings)) {
        this.autorouter = fallback.explore(this.job, this.autorouter);
      }
      this.job.resultPhaseMetrics.fallback = fallback.detail();
    } else if (this.job.routerSettings.isFanoutEnabled()
        && !this.job.thread.isStopAutoRouterRequested()) {
      Integer originalMaxPasses = this.job.routerSettings.autorouter.maxPasses;
      try {
        this.job.routerSettings.autorouter.maxPasses = 0;
        this.autorouter.runBatchLoop();
      } finally {
        this.job.routerSettings.autorouter.maxPasses = originalMaxPasses;
      }
    }

    // The kept router's stages, not those of the last fallback attempt.
    this.job.resultPhaseMetrics.fanout = this.autorouter.phaseMetrics.fanout;
    this.job.resultPhaseMetrics.autorouter = this.autorouter.phaseMetrics.autorouter;
    this.job.board.finishAutoroute();
    for (StageListener listener : this.stageListeners) {
      listener.afterRouting(this.autorouter);
    }
  }

  private void runOptimizationStage() {
    if (this.optimizer == null || this.job.thread.isStopRequested()) {
      return;
    }

    this.job.stage = RoutingStage.OPTIMIZATION;
    for (StageListener listener : this.stageListeners) {
      listener.beforeOptimization(this.optimizer);
    }
    this.optimizer.runBatchLoop();
    for (StageListener listener : this.stageListeners) {
      listener.afterOptimization(this.optimizer);
    }
  }

  private static String knownStrategyNames() {
    StringBuilder names = new StringBuilder();
    for (RoutingFallback.Strategy strategy : RoutingFallback.KNOWN) {
      if (!names.isEmpty()) {
        names.append(", ");
      }
      names.append(strategy.name);
    }
    return names.toString();
  }

  private static void normalizeRouterAlgorithm(RoutingJob job) {
    String algorithm = job.routerSettings.autorouter.algorithm;
    if (!RouterSettings.ALGORITHM_CURRENT.equals(algorithm)) {
      job.logWarning(
          "The algorithm '"
              + algorithm
              + "' is not supported. The default algorithm '"
              + RouterSettings.ALGORITHM_CURRENT
              + "' will be used instead.");
      job.routerSettings.autorouter.algorithm = RouterSettings.ALGORITHM_CURRENT;
    }
  }
}
