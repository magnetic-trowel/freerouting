# E18: result-preserving speedups after the October cuts

Four exact rewrites (`patch.diff`): a binary-heap maze expansion queue with the tree set's
semantics, and three changes on the room-completion path; three more ranked candidates are designed
but not implemented. Every change must keep the SES byte-identical.

## Status

Designed; smoke-tested on Issue508-DAC2020_bm01, Issue066-Project_GP8B and Issue297-myboard;
not yet run at scale.

## Hypothesis

The profile changed after the earlier cuts (layer-masked tree walks, the shove-check cache): on
GP8B and Issue157 the tree walk itself (`MinAreaTree.overlapsUnsortedUnlocked`, 17-18% self time),
room completion (`ShapeSearchTree45Degree.completeShapeUnlocked`, 11% self) and the generic
polygon containment test that room completion calls on octagons (`TileShape.contains`, 8.3% self
on GP8B) are the top three frames. The patch removes most of the third, about a third of the
second's node tests, and one octagon allocation per tree level on insertion. Expected: job CPU
-5% to -10% on boards where room completion dominates (GP8B, fanout-heavy boards), less on bm01.

On the expensive tail of a large board the profile is different: in Issue297's first pass
(`max_items=100`, 105 s job CPU, 96 of them after the 60th connection) 88% of the routing thread
is in the maze search proper, and `TreeMap.put` (72.4% inclusive, 43.2% self) with
`MazeListElement.compareTo` (23.4% self) is the expansion list (`MazeSearchEngine.java:84`, a
`TreeSet`) growing to a very large size in searches that drain it; `pollFirst` is only 6.0%. A
binary heap inserts with O(1) comparisons on average instead of O(log n) pointer-chasing ones.
Expected: -30% to -60% job CPU on such tails; SES byte-identical on every board.

## Hot spots (JFR, `settings=profile`, fix9.jar = head behaviour)

Recordings and full aggregations: `SP/agent-exp-perf/jfr/<run>/{rec.jfr,agg.txt}`; aggregator
`SP/agent-exp-perf/jfragg.py` (phase buckets, outer phases, self/inclusive frames, allocation by
class and site, `--callers`, `--alloc-callers`, `--timeline`). Routing thread only.

| Share of routing-thread samples | GP8B, 300 items | Issue157, 200 items | Issue297, 60 items |
|---|---|---|---|
| Samples | 7144 | 1412 | 579 |
| Room completion (innermost bucket) | 38.5% | 26.3% | 34.5% |
| Insertion (incl. fanout insertion, normalize) | 20.0% | 21.2% | 29.9% |
| Shove / via feasibility checks | 20.0% | 12.0% | 9.2% |
| Maze search proper | 11.1% | 15.3% | 8.8% |
| Statistics, incomplete counts | 3.0% | 4.7% | 5.2% |
| Fanout stage (outer) | 34.3% | 18.6% | 57.0% |

Top self-time frames, GP8B: `MinAreaTree.overlapsUnsortedUnlocked` 18.0%,
`ShapeSearchTree45Degree.completeShapeUnlocked` 11.2%, `TileShape.contains` 8.4% (7.8% called
from `completeShapeUnlocked`), `TreeMap.put` 2.5%, `IntOctagon.normalize` 2.4%,
`restrainShape` 2.0%, `BigInteger.multiplyByInt` 1.9% (from `IntPoint.perpendicularProjection`),
`MazeListElement.compareTo` 1.9%, `Line.direction` 1.9%, `MinAreaTree.positionLocate` 1.4%.
Issue157 has the same top two (17.3%, 11.0%).

Allocation (sampled weight), GP8B 33.9 GB: `IntOctagon` 16%, `IntPoint` 12.9% (largest site
`Line.<init>` 7.2%, the two points of every `IntOctagon.borderLine`), `int[]` 9.6%, `BigInteger`
8.6% and `MutableBigInteger` 3.4% (sites: `IntPoint.perpendicularProjection` 12% together),
`IntOctagon.union` 5.1% (Issue157: 4.8% of 7.1 GB from `MinAreaTree.positionLocate`).

Callers of the tree walk on Issue157: `Trace.getNormalContacts` 5.1% of all samples (three tree
queries per call), via-feasibility `ForcedPadRouter.checkForcedPad` 2.0%,
`BasicBoard.overlappingItems` 2.0%, `PolylineTrace.split` 1.1%, `ExpansionDrill` 1.0%.

## Candidates, ranked by expected gain

