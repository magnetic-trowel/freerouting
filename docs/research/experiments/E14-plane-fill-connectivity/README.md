# E14: plane fill connectivity check and repair

At the end of each pass loop, find the pads and vias of pour nets that foreign tracks have cut off
from the actual pour fill (the board still counts them as connected through the pour polygon) and
route each cut-off group to the rest of its net, without rip-up.

## Status

Designed; smoke-tested on Issue054-tairakb and Issue269-caniot-tiny-arm (5 passes); not yet run
at scale.

## Hypothesis

Boards with pours end with fewer real open connections: the reload DRC's
`isolated_island_unconnected` findings for pins and vias drop (tairakb: pad U3-23, item 6056,
connected to neither GND pour, should become connected). No change in clearance violations
(the repair routes without rip-up and drops new copper that violates clearance). Cost: one
detailed-fill computation per pour per pass loop (two with the repair), measured at 4.3 s of
wall time on tairakb under heavy machine load (about a third of a 5-pass routing run, about a
tenth of a 25-pass run). Boards without pours: no change, no cost.

## Mechanism

- `AutorouteConnectionRouter.routeAttempt` (`src/main/java/app/freerouting/autoroute/pipeline/AutorouteConnectionRouter.java:123-128`)
  returns CONNECTED_TO_PLANE as soon as the item's connected set contains a `ConductionArea`, and
  `BatchAutorouter.getAutorouteItems` (`BatchAutorouter.java:497-503`) never queues such items.
  Even before that, `Item.getConnectedSet` (`board/model/items/Item.java:683-711`) runs through any
  pour polygon, so the ratsnest, the incomplete count and the router all count a pad inside the
  polygon as connected.
- Pours are not obstacles for foreign nets, so foreign tracks may cut a pad's thermal spokes and the
  fill around it. The real fill is `ConductionArea.getDetailedFillArea`
  (`board/model/items/ConductionArea.java:106-325`: polygon minus foreign clearances, with spokes
  for same-net drill items); `DesignRulesChecker.decomposeAreaIntoIslands`
  (`drc/DesignRulesChecker.java:318-389`) splits it into islands.
- The new `drc.PlaneFillConnectivity.analyze(board, net)` groups the net's items by copper that does
  not run through a pour (`getConnectedSet(net, true)`), joins groups whose drill centres or trace
  ends lie in the same fill island of any pour of the net (across layers, so through-hole pins
  join the islands of both layers), takes the largest class as the main group (ties: lowest item
  id), and reports the other classes that the board counts as connected to the main group. Groups
  the board already sees as unconnected are ordinary incompletes and are not reported.
- The new `autoroute.pipeline.PlaneFillRepair.run` (called in `AutorouteBatchLoop.run` after the
  best-board restore, before the final tail removal, `AutorouteBatchLoop.java:648` at d4383b44)
  routes each cut-off group to the main group with `AutorouteEngine.autorouteConnection` (no
  rip-up, plane via costs), pulls it tight and removes it again with
  `BatchAutorouter.enforceStrictDrc` if it violates clearance.
- Important detail found in the smoke test: the new trace closes a cycle through the pour polygon,
  so trace normalization (`PolylineTrace.java:649`, `BasicBoard.removeIfCycle`) removed it at once
  and the repair was a no-op. The repair therefore sets the net class's
  `ignoreCyclesWithAreas` while it routes and pulls tight, and restores it afterwards.

## Change

Code: `patch.diff` (applies with `git apply` on d4383b44).

- `src/main/java/app/freerouting/drc/PlaneFillConnectivity.java` (new): the analysis above.
- `src/main/java/app/freerouting/autoroute/pipeline/PlaneFillRepair.java` (new): the repair and
  one INFO summary line per pass loop (`Experiment plane_fill_check (...): N pour net(s), G
  group(s) with I item(s) cut off ..., R reconnected, F not; afterwards ...; S s. Cut off: ...`).
- `AutorouteBatchLoop.run`: one guarded call.
- `src/test/java/app/freerouting/drc/PlaneFillConnectivityTest.java` (new, 4 tests).

Switch: `-Dfreerouting.exp.plane_fill_check=false|report|true`, read once at class load.
`false` (default) runs nothing, so the routing is byte-identical to head. `report` analyses and
logs only; the SES is unchanged (verified on tairakb: report SES equal to the SES of the first,
cycle-removed repair attempt; equality with the off arm is part of the smoke runs below).
`true` analyses, repairs, analyses again and logs.

Not run when the job timed out or was cancelled, in nested optimizer routers, or when the pass
loop was skipped as a repeat.

## Determinism and time-budget check

Deterministic: nets in ascending number, pours and items sorted by id, union-find with the lower
root winning, main group by size then lowest id, cut-off groups by lowest item id. The fill
geometry is the same `java.awt.geom.Area` computation the DRC uses (deterministic doubles). The
only time limit is the per-search `TimeLimit` of 100 s that the router's first pass also uses (a
safety stop on one maze search, as in `AutorouteConnectionRouter.java:138-140`); no decision
depends on elapsed time or remaining budget. The check is skipped after a timeout, which only
stops work.

## Boards

