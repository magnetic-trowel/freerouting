# E13: optimizer early exit from candidate score bounds

Skip an optimizer pass when an upper bound on what its candidates can achieve shows that the pass
cannot raise the optimizer score (exact arm), or cannot raise it by more than a margin (bounded
arm).

## Status

Designed; smoke-tested on Issue029-hw48na (150 items, one optimizer pass) and
Issue676-ch32v-tx118s (a floor board); not yet run at scale.

## Hypothesis

Exact arm (`bound`, `bound_all`): byte-identical SES on every board; the optimizer CPU of floor
boards (see E11) drops to about zero, and no other board changes at all (the rule cannot fire
while the score is above 0, see below). Bounded arm (`bound_margin2.5`): with the current bound it
also fires only at the floor; it is included to measure that, and as the hook for a tighter bound.

## Mechanism

References are to commit d4383b44, `autoroute/pipeline/BatchOptimizer.java`.

- A candidate rips its connections (`rippedConnectionsOf`, `:1352-1371`) and routes them again
  (`:1320`). If the new route adds nothing, the board loses the ripped traces' length, vias and
  bends; any real route adds length, so the lower-bound score with those items removed bounds the
  candidate's score from above (the V2/V3 scores never rise with length, vias or bends;
  `core/scoring/BoardStatistics.java:851-920`).
- With `score_ranked` (default) a candidate is improved only if it raises the score on its own
  (`:1555-1561`), and only improved candidates are committed (`:968-1000`, `:1081-1091`), so the
  pass can improve only if the best single bound exceeds the current score. The other selections
  may combine commits; the pass end accepts only a strict score rise (`:441-490`, `:664`), so the
  bound of the union of all ripped items decides.
- Off the floor every candidate's bound is above the current score (removing any trace raises the
  score), so the exact rule never fires; at the clamp floor (`BoardStatistics.java:891-896`) a
  bound is above 0 only if one candidate's items exceed the whole excess above the minimum length.
- A skipped pass returns before evaluating any candidate (after the max-items cut and the empty
  check, `:753-773`). The loop then sees an unchanged board exactly as after a pass without
  commits: rejected, incumbent restored, the rip-up phase switch and the threshold stop apply as
  before (`:441-547`). Not run: the `totalItemsOptimized` count of that pass (matters only with
  `optimizer.max_items`) and the `resultMap` refill (read only by the PRIORITIZED strategy).

Assumption behind exactness: a candidate does not shorten items outside its ripped connections.
The nested router may rip foreign connections and re-route them (rip-up costs are finite), which
could in principle shorten them; at the floor that would have to remove the whole excess above the
minimum length, so the campaign's identity check is the proof obligation.

## Change

Code, `patch.diff` (applies on d4383b44; `BatchOptimizer.java`, test
`BatchOptimizerEarlyExitTest`), read once when the class loads:

- `-Dfreerouting.exp.optimizer_early_exit=off|gain_bound` (default `off` = head).
- `-Dfreerouting.exp.optimizer_early_exit_margin=<percent>` (default 0): also skip a pass when the
  union bound is at most this many percent above the current score; such a pass could gain at most
  that much (the quantified change).

The bound costs one `rippedConnectionsOf` per candidate, the same work `candidateKey` already does
at pass start (`:832`).

## Determinism and time-budget check

The bound is a pure function of the pass's board and candidate list; it reads no clock or budget.

## Boards

`boards.txt`: the three floor boards of E11 (the rule must fire and the SES must match), four
boards where it must never fire (bm01 with two optimizer passes, Issue155, Issue558, hw48na), and
Issue103 optimizer-only with two passes for the margin arm.

## Variants

`variants.txt`: `control`, `bound`, `control_all` and `bound_all` (the union bound under
`commit_selection=all`, compared with `control_all`), `bound_margin2.5`.

## How to run

```
scripts/experiments/build_variant.sh d4383b44 docs/research/experiments/E13-optimizer-early-exit/patch.diff build/e13.jar
python3 scripts/experiments/paired_campaign.py --out logs/E13 \
  --boards docs/research/experiments/E13-optimizer-early-exit/boards.txt \
  --variants docs/research/experiments/E13-optimizer-early-exit/variants.txt \
  --flags "-mt 1 -mp 25 --router.optimizer.enabled=true --router.optimizer.max_threads=1" \
  --parallel 4 --timeout 01:00:00
```
Count the rule's firings with `grep -c "Optimizer pass #.* skipped:" <run>/console.log`.

## Metrics and decision rule

- `bound` vs `control` and `bound_all` vs `control_all`: SES sha1 identical on every board
  (required; any mismatch rejects the exact arm or the assumption above), optimizer CPU, firings.
- `bound_margin2.5` vs `control`: final optimizer score loss per board (must be at most 2.5% of
  the gain, by construction), CPU saved; reload-DRC violations never increase, completion never
  drops.
- Adopt the exact arm if every SES is identical and it fires on at least one board; it then
  replaces E11's `skip` arm (same effect, with a proof instead of a heuristic).

## Cost estimate

Like E11 for the floor boards (seconds each); bm01 with two passes about 240 s per variant;
Issue103 with two passes about 2,250 s for the control and each variant that does not fire. Five
variants: about 4 CPU-hours, three of them Issue103.

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

1. The bound is loose off the floor: removing a candidate's items is far more than re-routing
   them can save, so the margin arm has nothing to work with. A tighter per-candidate bound needs
   a reconnection lower bound (for a two-pin net the distance between the pads minus their
   extents; for multi-pin nets the distance between the components the rip leaves), which also
   has to respect T-junctions onto the rest of the net.
2. A candidate whose items include fixed ones returns unimproved anyway; including them in the
   bound only loosens it.
