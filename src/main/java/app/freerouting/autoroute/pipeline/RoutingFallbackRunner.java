package app.freerouting.autoroute.pipeline;

import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.results.RoutingResultManifest;
import app.freerouting.drc.ClearanceBaseline;
import app.freerouting.settings.RouterSettings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Re-routes an unfinished board under the configured fallback strategies and keeps the best result.
 * The alternatives run first; then a residual board (a few open connections) is continued from the
 * best result. A fanout-only strategy whose fanout leaves a board an earlier pass loop already
 * started on stops after that fanout. All attempts share one search budget counted in maze search
 * steps, so which attempts run does not depend on the machine's speed or load. The job's work
 * budget (autorouter.max_work), when set, bounds them too: it counts the default configuration's
 * pass loop and every attempt's together, and once it is spent no attempt starts.
 *
 * <p>Ported from freerouting.net (Erik Gavriluk, Experimental Television Center).
 */
final class RoutingFallbackRunner {

  private final Map<String, RoutingFallback.Outcome> passLoopStarts = new LinkedHashMap<>();
  private final BatchAutorouter listenerSource;

  /** The attempts run and the one chosen, for the result manifest; null if none ran. */
  private RoutingResultManifest.FallbackDetail detail;

  /** Maze search steps the fallback attempts may still use; Long.MAX_VALUE means no limit. */
  private long searchStepsLeft = Long.MAX_VALUE;

  /** Passes a continuation may run without beating its incumbent's score. */
  static final int CONTINUATION_STAGNATION_PASSES = 5;

  RoutingFallbackRunner(BatchAutorouter listenerSource) {
    this.listenerSource = listenerSource;
  }