| # | Change | Evidence | Status |
|---|---|---|---|
| 0 | Maze expansion list as a binary heap with tree-set semantics | `TreeMap.put` 72.4% inclusive on the Issue297 tail; 2.5-2.9% self on GP8B and Issue157 | in `patch.diff` |
| 1 | Allocation-free point and octagon containment on `IntOctagon` | `TileShape.contains` 8.4% self (GP8B); `Line.<init>`+`borderLine` 11% of allocation | in `patch.diff` |
| 2 | Inline bounding-octagon coordinates in tree nodes | `overlapsUnsortedUnlocked` 17-18% self, pointer chase node -> `IntOctagon` | designed |
| 3 | `completeShapeUnlocked`: skip the pop-time test for nodes pushed after the last shrink | `completeShapeUnlocked` 11% self | in `patch.diff` |
| 4 | `IntPoint.perpendicularProjection` in exact `long` arithmetic, BigInteger only on overflow | BigInteger 4.6% CPU, 12% of allocation (GP8B) | designed |
| 5 | `Trace.getNormalContacts()`: one bounds query instead of bounds + two end-point queries | 10.2% inclusive, 5.1% in the tree walk (Issue157) | designed |
| 6 | `MinAreaTree.positionLocate`: keep the node bounds when they already contain the leaf | `IntOctagon.union` 4.8-5.1% of allocation, `positionLocate` 1.4-2.1% self | in `patch.diff` |

### 0. Binary-heap expansion list (in the patch)

`MazeSearchEngine.mazeExpansionList` (d4383b44 `MazeSearchEngine.java:52`, created at `:84`) is a
`TreeSet<MazeListElement>`; the search adds every candidate door section
(`MazeSearchEngine.java:548`, `:964`, `:1088`, `MazeExpansionEngine.java:103`, `:146`, `:383`) and
`occupyNextElement` polls the smallest until it finds an unoccupied section (`:322-:337`). A popped
element is not always followed by occupying its section (drill pages, delayed rip-up occupation,
`:346-:381`), so duplicates matter and the queue must reject exactly what the tree set rejects.

Change: new `MazeExpansionQueue` (array binary heap) keeps the key of every held element: sorting
value, expansion value, door id and section number, the four fields `MazeListElement.compareTo`
(`MazeListElement.java:80`) compares, with -0.0 folded into 0.0 because `compareTo` uses `<`/`>`.
`add` rejects an element whose key is held, as `TreeSet.add` rejects an element comparing equal to
a held one (this includes the colliding-door-id case). The held elements are pairwise distinct
under `compareTo`, so the heap minimum is the unique smallest element and `pollFirst` returns
what the tree set returns. The anonymous subclass with the fanout escape-window filter is
unchanged. Caveat: a NaN sorting or expansion value would make the tree set's order ill-defined;
the costs are finite sums, so this does not arise. `MazeExpansionQueueTest` runs 400k random
add/poll operations with colliding ids, ties and -0.0 against a `TreeSet` and checks every return
value and every polled element by identity.

### 1. Octagon containment without line objects (in the patch)

`ShapeSearchTree45Degree.completeShapeUnlocked` (d4383b44 `ShapeSearchTree45Degree.java:231` and
`:244`) calls `ignoreShape.contains(intersection)` and `ignoreShape.contains(currentShape)`; the
final filter (`:336`) calls `getContainedShape().contains(getShape())`. The static type is
`TileShape`, so `TileShape.contains(TileShape)` (`TileShape.java:186`) runs: for each of the 8
corners of the argument it allocates an `IntPoint` and calls `isOutside` (`TileShape.java:143`),
which allocates a `Line` with two `IntPoint`s per border line (`IntOctagon.borderLine`,
`IntOctagon.java:286`) and evaluates `Line.sideOf` -> `IntPoint.sideOf(Line)`
(`IntPoint.java:117`), a determinant in `double`.

Change: `IntOctagon` overrides `isOutside(Point)` for `IntPoint` arguments and
`contains(TileShape)` for `IntOctagon` arguments. Why it is exact: for border line `i` with points
`a`, `b` from `borderLine(i)`, `Line.sideOf(p) == ON_THE_LEFT` if and only if `Side.of(det) == ON_THE_LEFT`
if and only if `det > 0`, with `det = (p.x - a.x) * (b.y - a.y) - (p.y - a.y) * (b.x - a.x)` evaluated on
ints that are exact in `double`. `b - a` is one of (1,0), (1,1), (0,1), (-1,1), (-1,0), (-1,-1),
(0,-1), (1,-1), so each determinant reduces to one integer comparison with the same integer
differences (`x - lowerRightDiagonalX > y` and so on; the two sums are formed in `long`). The
corner loop uses the existing `cornerX`/`cornerY`, which are `corner(no)` without the point. No
normalization is assumed; `RationalPoint` arguments and non-octagon shapes keep the generic code.
A unit test compares both rules on 100k random tight, loose, empty and degenerate octagons and
points on and next to their corners; a separate 4M-point run against the candidate jar agreed
everywhere.

