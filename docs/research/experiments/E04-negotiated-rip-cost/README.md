# E04 Negotiated rip cost

A per-net rip history, kept across passes and board restores, multiplies the cost of ripping a
victim net (the history term of PathFinder-style negotiated congestion), to break the rip cycles
that make the pass loop oscillate.

## 2. Status

Designed; smoke-tested on Issue508-DAC2020_bm04 (8 passes) and the off switch on bm02/bm07; not
yet run at scale. The smoke showed no effect on bm04 at any weight (section 12): extend the
history to the other rip paths before running the campaign.

## 3. Hypothesis

On the oscillating boards (bm04 2 <-> 12 open, bm05 9, Issue039 9, BBD Mars-64 4) the same few
nets rip each other in a cycle: A rips B, B rips A on the next pass or after a board restore, and
the loop ends by restoring its best board. Making a net more expensive to rip each time it is
ripped should let one side of each cycle win, so the open count stops alternating and settles
lower. Expected: open connections on the oscillating boards -25% or better (for example bm04 to 0
or 1, bm05 below 9), vias within +3%, no new clearance violations, no completion loss on the boards
that complete today. A weight that is too high freezes early routes and should lose completion;
the sweep (0.5, 1, 2) is meant to find the knee.

## 4. Mechanism (code at d4383b44)

- The rip cost of an obstacle is `ctrl.ripupCosts * costFactor`, divided by the victim connection's
  detour (`src/main/java/app/freerouting/autoroute/maze/MazeRipupResolver.java:128-145`).
  `ctrl.ripupCosts` is `startRipupCosts * passNo` (`autoroute/pipeline/AutorouteConnectionRouter.java:112`),
  the same for every victim net: nothing distinguishes a net that was ripped ten times from one
  that was never ripped. (The detour randomization at `MazeRipupResolver.java:137` is dead because
  `AutorouteControl.ripupPassNo` is never assigned; see the research review.)
- A successful attempt leaves its victims ripped for later items and passes
  (`AutorouteConnectionRouter.java:28-35`), and a failing item's own net is ripped near the pin
  from its second failure (`AutoroutePassRunner.java:171-178`).
- From pass 8, every 4th pass restores the best earlier board when the current one is worse
  (`AutorouteBatchLoop.java:386-431`, constants `BatchAutorouter.java:41-58`), resets the
  anti-oscillation item order (`AutoroutePassRunner.java:324-328`) and routes the restored board
  again with a higher, still uniform rip cost, so the same cycle repeats; at the end the best board
  is restored (`AutorouteBatchLoop.java:608-620`). On bm04 at 25 passes the open counts run
  8,10,9,11,8,8,8,5,9,11,3,3,9,9,9,2,11,12,2,2,11,12,8,2,11 and the job ends with 2
  (`SP/camp3/run1/master/Issue508-DAC2020_bm04/console.log`).
- The history lives in the `BatchAutorouter` (not on the board), so it survives the board
  restores, which replace `router.board` with a deserialized copy.

## 5. Change

Code, `patch.diff` (applies with `git apply` on d4383b44; commit 4fd1ad69 in the design worktree):

- New `autoroute/pipeline/NetRipHistory.java`: an `int[]` of rip counts indexed by net number,
  grown to `rules.nets.maxNetNumber() + 1`. `record(rippedItems)` adds 1 to every net of the
  attempt's ripped items (each net once per attempt), after every connection attempt
  (`AutoroutePassRunner`, right after `router.autorouteItem` and its profiling). Unlike the earlier prototype
  (`SP/saved-patches/wt-shove.diff`, which counted only ROUTED attempts), it counts every attempt
  whose ripped items are off the board: without `router.strict_drc` the victims of an INSERT_ERROR
  or of a strict-DRC rejection from pass 3 stay ripped too; with `strict_drc` a rolled-back attempt
  clears its ripped list (`AutorouteConnectionRouter.java:88-90`) and is not counted.
- `AutorouteControl` gets `netRipHistory` and `ripHistoryWeight`; `AutorouteConnectionRouter`
  sets them on the normal and the necked control.
- `MazeRipupResolver.checkRipup` multiplies the rip cost by `1 + weight * max(count of the
  obstacle's nets)` before the detour division and the `Integer.MAX_VALUE / 100` cap.
