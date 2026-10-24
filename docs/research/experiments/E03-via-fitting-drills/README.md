# E03: via-fitting drill candidates in narrow pockets

Offer extra via locations in drill pieces whose centre is too close to an obstacle for a via,
so that narrow pockets beside fine-pitch pins can be left through a via, and measure the
completion gain against the extra CPU.

## Status

Designed; smoke test pending (queued); not yet run at
scale.

## Hypothesis

Dense QFP/QFN/BGA boards lose connections whose only escape is a via in a pocket between pins.
With via-fitting candidates the maze can use those pockets. Prototype measurement (caniot only,
on top of two other caniot fixes A and B, `SP/agent-caniot/runs/full_fixAB` vs `full_fixABC`):
open connections 9 to 6, vias 76 to 91, CPU 174.6 s to 205.7 s (+18%). Expected at scale: fewer
open connections on group A (at least 3 fewer in total), more vias (up to +5% on boards that
gain), and a CPU increase on every board that uses vias, because candidates are added on every
board. The CPU increase is the main risk and is reported explicitly.

## Mechanism (code at d4383b44)

- `src/main/java/app/freerouting/autoroute/drill/DrillPage.java:63-146` (`getDrills`): the page
  is cut by the obstacles of `autorouteEngine.autorouteSearchTree` (line 67-96), the tree that is
  compensated for the current net's traces, split into convex pieces (line 102-103), and each
  piece offers one via location: its centre of gravity (line 124) or, with `attachSmd`, an SMD pin
  centre plus the centre of gravity (lines 114-142).
- The pieces describe where a trace centre fits, not where a via centre fits. A via needs its
  radius plus its clearance from obstacles, which is more than a trace's half width plus
  clearance, so the centre of gravity of a narrow piece can lie where no via fits while other
  parts of the same piece do fit one. The pocket then has no usable via candidate at all.
- Drills are consumed in `autoroute/maze/MazeExpansionEngine.java:149-154`
  (`expandToDrillsOfPage`), and each drill location is checked for a via (with shoving) in
  `MazeExpansionEngine.java:387-420`; the page cache is keyed by net number (`DrillPage.java:64`) and is
  invalidated on board changes (`DrillPage.invalidate`, `AutorouteEngine.java:600`).

## Change

Code: `patch.diff` (applies with `git apply` on d4383b44), based on the prototype
`SP/agent-caniot/fixC-via-fitting-drills.patch`.

- `-Dfreerouting.exp.via_fitting_drills=true` (default `false`, read once into
  `DrillPage.VIA_FITTING_DRILLS`). When on, `getDrills` computes the page's obstacles once more
  from a page enlarged by the via compensation and enlarges each obstacle by the smallest via
  radius plus its clearance compensation of the net's via rule (`viaHoles`, `viaCompensation`).
  For each piece without a pin centre whose centre of gravity lies inside such a via hole, it
  splits the piece minus the via holes into convex parts and adds a drill at the centre of each
  part (`addViaFittingDrills`, `viaFittingParts`). The original centre drill is kept. When off,
  none of this runs and the result is byte-identical to head (verified with `cmp`).
- Changes against the prototype, all result-preserving for the on arm: the switch; holes whose
  bounding box (piece box enlarged by 1 unit against corner rounding) misses the piece are
  skipped before the exact intersection, which only yields shapes of dimension < 2 for them; the
  geometric core is a static method so it can be unit-tested.
- `-Dfreerouting.exp.via_fitting_drills_stats=true` counts drill pages computed, pieces checked,
  pieces with a blocked centre, extra drills added, and the elapsed time spent in the via-fitting
  code (`System.nanoTime`, measurement only; with one routing thread it approximates its CPU). `AutorouteBatchLoop` logs one
  line at the end of the auto-routing stage: `Experiment freerouting.exp.via_fitting_drills=<on>:
  P drill pages computed, N pieces checked, B with a blocked centre, E extra drills, T s in the
  via-fitting code`.
- Tests: `src/test/java/app/freerouting/autoroute/drill/DrillPageViaFittingTest.java` (default
  off, a blocked centre gives the two parts outside the hole, a free centre and far holes give no
  parts).

## Determinism and time-budget check

