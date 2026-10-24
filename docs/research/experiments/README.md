# Experiment packages

Ready-to-run experiments for Freerouting's routing quality and speed. Each package was designed
against commit `d4383b44` and contains a `README.md` (hypothesis, mechanism with code references,
change, determinism check, boards, variants, run commands, decision rule, cost estimate, smoke
results, risks), a `boards.txt`, a `variants.txt` and, for code experiments, a `patch.diff`.

Code experiments are switched by a JVM system property `-Dfreerouting.exp.<name>=...` that is read
once at startup and defaults to the current behaviour, so one jar runs every arm and the
property-off arm is meant to be byte-identical to head. None of the patches is applied to the main
code; a package only becomes a change after its campaign passes its decision rule.

The packages were smoke-tested on a shared 4-core machine, so CPU and wall times in them are noisy
and several smoke runs are still marked pending. None has been run at scale.

## Index

| Id | Package | Kind | Idea | Smoke result |
|---|---|---|---|---|
| E01 | [`E01-wire-snap-tail-cleanup`](E01-wire-snap-tail-cleanup/README.md) | code | Snap DSN wire ends within 1 um onto pin centres; remove leftover tails and dangling vias at the end of routing. | Control byte-identical to head on 3 boards; removes dangling tracks and vias; caniot at 5 passes: router unrouted 28 to 24, reload-DRC unconnected 19 to 20. |
| E02 | [`E02-maze-door-tiebreak`](E02-maze-door-tiebreak/README.md) | code | Keep two doors that share an id and an equal cost in the maze list instead of dropping the second. | Smoke pending. |
| E03 | [`E03-via-fitting-drills`](E03-via-fitting-drills/README.md) | code | Offer via locations in narrow drill pockets beside fine-pitch pins. | Smoke pending; earlier caniot prototype: 9 to 6 open, +15 vias, +18% CPU. |
| E04 | [`E04-negotiated-rip-cost`](E04-negotiated-rip-cost/README.md) | code | Per-net rip history that raises the cost of ripping the same nets again. | No effect on bm04 at any weight; extend the history to the other rip paths first. |
| E05 | [`E05-same-pass-requeue`](E05-same-pass-requeue/README.md) | code | Re-route the nets an attempt ripped within the same pass. | bm04 at 8 passes: 5 to 1 open, +3 vias, -7% CPU (one board, one sample). |
| E06 | [`E06-transactional-last-mile`](E06-transactional-last-mile/README.md) | code | On stagnation, try one cheap rip on a board copy and keep it only if the open count drops. | bm04: transactions kept, but the run ended at 7 open against 5. |
| E07 | [`E07-fallback-order`](E07-fallback-order/README.md) | settings | Cheaper fallback strategy orders and subsets. | bm06: short-escape first with residual 0 gives the same SES at 42.9 s against 85.9 s CPU. |
| E08 | [`E08-parallel-fallback`](E08-parallel-fallback/README.md) | code | Run the fallback alternatives on threads and fold them in strategy order. | Identical SES at 0, 2 and 4 threads on Issue313 and at 0 and 2 on Issue367; bm06 not run. |
| E09 | [`E09-cost-sweep`](E09-cost-sweep/README.md) | settings | Fractional factorial over seven router costs, with seed-noise arms. | Flags verified on bm02 and caniot; seed noise is large. |
| E10 | [`E10-per-board-adaptive`](E10-per-board-adaptive/README.md) | settings | Test whether a rule computed from the DSN picks better settings per board. | Feature extractor and fitting scripts validated on synthetic data; no routing runs. |
| E11 | [`E11-optimizer-score-floor`](E11-optimizer-score-floor/README.md) | code | Soft score floor or skip the optimizer on boards whose V3 score is clamped at 0. | Smoke on hw48na and ch32v. |
| E12 | [`E12-optimizer-pass-policy`](E12-optimizer-pass-policy/README.md) | code | Rules for running another optimizer pass. | Smoke on hw48na. |
| E13 | [`E13-optimizer-early-exit`](E13-optimizer-early-exit/README.md) | code | Skip an optimizer pass whose candidates cannot raise the score. | Smoke on hw48na and ch32v; exact arm keeps the SES. |
| E14 | [`E14-plane-fill-connectivity`](E14-plane-fill-connectivity/README.md) | code | Find pads cut off from the pour fill and reconnect them. | Detection finds tairakb's cut-off GND pad; the first repair was undone by trace normalization. |
| E15 | [`E15-routed-means-connected`](E15-routed-means-connected/README.md) | code | Fail a connection whose inserted path does not reach its target. | Fires on 56-66% of ROUTED connections; diagnose before any campaign. |
| E16 | [`E16-net-ordering`](E16-net-ordering/README.md) | code | Deterministic alternative orders for the autoroute pass. | See package. |
| E17 | [`E17-dense-escape`](E17-dense-escape/README.md) | code | Planned escape pattern for fine-pitch QFP and BGA parts. | See package. |
| E18 | [`E18-result-preserving-speedups`](E18-result-preserving-speedups/README.md) | code | Speedups that must keep the SES byte-identical. | Smoke on bm01, Issue066 and myboard. |
| E19 | [`E19-large-board-scaling`](E19-large-board-scaling/README.md) | settings | Profiles and heap measurements on large boards. | Smoke on bm01, myboard and TeamAdapt. |
| E20 | [`E20-memory-footprint`](E20-memory-footprint/README.md) | code | Live-heap measurement; compress board history and fallback input. | Identical SES on 3 boards; TeamAdapt live heap 69.4 to 63.4 MB. |

## How to run

Every package writes its run commands against the paired harness in `scripts/experiments/`
(`build_variant.sh` builds a jar from a commit plus an optional patch, `paired_campaign.py` routes
every board with every variant under the same flags and checks each result with the reload DRC).
The default common flags are `-mt 1 -mp 25 --router.optimizer.enabled=false` unless the
experiment is about the optimizer or threading. The timeout is a safety stop only: runs that hit
it are reported separately and left out of comparisons.

Decision rules share three requirements: clearance violations from the reload DRC never increase,
completion never drops on any board that finishes, and a speed change must keep the SES
byte-identical unless the package says otherwise.

## Caveats for comparisons

- Until the rules-precedence fix (commit `c9cea243`), the `(autoroute_settings ...)` scope of
  a `<design>.rules` file next to the DSN overrode the command line. Two campaign boards ship such
  a file: `Issue029-hw48na` and `Issue107-freq_teiler_200kHz_kicad`. Their earlier runs with
  `--router.optimizer.enabled=false` still ran the optimizer. Compare them only between runs of
  the same jar generation.
- Boards that hit the timeout depend on machine load; leave them out of comparisons.
- The smoke runs of E11-E13 used Issue029-hw48na before that fix, so their optimizer settings
  came partly from the board's rules file.
- Wire lengths quoted from the earlier campaign summaries came from a script that skipped SES
  paths with fractional coordinates and under-counted length (Issue297-myboard: 3208.7 mm against
  3499.7 mm). Via and completion counts are not affected; `scripts/experiments/paired_campaign.py`
  parses every path.
- The reload DRC drops SES wires whose corners have fractional coordinates
  (`SesPathGeometry.crossingLines`), which inflates its unconnected count on some boards
  (Issue209-split05: 38 of 354 wires lost). Compare router unrouted counts alongside it.
