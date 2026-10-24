# E09 Cost sweep: screening the search-cost defaults for completion and vias

A settings-only, 16-run resolution IV fractional factorial over seven router parameters, with
item-order seed arms that measure the chaotic noise floor, to find out whether any default should
change for completion, via count or length.

## 1. Status

Designed; smoke-tested on Issue508-DAC2020_bm02 and Issue269-caniot-tiny-arm (control, d01,
d02, d16, seed1 at `-mp 5`); not yet run at scale.

## 2. Hypothesis

The default costs (via 50, plane via 5, start rip-up 100, SMD-net via factor 0.1, undesired
direction 1.0 plus aspect term, pull-tight 500 board units, fanout on) were set by hand or
inherited from v1.9 and never swept on the current router. A setting that closes at least 25 % of
the 38 connections the router leaves open on the eight target boards (bm04 2, bm05 9, Issue039 9,
Issue313 2, Pajalnaja 2, TeamAdapt 4, Issue684 4, Issue367 6) or lowers vias by at least 5 %
in total, without losing completion or adding violations on any board, should exist if a default
is far from its optimum. The expected outcome, honestly, is that most effects are within the
seed noise floor: then the result is that the defaults are not the lever, which is also worth
knowing before any code change.

## 3. Mechanism (commit d4383b44)

How each factor reaches the search:

| Factor | Flag | Where it acts |
|---|---|---|
| A via cost | `router.scoring.via_costs` | `AutorouteConnectionRouter.java:104-106` picks it for non-plane nets; `AutorouteControl.java:345-354` scales it by the largest via radius (board units) and, for pure-SMD nets, by C: `minNormalViaCost = via_costs * maxViaRadius * factor`. Added to the expansion value at every layer change (`MazeExpansionEngine.java:86,125`) and used in the A* lower bound (`DestinationDistance.java:48-91`). So a via costs as much as `via_costs` via radii of preferred-direction trace (50 x 0.3 mm = 15 mm at default). The explore attempts already vary it by x/÷ 2, 4, 8 (`RoutingSupervisor.java:41-44,126-131`). |
| B start rip-up cost | `router.scoring.start_ripup_costs` | `AutorouteConnectionRouter.java:112,298`: `ripupCosts = start * passNo`, so B scales rip-up resistance in every pass alike and the growth per pass is fixed at linear. Per obstacle: `ripupCosts * halfWidth / detour` (`MazeRipupResolver.java:128-145`); fanout vias are protected only while `ripupCosts <= 2 * start` (`MazeRipupResolver.java:98-99`), i.e. in passes 1-2 whatever B is. Fanout uses `start * (pass + 1)` (`BatchFanout.java:184`). B also seeds `MazeSearchEngine.randomGenerator` (`MazeSearchEngine.java:79-80`), but that generator is only read in the dead randomization branch (`MazeRipupResolver.java:136`, `ripupPassNo` is never assigned), so there is no hidden seed confound. |
| C SMD-net via factor | `router.autorouter.smd_net_via_cost_factor` | `AutorouteControl.java:340-351`: multiplies the via cost of nets whose pins are all single-layer pads. At 0.1 a via on such a net costs 1.5 mm of trace. The `open-escape` fallback sets it to 1.0 (`RoutingFallback.java:79-84`). |
| D undesired-direction trace cost | `router.scoring.default_undesired_direction_trace_cost` | `RouterSettings.java:402-405,478-486`: per layer `D + 0.1*round(10*w/h)` (or `h/w`) against the preferred direction, so a square board already pays about 2x against direction at D = 1. Layers alternate preferred direction (`RouterSettings.java:448-452`); on more than 2 signal layers outer layers get `+0.2*signalLayers` (`RouterSettings.java:490-503`). Used per segment in `MazeExpansionEngine.java:76-77,130-131` and `MazeSearchEngine.java:879-880`. |
| E pull-tight accuracy | `router.trace_pull_tight_accuracy` | Minimum translate distance of the tightener after every routed connection (`AutorouteConnectionRouter.java:203-209`, `TraceTightener.java:123,286`), clamped to at least 100 board units. It is in board units, so 500 is 50 um on KiCad boards (resolution um 10) but 12.7 um on EasyEDA boards (mil 1000) and 0.5 um on Issue676 (mm 1000000); see E10. Affects length, CPU, and through freed space the later connections. |
| F plane via cost | `router.scoring.plane_via_costs` | `AutorouteConnectionRouter.java:104-106`: replaces A for nets with `contains_plane`. Only boards with `(plane ...)` scopes are affected (Issue093, 153, 054, 209, 957, 690, 676, 367, 732). |
| G fanout pre-pass | `router.fanout.enabled` | `AutorouteBatchLoop.java:108-160` runs the SMD escape pre-pass (bm02: 20 escape vias before routing, 16 vias in the result). Confound: every default fallback strategy needs fanout (`RoutingFallback.java:56-96,105-110`, `RoutingFallbackRunner.java:51-53`), so G = false also removes the fallback chain. G therefore measures "fanout plus fanout-based fallback". In the master arm the fallback chain ran on Issue313, TeamAdapt, Issue684, Issue367 and bm06 (5 attempts each; 92 to 136 passes in total) and kept the default board on three of the five (Issue684 chose `direct-first`, bm06 `short-escape`); bm04, bm05 and Issue039 reach `-mp 25` and skip it. |