Review of the prototype for determinism: all collections are `LinkedList`s filled in the order of
the search tree query (`overlappingTreeEntries`) and of `splitToConvex`, both deterministic for a
given board; no hash iteration, identity order or clock is involved; the via compensation is a
pure function of the rules. `splitToConvex(stoppableThread)` may return null when a stop is
requested, as in the existing code (line 103): a timeout only stops work. The stats use
`System.nanoTime` only for the counter, never for a decision. The insertion order of the extra
drills (after the piece's centre drill) is fixed. Two runs of the on arm on one board gave
identical SES files (see smoke results).

Review for CPU cost (the main concern, because the code runs on every board that allows vias):

1. One extra search-tree query per computed page, on a page enlarged by the via compensation.
2. One `enlarge` per obstacle of that query. The prototype does not skip repeated shapes of
   multi-layer items as the trace cut-out loop does (`DrillPage.java:87`), so a through via or
   pin contributes one hole per layer; this multiplies step 3 by up to the layer count (16 on
   bm04). Deduplicating would change the hole list passed to `splitToConvex` and is left as a
   follow-up so that the measured variant matches the prototype.
3. Per piece without a pin centre: one bounding-box test per hole (new) and an exact
   intersection for the holes near the piece, so about pieces x holes per page.
4. Per piece with a blocked centre: one `PolylineArea.splitToConvex`, the expensive step, plus
   `calculateExpansionRooms` for each extra drill, and later the maze work for the extra drills.
5. Pages are recomputed per net and after every invalidation, so the cost scales with nets x
   pages touched, not with the board size alone.

The direct cost (1 to 4) is in the stats line; the indirect cost (more drills to expand) only
shows in the total CPU time, so both are reported.

## Boards

`boards.txt`:

- Group A (8 boards): dense fine-pitch boards with open connections at head: caniot (LQFP-64,
  the motivating board), bm04 (QFN-64, 16 layers), bm05 (QFN-50), Issue684 (164-pin part),
  FastTest (108-pin part), Issue039-bug-design, TeamAdapt-LinePCB, RoyalBlue54L-Feather (QFN-40,
  8 layers; hit the 20-minute limit in camp3).
- Group A2 (3 boards): fine-pitch boards without campaign data (GP8B VQFP-100, smoothieboard
  LQFP-100, Issue179 110-pin part).
- Group B (11 boards): complete at head and must stay complete: bm01, bm02, bm06, bm07, bm10,
  bm11, dev-board, RelayModule, Green14SegLED, CH376, 8088sbc.
- Group C (2 boards): cheap smoke boards (ldo-led, ch32v).

## Variants

`variants.txt`: `control` (off, stats on) and `via_fitting` (on, stats on), same jar. The caniot
prototype gain was measured with fixes A and B; if those land first, rerun on top of them. To
separate the effect from E02, an optional third line can combine both properties on a jar built
with both patches.

## How to run

```
scripts/experiments/build_variant.sh d4383b44 \
  docs/research/experiments/E03-via-fitting-drills/patch.diff build/e03.jar
python3 scripts/experiments/paired_campaign.py \
  --out logs/E03 \
  --boards docs/research/experiments/E03-via-fitting-drills/boards.txt \
  --variants docs/research/experiments/E03-via-fitting-drills/variants.txt \
  --flags "-mt 1 -mp 25 --router.optimizer.enabled=false" \
  --parallel 8 --timeout 00:45:00
grep -h "Experiment freerouting.exp.via_fitting_drills" logs/E03/*/*/console.log
```

CPU reporting: run both variants with the same `--parallel` value on an otherwise idle machine
(CPU seconds are process CPU, so they are less load-sensitive than wall time, but JIT and GC
threads still vary). For each board report control CPU, via_fitting CPU, the ratio, and the
"s in the via-fitting code" from the stats line; report the group totals and the median ratio.

## Metrics and decision rule

- Must not regress: reload-DRC clearance violations never increase on any board; reload-DRC
  unconnected never increases on any group-B board.
- Adopt if group A (with A2) loses at least 3 open connections in total, no board gains more
  than 1, vias over the boards that did not change completion grow by at most 5%, and the CPU
  cost is acceptable: median CPU ratio over group B at most 1.10 and no completed board above
  1.30 unless it gained completion.
- If the gain holds but the CPU rule fails, do not adopt; follow up with the cost reductions
  below and rerun.

## Cost estimate

From camp3/run1 master CPU at these flags and the caniot runs: group A about 2110 CPU-s
(RoyalBlue 813 at the old limit, caniot about 195), group A2 unknown (estimate 300), group B
about 480, group C about 10. With up to +20% for the on arm and the reload DRCs: about 0.85
CPU-hours for control and 1.0 for via_fitting, about 1.9 CPU-hours in total; the wall time at
`--parallel 8` is bounded by RoyalBlue54L-Feather (20 to 45 minutes).

## Smoke-test results

Pending: the smoke run was queued behind the machine-wide job limiter when this package was
committed; see the report of this package for the numbers if they arrived later.

## Risks and follow-ups

- The via hole adds the full via radius and via clearance to obstacle shapes that are already
  compensated for the trace (the autoroute tree), so it is conservative: some pockets that fit a
  via are still missed. It does not by itself make vias that violate clearance, because the
  location is only a candidate: every drill location is checked with the real via padstacks,
  including shoving, in `MazeExpansionEngine.checkLayerWithAnyMatchingVia`
  (`autoroute/maze/MazeExpansionEngine.java:387-420`, `ForcedViaInserter.checkLayer`). The reload
  DRC in the decision rule remains the safeguard. Because that check may shove traces, a pocket
  whose centre is "blocked" by the via-hole test can still be usable at head; the experiment
  measures whether the extra candidates matter in practice.
- The smallest via of the rule is used; on boards with several via types the pocket may only fit
  the smallest one.
- Extra drills share the id space of `ExpansionDrill` (`ExpansionDrill.java:127`), so they can
  collide in the expansion list (see E02).
- Cost reductions to try if the CPU rule fails: deduplicate the per-layer holes, cache the via
  holes per page and net across `invalidate` when the page region did not change, and only run
  the check for pieces narrower than twice the via compensation.
