# E11: optimizer score floor

Let the V3 optimizer score keep ranking boards whose excess length exceeds the minimum length
(soft floor), or skip the optimizer at once on such boards (skip arm), instead of running passes
that cannot be accepted.

## Status

Designed; smoke-tested on Issue029-hw48na (150 items, one optimizer pass) and
Issue676-ch32v-tx118s (a floor board); not yet run at scale.

## Hypothesis

On boards whose V3 score is clamped at 0:

- `skip`: the same SES as the control (nothing can be accepted at the floor today) with the
  optimizer CPU removed (the control runs two passes that are rejected).
- `soft`: the optimizer can rank and accept candidates again, so vias and trace length drop on
  floor boards (expect a few percent fewer vias on the plane boards, which are via-heavy), at the
  cost of the optimizer CPU that the control already spends.

On every board whose score stays above the knee (50 by default) the result must be
byte-identical: the soft floor equals the clamp there.

## Mechanism

References are to commit d4383b44.

- `BoardStatistics.getV3OptimizerScore` (`core/scoring/BoardStatistics.java:870-897`) returns
  `max(0, 1000 - 1000 * excess / max(minTraceLength, 1))`. Once the equivalent excess (length over
  the MST bound plus vias and bends converted to length) reaches the minimum length, every board
  scores 0.
- With the default `commit_selection=score_ranked` a candidate counts as improved only if its
  score gain is positive (`BatchOptimizer.java:1555-1561`), and a pass is accepted only if the
  score rises strictly (`BatchOptimizer.java:652-668`, `664`). At the floor a single candidate
  would have to remove the whole excess above the minimum length, so no candidate is improved,
  the pass is rejected and the incumbent restored (`BatchOptimizer.java:441-490`).
- The pass-improvement percentage is 0 when the score before the pass is 0
  (`BatchOptimizer.java:501`). Pass 1 then switches from increased to normal rip-up costs and
  forces another pass (`BatchOptimizer.java:525-531`); pass 2 stops the loop
  (`BatchOptimizer.java:536-547`). Each pass evaluates candidates until the consecutive-failure
  canary stops it (12 in pass 1, 50 afterwards; `BatchOptimizer.java:816-917`).
- Why the plane boards are at the floor: the bounds (`BoardStatisticsBoundsCalculator.java:66-90`)
  take a plane as one terminal at the pour's centre of gravity (`:170`) and give its SMD pads no
  mandatory via, while the router drops one via per pad onto the plane; with one via worth several
  mm of trace the via excess alone exceeds the small MST length of such boards.

## Change

Code, `patch.diff` (applies on d4383b44; `src/main/java/.../BoardStatistics.java`,
`BatchOptimizer.java`, test `OptimizerScoreSoftFloorTest`). One JVM property, read once when the
class loads:

- `-Dfreerouting.exp.optimizer_score_floor=clamp` (default): current behaviour; the code path is
  the same arithmetic as head (`penalty` is the same expression, evaluated in the same order).
- `=soft`: the V3 score is `1000 - penalty` down to the knee and `knee^2 / (knee + t)` below it,
  where `t` is the penalty beyond the knee. Continuous with the same slope at the knee, strictly
  decreasing, always positive; in the tail a relative score change equals the relative change of
  the excess beyond the knee point, so the 2.5% pass threshold keeps a meaning.
  `-Dfreerouting.exp.optimizer_score_floor_knee=<score>` (default 50, clamped to [1, 1000]).
- `=skip`: a new pre-flight guard (after the incomplete-connections guard) skips the optimizer
  when the V2/V3 score is at most 0. Not applied when `enable_preflight_guards=false`.

The soft floor changes every consumer of the V3 score (result JSON, DRC report `optimizerScore`,
pass logs), which is intended for the experiment.

## Determinism and time-budget check

Both arms are pure functions of the board and settings; no decision reads the clock or the
remaining budget. The property is read once per JVM.

## Boards

