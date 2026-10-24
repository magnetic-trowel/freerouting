# E02: maze door tie-break by insertion order

Keep two different doors that share a door id and an equal cost in the maze expansion list,
instead of silently dropping the second one, and measure the effect on the boards that still
have open connections.

## Status

Designed; smoke test pending (queued); not yet run at scale.

## Hypothesis

The legacy order drops a door whenever its id collides with another door's id at exactly equal
sorting and expansion values. The dropped door is a real alternative that the search never
expands. Keeping it makes the search explore every door it generates, which can only add
alternatives; it changes routes on most boards because ties are broken differently.

What was measured before (nine easy boards, all complete in both arms): 8 of 9 boards change,
all stay complete, vias +11 (+3%, 375 to 386), wire length -0.9% to +2.1% per board. On easy
boards this is noise around a neutral result. The open question is the hard boards: the
expectation is a small completion gain (0 to 2 fewer open connections over the ten hard boards)
at neutral vias; a loss is equally plausible, because a different tie order is effectively a
different random seed. The experiment decides whether the change is worth adopting as a
correctness fix (a search that does not lose candidates) or only as a neutral one.

## Mechanism (code at d4383b44)

- `src/main/java/app/freerouting/autoroute/maze/MazeListElement.java:79-113`: `compareTo` orders
  by `sortingValue`, `expansionValue`, `door.getId()` and `sectionNoOfDoor`, and returns 0 when
  all four are equal (line 112).
- Door ids are hash-like and not unique:
  `autoroute/expansion/ExpansionDoor.java:215-220` (`min(id1, id2) * 31 + max(id1, id2)`, so the
  room pairs (1, 62) and (2, 31) give the same id), `autoroute/drill/ExpansionDrill.java:127-130`
  (`31 * (31 * location.getId() + firstLayer) + lastLayer`),
  `autoroute/expansion/TargetItemExpansionDoor.java:71-74` (`31 * item.getId() + room.getId()`),
  `autoroute/drill/DrillPage.java:205-208`. All classes share one id space, so a door can also
  collide with a drill or a target door.
- `autoroute/maze/MazeSearchEngine.java:84-125`: the expansion list is a `TreeSet`; `add` (line
  123) returns false for an element that compares equal, so the second door is never expanded.
  Elements are added from `MazeSearchEngine.java:548, 964, 1088` and
  `MazeExpansionEngine.java:103, 146, 383`, and polled at `MazeSearchEngine.java:327`.

## Change

Code: `patch.diff` (applies with `git apply` on d4383b44).

- `MazeListElement` gets a `long insertionNo` (0 by default). `compareTo` first compares the four
  legacy keys (moved unchanged to `compareLegacyKeys`) and then `Long.compare(insertionNo,
  other.insertionNo)`.
- New `autoroute/maze/MazeTieBreak.java`, one instance per expansion list. Its `beforeAdd` is
  called by the list's `add` override after the fanout escape-window filter and before
  `super.add`. With `-Dfreerouting.exp.maze_tiebreak=insertion` it assigns 1, 2, 3, ... in
  insertion order (an element that is added again keeps its first number). With the default
  `legacy` every number stays 0, so the last comparison returns 0 as before: the property-off
  arm is byte-identical to head (verified with `cmp`, see smoke results).
- `-Dfreerouting.exp.maze_tiebreak_stats=true` counts, per added element, whether the list already
  held an element with equal legacy keys of the same door (benign duplicate) or of a different
  door (the collision; dropped in the legacy order, kept in the insertion order), and how many of
  the collisions are between doors of different classes. The probe is a read-only
  `TreeSet.ceiling`, so it does not change routing. `AutorouteBatchLoop` logs one line at the end
  of the auto-routing stage:
  `Experiment freerouting.exp.maze_tiebreak=<mode>: N maze list adds, S tied with an element of
  the same door, D tied with an element of a different door with the same id (C of them a door of
  another class)`. The counters are process totals (fanout and auto-routing stages; the
  optimizer is off in this experiment).
- Tests: `src/test/java/app/freerouting/autoroute/maze/MazeListElementTest.java` (legacy drop,
  insertion keep, cost order first, same-door counting, stable insertion number).

## Determinism and time-budget check

