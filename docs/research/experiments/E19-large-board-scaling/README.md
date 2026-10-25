# E19: large-board scaling: where the time goes, the expansion list, retained rooms

Per-pass cost breakdown of the boards that dominate campaign wall time, how it grows with the
item count, and a paired test of the two changes aimed at the expensive tail: the binary-heap
expansion list of E18 (result-preserving) and the existing, dormant switch that keeps expansion
rooms between connections (result-changing).

## Status

Designed; smoke-tested on Issue508-DAC2020_bm01 (heap and retained-database arms),
Issue297-myboard and Issue157-TeamAdapt-LinePCB (profiles at item checkpoints); not yet run at
scale.

## Where the time goes

Sources: the head-like campaign logs `SP/camp3/run1/master/<board>/console.log` (25 passes, one
thread, optimizer off, 20-minute limit; per-pass CPU is the difference of the cumulative
"using N CPU seconds" values, extracted with `SP/agent-exp-perf/passes.py`), the older A/B run
`SP/ab/runs/c2_v0/Issue555-BBD_Mars-64`, and JFR profiles of fix9.jar bounded by
`--router.max_items` (`SP/agent-exp-perf/jfr/<run>/agg.txt`, routing thread only).

### Per pass

| Board | Fanout CPU | Pass 1 | Later passes | Job | Shape of the cost |
|---|---|---|---|---|---|
| Issue297-myboard | 3.4 s | 96 items, 116.6 s CPU | 33/18/11 left: 47/25/32 s; then 3 left: 20-25 s per pass, 25 passes x 2 stages | 1003 s CPU, 218 GB allocated, timed out | a few hard connections that fail every pass, ~7 s CPU per failed attempt |
| Issue732-RoyalBlue54L | 42.0 s (16.6 GB) | 78 items, 122.8 s | 44 to 24 left: 51-73 s per pass | 743 s, 357 GB, timed out after 12 passes | many open connections, ~1.5-2.3 s CPU each, every pass |
| Issue157-TeamAdapt | 3.5 s | 117 items, 13.2 s | 4 left: 0.2-0.8 s per pass | 124 s | five fallback stages, each restarting from fanout (fanout 1.5-3.5 s each) |
| Issue555-BBD_Mars-64 | 3.9 s | 90 items, 12.2 s | 59 to 8 left: 3-7 s per pass | ~500 s wall | six fallback stages, fanout repeated (up to 8.6 s, 7.4 GB) |

The per-pass cost does not fall with the number of open connections on Issue297: the last three
connections cost as much per pass as eleven did in pass 3. Each failing attempt runs a full maze
search with rip-up over the whole board, and each search starts with an empty room database
(`RoutingBoard.initAutoroute`, `RoutingBoard.java:893`, builds a new `AutorouteEngine` per
connection unless the database is retained).

### Inside a pass (JFR, routing thread)

| Innermost category | GP8B, 300 items | Issue157, 200 items | Issue297, 60 items |
|---|---|---|---|
| Room completion (`completeExpansionRoom`, `completeShape`, door calculation) | 38.5% | 26.3% | 34.5% |
| Insertion (found-connection insert, normalize, pull-tight, fanout insert) | 20.0% | 21.2% | 29.9% |
| Shove and via feasibility checks | 20.0% | 12.0% | 9.2% |
| Maze search proper (door expansion, drill pages, A* queue) | 11.1% | 15.3% | 8.8% |
| Rip-up and undo | 3.4% | 2.6% | 1.4% |
| Statistics and incomplete counts (`BoardStatistics`, `NetRoutingLedger`) | 3.0% | 4.7% | 5.2% |
| Board snapshot hashing | 0.4% | 2.0% | 3.3% |
| DRC | 0.3% | 2.0% | 1.6% |

Outer phase: fanout is 34% (GP8B), 19% (Issue157) and 57% (Issue297 at 60 items) of the routing
thread; the fallback chain is under 1% inside these bounded runs but multiplies the whole job on
Issue157 and Mars-64 (see the table above). Statistics, hashing and DRC together stay under 10% in
every profile, so a cheaper statistics path is worth at most a few percent; room completion is the
largest single target, followed by insertion and the shove checks.

### Scaling with the item count

Issue297, same flags (`-mt 1 -mp 25 --router.optimizer.enabled=false`):

| `max_items` | Job CPU | Unrouted after |
|---|---|---|
| 60 (fix9, this session) | 5.2 s | 56 (pass 1 cut at 60 items) |
| 100 (fix9, this session, JFR on) | 105.5 s (pass 1: 105.5 s, 43 unrouted; stopped at the item limit) | 43 |
| 96 = full pass 1 (campaign, head-like master) | 116.6 s | 33 |