  BatchAutorouter run(
      RoutingJob job,
      BatchAutorouter defaultRouter,
      List<RoutingFallback.Strategy> strategies,
      byte[] input) {
    // A stopped pass loop is not retried (RoutingFallback.isRetryable), so measuring its board
    // first, a full statistics and airline analysis, would only delay the stop.
    if (stopped(job)) {
      return defaultRouter;
    }
    ClearanceBaseline inputBaseline = job.board.clearanceBaseline;
    RoutingFallback.Outcome best =
        RoutingFallback.Outcome.of("default", job.board, job.routerSettings);
    if (best.isCompleteAndClean()) {
      return defaultRouter;
    }
    BatchAutorouter.StopReason reason = defaultRouter.lastStopReason;
    if (!RoutingFallback.isRetryable(reason)) {
      job.logInfo(
          "Routing fallback skipped: the pass loop stopped at its "
              + RoutingFallback.describe(reason)
              + " with "
              + best
              + ".");
      return defaultRouter;
    }
    if (workBudgetSpent(job)) {
      job.logInfo(
          "Routing fallback skipped: the job's work budget is spent ("
              + job.workSpent
              + " of "
              + job.routerSettings.autorouter.maxWork
              + " work units) with "
              + best
              + ".");
      return defaultRouter;
    }
    boolean fanoutRuns = job.routerSettings.isFanoutEnabled() && !job.board.getSmdPins().isEmpty();
    List<RoutingFallback.Strategy> applicable =
        RoutingFallback.applicable(strategies, job.routerSettings, fanoutRuns);
    List<RoutingFallback.Strategy> continuations = new ArrayList<>();
    List<RoutingFallback.Strategy> alternatives = new ArrayList<>();
    for (RoutingFallback.Strategy strategy : applicable) {
      if (strategy.continueBest) {
        continuations.add(strategy);
      } else {
        alternatives.add(strategy);
      }
    }
    boolean residual = RoutingSupervisor.residual(job.routerSettings, best.unrouted);
    if (alternatives.isEmpty() && continuations.isEmpty() && !residual) {
      return defaultRouter;
    }
    String alternativesText =
        alternatives.size()
            + " alternative "
            + (alternatives.size() == 1 ? "strategy" : "strategies");
    String plan;
    if (residual && !alternatives.isEmpty()) {
      plan = "trying " + alternativesText + ", then continuing the best board";
    } else if (residual) {
      plan = "continuing the board";
    } else {
      plan = "trying " + alternativesText;
    }
    job.logInfo(
        "Routing fallback: the default configuration stopped at "
            + RoutingFallback.describe(reason)
            + " with "
            + best
            + "; "
            + plan
            + ".");
    if (defaultRouter.passLoopStartHash != null) {
      this.passLoopStarts.put(defaultRouter.passLoopStartHash, best);
    }
    record(best, defaultRouter);
    this.searchStepsLeft = searchBudget(job.routerSettings, defaultRouter.searchStepsUsed());
    if (this.searchStepsLeft != Long.MAX_VALUE) {
      job.logInfo(
          "Routing fallback search budget: "
              + this.searchStepsLeft
              + " steps for all attempts (the default configuration used "
              + defaultRouter.searchStepsUsed()
              + ").");
    }
    ClearanceBaseline defaultClearance =
        RoutingFallback.captureClearance(job.board, best.violations);
    Attempt bestAttempt = new Attempt(best, job.board, defaultRouter, defaultClearance);
    // Alternatives run before the residual continuations: they start from the input board, so
    // they are what the budget can least afford to skip, while a continuation polishes whichever
    // board is best when they are done.
    for (int i = 0; i < alternatives.size(); i++) {
      RoutingFallback.Strategy strategy = alternatives.get(i);
      if (stopped(job) || bestAttempt.outcome.isCompleteAndClean()) {
        break;
      }
      if (budgetUsedUp(job, alternatives.subList(i, alternatives.size()))) {
        break;
      }
      if (workBudgetSpent(job, alternatives.subList(i, alternatives.size()))) {
        break;
      }
      Attempt attempt = runAttempt(job, input, strategy, false, inputBaseline);
      if (attempt == null) {
        continue;
      }
      if (attempt.router.lastStopReason == BatchAutorouter.StopReason.REPEATED) {
        RoutingFallback.Outcome repeated =
            this.passLoopStarts.get(attempt.router.passLoopStartHash);
        job.logInfo(
            "Routing fallback '"
                + strategy.name
                + "': its fanout left the board an earlier pass loop started on; stopped after the fanout ("
                + (repeated != null ? repeated : attempt.outcome)
                + ").");
        if (stopAlternatives(bestAttempt.outcome, attempt.router.lastStopReason)) {
          break;
        }
        continue;
      }
      if (strategy.fanoutOnly
          && attempt.router.passLoopStartHash != null
          && attempt.router.lastStopReason != BatchAutorouter.StopReason.TIMED_OUT) {
        this.passLoopStarts.putIfAbsent(attempt.router.passLoopStartHash, attempt.outcome);
      }
      job.logInfo(
          "Routing fallback '"
              + strategy.name
              + "': "
              + attempt.outcome
              + " after "
              + RoutingFallback.describe(attempt.router.lastStopReason)
              + ".");
      if (replaces(attempt, bestAttempt)) {
        bestAttempt = attempt;
      } else {
        job.board = bestAttempt.board;
      }
      if (stopAlternatives(bestAttempt.outcome, attempt.router.lastStopReason)) {
        break;
      }
    }
    List<RoutingFallback.Strategy> after = new ArrayList<>(continuations);
    after.addAll(automaticContinuations(job.routerSettings));
    if (!after.isEmpty()) {
      bestAttempt = runContinuations(job, bestAttempt, after);
    }
    job.board = bestAttempt.board;
    job.logInfo(
        "Routing fallback chose '"
            + bestAttempt.outcome.strategy
            + "': "
            + bestAttempt.outcome
            + ".");
    this.detail.chosen = bestAttempt.outcome.strategy;
    return bestAttempt.router;
  }

  /** The fallback and exploration attempts and the chosen one; null if no attempt ran. */
  RoutingResultManifest.FallbackDetail detail() {
    return this.detail;
  }

  private void record(RoutingFallback.Outcome outcome, BatchAutorouter router) {
    if (this.detail == null) {
      this.detail = new RoutingResultManifest.FallbackDetail();
    }
    RoutingResultManifest.FallbackAttempt attempt = new RoutingResultManifest.FallbackAttempt();
    attempt.strategy = outcome.strategy;
    attempt.stopReason = RoutingResultManifest.stopReason(router.lastStopReason);
    attempt.unrouted = outcome.unrouted;
    attempt.newOrChangedViolations = outcome.newOrChanged;
    attempt.clearanceViolations = outcome.violations;
    attempt.routerScore = outcome.score;
    attempt.durationSeconds =
        seconds(router.phaseMetrics.fanout) + seconds(router.phaseMetrics.autorouter);
    this.detail.attempts.add(attempt);
  }