`boards.txt`: three floor boards found by scanning the reload-DRC `optimizerScore` of the head
campaign (`SP/camp3/run1/master/*/drc.json`): Issue957-smoke-ldo-led, Issue676-ch32v-tx118s and
Issue093-interf_u (all three routed to 0 open connections by the router, score 0.00). On
Issue957 head's optimizer is already skipped by the "all vias are mandatory layer transitions"
guard (probe run, `BatchOptimizer.java:220-223`), so it only checks that the arms leave a
guarded board alone; Issue676 and Issue093 are the boards where the control spends the
rejected passes. The boards
named in the review (Camera-Modules LG-G2 shim, induction-hob temperature sensor) are not in
`fixtures/`. Regression boards: bm01, bm02, bm07, Issue155, Issue558 (scores 770 to 860) and the
hw48na smoke board; Issue367-Charger (310) is the only fixture near the knee and changes only with
knee 200.

## Variants

`variants.txt`: `control`, `soft50`, `soft200`, `skip`.

## How to run

```
scripts/experiments/build_variant.sh d4383b44 docs/research/experiments/E11-optimizer-score-floor/patch.diff build/e11.jar
python3 scripts/experiments/paired_campaign.py --out logs/E11 \
  --boards docs/research/experiments/E11-optimizer-score-floor/boards.txt \
  --variants docs/research/experiments/E11-optimizer-score-floor/variants.txt \
  --flags "-mt 1 -mp 25 --router.optimizer.enabled=true --router.optimizer.max_threads=1" \
  --parallel 4 --timeout 00:40:00
```
(`head` in `variants.txt` is the jar built from d4383b44 plus this patch.)

## Metrics and decision rule

Per board: reload-DRC violations by type and unconnected items, vias, wire length, optimizer CPU
(the "Optimization stage completed ... total CPU seconds" line), SES sha1.

- Every variant: reload-DRC violations never increase and completion never drops on any board.
- `soft50` and `soft200` must be byte-identical to the control on every board whose control score
  stays above the knee (a mismatch is a bug in the switch, not a result).
- Adopt `skip` if it is byte-identical to the control on all floor boards (it saves the optimizer
  CPU outright). Adopt `soft50` instead if it lowers the total vias of the floor boards by at
  least 3% or their total length by at least 2%, with no length or via increase above 1% on any
  board, at an optimizer CPU per floor board no worse than 3x the control. Otherwise adopt `skip`
  and fix the bounds (follow-up 1).

## Cost estimate

Floor boards route in 5 to 40 s CPU each at 25 passes; the control's two rejected optimizer passes
cost one canary's worth of candidates each. Smoke: see below. Ten boards x four variants is about
2 CPU-hours at one optimizer thread, dominated by bm01 (about 120 s per run with one pass) and the
regression boards with up to 100 optimizer passes.

## Smoke-test results

Pending at hand-off: the machine's job slots were saturated (about 100 queued jobs), so the
smoke runs are queued behind them in one slot (`SP/agent-exp-optimizer/smoke.sh`). They run,
from one jar built from d4383b44 plus the E11, E12 and E13 patches, Issue029-hw48na with
`-mt 1 --router.max_items=150 --router.optimizer.max_passes=1` for the head jar (`fix9.jar`), the
property-off control and one arm of each experiment, and Issue676-ch32v-tx118s (`-mt 1 -mp 25`,
default optimizer) for the control and the E11 and E13 arms, then spotlessCheck, checkstyle and
the targeted tests. Results: `smoke-summary.txt` in this directory's copy under
`SP/experiments/` (CPU, wall, router counts, optimizer lines, SES sha1 and identity against the
control).

Probe with the head jar (`fix9.jar`, `-mt 1 -mp 25`, default optimizer): Issue957-smoke-ldo-led
routes to 0 unrouted and 0 violations in 4.7 s user CPU (9.9 s wall), and the optimizer is
skipped by the mandatory-via guard, so this board does not pay for the floor.

## Risks and follow-ups

1. The floor on plane boards is a bound problem first: a plane-net SMD pad on a layer without the
   pour needs one via, which the bounds do not count. Counting it in `minViaCount` (and not taking
   the pour centroid as an MST terminal) would lift these boards off the floor with the clamp
   kept; that is a separate experiment.
2. With `soft` the 2.5% threshold acts on small scores; a board deep in the tail may run more
   passes than before. Watch optimizer CPU on the floor boards.
3. The soft tail is also used by the pre-flight score guards (`>= 950`, `>= 995`); they are far
   above any sensible knee, so they do not change.