- Switch: `-Dfreerouting.exp.rip_history=<weight>`, read once at class load. 0, negative, NaN,
  infinite or unparsable values disable it. Disabled, `AutorouteControl.netRipHistory` stays null
  and no code path changes, so the off arm is byte-identical to head (checked, section 12).
- Tests: `NetRipHistoryTest` (parsing, off by default, counting once per attempt, shared array),
  `RipHistoryFactorTest` (the factor).

## 6. Determinism and time-budget check

The counts are integers updated in item and pass order, and nets are visited in ascending net
number (`TreeSet`); nothing reads a clock or the remaining budget. The same input and settings give
the same history and the same routes. The history is per `BatchAutorouter`; the optimizer and the
fallback chain create their own routers and start from zero history (deterministically).

## 7. Boards

`boards.txt`: the four oscillating boards (BBD Mars-64 at 12 passes, it needed 513 s at 25), the
other boards with router-reported opens in `SP/camp3/run1/summary.jsonl` (Issue313, Issue110
Pajalnaja, Issue157, Issue684, Issue297 at 10 passes and Issue732 at 6 passes, both of which hit
the 20-minute limit at 25), boards whose reload DRC shows design-blocked opens while the router
reports none, and 14 boards that complete today. Cheap smoke boards: bm02, bm07 (about 8 s each).

## 8. Variants

`variants.txt`: `control` (weight 0), `rip_w0.5`, `rip_w1`, `rip_w2`, all on the same jar.

## 9. How to run

```
scripts/experiments/build_variant.sh d4383b44 docs/research/experiments/E04-negotiated-rip-cost/patch.diff build/E04.jar
python3 scripts/experiments/paired_campaign.py --out logs/E04 \
  --boards docs/research/experiments/E04-negotiated-rip-cost/boards.txt \
  --variants docs/research/experiments/E04-negotiated-rip-cost/variants.txt \
  --head-jar build/E04.jar \
  --flags "-mt 1 -mp 25 --router.optimizer.enabled=false" \
  --parallel 4 --timeout 00:30:00
```
(Without `--head-jar`, `--head-commit d4383b44 --head-patch
docs/research/experiments/E04-negotiated-rip-cost/patch.diff` makes the harness build the jar itself.) All variants
run with the same jar, so the identical-SES matrix also checks the off switch on every board: the
control must equal a run of a jar built from plain d4383b44 (add `plain <that jar>` as a second
line to `variants.txt` on a few boards to see it in the matrix).

The per-pass open counts, which show the oscillation, are in the router log:
`grep -o 'pass #[0-9]* .*unrouted' logs/E04/runs/<board>/<variant>/console.log`.

## 10. Metrics and decision rule

Per board and variant, from `summary.jsonl`: router unrouted and violations, reload-DRC
unconnected and clearance violations by type, vias, wire length, CPU seconds, passes. Runs that hit
the 30-minute safety stop are reported separately and excluded.

- Must not regress (any single board): reload-DRC clearance violations never increase; router
  unrouted and reload-DRC unconnected never increase; a board that is complete in the control
  stays complete.
- Must improve: the sum of open connections (reload-DRC unconnected, design-blocked opens
  excluded by comparing against the control) over the oscillating and open boards drops by at
  least 3, or at least two boards become complete, with no board worse.
- Secondary (tie-breakers, may not regress by more than the stated amount): vias +3%, wire length
  +2%, CPU time +25% summed over the boards that finish inside the time limit.
- Adopt (with a `docs/research` note and the setting made default or removed) only if the rule
  holds; otherwise record the numbers under "Measured and not adopted".

## 11. Cost estimate

From the head-like master timings in `SP/camp3/run1` (one thread, optimizer off) scaled to the pass
limits in `boards.txt`, plus BBD Mars-64 at 12 passes (about 250 s): about 3,250 s of wall time
per variant, roughly 0.9 CPU-hours (the router is single-threaded; GC adds 10-20%). Four variants:
about 3.6 CPU-hours, plus about 0.2 hours for the reload DRCs. The history adds no measurable work
per attempt (one array lookup per rip check); run time changes only through different routes.
At `--parallel 4` on a 4-core machine: about 1 hour wall.

