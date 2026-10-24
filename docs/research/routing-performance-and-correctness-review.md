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
- Command-line options and `FREEROUTING__*` environment variables apply to their run only: a GUI
  save no longer stores them in `freerouting.json` (a one-off
  `--api_server.authentication.enabled=false` became the default of later headless servers), nor
  runtime state such as a failed server start or a derived MCP target address. The analytics
  opt-out is the one value that is kept.
- The API server keeps at most 200 finished jobs and the routed boards of the newest ones only
  (twice `max_parallel_jobs`); session eviction never picks the GUI session or one with an active
  job; the in-memory log keeps at most 20,000 entries (2,000 per job) with bounded exceptions, and
  drops a job's entries with the job. Before, all of these grew until 5,000 jobs or sessions.
- `Item.setFixedState` and `unfix` were invisible to the shove-check cache (the content version
  does not count the fixed state), so a cached answer could reflect fixed states that
  `DirectPinRouting` had already restored.
- A pass that throws no longer ends the loop on whatever state the exception left: the board the
  pass started from is restored and the loop stops with the reason ERROR.
- A stub joining a found connection to a trace has the width of that trace's layer (it had the
  other end's layer width).
- Board copies (`deepCopy`, `BoardImage`) keep the transient baseline of pre-existing clearance
  violations. Without it a fallback continuation counted the input's violations as new (Issue313:
  "40 new violations (40 total)") and could never replace the incumbent.
- The zone-island DRC finds islands by containment depth and merges the band contours that
  `java.awt.geom.Area` cuts one region into. The old contour-by-contour merge was quadratic (the
  reload DRC of tairakb ran for over 30 minutes, now 14 s) and counted one pour as several islands
  (wavefolder: 114 dead-copper islands, now 0). Thermal spokes of same-net vias now reach the pour,
  and pour fills are clipped to the board outline.
- The DRC of a DSN plus SES no longer doubles the design's own wiring (the session carries it too),
  merges the router settings before loading (copper-to-edge and hole-clearance overrides), applies
  the via rule to session vias on SMD pads, and reads v2.5.0 via padstack names. Hole-clearance
  counts after reload now equal the input's and the router's (Mars-64 194 to 64, RelayModule 44 to
  0).

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

Maze door tie-break (`MazeListElement.compareTo` returned 0 for distinct doors whose hash-like ids
collide at equal cost, so the expansion set dropped one): with insertion order as the last
tie-break, 8 of 9 easy boards change, all still complete, vias +11 (+3%), length -0.9% to +2.1%.
Held on a local branch until the boards with open connections are measured.

Dropping the `clearance_safety_margin` from the path locators (it makes bm06's default pass loop
stall, so the fallback chain runs 92 passes instead of 7): bm06 converges in 8 passes, but over 10
boards vias go from 414 to 427. Not adopted.

Checking each optimizer commit for clearance as it lands, instead of dropping commits from the end
at pass end: the drop path never fired in 30 optimizer runs. Reusing an optimizer candidate's
result in the next pass when its region is unchanged: not possible as stated, because passes
alternate the preferred-direction setting.

## Confirmed bugs that remain open

Security (described only as far as needed to fix them):

- MCP local-file tools and the `file_path` field of `/v1/autoroute` read and write paths chosen by
  the client, guarded only by a short denylist. An allowlisted root resolved with `toRealPath`, a
  size cap and loopback-only enablement are needed.

Geometry and DRC:

- `Simplex.cutoutFrom` drops parts of outer minus inner: about 2% of random overlapping convex pairs
  lose area, and a keepout with a `(window ...)` lost 57% of its coverage in one repro, so traces
  route through solid keepout and DRC does not see it. The first-division-line merge is the main
  cause; limiting it to corners that start a ray cut the failure rate from 954 to 79 in 50,000 pairs,
  so a complete fix needs a rework with property tests. Fixed on this branch: the division's
  result is checked (no overlap with the cut shape or between pieces, areas add up) and an exact
  half-plane sweep replaces it when the check fails; the fuzz harness then finds no failure at any
  coordinate scale. Only Issue756 tomu-fpga8/9 have areas with holes in the fixtures, and both
  route to identical session files.
- Plane nets: an item counts as connected (CONNECTED_TO_PLANE) as soon as its connected set touches
  the pour polygon, but foreign-net tracks routed through the pour can cut it off from the actual
  fill. On tairakb pad U3-23 ends up connected to neither GND pour and nothing reports it.
- The zone-island DRC checks connectivity per layer, so an island reached through a through-hole
  pin or via on another layer is reported as unconnected.
- `PolygonShape.contains(FloatPoint)` rejects interior points on its convex-split seams, and
  `contains` means border-inclusive on octagons and circles but border-exclusive on simplices and
  boxes.

Optimizer and scoring:

- The V3 optimizer score floors at 0 once the excess length reaches the minimum length; on such
  boards no candidate can improve and the optimizer runs two full passes for nothing
  (Camera-Modules LG-G2 shim, induction-hob temperature sensor).
- Commits are only checked for clearance at pass end; one violating commit makes the drop loop revert
  every later, clean commit.
- A nested optimizer router that reaches `max_items` calls `requestStop()` on the shared job thread
  (`AutoroutePassRunner.java:119`) and stops the whole job. The interrupted pass now commits nothing
  instead of a winner from a cut-off candidate set.

Pipeline and jobs:

- The routing fallback chain runs up to five more full pass loops after a stalled default loop:
  bm06 takes 92 passes (7 in v2.5.0, 3x the CPU) although each pass is faster than in v2.5.0.
  Trying `short-escape` first halves that with identical output on bm06, Issue313 and Issue367,
  but changes which strategy wins elsewhere.
- `router.fanout.timeout` hands the partially fanned-out board to the autorouter, so the routing
  depends on machine speed when that timeout is set.
- `GET /v1/sessions/{id}/logs` is always empty: nothing logs with a session id as topic.
- A log listener that throws propagates into the thread that logged, possibly a routing thread.
- `GlobalSettings.setDefaultValue` edits a fresh copy of the file, so the long-lived session copy
  overwrites it at its next save for paths the command line did not set; floats are rounded to two
  decimals when the settings are saved.
- The router reports a connection ROUTED without checking that the inserted path reaches its
  target (`AutorouteEngine.autorouteConnection`); off-centre fixed track ends made this loop
  silently on caniot.

## Second red-team round (October 2026)

Nine red-team reviews covered the autorouter, the optimizer, geometry, DRC, file formats, settings,
the REST and MCP servers, the GUI and concurrency. Fixed on this branch from that round: the GUI
file-dialog and settings-reload defects, the rules-file precedence and settings parsing defects,
the REST and MCP request hardening, the optimizer's leftover candidates and cut-off passes, and
the DRC report order, `-drc` post-load race and pin and via shape-cache publication.

No routing nondeterminism was found: SES files were byte-identical for `-mt 1` against `-mt 3`
(bm02, Green14SegLED and tairakb, with optimizer commits) and across G1, Serial and Parallel GC and
`-XX:hashCode=2`. A brute-force clearance checker (exact distances between every pair of tree
shapes) agrees with `DesignRulesChecker.getAllClearanceViolations()` on 9 routed boards and two
synthetic boards built to sit just below and above each clearance.

Open, by area:

- **SES reload drops wires.** `SesPathGeometry.crossingLines` rejects wires whose corners have
  fractional coordinates and `SesReader.processWireScope` drops the whole wire: 38 of 354 wires on
  Issue209-split05 and 14 of 406 on Issue297-myboard. The reload DRC's unconnected count is
  inflated on those boards.
- **Geometry.** `Polyline.offsetShapes` can cut real copper out of a trace's tree shapes when the
  polyline winds back or turns the other way (11-15 of 20,000 random polylines);
  `Line.sideOfIntersection` misjudges near-collinear corners at coordinates around 1e6-1e7. A
  candidate fix for both exists and keeps the SES on four boards so far, but it changes tree
  shapes and needs the full before/after protocol. `IntOctagon` normalized octagons can have
  half-integer vertices, and `IntOctagon.cutoutFrom(IntBox)` returns overlapping pieces (no area
  lost).
- **Board load.** `Issue208-freerouting.dsn` takes 12-34 s and 3.2-3.8 GB of allocation to load:
  48 keepout circles of 501 mm are tiled over their whole bounding box into 393,648 tree shapes.
- **Units.** `BoardStatistics` lengths in "mm" ignore the board resolution (10x off on a um-10
  board, 1000x on mil-1000); the optimizer compares against the same scaling, so both must change
  together. `router.trace_pull_tight_accuracy` is in board units (500 means 0.5 um to 50 um
  depending on the board). Outline strips use 100 board units whatever the resolution.
- **Optimizer.** The "all vias are mandatory layer transitions" guard is vacuously true when every
  via is user fixed and skips the optimizer whatever the trace length (Issue093-interf_u); a fix
  measured 0.88 mm shorter traces for 33 s more CPU on interf_u and is held for a wider campaign.
  The clearance veto counts violations, duplicate candidates are re-routed serially, and the
  Manhattan length bound is not a lower bound on 45-degree boards.
- **Autorouter state.** `RoutingBoard.failureLog` is never cleared and is copied with the board,
  so continuations, exploration, optimizer candidates and GUI re-runs start with earlier failure
  counts. The final tail cleanup is skipped whenever the loop stops on its own criterion. The
  result manifest describes the last fallback attempt, not the chosen one, and `phases.fallback`
  is never filled. The board-rank stagnation stop can never fire.
- **Settings.** Floats and doubles are written with two decimals by the shared Gson (settings file,
  KiCad JSON, DRC JSON, API responses); the `logging.*` section of `freerouting.json` is ignored;
  a log file location given by environment variable also becomes the user-data directory; the
  documented fanout `pin_sorting_order` values do not match the code.
- **REST and MCP.** Failed API-key attempts are not throttled, each connected SSE client holds a
  thread, and an API key is not bound to a profile (ownership trusts the profile header).
- **Planes.** A trace that reconnects a pad cut off from the fill closes a cycle through the pour
  polygon and is deleted again by trace normalization, so a repair needs connectivity through fill
  islands. The check that a ROUTED connection reaches its target fires on 56-66% of connections
  when run inside `AutorouteEngine`, which points to the following `optChangedArea` step.
- **Memory.** The printed "peak heap usage" is heap occupancy including garbage, 3-10x the live
  set (TeamAdapt: about 69 MB live against 311-342 MB printed). No leak between jobs was found.
- **Output cost.** Headless runs write the SES plus a full DSN (for its checksum) on the routing
  thread every 10 s in CLI mode and on every board update with the API server running; not yet
  measured.

The experiment packages under `docs/research/experiments/` turn the measurable ideas from this
round into ready-to-run campaigns.

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
   bm06 spends 61% of its job in four sequential fallback pass loops.
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