  private static float seconds(RoutingResultManifest.PhaseDetail phase) {
    return phase.durationSeconds != null ? phase.durationSeconds : 0f;
  }

  /**
   * Exploration attempts after fallback. Each attempt is a full route of the best board with a
   * lighter fallback list. Later tries of a kind go further. Stops after {@code explore_patience}
   * attempts without an improvement, or when the board is complete.
   */
  BatchAutorouter explore(RoutingJob job, BatchAutorouter incumbent) {
    if (!RoutingSupervisor.explores(job.routerSettings) || stopped(job)) {
      return incumbent;
    }
    RoutingSupervisor supervisor = new RoutingSupervisor();
    String incumbentName = this.detail != null ? this.detail.chosen : "default";
    RoutingFallback.Outcome best =
        RoutingFallback.Outcome.of(incumbentName, job.board, job.routerSettings);
    // The incumbent's own violations are its baseline, as for the default attempt in run():
    // without one, every candidate with any violation (even the input's) counts them all as new
    // and can never replace the incumbent.
    ClearanceBaseline incumbentClearance =
        RoutingFallback.captureClearance(job.board, best.violations);
    Attempt bestAttempt = new Attempt(best, job.board, incumbent, incumbentClearance);
    if (this.detail == null) {
      record(best, incumbent);
    }
    RouterSettings jobSettings = job.routerSettings;
    int round = 0;
    while (!bestAttempt.outcome.isCompleteAndClean()
        && !supervisor.explorationExhausted(jobSettings)
        && !stopped(job)
        && !workBudgetSpent(job, List.of())) {
      round++;
      RoutingSupervisor.Attempt planned =
          supervisor.nextAttempt(round, jobSettings, bestAttempt.board);
      RouterSettings settings = jobSettings.clone();
      RoutingSupervisor.apply(planned, jobSettings, settings);
      byte[] input;
      try {
        input = bestAttempt.board.serialize(false);
      } catch (RuntimeException e) {
        job.logWarning("Exploration stopped; the best board could not be saved: " + e.getMessage());
        break;
      }
      RoutingBoard copy = (RoutingBoard) BasicBoard.deserialize(input);
      if (copy.clearanceBaseline == null) {
        copy.clearanceBaseline = bestAttempt.board.clearanceBaseline;
      }
      if ("neighbourhood".equals(planned.kind())) {
        int ripped = RetryNeighbourhood.ripNearOpenPins(copy, planned.tryNumber());
        job.logInfo(
            "Exploration '"
                + planned.name()
                + "' ripped "
                + ripped
                + " item(s) within "
                + RetryNeighbourhood.Reach.of(planned.tryNumber()).radiusMm()
                + " mm.");
      }
      job.logInfo("Exploration attempt '" + planned.name() + "' from " + bestAttempt.outcome + ".");
      int openBefore = bestAttempt.outcome.unrouted;
      Attempt attempt =
          runPrepared(
              job, copy, settings, false, "explore-" + planned.kind(), Float.NEGATIVE_INFINITY);
      boolean kept = false;
      if (attempt != null && replaces(attempt, bestAttempt)) {
        bestAttempt = attempt;
        kept = true;
      }
      int openAfter = bestAttempt.outcome.unrouted;
      supervisor.attemptDone(planned, openBefore, openAfter, kept);
    }
    job.board = bestAttempt.board;
    job.routerSettings = jobSettings;
    this.detail.chosen = bestAttempt.outcome.strategy;
    return bestAttempt.router;
  }

