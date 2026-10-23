# Routing performance and correctness review (October 2026)

Status: the changes listed under "On this branch" are implemented and measured. The rest of this
document records measured-and-rejected experiments, confirmed bugs that are still open, and ranked
opportunities with the evidence collected so far.

Method: JFR profiles of the 2.5.1-SNAPSHOT jar on bm01, BBD Mars-64, FHT-8086, GP8B and Issue103,
code review of the routing, optimizer, geometry, DRC, I/O, settings and job layers, and an A/B
harness that runs two jars with the same flags and settings (one optimizer thread unless stated,
two runs at a time on a shared 4-core machine). A result-preserving change has to produce a
byte-identical SES file and the same sequence of logged board hashes. The one hash that differs
from the base commit is the last optimizer-pass hash on boards where the optimizer committed
candidates (bm01, Issue103): the hash serializes the item list including its undo copies, and the
`popSnapshot` fix below drops the dead copies at the bottom undo level. The board contents and the
SES files are identical.

## On this branch

### Result-preserving speedups

Suite: bm01 (one optimizer pass), Mars-64 (10 passes, one optimizer pass), FHT-8086 (200 items),
GP8B (300 items), hw48na (150 items, one optimizer pass, at 1 and 3 optimizer threads), interf_u,
bm05 (8 passes), rpi_splitter. Every SES file matches the base commit.

CPU of the job thread, base commit against the branch before the shove-check cache:

| Case | Base CPU | Branch CPU | Change |
|---|---|---|---|
| bm01, one optimizer pass | 183.9 s | 118.5 s | -36% |
| BBD Mars-64, 10 passes, one optimizer pass | 95.2 s | 66.7 s | -30% |
| FHT-8086 (6 layers), 200 items | 42.5 s | 25.2 s | -41% |
| GP8B (4 layers), 300 items | 74.0 s | 49.6 s | -33% |
| hw48na, 150 items, one optimizer pass | 20.4 s | 9.6 s | -53% |
| hw48na, same, 3 optimizer threads | 20.3 s | 9.7 s | -52% |
| bm05, 8 passes, one optimizer pass | 45.0 s | 31.6 s | -30% |
| Total, 9 runs | 482.8 s | 312.4 s | -35% |

Issue103 (an already routed board, optimizer only, one pass): 269.4 s to 144.2 s with one thread
(-46%) and 156.3 s to 91.0 s with three threads (-42%), same SES.

The shove-check cache (last item below) was measured separately, with the previous branch head
and the new jar running each case side by side: all 9 SES files and hash sequences identical, total
CPU -4.6% (bm01 -6.8%, Mars-64 -5.7%, FHT-8086 -4.8%, bm05 -4.4%, GP8B unchanged).

What changed, in order of measured effect:

- Search-tree nodes carry a conservative layer bit set. Room completion and layer-specific overlap
  queries skip subtrees without the query's layer; the surviving leaves keep their DFS order and
  overlap results keep their total sort order.
- Trace and diagnostic strings that the logger would discard are no longer built
  (`FRLogger.isGranularTraceActive()` mirrors exactly when `trace()` has an effect).
- `IntPoint.differenceBy`/`sideOf` skip intermediate vectors with the identical arithmetic;
  `ItemSearchTreesInfo` is array-backed; the reflective `Freerouting.globalSettings` lookup is
  cached; room completion reads leaf bounding octagons from the leaf.
- Optimizer candidates no longer build board statistics that only listeners read; the fanout
  statistic queries one pin per net.
- Optimizer candidates that rip the same connections (the vias of one connection, the branches
  beside a fork) are evaluated once; the others reuse the result under their own item id.
- The optimizer keeps a sliding window of candidates in flight instead of a barrier per chunk, and
  builds its candidate order with one sort instead of one board scan per candidate.
- Score bounds: one pass over the item list for all nets (was one per net) and no class-wide lock
  during the computation; `StoppableThread` reads are lock-free.
- `MinAreaTree` insertion computes octagon union areas without allocating the union;
  `Simplex.intersects` returns early when a grid direction separates the bounding octagons (exact,
  with a rounding margin); incomplete rooms live in a linked set.
