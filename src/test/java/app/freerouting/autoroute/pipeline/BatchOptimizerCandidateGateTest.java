package app.freerouting.autoroute.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.freerouting.core.scoring.BoardStatistics;
import org.junit.jupiter.api.Test;

/** Verifies the optimizer candidate vetoes and score-ranking rule. */
class BatchOptimizerCandidateGateTest {

  @Test
  void connectivityRegressionVetoesHigherOptimizerScore() {
    BoardStatistics candidate = statistics(1, 0);

    assertEquals(
        "CONNECTIVITY_REGRESSION",
        BatchOptimizer.optimizerCandidateRejectionReason(0, 0, 500.0f, candidate, 900.0f));
  }

  @Test
  void newClearanceViolationVetoesHigherOptimizerScore() {
    // One violation, as the incumbent had, but a new one: the count alone would accept it.
    BoardStatistics candidate = statistics(0, 1);

    assertEquals(
        "NEW_CLEARANCE_VIOLATION",
        BatchOptimizer.optimizerCandidateRejectionReason(0, 1, 500.0f, candidate, 900.0f));
  }

  @Test
  void moreCompleteUglierCandidateIsRejected() {
    BoardStatistics candidate = statistics(0, 0);

    assertEquals(
        "OPTIMIZER_SCORE_NOT_IMPROVED",
        BatchOptimizer.optimizerCandidateRejectionReason(1, 0, 500.0f, candidate, 400.0f));
  }

  @Test
  void candidateWithNoVetoAndHigherOptimizerScoreIsAccepted() {
    BoardStatistics candidate = statistics(0, 0);

    assertNull(BatchOptimizer.optimizerCandidateRejectionReason(1, 0, 500.0f, candidate, 600.0f));
  }

  private static BoardStatistics statistics(int incompleteCount, int clearanceViolationCount) {
    BoardStatistics statistics = new BoardStatistics();
    statistics.connections.incompleteCount = incompleteCount;
    statistics.clearanceViolations.totalCount = clearanceViolationCount;
    return statistics;
  }
}
