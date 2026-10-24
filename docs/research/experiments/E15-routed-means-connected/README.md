# E15: routed means connected

After a found connection is inserted, check that its start and destination sets are now
connected; if not, remove the new traces and vias of the net and report FAILED instead of ROUTED.

## Status

Designed; smoke-tested on Issue054-tairakb and Issue269-caniot-tiny-arm (5 passes); not yet run
at scale. **Do not run the campaign before the check is diagnosed**: in the smoke test it fired
on 56-66% of the connections the maze reported routed, which is not plausible as a true rate
(see Smoke-test results).

## Hypothesis

Robustness, not completion: connections that the router reports ROUTED although the inserted path
does not reach its target (seen on caniot, off-centre fixed track ends) become visible as FAILED,
enter the failure log and the existing failure handling (`AutoroutePassRunner.java:166-182`:
after two failures the net's traces are ripped), and stop costing a full maze search in every
pass. Expected: the check fires rarely (well under 1% of routed connections) and costs well under
1% of routing CPU; completion unchanged or better on boards where it fires, unchanged elsewhere.

## Mechanism

`AutorouteEngine.autorouteConnection`
(`src/main/java/app/freerouting/autoroute/maze/AutorouteEngine.java:135-247` at d4383b44) returns
ROUTED whenever `FoundConnectionInserter.getInstance` (line 233) returns non-null; it never checks
that the inserted copper actually connects the start set to the destination set. The insertion can
end short of its target (for example at an off-centre end of a fixed track, where the path's end
point is not a contact of the target), normalization can remove a piece, or a stub can miss. The
pass then counts the connection as routed, the item stays incomplete, and the next pass repeats
the same search.

The patch adds, behind the switch, `verifyInsertedConnection` right before the ROUTED return: the
start set may hold several components (for a normal net it is the unconnected rest of the net),
so each start component still on the board is expanded once with `getConnectedSet(net)` until one
contains a destination item. If none does, the traces and vias of the net with ids above the id
counter before the insertion are removed (the same removal `BatchAutorouter.enforceStrictDrc`
uses) and the result is FAILED with the reason "the inserted path does not connect them". If no
start or no destination item is left on the board, the check counts the connection as not
verifiable and keeps ROUTED. With `router.strict_drc` the attempt's undo snapshot also rolls back
the ripped victims (`AutorouteConnectionRouter.java:81-92`). The check covers every caller of
`autorouteConnection`: the batch router, the necked retry, direct pin routing (fanout) and the
interactive autoroute.

## Change

Code: `patch.diff` (applies with `git apply` on d4383b44).

- `AutorouteEngine`: the check, counters, and `verifyConnectionSummary()`.
- `AutorouteBatchLoop.run`: logs one INFO line at the end of each pass loop when the switch is on:
  `Experiment verify_connection: V connection(s) verified, U reported routed but unconnected (now
  FAILED), N not verifiable, S s spent checking.` (cumulative for the JVM).
- `src/test/java/app/freerouting/autoroute/maze/AutorouteEngineConnectionStateTest.java` (new,
  3 tests).

Switch: `-Dfreerouting.exp.verify_connection=true`, read once at class load; default false, which
skips the check and the id-counter read entirely, so the routing is byte-identical to head.

## Determinism and time-budget check

The check is a pure function of the board after the insertion (connected sets in item order), the
removal set is ordered by the net's item list, and no decision depends on time. The elapsed-time
counter is only logged.

## Boards

`boards.txt`: caniot (the known silent loop), the boards that end with open connections in the
campaign of record (bm04, bm05, Issue039, TeamAdapt, Pajalnaja), two plane boards (destination set
includes the pour) and three cheap complete boards that must not change.

## Variants

`variants.txt`: `control` (head), `verify`.

## How to run

```
scripts/experiments/build_variant.sh d4383b44 docs/research/experiments/E15-routed-means-connected/patch.diff /tmp/e15.jar
python3 scripts/experiments/paired_campaign.py --out /tmp/e15 \
  --boards docs/research/experiments/E15-routed-means-connected/boards.txt \
  --variants docs/research/experiments/E15-routed-means-connected/variants.txt \
  --flags "-mt 1 -mp 25 --router.optimizer.enabled=false" --parallel 8 --timeout 00:20:00
grep -h "Experiment verify_connection" /tmp/e15/*/verify/*/route.log
```

## Metrics and decision rule

- Report per board: how often it fires (U), U / (V + U), the check's seconds vs the router CPU,
  and the identical-SES matrix (boards where U = 0 must be byte-identical to control, since the
  check then changes nothing).
- Must not regress: reload-DRC clearance violations never increase; router unrouted and DRC
  `unconnectedItems` never increase on any board.
- Adopt if it never regresses and either improves completion or removes repeated futile searches
  (fewer passes or less CPU) on at least one board where it fires, with a check cost under 1% of
  routing CPU. If it fires but changes nothing measurable, adopt it anyway as a correctness fix
  only if the CPU cost is negligible; the logged reason then explains the open connection.

## Cost estimate

11 boards x 2 variants at 25 passes: about 1.5-2 CPU-hours (bm04 and bm05 dominate at 2-4 min,
TeamAdapt and Issue039 about 5 min each in the campaign of record).

## Smoke-test results

5 passes, one thread, optimizer off, machine heavily oversubscribed (load 17-19 on 4 cores).

| Board | Variant | Router result per pass (unrouted) | Router CPU s | verified / fired / unverifiable | Check s | Reload DRC |
|---|---|---|---|---|---|---|
| tairakb | off | 12, 8, 5, 1, 1 | 8.90 | - | - | not run (queued) |
| tairakb | verify | 36, 11, 4, 1, 0 | 12.11 | 326 / 414 / 0 | 0.60 | unconnectedItems 2 (zone islands), no clearance violations |
| caniot | off (E14 jar, no pours, so the same code path) | 48, 41, 34, 31, 28 | 40.69 | - | - | unconnectedItems 19, no clearance violations |
| caniot | verify | 47, 35, 27, 27, 28 | 38.96 | 249 / 479 / 0 | 0.71 | unconnectedItems 21, no clearance violations |

- The check itself is cheap (0.6-0.7 s, under 2% of router CPU) and never unverifiable.
- It fires far too often: if 414 of 740 insertions on tairakb really did not connect, the board
  could not finish in 5 passes, but it does (0 unrouted, better than off). The pass-1 jump from 12
  to 36 unrouted shows that the removal of the new copper is what costs. Most firings are
  therefore probably false positives of the check's placement, not real defects. Leading
  hypothesis (unverified): `FoundConnectionInserter` leaves the end of the new path short of its
  target and the contact is only made by the trace normalization and pull-tight that
  `AutorouteConnectionRouter` runs right after the maze returns
  (`optChangedArea`, `AutorouteConnectionRouter.java:204-210`), so the check must run after that
  step (in `AutorouteConnectionRouter.routeAttempt` and `retryConnectionNecked`, and after
  `optChangedArea` in `RoutingBoard.autorouteConnection`) rather than inside
  `AutorouteEngine.autorouteConnection`. Before any campaign: run one board with
  `--logging.console.level=TRACE` (or a debug print) on the marker
  `[unconnected_after_insert]`, check two firings by hand, move the check, and re-smoke.
- SES: verify differs from off on both boards (expected, since it fires).
- Unit tests: `AutorouteEngineConnectionStateTest` 3/3 pass. spotlessCheck, checkstyleMain and
  checkstyleTest pass.

## Risks and follow-ups

- The removal takes every new trace and via of the net, including pieces of existing traces that
  the insertion split or combined (they get new ids); those connections then open and are routed
  again later. This is the same trade-off `enforceStrictDrc` makes; with `router.strict_drc` the
  snapshot undo restores the board exactly.
- A FAILED result triggers the necked retry when `router.neck_width_um` is set, and from the
  second failure the net's traces are ripped (`AutoroutePassRunner.java:173-178`), so routes on
  boards where it fires will change.
- Root cause of the short insertions (off-centre fixed track ends) is not addressed; this only
  makes them visible. The TRACE marker `[AutorouteEngine.verify_inserted_connection]
  [unconnected_after_insert]` names the start and destination items for a follow-up.