- The shove-check cache keeps answers across board changes elsewhere: each entry records the
  region its check read from the default search tree, and a per-board grid of change stamps drops
  only entries whose region changed (see `docs/internals/shove-check-cache.md`). On bm01 (6 passes)
  cache misses fall from 444k to 356k. A verify mode recomputed about one million retained answers
  on six boards, including three optimizer threads, without a mismatch.

### Correctness fixes

- Keepouts and planes whose outline has a collinear vertex no longer vanish from the search trees
  and DRC (NaN from `acos` of a cosine rounded above 1 made the winding number 0).
- `RationalPoint.perpendicularProjection` uses the homogeneous coordinate.
- DRC, statistics, optimizer bounds, neck width and meander constants convert board units with the
  coordinate transform, not the declared resolution (10x to 100x off on LibrePCB boards).
- Locale-independent case conversion and number formatting (Turkish locales could not load mil or
  inch boards).
- Thread-safe DSN number parsing; symmetric KiCad JSON clearance matrix; SES coordinates without
  exponents; unreadable rule numbers and wide path outlines.
- Log subscribers can no longer abort a running job; a null job priority no longer stops the
  scheduler; the post-load scan runs after rules and session are applied, so the optimizer's score
  bounds match the rules; `/v1/autoroute` keeps its session; cancelled jobs are not started; a
  fatal VM error no longer leaks a parallel-job slot.
- API jobs merge the caller's settings, not the board defaults the load filled in, so DSN layer rules
  and CLI per-layer settings survive.
- Undo stacks stay in step, and popping the bottom snapshot drops dead undo copies.
- With `router.strict_drc` a rejected attempt is rolled back in place; the board is no longer
  replaced in the middle of a pass.
- `/v1/autoroute` honours `timeout_seconds`: the budget went into `job.timeoutAt`, which the job
  thread overwrites from the job timeout when it starts, so a 2 s request on bm01 took 332 s. It
  now takes 3.4 s, and the endpoint no longer reads the board while the router still runs.
- A cancel that races with the end of a job can no longer leave the job in STOPPING (the two state
  changes used different locks).
- `ReflectionUtil.copyFields` copies primitive and String arrays instead of sharing them between
  the source and the merged settings.
- `Item.setFixedState` and `unfix` were invisible to the shove-check cache (the content version
  does not count the fixed state), so a cached answer could reflect fixed states that
  `DirectPinRouting` had already restored.

## Measured and not adopted

Completion campaign: 14 boards (bm02, bm04, bm05, bm06, bm07, bm09, bm10, bm11, BBD Mars-64,
Mars-64 revE, RelayModule, wavefolder, tairakb, Green14SegLED), 25 passes, one thread, optimizer
off, 15-minute job limit. Clearance violations were unchanged in every variant.

| Variant | Open connections (13 boards, without BBD Mars-64) | Vias | BBD Mars-64 |
|---|---|---|---|
| Base (this branch) | 11 (bm04 2, bm05 9) | 773 | 4 open, 513 s |
| Roll back every failed attempt (restore the ripped victims) | 17 (bm04 1, bm05 16) | 769 | 7 open |
| Assign `AutorouteControl.ripupPassNo` (it is never set, so the detour randomization in `MazeRipupResolver` from pass 4 is dead code) | 11 | 772 | 3 open |

Rollback is not adopted: leaving the victims ripped acts as exploration. Assigning `ripupPassNo`
is not adopted: it changes routes without a measurable completion gain.

Same-pass requeue (after a successful attempt, queue one item per broken component of each ripped
net, at most twice per net and pass) on the same campaign: 10 open connections (bm04 0, bm05 10)
and 716 vias (-7%); BBD Mars-64 reached 1 open connection but hit the 15-minute limit. This variant
ran under heavier machine load than the others, so its times are not comparable. A paired campaign
on 21 further boards is in progress; the change is not adopted until it completes.

## Confirmed bugs that remain open

Security (described only as far as needed to fix them):

- MCP local-file tools and the `file_path` field of `/v1/autoroute` read and write paths chosen by
  the client, guarded only by a short denylist. An allowlisted root resolved with `toRealPath`, a
  size cap and loopback-only enablement are needed.
- `POST /v1/jobs/enqueue` builds the whole `RoutingJob` from client JSON, including fields the
  server should own. A request DTO with an explicit field list is needed.