Parameters looked at and left out of the design:

- `default_preferred_direction_trace_cost`: scaling all trace costs by k is the same as dividing
  A, B and F by k, so it is redundant with A and B.
- `default_bend_cost` (0) and per-layer bend costs (`MazeSearchEngine.java:854-869`): a pure
  length/bend trade-off, low priority for completion.
- `automatic_neckdown` (on), `neck_width_um` (0), `start_necked` (on): `start_necked` only acts
  when `neck_width_um > 0` (`AutorouteConnectionRouter.java:147-149`), so it is inert at the
  defaults; necking is a separate experiment (needs a manufacturable width per board).
- Fanout `min_escape_length_mm` / `max_escape_length_mm` (2.5 / 4.5, `MazeSearchEngine.java:88-120`),
  `max_passes` (20), `max_milliseconds_per_pin` (step budget, `BatchFanout.java:186-189`),
  `pin_sorting_order`: secondary to G; worth a follow-up sweep on SMD boards only if G matters.
  Note the documented values `distance_to_closest_on_net` and `surroundings_density` are not
  recognized: `BatchFanout.java:837,844` compare against `distanceToClosestOnNet` and
  `surroundingsDensity`, so the documented spelling silently falls back to pin-index order.
- Stagnation and restore windows (`STOP_AT_PASS_MINIMUM` 8, `STOP_AT_PASS_MODULO` 4,
  `STAGNATION_PASS_LIMIT` 10, `MAXIMUM_TRIES_ON_THE_SAME_BOARD` 3, `LAST_MILE_*`,
  `BatchAutorouter.java:36-64`), the rip-up growth law (linear in the pass number) and the
  per-connection step budget (`100000 * 2^(pass-1)` nominal ms, `AutorouteConnectionRouter.java:138-140`)
  are hard-coded: changing them needs a code experiment.
- `fallback_strategies`, `residual_repair_max_open` (4), `explore_patience` (0): these are
  extra routing attempts, not costs; they trade CPU for completion and deserve their own
  experiment. Note that the fallback runs only when the pass loop stops on STAGNATION,
  NO_IMPROVEMENT, COMPLETED or ERROR, not on MAX_PASSES (`RoutingFallback.java:166-171`), so at
  `-mp 25` it fires only on boards that stagnate earlier.
- `item_order_seed`: not a candidate default; used here to measure noise.
- Optimizer settings: the optimizer is off in this experiment.

## 4. Change

Settings only. Flags per factor (low / high, default in brackets), all on the `head` jar:

| | Flag | Low | High | Default |
|---|---|---|---|---|
| A | `--router.scoring.via_costs` | 25 | 100 | 50 |
| B | `--router.scoring.start_ripup_costs` | 50 | 200 | 100 |
| C | `--router.autorouter.smd_net_via_cost_factor` | 0.03 | 0.3 | 0.1 |
| D | `--router.scoring.default_undesired_direction_trace_cost` | 0.5 | 2.0 | 1.0 |
| E | `--router.trace_pull_tight_accuracy` | 160 | 1600 | 500 |
| F | `--router.scoring.plane_via_costs` | 2 | 12 | 5 |
| G | `--router.fanout.enabled` | false | true | true |

Ranges: A to F are x/÷ 2 to 3.2 around the default on a log scale, so the control is the centre
point of the numeric factors and curvature shows up as control-versus-mean-of-design. A at 25..100
stays inside what the explore attempts already use (6..400). B at 50..200 spans pass 1 of the
default to pass 2. C at 0.03..0.3 keeps SMD-net vias cheaper than normal vias. D at 0.5 still
keeps against-direction routing at least as expensive as preferred on a 2:1 board. E at 160 is
just above the 100-unit clamp; 1600 is 160 um on KiCad boards, about half a track pitch. F at 2..12
keeps plane vias cheaper than signal vias (A low = 25).

Design: 2^(7-3), base factors A..D in Yates order, E = ABC, F = BCD, G = ACD (resolution IV;
every main effect is free of two-factor interactions). `make_variants.py` writes `variants.txt`:
`control`, `seed1..seed3` (control plus `--router.autorouter.item_order_seed=N`, which permutes the
signal-item order from pass 2 on, `AutoroutePassRunner.java:276-292`), then `d01..d16`.

## 5. Determinism and time-budget check

No code change. Every flag is a fixed input to a deterministic search; the per-connection and
fanout budgets count steps, not clock time (`TimeLimit.java`). `item_order_seed` is a fixed
seed. The job timeout is a safety stop only; runs that reach it are excluded.

## 6. Boards

`boards.txt` (30 boards; each board line is followed by a comment with its family and its master
result from `SP/camp3/run1`):

- Targets (8): every board with router unrouted > 0 in the master arm that finished in time:
  bm04, bm05, Issue039 (the three oscillating boards), Issue313, Issue110-Pajalnaja,
  Issue157-TeamAdapt, Issue684, Issue367 (its summary record reads null because its final line
  says "1 violation"; the router reported 6 unrouted).
- Plane boards (7): router-complete but reload DRC reports unconnected items; they are the only
  boards where F acts.
- Regression guards (15): complete boards across the families, including the cheap smoke boards
  bm02 and caniot-tiny-arm.