  private Attempt runContinuations(
      RoutingJob job, Attempt bestAttempt, List<RoutingFallback.Strategy> continuations) {
    for (int i = 0; i < continuations.size(); i++) {
      RoutingFallback.Strategy strategy = continuations.get(i);
      if (bestAttempt.outcome.isCompleteAndClean() || stopped(job)) {
        break;
      }
      if (!strategy.continueBest
          && !RoutingSupervisor.residual(job.routerSettings, bestAttempt.outcome.unrouted)) {
        break;
      }
      if (budgetUsedUp(job, continuations.subList(i, continuations.size()))) {
        break;
      }
      if (workBudgetSpent(job, continuations.subList(i, continuations.size()))) {
        break;
      }
      RouterSettings settings = job.routerSettings.clone();
      strategy.apply(settings);
      settings.setFanoutEnabled(false);
      // The incumbent already stagnated once: a continuation must beat its score within a short,
      // fixed number of passes or stop, so the budget left goes to the next attempt.
      settings.autorouter.stagnationPassLimit =
          Math.min(stagnationPassLimit(settings), CONTINUATION_STAGNATION_PASSES);
      RoutingBoard copy = bestAttempt.board.deepCopy();
      Attempt attempt =
          runPrepared(job, copy, settings, true, strategy.name, bestAttempt.outcome.score);
      if (attempt == null) {
        break;
      }
      job.logInfo(
          "Routing fallback continuation '"
              + strategy.name
              + "': "
              + attempt.outcome
              + " after "
              + RoutingFallback.describe(attempt.router.lastStopReason)
              + ".");
      if (replaces(attempt, bestAttempt)) {
        bestAttempt = attempt;
      } else if (!strategy.continueBest) {
        job.board = bestAttempt.board;
        job.logInfo(
            "Routing fallback: stopping residual continuations after '"
                + strategy.name
                + "' did not improve the incumbent.");
        break;
      } else {
        job.board = bestAttempt.board;
      }
    }
    return bestAttempt;
  }

  private Attempt runAttempt(
      RoutingJob job,
      byte[] input,
      RoutingFallback.Strategy strategy,
      boolean keepFanoutVias,
      ClearanceBaseline inputBaseline) {
    RoutingBoard board = (RoutingBoard) BasicBoard.deserialize(input);
    if (board.clearanceBaseline == null) {
      board.clearanceBaseline = inputBaseline;
    }
    RouterSettings settings = job.routerSettings.clone();
    strategy.apply(settings);
    Attempt attempt =
        runPrepared(job, board, settings, keepFanoutVias, strategy.name, Float.NEGATIVE_INFINITY);
    if (attempt != null
        && strategy.fanoutOnly
        && attempt.router.lastStopReason == BatchAutorouter.StopReason.REPEATED) {
      RoutingFallback.Outcome repeated = this.passLoopStarts.get(attempt.router.passLoopStartHash);
      if (repeated != null) {
        return new Attempt(
            new RoutingFallback.Outcome(
                strategy.name,
                repeated.unrouted,
                repeated.newOrChanged,
                repeated.violations,
                repeated.score,
                repeated.designBlocked),
            job.board,
            attempt.router,
            attempt.baseline);
      }
    }
    return attempt;
  }

  private Attempt runPrepared(
      RoutingJob job,
      RoutingBoard board,
      RouterSettings settings,
      boolean keepFanoutVias,
      String strategyName,
      float stagnationBaselineScore) {
    RouterSettings savedSettings = job.routerSettings;
    RoutingBoard savedBoard = job.board;
    job.board = board;
    job.routerSettings = settings;
    try {
      boolean removeUnconnectedVias = !keepFanoutVias && !settings.isFanoutEnabled();
      BatchAutorouter router =
          BatchAutorouter.createAttempt(job, board, settings, removeUnconnectedVias);
      router.copyListenersFrom(this.listenerSource);
      router.setStagnationBaselineScore(stagnationBaselineScore);
      if (this.searchStepsLeft != Long.MAX_VALUE) {
        router.setSearchStepLimit(this.searchStepsLeft);
      }
      RoutingFallback.Strategy strategy = RoutingFallback.find(strategyName);
      if (strategy != null && strategy.fanoutOnly) {
        router.setRepeatsPassLoop(hash -> hash != null && this.passLoopStarts.containsKey(hash));
      }
      try {
        router.runBatchLoop();
      } finally {
        if (this.searchStepsLeft != Long.MAX_VALUE) {
          this.searchStepsLeft -= router.searchStepsUsed();
        }
      }
      RoutingFallback.Outcome outcome =
          RoutingFallback.Outcome.of(strategyName, job.board, savedSettings);
      ClearanceBaseline baseline = RoutingFallback.captureClearance(job.board, outcome.violations);
      record(outcome, router);
      return new Attempt(outcome, job.board, router, baseline);
    } catch (RuntimeException e) {
      job.board = savedBoard;
      job.logError("Routing fallback attempt '" + strategyName + "' failed and was discarded.", e);
      return null;
    } finally {
      job.routerSettings = savedSettings;
    }
  }

  private static boolean replaces(Attempt candidate, Attempt incumbent) {
    int changed =
        RoutingFallback.newOrChangedClearance(
            candidate.board, candidate.outcome.violations, incumbent.baseline);
    return RoutingFallback.Outcome.canReplace(candidate.outcome, incumbent.outcome, changed);
  }