Geometry and DRC:

- `Simplex.cutoutFrom` drops parts of outer minus inner: about 2% of random overlapping convex pairs
  lose area, and a keepout with a `(window ...)` lost 57% of its coverage in one repro, so traces
  route through solid keepout and DRC does not see it. The first-division-line merge is the main
  cause; limiting it to corners that start a ray cut the failure rate from 954 to 79 in 50,000 pairs,
  so a complete fix needs a rework with property tests. A coverage check with a conservative
  fallback is safe for keepouts but would mark plane windows as copper.
- `MazeListElement.compareTo` returns 0 for distinct doors whose hash-like ids collide
  (`ExpansionDoor.getId` is `min * 31 + max`) and whose costs are exactly equal, and the TreeSet then
  drops one of them.
- `FoundConnectionInserter.connectToTrace` builds the stubs with the other end's layer width; only
  boards with per-layer widths are affected.
- `PolygonShape.contains(FloatPoint)` rejects interior points on its convex-split seams, and
  `contains` means border-inclusive on octagons and circles but border-exclusive on simplices and
  boxes.

Optimizer and scoring:

- The V3 optimizer score floors at 0 once the excess length reaches the minimum length; on such
  boards no candidate can improve and the optimizer runs two full passes for nothing
  (Camera-Modules LG-G2 shim, induction-hob temperature sensor).
- Commits are only checked for clearance at pass end; one violating commit makes the drop loop revert
  every later, clean commit.
- Optimizer candidates share the job's stop flag; a nested router that reaches `max_items` stops the
  whole optimizer, and in-flight candidates are then counted as not improved.

Pipeline and jobs:

- An exception in a pass is logged and treated as "nothing left to route", so the job reports
  COMPLETED with open connections.
- `router.fanout.timeout` hands the partially fanned-out board to the autorouter, so the routing
  depends on machine speed when that timeout is set.
- Finished jobs keep their board, input and output until 5000 jobs exist; session eviction at 5000
  picks an arbitrary session, possibly one with a running job.
- CLI router arguments given at a GUI launch are written into `freerouting.json` and apply to every
  later CLI and API run.

## Opportunities, ranked by expected value

Result-preserving:

1. Sharper shove-check invalidation. Before the region check, a shadow run on bm01 found that 42%
   of the cache misses had an answer from an earlier board version and that 97% of those answers
   were still right. The region check recovers about half of them (misses 444k to 356k); the rest
   are checks whose region did change, because routes are inserted exactly where the checks were
   made, without changing their outcome. A finer grid (256 cells per axis) gained only 3%.
   Revalidating the first-level query result (same leaves in the same order) before dropping an
   entry is the next step.
2. Run the routing-fallback alternatives in parallel and fold the results in strategy order. All
   alternatives start from the same input bytes; the fold is the existing sequence of `replaces()`
   and stop checks. Blockers: shared `RoutingJob` fields and the global `PerformanceProfiler`.
3. Parallel read-only analysis at quiescent points (full DRC, incomplete counts, the integer parts of
   `BoardStatistics`), merged in item order. Lazy caches written during reads (`Pin`/`Via` shape
   arrays, `Item.getPrecalculatedTreeShapes`, `BoardOutline.getEdgePinNets`) must be made
   publish-safe first. DRC is a small share of the profiles (under 2% on Issue103), so this mainly
   helps large boards.
4. A per-search memo of via feasibility (`ForcedViaInserter.checkLayer`), which must replay id
   consumption and shove-failure writes as `ShoveCheckCache` does.

Result-changing (need the full before/after protocol):

1. Negotiated rip costs: a per-net rip history kept across board restores, multiplying the victim's
   rip cost, to break the rip cycles behind the boards that oscillate and restore their best board.
2. Re-route the connections a successful attempt ripped within the same pass (see the requeue
   measurement above).
3. Transactional last-mile repair on stagnation: route one open connection with rip-up at pass-1 cost,
   then its victims, keep the result only if the open count dropped with no new violation.
4. A tighter A* bound for inner layers (one via cost when the target is only on another inner layer)
   and lazy seeding of start rooms for large nets.
5. Retaining the expansion-room database between connections (room completion is 19-26% of CPU);
   rooms depend on completion order, so routes change.