`boards.txt`: the fixtures with `(plane ...)` scopes and the most `isolated_island_*` findings in
the reload DRC (tairakb, interf_u, wavefolder, Charger, ch32v, ecc83, smoke-ldo-led and others),
larger pour boards that must not regress, and two pour-free boards (bm02, caniot) where the check
must be a no-op.

## Variants

`variants.txt`: `control` (head), `report` (detection only, must be byte-identical to control),
`repair`.

## How to run

```
scripts/experiments/build_variant.sh d4383b44 docs/research/experiments/E14-plane-fill-connectivity/patch.diff /tmp/e14.jar
python3 scripts/experiments/paired_campaign.py --out /tmp/e14 \
  --boards docs/research/experiments/E14-plane-fill-connectivity/boards.txt \
  --variants docs/research/experiments/E14-plane-fill-connectivity/variants.txt \
  --flags "-mt 1 -mp 25 --router.optimizer.enabled=false" --parallel 8 --timeout 00:20:00
```
(`head` in variants.txt is the jar built from the patched commit.) Extract the experiment lines
with `grep "Experiment plane_fill_check" /tmp/e14/*/repair/*/route.log`.

## Metrics and decision rule

- Must improve: reload-DRC `isolated_island_unconnected` findings that name a pin or via (and
  `unconnectedItems`) on at least two boards; the `afterwards` count in the log is 0 or lower than
  before on every board where it fires.
- Must not regress: reload-DRC clearance violations never increase on any board; router unrouted
  count and DRC `unconnectedItems` never increase on any board; `report` is byte-identical to
  `control` on every board; pour-free boards are byte-identical in all variants.
- Cost: the check's own seconds (in the log line) under 10% of the routing CPU at 25 passes on
  every board, else make it incremental (see follow-ups) before adoption.
- Adopt as default if all of the above hold; otherwise keep `report` as a warning only.

## Cost estimate

17 boards x 3 variants at 25 passes: the pour boards take 10 s to 5 min of CPU each in the
campaign of record (tairakb 33 s, interf_u 12 s, wavefolder 8 s, Charger 16 s), the larger ones
(smoothieboard, CM5, avionics hub) up to 20 min. Roughly 2-3 CPU-hours in total, plus the reload
DRC (tairakb 14 s per run).

## Smoke-test results

5 passes, one thread, optimizer off, machine heavily oversubscribed (load 17-19 on 4 cores, so
wall times are inflated; the router's CPU seconds are more reliable).

| Board | Variant | Router result | Router CPU s | Check s (wall) | Log line | Reload DRC |
|---|---|---|---|---|---|---|
| tairakb | off | 1 unrouted, 0 viol. | 8.90 | - | - | not run (queued) |
| tairakb | report | 1 unrouted, 0 viol. | 11.32 | 4.25 | 1 group, 1 item cut off: GND pin #46 of component #34 | unconnectedItems 4 (3 zone islands: pin 6056 from both pours, pin 6030 from pour #3 only), no clearance violations |
| tairakb | repair, first version | 1 unrouted, 0 viol. | 13.65 | 9.75 | "1 reconnected", afterwards still 1 cut off | identical to report |
| caniot | repair | 28 unrouted, 0 viol. | 40.69 | 0.00 | 0 pour nets | unconnectedItems 19, no clearance violations |

- SES: tairakb off, report and the first repair version are byte-identical (sha1 4c44e45b...),
  so `report` does not change the result. The first repair version was a no-op because the new
  trace closed a cycle through the pour polygon and normalization removed it (see Mechanism);
  the committed version keeps it by ignoring area cycles during the repair. Its smoke run
  (`repair` on tairakb with the committed code) and the off-vs-head (fix9.jar) comparison were
  still queued behind the machine-wide slot limiter when this package was written; run them
  first.
- The analysis reports 1 cut-off item on tairakb where the reload DRC reports 3 island entries:
  pin 6056 is cut off from both GND pours (the real defect, U3-23); pin 6030 is reported by the
  DRC for pour #3 only and is connected through the other layer, which the DRC's per-layer check
  cannot see and this analysis can.
- Unit tests: `PlaneFillConnectivityTest` 4/4 pass. spotlessCheck, checkstyleMain and
  checkstyleTest pass (no new warnings in the new files).

## Risks and follow-ups

- The repair trace closes a cycle through the pour polygon. It survives the repair because the
  net class ignores area cycles while the repair runs, but anything that later normalizes or
  pulls tight that trace (the optimizer, a later pass loop of the fallback chain, interactive
  editing) can remove it again as a cycle, and the ratsnest still does not see the open
  connection. A complete fix needs the connectivity model itself (getConnectedSet through pours)
  to use fill islands, or a per-trace "keep" mark.
- Only the end of a pass loop is covered: within the loop the router still skips cut-off items.
  A per-pass variant (requeue cut-off groups as ordinary connections with rip-up) is the next step
  if the end-of-loop repair often fails for lack of space.
- Cost: the fill computation is the zone-island DRC's (tairakb 4.3 s wall here). Caching fills per
  pour by the set of overlapping items, or computing islands only around plane-net pads, would
  make a per-pass check affordable.
- The analysis attaches items to islands by drill centre and trace end points, like the DRC; a
  trace that touches an island only in its middle is not attached (conservative: reports a
  cut-off that is not one, and the repair then adds a redundant trace).