### 2. Inline node bounds (designed)

`MinAreaTree.overlapsUnsortedUnlocked` (`MinAreaTree.java:94`) visits a node, follows
`node.boundingShape` to a separate `IntOctagon` and makes two virtual calls
(`IntOctagon.intersects(Shape)` -> `shape.intersects(IntOctagon)`) per node. Store the eight
bounds as `int` fields in `ShapeTree.TreeNode` next to `layerMask`, written wherever
`boundingShape` is assigned (`ShapeTree.java:263`, `:284`, `MinAreaTree.java:189`, `:269`, `:282`;
a single `setBounds` method), and test them inline when the query is an `IntOctagon` and the tree
is 45-degree; keep `boundingShape` for everything else. Exact because `IntOctagon.intersects`
(`IntOctagon.java:695`) is a pure function of the eight fields. Expected: a share of the 17-18%,
mostly cache misses; the gain must be measured, and memory grows by 32 bytes per node.

### 3. Skip the redundant pop-time test in room completion (in the patch)

`completeShapeUnlocked` tests a child against `boundingShape` before pushing it (`:317`, `:322`) and
again when popping it (`:178`). `boundingShape` only changes after an obstacle restrains the room
(`:307`). The patch keeps a watermark: entries below it were pushed before the last change (or are
the untested root) and are tested again on pop; entries at or above it were tested with the
current `boundingShape` and are not. Exact because `intersects` is a pure function of two
unchanged shapes; the visit order, the leaves tested and the obstacles applied are the same.
Invariant: the watermark never exceeds the stack size (set to the size on a change, lowered to
the popped index when an older entry is popped). The 90-degree and any-angle trees
(`ShapeSearchTree90Degree.java:86`, `ShapeSearchTree.java:913`) have the same loop and can take the
same change.

### 4. Exact projection without BigInteger (designed)

`IntPoint.perpendicularProjection` (`IntPoint.java:150`, used by `TileShape.nearestBorderPoint`)
computes `projX`, `projY` and the denominator with `BigInteger`. With board coordinates below
2^26 and border-line vectors of length 1, every product fits in a `long`. Compute with
`Math.multiplyExact`/`addExact`, fall back to the BigInteger code on `ArithmeticException`; return
`new IntPoint((int) (projX / d), (int) (projY / d))` when both `Math.floorMod(..., d) == 0`
(BigInteger `mod` is non-negative and the divisions are exact, so the quotients and the
`intValue()` truncation agree), otherwise `new RationalPoint(BigInteger.valueOf(projX), ...)` with
the same values.

### 5. One query for trace contacts (designed)

`Trace.getNormalContacts()` (`Trace.java:181`) queries the end points (twice, through
`getNormalContacts(Point, boolean)`) and then the trace's bounding octagon. The bounding query
covers both end points, so the end-point candidates can be filtered from its result with the same
tests (`TileShape.getInstance(point)` overlap with each candidate's tree shape on the layer, then
the existing corner/contains checks). The result is a `TreeSet<Item>`, so order is unaffected.
Needs care: `overlappingObjects` filters by exact tree-shape overlap, which the filter must
replicate; verify with a shadow comparison on the suite before switching.

### 6. Keep containing node bounds on insertion (in the patch)

`MinAreaTree.positionLocate` (`MinAreaTree.java:184`) replaces every node bounds on the descent
with `leaf.union(node)`. `IntOctagon.union` (`IntOctagon.java:396`, coordinate-wise min/max) of a
leaf that `isContainedIn` (`IntOctagon.java:672`) the node (coordinate-wise comparisons in the matching direction for all
eight fields) has exactly the node's fields, so the patch keeps the node's object. Search trees are
transient (`BasicBoard.searchTreeManager`), so board serialization and hashes are unaffected; no
code compares node bounds by identity.

## Change