The insertion number is a per-list counter incremented in the order of `add` calls, which is
fixed by the deterministic search; no hashing, identity order, clock or budget is involved. The
stats probe reads the set only. For two elements of the same door section the earlier one still
comes first, and the later one is skipped when polled because the section is occupied by then
(`MazeSearchEngine.java:327` onwards), which matches the old drop. Determinism of the insertion
arm itself was checked by running it twice on one board (identical SES, see smoke results).

## Boards

`boards.txt`:

- Group A (10 boards): every board of `camp3/run1/summary.jsonl` whose master arm ended with
  open connections: FastTest 2, TeamAdapt-LinePCB 4, Pajalnaja_stancija 2, bm04 2, bm05 9,
  Issue684 4, Issue039-bug-design 9, Charger 6 (the campaign parser missed it because the log said
  "1 violation"), myboard 3 and RoyalBlue54L-Feather 24 (both hit the 20-minute limit there).
- Group B (9 boards): the nine easy boards of the earlier measurement (bm02, bm06, bm07, bm09,
  bm10, bm11, RelayModule, wavefolder, Green14SegLED; their master vias sum to 375, which matches
  the earlier "+11 (+3%)"). They must stay complete. bm02 doubles as the cheap smoke board.

## Variants

`variants.txt`: `control` (legacy, stats on) and `insertion` (stats on). Both run the same jar.

## How to run

```
scripts/experiments/build_variant.sh d4383b44 \
  docs/research/experiments/E02-maze-door-tiebreak/patch.diff build/e02.jar
python3 scripts/experiments/paired_campaign.py \
  --out logs/E02 \
  --boards docs/research/experiments/E02-maze-door-tiebreak/boards.txt \
  --variants docs/research/experiments/E02-maze-door-tiebreak/variants.txt \
  --flags "-mt 1 -mp 25 --router.optimizer.enabled=false" \
  --parallel 8 --timeout 00:45:00
grep -h "Experiment freerouting.exp.maze_tiebreak" logs/E02/*/*/console.log
```

(`head` in `variants.txt` is the jar built from the commit under test, here `build/e02.jar`. The
log path in the last line depends on the harness layout; the line is in each run's console log.)
The 45-minute safety stop lets myboard and RoyalBlue finish; runs that still hit it are excluded
from the comparison as the harness describes.

Optional second checkpoint (AGENTS.md asks for two): rerun group A with `-mp 10` in `--flags`.

## Metrics and decision rule

Per board and variant from `summary.jsonl`: reload-DRC unconnected and clearance violations by
type, router unrouted, vias, wire length, CPU seconds, SES identity against control; plus the
collision counters from the log line.

- Must not regress: reload-DRC clearance violations never increase on any board; completion
  (reload-DRC unconnected) never gets worse on any group-B board.
- Adopt if, over group A, the sum of open connections drops by at least 2 with no board worse by
  more than 1, vias within +3% overall, CPU within +10% overall.
- If group A is neutral (within +-1 open connection in total) and nothing regresses, adopt it as
  a correctness fix only if the counters show collisions on most boards (the search loses real
  candidates); otherwise keep legacy and close the experiment.
- Any lost completion on a group-B board or any added violation rejects the change.

## Cost estimate

From the camp3/run1 master CPU times at the same flags: group A about 3470 CPU-s (myboard 1464
and RoyalBlue 813 of it, both at the old 20-minute limit, so more with the 45-minute stop),
group B about 280 CPU-s. Per variant about 1.1 CPU-hours including the reload DRCs, so about
2.2 CPU-hours for the two variants (up to about 3 with the longer stop). At `--parallel 8` the
wall time is bounded by myboard (about 25 to 45 minutes).

## Smoke-test results

Pending: the smoke run was queued behind the machine-wide job limiter when this package was
committed; see the report of this package for the numbers if they arrived later.

## Risks and follow-ups

- A different tie order acts like a reseed: per-board changes in either direction are expected,
  so judge on group totals, not single boards.
- The proper fix is unique door ids (for example a per-search sequence number), which would
  remove the collision instead of working around it in the comparator; that changes the
  ordering of every id tie, not only the colliding ones, and needs its own measurement.
- The counters are process totals; with the optimizer on they would include optimizer searches
  after the stage line (the line is printed at the end of the auto-routing stage).
- `ExpansionDrill` ids collide whenever two drills share a location and layer range, which E03
  (via-fitting drills) makes more likely; run E03 with and without E02 if both are adopted.