The first 60 connections cost 5.2 s; the next 40 cost 100 s, a 20-fold higher cost per
connection. The expensive connections are the late, congested ones, and what makes them expensive
is not room completion but the expansion list. JFR of the 100-item run, routing thread, 11405
samples, in 20-second windows: after the first window (fanout and the easy connections: room
completion 29%, insertion 20%, maze 31%) every window is 76-99% maze search. Self time:
`TreeMap.put` 43.8%, `MazeListElement.compareTo` 23.7%, `TreeMap.fixAfterInsertion` 5.4%,
`fixAfterDeletion` 4.6%; inclusive `TreeMap.put` 72.4% (64.3% from
`MazeSearchEngine.expandToDoorSection`, 7.6% from `MazeExpansionEngine.expandToDrillPage`) against
6.0% for `pollFirstEntry`. `MazeListElement` is 14.3% and `TreeMap$Entry` 10.4% of the 13.7 GB
sampled allocation. The add path costs 12 times the poll path: every add is an O(log n) descent with
cache misses through a large red-black tree, while removing the first entry needs no comparison.
(Whether the searches also push many more entries than they pop is not measured; a counter in
`occupyNextElement` would answer it.) The cost per connection
therefore grows with the size of the search frontier, which grows with the routed copper around
the connection.

## Hypothesis

H1 (result-preserving, E18 `patch.diff`): the binary-heap expansion list removes most of the
`TreeMap.put` cost on the tail; expected -30% to -60% job CPU on Issue297 at 100 and 200 items and a
smaller gain on RoyalBlue54L; byte-identical SES everywhere.

H2 (result-changing): keeping the expansion rooms between connections
(`-Dfreerouting.benchmark.retain_autoroute_database=true`) cuts room completion, the largest share
of a pass on the medium boards, most on boards whose late passes retry the same few connections on
an unchanged board (Issue297) and on boards with many open connections per pass (RoyalBlue54L).
Expected before the smoke run: job CPU -15% to -30% at the same item limit; the bm01 smoke run
(below) makes this unlikely.
Routes change (room shapes depend on completion order), so completion, vias and violations must be
measured.

## Mechanism

- `RoutingBoard.initAutoroute` (`RoutingBoard.java:893`) keeps the engine only when
  `retainAutorouteDatabase` is set; `BatchAutorouter` reads it once from the system property
  `freerouting.benchmark.retain_autoroute_database` (`BatchAutorouter.java:67`, `:230`), default
  false. With it off, every connection gets a new `AutorouteEngine` (new drill-page array, empty
  room lists) and rebuilds every room it visits.
- With it on, `AutorouteEngine.initConnection` (`AutorouteEngine.java:101`) drops only the
  net-dependent complete rooms when the net changes and invalidates the rooms around the new net's
  items; `RoutingBoard.additionalUpdateAfterChange` (`RoutingBoard.java:106`) removes the complete
  rooms and drill pages that touch any item shape that changes. The rest of the room graph, built
  against the same obstacles, is reused by the next search.
- Undo and board copies drop the engine (`RoutingBoardUndoFacade.java:69`,
  `RoutingPipeline.java:144`), so restored boards never see stale rooms.