  private static boolean stopAlternatives(
      RoutingFallback.Outcome best, BatchAutorouter.StopReason reason) {
    return best.isCompleteAndClean()
        || reason == BatchAutorouter.StopReason.WORK_BUDGET
        || reason == BatchAutorouter.StopReason.TIMED_OUT
        || reason == BatchAutorouter.StopReason.CANCELLED;
  }

  /**
   * Whether the search budget is used up; if so, logs the attempts that will not run. The budget
   * counts maze search steps, so the same attempts run on every machine.
   */
  private boolean budgetUsedUp(RoutingJob job, List<RoutingFallback.Strategy> notRun) {
    if (this.searchStepsLeft > 0) {
      return false;
    }
    StringBuilder names = new StringBuilder();
    for (RoutingFallback.Strategy strategy : notRun) {
      if (!names.isEmpty()) {
        names.append(", ");
      }
      names.append("'").append(strategy.name).append("'");
    }
    job.logInfo("Routing fallback: the search budget is used up; not running " + names + ".");
    return true;
  }

  /**
   * Whether the job's work budget (autorouter.max_work) is spent; if so, logs the attempts that
   * will not run. The budget is the job's, so once it is spent no attempt starts: a pass loop would
   * stop before its first pass, but its fanout and setup would still run outside the budget.
   */
  private static boolean workBudgetSpent(RoutingJob job, List<RoutingFallback.Strategy> notRun) {
    if (!workBudgetSpent(job)) {
      return false;
    }
    StringBuilder names = new StringBuilder();
    for (RoutingFallback.Strategy strategy : notRun) {
      if (!names.isEmpty()) {
        names.append(", ");
      }
      names.append("'").append(strategy.name).append("'");
    }
    job.logInfo(
        "Routing fallback: the job's work budget is spent ("
            + job.workSpent
            + " of "
            + job.routerSettings.autorouter.maxWork
            + " work units)"
            + (names.isEmpty() ? "; no further attempt starts." : "; not running " + names + "."));
    return true;
  }

  /** Whether the job's work budget (autorouter.max_work) is set and spent. */
  static boolean workBudgetSpent(RoutingJob job) {
    Long maxWork =
        job.routerSettings != null && job.routerSettings.autorouter != null
            ? job.routerSettings.autorouter.maxWork
            : null;
    return maxWork != null && maxWork > 0 && job.workSpent >= maxWork;
  }

  /** The fallback search budget never goes below this many steps. */
  static final long MIN_SEARCH_BUDGET_STEPS = 8_000_000L;

  /**
   * The fallback search budget in steps: the configured percentage of the steps the default
   * configuration used, but at least {@link #MIN_SEARCH_BUDGET_STEPS}. Long.MAX_VALUE when the
   * settings set no limit.
   */
  static long searchBudget(RouterSettings settings, long defaultSteps) {
    Integer percent =
        settings.autorouter != null ? settings.autorouter.fallbackSearchBudgetPercent : null;
    if (percent == null || percent <= 0) {
      return Long.MAX_VALUE;
    }
    long share = Math.max(0, defaultSteps) / 100 * percent;
    return Math.max(MIN_SEARCH_BUDGET_STEPS, share);
  }

  private static int stagnationPassLimit(RouterSettings settings) {
    return settings.autorouter != null && settings.autorouter.stagnationPassLimit != null
        ? settings.autorouter.stagnationPassLimit
        : BatchAutorouter.STAGNATION_PASS_LIMIT;
  }

  private static boolean stopped(RoutingJob job) {
    return job.thread.isStopAutoRouterRequested();
  }

  private static List<RoutingFallback.Strategy> automaticContinuations(RouterSettings settings) {
    List<RoutingFallback.Strategy> result = new ArrayList<>();
    RoutingFallback.Strategy continueStrategy = RoutingFallback.find("continue");
    if (continueStrategy != null) {
      result.add(continueStrategy);
    }
    if (settings.getNeckWidthUm() > 0) {
      RoutingFallback.Strategy noNeck = RoutingFallback.find("continue-no-neck");
      if (noNeck != null) {
        result.add(noNeck);
      }
    }
    return result;
  }

  private record Attempt(
      RoutingFallback.Outcome outcome,
      RoutingBoard board,
      BatchAutorouter router,
      ClearanceBaseline baseline) {}
}
