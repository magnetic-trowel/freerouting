# E12: optimizer pass policy

Measure which rule for running another optimizer pass (improvement threshold, maximum passes,
preferred-direction alternation, rip-up cost phases, commit selection, commits per pass,
consecutive-failure canary) gives the best final optimizer score per CPU-second.

## Status

Designed; smoke-tested on Issue029-hw48na (150 items, one optimizer pass); not yet run at scale.

## Hypothesis

Most of the optimizer's CPU goes into passes that gain little: on Issue103 (already routed,
optimizer only) pass 1 gains 0.17% in 144 s and the second pass, which exists only because pass 1
switches from increased to normal rip-up costs, runs about 2,100 s and finds nothing. Expected:
`ripup_increased_only` or `failures12` keep at least 99.5% of the control's score gain on every
board at 30-60% less optimizer CPU; `sel_all`/`commits8` may gain a little score at the same CPU.

## Mechanism

References are to commit d4383b44, `autoroute/pipeline/BatchOptimizer.java`.

- Pass loop `:394-548`: stops at `max_passes` (default 100, `DefaultSettings.java:239`), at
  `max_items`, or when the pass improvement in percent is at or below
  `improvement_threshold` (default 2.5, `DefaultSettings.java:168`; test at `:536-547`).
- Rip-up cost phases: the loop starts with increased rip-up costs (`:299`); the first pass at or
  below the threshold switches to normal costs and forces one more pass (`:525-531`).
- Preferred directions alternate per pass, odd passes on (`:415`).
- Commit selection (`:741-752`, `:1005-1110`): `score_ranked` (default,
  `DefaultSettings.java:252`), `all`, `best_prefix`, `score_gated`; `max_commits_per_pass`
  (default 0 = unlimited, `DefaultSettings.java:251`, mapping at `:1013`).
- Consecutive-failure canary: a pass stops after 12 (pass 1) or 50 (later passes,
  `DefaultSettings.java:249`) candidates in a row that do not improve (`:816-917`). On Issue103 a
  candidate costs about 40 s, so the canary of 50 is what pass 2 costs.

## Change

Settings for most variants (all existing `router.optimizer.*` options):
`improvement_threshold`, `max_passes`, `commit_selection`, `max_commits_per_pass`,
`max_consecutive_failures`.

Two small code switches, `patch.diff` (applies on d4383b44; `BatchOptimizer.java`, test
`BatchOptimizerPassPolicyTest`), read once when the class loads, defaults equal to head:

- `-Dfreerouting.exp.optimizer_preferred_directions=alternate|inverted|always|never`
  (default `alternate` = odd passes, as at `:415`).
- `-Dfreerouting.exp.optimizer_ripup_phase=default|increased_only|normal_only`
  (`default` as at `:299` and `:525`; `increased_only` never switches to normal costs and
  `normal_only` starts with them, so both stop at the first pass at or below the threshold).

## Determinism and time-budget check

All variants are fixed policies of pass number and measured score; none reads the clock or the
remaining budget. Candidates are consumed in candidate order regardless of thread count, so the
policy decisions are deterministic.

## Boards

`boards.txt`: Issue103-Board-Routed (optimizer only, pre-flight guards off as in the review's
measurements), bm01 (25 passes), hw48na (150 items), bm05 (8 passes), BBD Mars-64 (10 passes),
rpi_splitter (8 passes) and Issue155-CH376 (complete, cheap). Routing is identical across the
variants on each board, so differences are the optimizer's.

## Variants

`variants.txt`, first line the control (all defaults): threshold 0/1/5, `max_passes` 1/2/4,
preferred directions always/never/inverted, rip-up phase increased-only/normal-only, commit
selection all/best_prefix/score_gated, commits per pass 1/8, canary 12/25.

## How to run

```
scripts/experiments/build_variant.sh d4383b44 docs/research/experiments/E12-optimizer-pass-policy/patch.diff build/e12.jar
python3 scripts/experiments/paired_campaign.py --out logs/E12 \
  --boards docs/research/experiments/E12-optimizer-pass-policy/boards.txt \
  --variants docs/research/experiments/E12-optimizer-pass-policy/variants.txt \
  --flags "-mt 1 --router.optimizer.enabled=true --router.optimizer.max_threads=1" \
  --parallel 4 --timeout 02:00:00
```
The per-board flags must come before the variant flags on the command line (a variant's
`max_passes` must win over none; no board line sets an optimizer pass option). Run Issue103 last
or on its own machine: its control alone is about one CPU-hour.

## Metrics and decision rule

Per board and variant: final optimizer score (from "Optimization stage completed ... final
optimizer score"), vias, wire length, optimizer CPU seconds (same line), passes, reload-DRC
violations and unconnected items.

- Hard constraints: reload-DRC violations never increase and completion never drops on any board.
- Gain per board = final optimizer score minus the score at the start of the optimizer (identical
  across variants). Efficiency = total gain over all boards / total optimizer CPU.
- Adopt a variant (as the new default) only if it is a Pareto improvement over the control:
  total gain at least 99.5% of the control's and efficiency at least 20% better, or total gain
  better with CPU no worse than +10%; and no board loses more than 1% of its gain or gains vias.
  Combine the winning settings and confirm the combination in a second campaign.

## Cost estimate

Control CPU from earlier runs at one optimizer thread: Issue103 about 145 s per pass for pass 1
and about 2,100 s for pass 2, bm01 about 120 s with one optimizer pass, hw48na about 10 s, bm05
about 32 s, Mars-64 about 67 s (one optimizer pass each). With default `max_passes` the control
and the slow variants run several passes: about 19 variants x 7 boards, roughly 25 CPU-hours, of
which Issue103 is about two thirds. Dropping Issue103 to `max_passes=2` (board flag) halves it.

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

1. `max_items` and the canary make the result depend on candidate order; a variant that changes
   the order (`dir_*`) changes which candidates run, so compare whole runs, not passes.
2. Variants with more passes are the ones most likely to hit `--timeout`; those runs are excluded
   by the harness, which biases against them. Use a long safety timeout.
3. Follow-up: an improvement threshold measured on the excess (1000 minus score) instead of the
   score itself; on a score of 790 the 2.5% threshold asks for an 8% cut in excess.