Validity argument: a retained complete room was free space for its net when it was built; any item
inserted later that touches it removes it. A retained room may be smaller than a fresh one (an
obstacle that bounded it was removed), which changes the decomposition and thus the routes, but
not their legality. The two points to verify by DRC are the net-dependent flag (rooms whose shape
depends on the current net's own items) and items whose tree shapes change without passing through
`additionalUpdateAfterChange`.

Follow-up design (code, not in this package): replace the item-based invalidation by the
`SearchTreeChangeGrid` stamps that `ShoveCheckCache` uses (`docs/internals/shove-check-cache.md`),
so that a room is dropped only when a cell it covers changed since it was completed, and keep the
rooms of nets that failed in the previous pass across the pass boundary (where Issue297 spends its
time). Rooms are search-tree objects, so the grid would have to ignore room insertions and removals
when it stamps cells.

### Next step after H1 (design, not implemented)

After H1: if the tail pushes many entries for door sections that are
already occupied when they are pushed (they are polled later and skipped), not pushing them is
exact except for one interaction: a held entry blocks later entries with an equal
key, including entries of a different door with a colliding id. Keep such entries as ghost keys
(in the key set, not in the heap) until the heap minimum passes their sorting key, which needs a
second small heap of ghost keys; measure first how many polls are skipped as occupied (a counter
in `occupyNextElement`) to size the gain.

## Change

H1: code, the E18 `patch.diff`, built as a jar (`build/e18.jar`); it has no switch because both arms
must be byte-identical. H2: settings-only, an existing JVM property: the variant adds
`-Dfreerouting.benchmark.retain_autoroute_database=true`; with the property absent the jar is head,
byte-identical by construction.

## Determinism and time-budget check

The retained database is a deterministic function of the sequence of searches and board changes;
nothing depends on time or budget. The item-checkpoint runs use `--router.max_items`, a count.

## Boards

`boards.txt`: the five large boards bounded at two item checkpoints each (the scaling question),
bm01, GP8B and bm05 as quick boards that must not regress, interf_u for plane routing (the
retained database also serves the plane-net searches), and two complete-routing boards from the
completion campaign (bm04, bm06) where completion must not drop.

## Variants

`variants.txt`: `control head`, `heap build/e18.jar` (H1), `retain head
-Dfreerouting.benchmark.retain_autoroute_database=true` (H2), and `heap_retain` (both).

## How to run

```
scripts/experiments/build_variant.sh d4383b44 \
  docs/research/experiments/E18-result-preserving-speedups/patch.diff build/e18.jar
python3 scripts/experiments/paired_campaign.py --out logs/E19 \
  --boards docs/research/experiments/E19-large-board-scaling/boards.txt \
  --variants docs/research/experiments/E19-large-board-scaling/variants.txt \
  --flags "-mt 1 -mp 25 --router.optimizer.enabled=false" --parallel 2 --timeout 00:30:00
```

For the per-pass breakdown of a new run: `python3 SP/agent-exp-perf/passes.py <dir>/console.log`
(copy the script into `scripts/experiments/` if it is kept), and for a profile add
`-XX:StartFlightRecording=filename=<dir>/rec.jfr,settings=profile` to a variant and aggregate with
`python3 jfragg.py <dir>/rec.jfr --thread Thread-1`.

## Metrics and decision rule

- Never: a reload-DRC clearance violation count that rises on any board, or an unrouted count that
  rises on a board that is complete in the control.
- Measure: job CPU per board at the same item limit, unrouted at the checkpoint, vias, length.
- H1: identical SES sha1 against the control on every board; adopt with E18 if the large-board
  job CPU drops by at least 20%.
- H2: adopt (make the retained database the default) if total job CPU over the large boards drops by
  at least 15%, no board loses completion, violations do not rise anywhere, and vias over the quick
  boards rise by less than 2%.
- If CPU drops but completion moves in both directions, run the full 25-pass campaign on the
  E10-style board set before deciding.

## Cost estimate

From the campaign logs and the smoke runs: Issue297 100 items about 1-2 CPU-minutes per arm, 200
items about 3-4; RoyalBlue54L 100 items about 3, 200 items about 5; CM5_MINIMA_3 similar to
RoyalBlue (not measured); Issue157 and Mars-64 under 2 each; quick boards under 2 each. About 40
CPU-minutes per arm, 1.5 CPU-hours for the pair.

## Smoke-test results

bm01, 6 passes (`-mt 1 -mp 6 --router.optimizer.enabled=false`):

| Variant | Open connections after passes 1-6 | Job CPU | SES sha1 |
|---|---|---|---|
| heap (E18 candidate jar; byte-identity with the control is checked in E18) | 52/30/22/9/6/4 | 38.66 s | d6362ee24f03 |
| retain (fix9.jar, `-Dfreerouting.benchmark.retain_autoroute_database=true`) | 52/34/21/18/12/8 | 39.59 s | dc27f1807f03 |

The retained database runs without errors, but on bm01 it saves nothing measurable (pass 1:
13.94 s against 12.76 s for the heap jar; the control is in the E18 paired table) and leaves twice
as many connections open after 6 passes. With a new net at almost every connection, the
net-dependent rooms and the rooms around the new net's items are dropped anyway, so little is
reused. H2 is therefore unlikely to reach its threshold; run it at scale only alongside H1.

Issue297 at 60 and 100 items: see "Scaling with the item count" (fix9, JFR on). The control and
heap arms at 100 items are in the E18 paired table (`i297`).

RETAIN_GP8B_PLACEHOLDER

## Risks and follow-ups

- The retained-database mode is dormant code; it may contain stale-room bugs that only the reload
  DRC shows. Treat any new violation as a bug to fix, not a trade-off.
- Memory: retained rooms stay referenced by the engine for the whole pass; watch peak heap on
  RoyalBlue54L.
- Fallback-stage repetition (Issue157, Mars-64) is a separate cost; another package covers the
  parallel fallback.