## 12. Smoke-test results

All runs: one jar built from d4383b44 plus the three experiment patches (`combined.diff`; every
switch off by default), `-mt 1 --router.optimizer.enabled=false --router.job_timeout=00:20:00
-Xmx1500m`, run one at a time through the slot limiter on the shared 4-core machine (wall times
are load-dependent). Router counts are the job's own final line; no reload DRC was run for the
smoke (the router reported 0 clearance violations in every run).

Off switch (byte-identical to head): with no property set, the jar's SES equals the one from
`SP/ab/jars/fix9.jar` (head behaviour) on bm02 (sha1 125cea8b3db0, 0 open, 16 vias) and bm07 (sha1
366fba4d3405, 0 open, 31 vias), `-mp 8`. On bm04 `-mp 8` the control's per-pass open counts
(8,10,9,11,8,8,8,5) equal the first eight passes of the head-like master in `SP/camp3/run1`.

Determinism of the switched-on code: bm07 `-mp 8` with all three switches on
(`rip_history=1 requeue_ripped=true last_mile=8`) twice gave the same SES (sha1 8289c499821f; open
counts 1,0,0 against the control's 4,4,1,0,0, and 28 vias instead of 31).

Issue508-DAC2020_bm04, `-mp 8` (8 passes; unrouted is the router's count after each pass):

| run | unrouted after pass 1..8 | final unrouted | violations | vias | CPU s (wall s) |
|---|---|---|---|---|---|
| control (all off) | 8,10,9,11,8,8,8,5 | 5 | 0 | 62 | 85.1 (121) |
| rip_history=0.5 | 8,10,9,11,8,8,8,5 | 5 | 0 | 62 | 86.0 (117) |
| rip_history=1 | 8,10,9,11,8,8,8,5 | 5 | 0 | 62 | 87.8 (115) |
| rip_history=2 | 8,10,9,11,8,8,8,5 | 5 | 0 | 62 | 83.5 (111) |

**No effect on bm04.** All three weights gave an SES byte-identical to the control (sha1
cfcd312b76d9). A sanity run at an extreme weight, `rip_history=50 -mp 3`, was also identical to the
control at `-mp 3` (8,10,9 open; sha1 14395a7efa9d). The hooks are in the jar (checked with
`javap`), the property reaches the JVM (the other two switches, read the same way, change the
result), and the unit tests show the history counting and the factor. So within these passes no
maze search changed its choice because of the history, which means the maze rips are not what
drives bm04's oscillation: either few maze rips happen (each recorded net must later be met again
as a rip candidate for the factor to matter), or the open connections come and go through the
other rip paths, which the history does not see: the failed-net self-rip near a pin that failed
twice (`AutoroutePassRunner.java:171-178`), the stagnation blocker rip (`LastMileBlockerRipup`),
and the strict-DRC removal of new copper from pass 3 (`AutorouteConnectionRouter.java:356-375`).
A one-pass TRACE count of the maze rips on bm04 (`compare_trace_ripped_item` lines) was queued at
the end of the session; see the report for its result.

Before spending the campaign on this package, extend it so the history also counts (and prices)
the failed-net self-rip, for example by skipping or shrinking `failedNetRipItems` for nets whose
history is high, and re-smoke bm04; otherwise run only `rip_w2` against the control on the
oscillating boards (bm04 at 25 passes reaches restores, which 8 passes do not) as a cheap check.

## 13. Risks and follow-ups

- A high weight freezes the first routes: a net that had to be ripped often may be the one that
  must move. PathFinder balances history with a present-congestion term that grows per iteration;
  here the per-pass rip cost growth (`startRipupCosts * passNo`) plays that role. If 0.5..2 are all
  flat, try a history that decays at board restores (multiply by 0.5) or one that counts only
  rips by a different net than last time (cycle detection).
- Counting failed attempts' rips (a difference from the prototype) may over-penalize nets near
  pins that fail repeatedly; a fourth variant counting only ROUTED attempts would separate that.
- The fanout pre-pass and the fallback chain build their own routers and do not share the
  history; sharing it with the fallback loops is a follow-up if the pass loop alone helps.
- The router's per-pass logs do not print the history; for diagnosis add a debug line with the
  ten most-ripped nets at each board restore.