Code, `patch.diff` (applies to d4383b44 with `git apply`): candidates 0, 1, 3 and 6, plus unit
tests (`MazeExpansionQueueTest`; `IntOctagonTest`: `isOutsideMatchesTheBorderLineSides`,
`containsOctagonMatchesTheCornerTest`; `ArrayStackTest.sizeCountsTheLiveElements`). No `-Dfreerouting.exp.*` switch: the change must be
byte-identical in both arms, so the experiment compares the head jar with the patched jar; a
switch would add a branch to the innermost loops of a change meant to be merged as is.

## Determinism and time-budget check

All four rewrites compute the same booleans and the same octagon field values as before; no
iteration order, tie-break, counter or clock is involved. Nothing reads elapsed time or budget.

## Boards

`boards.txt`: bm01 (6 passes) and GP8B (300 items) are the profiled smoke pair; the large boards
that dominate campaign wall time (Issue157, Issue297, RoyalBlue54L, CM5_MINIMA_3, BBD Mars-64)
bounded by items or passes; bm05, interf_u (plane routing) and bm01 with one optimizer pass from the
earlier result-preserving suite.

## Variants

`variants.txt`: `control head` and `speedup build/e18.jar` (head plus `patch.diff`).

## How to run

```
scripts/experiments/build_variant.sh d4383b44 \
  docs/research/experiments/E18-result-preserving-speedups/patch.diff build/e18.jar
python3 scripts/experiments/paired_campaign.py --out logs/E18 \
  --boards docs/research/experiments/E18-result-preserving-speedups/boards.txt \
  --variants docs/research/experiments/E18-result-preserving-speedups/variants.txt \
  --flags "-mt 1 -mp 25 --router.optimizer.enabled=false" --parallel 2 --timeout 00:30:00
```

Run it twice (or with the variant order swapped) to separate the CPU effect from load noise.

## Metrics and decision rule

- Must hold on every board: identical SES sha1 between the two variants (the identical-SES matrix
  in `summary.md` must be all identical), so completion, violations, vias and length are equal
  by construction. Any difference is a bug in the patch, not a trade-off.
- Measured: job CPU seconds (router's "using N total CPU seconds") and process CPU per board.
- Adopt if total job CPU over the suite drops by at least 3% in both repetitions and no board is
  more than 3% slower in both.

## Cost estimate

From the smoke runs on a loaded 4-core machine (load average about 20): bm01 6 passes ~130 s
process CPU, GP8B 300 items ~110 s, Issue157 200 items ~40 s, Issue297 100 items ~60-120 s
(pass 1 of 96 items cost 117 s in the campaign), RoyalBlue 100 items ~170 s (fanout 42 s plus most
of pass 1), BBD Mars-64 10 passes ~70 s. About 2 x 2 x 15 CPU-minutes = 1 CPU-hour per repetition.

## Smoke-test results

Jars: control `SP/ab/jars/fix9.jar` (head behaviour; its main sources equal d4383b44), candidate
`SP/agent-exp-perf/build3/cand2.jar` (fix9 with the patched classes recompiled by javac 25 and
updated into the jar). Flags as in "How to run"; one optimizer-free routing thread.

Done before the paired runs (machine load average about 20, so CPU numbers are indicative only):

- `MazeExpansionQueueTest` (400k random operations against a `TreeSet`) and the two
  `IntOctagonTest` equivalence tests pass when run standalone against the candidate jar; a further
  4M-point / 200k-containment random comparison of the new `IntOctagon` methods against the
  border-line rule found no mismatch.
- bm01, 6 passes, candidate: open connections after each pass 52/30/22/9/6/4, job CPU 38.66 s,
  SES sha1 d6362ee24f03. The control of the same jar generation is in the paired run below.
- google-java-format 1.30.0 (`SP/bin/gjf --dry-run`) reports no changes on the touched files;
  `git diff --check` is clean.

Paired runs (control then candidate in the same limiter slot; `SP/agent-exp-perf/r2/`, table from
`python3 SP/agent-exp-perf/r2/table.py bm01 gp8b i297`):

PAIRED_TABLE_PLACEHOLDER

## Risks and follow-ups

- The `isOutside` override changes the code path for every `IntOctagon` point test in the program
  (DRC, outline checks, optimizer), not only room completion. It is exact by the derivation above
  and the randomized test, but a reviewer should re-derive the eight determinants.
- Candidates 2, 4 and 5 are not implemented; 2 has the largest potential and the largest code
  surface (all writes to node bounds must go through one setter).
- The measured gain is from paired runs on a heavily loaded shared machine; repeat on a quiet
  machine before quoting a percentage.