Families (for the "holds across families" rule): 2L-SMD (2 signal layers, SMD pin fraction
>= 0.6, 12 boards), 2L-MIX (2 signal layers, < 0.6, 13 boards), ML (>= 4 signal layers, 5 boards).
A KiCad `power` layer without a `(plane ...)` is routed as a signal layer (the reader warns "It
will be treated as a signal layer"), so caniot-tiny-arm, PCBench-Box0 and Issue733 are 2-layer
boards. Feature values come from `../E10-per-board-adaptive/dsn_features.py`.

`boards-expensive.txt` (bm01, Issue107, Issue297, Issue732) is for the confirmation stage only:
the last two hit the 20-minute limit in the master arm.

## 7. Variants

`variants.txt`: 20 lines, `control` first. Regenerate with `python3 -I make_variants.py`.

## 8. How to run

Stage 1, screening (20 variants x 30 boards):

```
python3 scripts/experiments/paired_campaign.py \
  --out logs/E09/stage1 \
  --boards docs/research/experiments/E09-cost-sweep/boards.txt \
  --variants docs/research/experiments/E09-cost-sweep/variants.txt \
  --flags "-mt 1 -mp 25 --router.optimizer.enabled=false" \
  --parallel 8 --timeout 00:20:00
python3 -I docs/research/experiments/E09-cost-sweep/analyze_e09.py logs/E09/stage1/summary.jsonl \
  docs/research/experiments/E09-cost-sweep/variants.txt docs/research/experiments/E09-cost-sweep/boards.txt
```

The `head` jar is built by the harness from the commit under test (no patch). Each run must log
no `Failed to apply CLI router setting` warning (`CliSettings.java:189`); grep the run logs once.

Stage 2, confirmation (only if stage 1 passes its screen): write `variants-confirm.txt` with
`control`, `cand` (the candidate default: every factor whose main effect passed the screen at its
better level, the others at the default), and `seedN` / `candN` for N = 1, 2, 3 (both with
`--router.autorouter.item_order_seed=N`), then run it on `boards.txt` plus `boards-expensive.txt`
with the same flags and `--timeout 00:30:00`.

## 9. Metrics and decision rule

Per run (from the harness summary): reload-DRC unconnected items and violations by type, router
unrouted, vias, wire length, CPU seconds, passes; runs that are not COMPLETED are excluded and
listed.

Stage 1 screen (`analyze_e09.py`):

1. Noise floor per board: the range of reload-DRC unconnected and the standard deviation of vias
   over `control` and `seed1..3`.
2. Main effect of each factor = mean over the eight high rows minus mean over the eight low rows,
   per board relative to the control (open difference; log ratio for vias, length, CPU), averaged
   per family and overall.
3. A factor passes the screen when its overall effect improves opens by at least 0.5 connections
   per target board or vias by at least 3 %, the improvement exceeds twice the pooled seed noise
   (opens: twice the mean seed range; vias: 2 x pooled sd / sqrt(8)), and the effect has the same
   sign in at least two of the three families and is not worse beyond noise in the third. Curvature check: if the control beats the mean of
   all 16 rows on opens, the optimum is near the default and no level change is adopted.

Stage 2 adoption rule for a new default (`cand` against `control`):

- Hard gates, on the seedless runs: no board may have more reload-DRC unconnected items or more
  router unrouted connections than the control; no board may gain clearance-type reload-DRC
  violations (`clearance`, `holeClearance`; ties allowed); no board may stop finishing in time.
  Dangling vias and tracks and dead-copper islands are hygiene counts that move with routing
  noise (the bm02 smoke: 0 in the control, 4 to 5 in three perturbed runs including `seed1`); for
  them only the total over all boards and seeds must not increase.
- Robustness: over the four runs per arm (no seed, seeds 1..3), the median opens of `cand` are not
  above the control's on any board, and the total opens and total vias are better on average.
- Breadth: the improvement (fewer opens, or at least 3 % fewer vias at equal opens) holds in at
  least two of the three families and no family is worse; a gain carried by one board does not
  count (recompute the totals without the best-improving board: they must still improve).
- Cost: total CPU at most +20 %.

Only then propose the new default in `DefaultSettings.java` (one constant per factor), with the
two-arm table in the change description.

## 10. Cost estimate

From the master arm in `SP/camp3/run1` (loaded 4-core machine, `-mt 1 -mp 25`): the 30 boards
take about 1,800 CPU seconds per variant (sum of the master CPU column, without the expensive
tier), plus about 5 % for the reload DRC. Stage 1: 20 variants, about 10 CPU-hours (about 1.3 h of
wall time with 8 parallel jobs). Settings that disable fanout or lower rip-up costs may change the
per-board time by 2x either way; budget 15 CPU-hours. Stage 2: 8 runs per board on 34 boards,
about 5 CPU-hours plus up to 8 x 2 x 30 min for Issue297 and Issue732 (up to 8 more hours; drop
them if they time out in both arms).

## 11. Smoke-test results

`SP/ab/jars/fix9.jar` (head behaviour), `-mt 1 -mp 5 --router.optimizer.enabled=false`, one limiter
slot, then the reload DRC with the same jar. Router = router unrouted; open = reload-DRC
unconnected items; dangling = `via_dangling` + `track_dangling`; CPU = the router's own CPU seconds;
length in mm from the SES wires. No run logged `Failed to apply CLI router setting`, every SES
differs from its control (SHA-1), and `d01` (fanout off) logged no fanout stage (the control logs
two fanout lines), so the flags are accepted and change routing.

| Board | Variant | Router | Open | Clearance viol. | Dangling | Vias | Length mm | CPU s |
|---|---|---|---|---|---|---|---|---|
| bm02 | control | 0 | 0 | 0 | 0 | 16 | 301.9 | 1.1 |
| bm02 | d01 | 0 | 0 | 0 | 0 | 15 | 300.1 | 1.2 |
| bm02 | d02 | 1 | 1 | 0 | 4 | 16 | 299.5 | 2.0 |
| bm02 | d16 | 0 | 0 | 0 | 5 | 15 | 330.3 | 2.2 |
| bm02 | seed1 | 0 | 0 | 0 | 5 | 21 | 303.3 | 1.8 |
| caniot | control | 28 | 19 | 0 | 14 | 43 | 2305.1 | 52.5 |
| caniot | d01 | 24 | 24 | 0 | 8 | 46 | 2154.5 | 41.1 |
| caniot | d02 | 41 | 28 | 0 | 14 | 19 | 1862.2 | 51.4 |
| caniot | d16 | 24 | 24 | 0 | 16 | 47 | 2525.8 | 67.0 |
| caniot | seed1 | 21 | 18 | 0 | 15 | 54 | 2475.2 | 55.7 |

Readings (two boards, five passes: not evidence for any default):
- The noise floor is large. `seed1` alone moves bm02 from 16 to 21 vias (+31 %) and caniot from
  28 to 21 router-unrouted; this is why every effect is compared with the seed arms.
- d02 (cheap SMD-net vias 0.03 with dear normal vias 100, low rip-up, low against-direction cost,
  coarse pull-tight, fanout on) is the worst row on both boards (bm02 loses a connection, caniot 41
  unrouted) but also has by far the fewest vias on caniot (19 against 43).
- Router unrouted and reload-DRC unconnected disagree on caniot (28 against 19 for the control),
  so the decision rule uses both.
- caniot does not finish in 5 passes (it took 41 to 67 CPU s); at `-mp 25` it is one of the more
  expensive guards. Its B.Cu is a KiCad `power` layer without a plane and is routed as a signal
  layer (87 of 484 wires on B.Cu in the control).
- Cost per run at `-mp 5`: bm02 about 2 CPU s plus about 8 s JVM start and reload DRC.

Reproduce: `SP/agent-exp-params/smoke/run_board.sh <board> SP/agent-exp-params/smoke/smoke-variants.txt`
inside `SP/bin/limited`, then `python3 -I SP/agent-exp-params/smoke/summarize.py SP/agent-exp-params/smoke`.

## 12. Risks and follow-ups

- Chaos: any change perturbs every later decision, so single-board differences are mostly noise;
  the seed arms and the family rule are there to keep a lucky board from deciding a default.
- `-mp 25` with the optimizer off is not the production setting (unlimited passes, optimizer on).
  A stage-1 winner must be re-checked with the production flags on the confirmation boards before
  it becomes a default; vias in particular are reduced again by the optimizer.
- G conflates fanout with the fallback chain (see section 3). If G matters, follow up with a
  fanout escape-window sweep (`min_escape_length_mm` 0/1.25/2.5, `max_escape_length_mm`
  3/4.5/8) on the SMD boards with fanout on.
- Factors chosen by reasoning, not data: if no factor passes, the next candidates are the
  hard-coded rip-up growth (code experiment: `start * passNo` versus geometric growth) and the
  stagnation windows.
- The documented fanout pin orders `distance_to_closest_on_net` / `surroundings_density` do not
  work (section 3); fix the parser or the docs before sweeping that setting.
