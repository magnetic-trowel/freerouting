# E01: wire-end pin snap and final tail cleanup

Measure two reviewed fixes on the corpus: (A) imported wire ends that miss a same-net pin centre by
at most 1 um are moved onto it, and (B) leftover fanout vias and tails are removed once after
routing even when the pass loop stopped early.

## Status

Designed; smoke-tested on Issue269-caniot-tiny-arm (`-mp 5`), Issue957-smoke-ldo-led and
Issue676-ch32v-tx118s (`-mp 25`); not yet run at scale.

## Hypothesis

- **Fix A (wire snap).** On the 27 fixtures whose fixed DSN wiring has ends 0.2-1 um off a same-net
  pin centre, the reload DRC reports fewer unconnected items, and the router spends fewer vias and
  less wire on connections that already exist in copper. Investigator measurement on
  `Issue269-caniot-tiny-arm` (`-mt 1 -mp 25`, optimizer off): unconnected 11 -> 9, vias 100 -> 78,
  `via_dangling` 12 -> 2. The router starts with fewer unrouted nets (99 -> 89). Expected corpus
  effect: unconnected items drop on most of the 27 boards and never rise; vias drop.
- **Fix B (final tail cleanup).** On boards whose pass loop stops before everything is routed (pass
  limit, no improvement, stagnation, timeout), the reload DRC reports no `via_dangling` and no
  `track_dangling` that come from fanout stubs. Investigator measurement on caniot with A applied:
  vias 78 -> 76, `via_dangling` 2 -> 0, unconnected unchanged at 9. Campaign `camp3/run1` (head-like
  master) shows the same symptom on unaffected boards: `Issue039-bug-design` 12 `via_dangling`,
  `Issue157-TeamAdapt-LinePCB` 13, `Issue209-split05` 1 plus 4 `track_dangling`. Expected: these
  counts go to 0; completion and clearance violations unchanged (a tail is by definition connected at
  one end only, so removing it cannot disconnect anything).
- **A+B.** Effects add: caniot 11 / 100 / 12 (unconnected / vias / dangling vias) -> 9 / 76 / 0.

## Mechanism

References are to commit d4383b44.

- Fix A: `Trace.getNormalContacts` connects a trace end to a pin or via only when the end equals the
  drill item's centre exactly (`src/main/java/app/freerouting/board/model/items/Trace.java:255-257`).
  The DSN reader rounds each wire corner to the board grid independently of the pad position
  (`src/main/java/app/freerouting/io/specctra/parser/Wiring.java:511`), and KiCad writes track ends
  and pad positions with different rounding, so a track that ends on a pad centre in KiCad lands
  0.2-0.5 um beside it. KiCad's `(type route)` wires load as `USER_FIXED`
  (`Wiring.calcFixed`, `Wiring.java:257-268`), and `Trace.isRoutable()` excludes user-fixed traces
  (`Trace.java:270-272`), so the router can neither rip up nor rebuild that wire. The pin stays an
  open that the router keeps trying to close by routing around the fixed copper, which costs vias,
  and that the reload DRC still reports at the end.
- Fix B: the pass loop removes tails and orphan fanout vias only when it ends with no stop request
  (`src/main/java/app/freerouting/autoroute/pipeline/AutorouteBatchLoop.java:648-657`). The pass
  limit, no-improvement, stagnation, error and timeout exits all call `stopFor`, which requests the
  stop (`AutorouteBatchLoop.java:341-344`, `:399`, `:406`, `:770-775`), so every board that does not
  finish completely keeps the fanout stubs of its unrouted pins: vias connected on one layer only.
  The same holds for every batch loop run by the fallback strategies
  (`RoutingFallbackRunner.java:321`). `RoutingPipeline.runRoutingStage` then hands the board to the
  output as it is (`src/main/java/app/freerouting/autoroute/pipeline/RoutingPipeline.java:125-131`).

## Change

Code: `patch.diff` (applies with `git apply` on d4383b44).

- `io/specctra/parser/Wiring.java`: after the corners of a `path` wire are rounded, its first and
  last corner are moved onto the centre of a pin that shares the wire's net, lies on the wire's
  layer, contains the corner and whose centre is at most `PIN_CENTER_SNAP_UM` = 1.0 um away
  (`snapEndsToPinCenters`; the nearest such centre wins; an end already on a centre is kept).
- `autoroute/pipeline/RoutingPipeline.java`: `removeLeftoverTails()` runs once after the main
  batch loop, the fallback strategies and exploration, unless the user cancelled the job or the
  router's board is no longer the job's board. It calls the same
  `BatchAutorouter.removeTails(StopConnectionOption.NONE)` that the pass loop uses.
- `settings/ExperimentFlags.java` (new): reads `-Dfreerouting.exp.<name>`; `true`/`false` (any case)
  select the arm, a missing property keeps the default, anything else keeps the default and logs a
  warning.
- Switches, read once into `static final` constants when the class is initialised:
  - `-Dfreerouting.exp.wire_snap=true|false` (`Wiring.WIRE_SNAP_ENABLED`), default **true**;
  - `-Dfreerouting.exp.final_tail_cleanup=true|false`
    (`RoutingPipeline.FINAL_TAIL_CLEANUP_ENABLED`), default **true**.

  Unlike the template's default, both switches default to the new behaviour, because the fixes are
  reviewed and intended to ship; the control arm sets both to `false` and is byte-identical to
  plain d4383b44 (verified with `cmp`, see smoke results).
- Tests: `WireEndPinSnapTest` (snap within 0.5 um, no snap at 3 um, no snap onto a foreign-net pin,
  and the switch-off path in a child JVM started with `-Dfreerouting.exp.wire_snap=false`) and
  `ExperimentFlagsTest` (parsing and defaults). The `final_tail_cleanup=false` path has no unit
  test; it is covered by the byte-identity check of the control arm.

## Determinism and time-budget check

- Fix A is a pure function of the DSN geometry: for each wire end it scans the items picked at that
  point (in the search tree's deterministic order) and takes the nearest same-net pin centre within
  a fixed distance in board units. It runs once at import, before routing.
- Fix B runs once at a fixed point of the pipeline. Whether it runs depends only on the user-cancel
  flag and board identity, not on elapsed time, remaining budget or the reason the loop stopped.
  After a job timeout it still runs (a timeout may stop work, but should not change what the result
  contains); the tail removal itself is cheap.
- `BatchAutorouter.removeTails` pulls the changed area tight with
  `TIME_LIMIT_TO_PREVENT_ENDLESS_LOOP` = 1000 (`BatchAutorouter.java:42`, `:638-650`;
  `TraceTightener.java:84-87`, `:205-222`). Despite the name this is not wall-clock time:
  `datastructures.TimeLimit` counts calls of `limitExceeded()` as steps, so the pull-tight stops at
  the same point on every machine and Fix B keeps the result independent of machine load. (An
  earlier draft of this package called it a wall-clock limit; that was wrong.)

## Boards

`boards.txt` (32 boards):

- the 27 fixtures with off-centre wire ends found by the import probe (`SnapProbe`, 53 fixtures
  with DSN wiring scanned; the end counts are in the trailing comments). These are where A acts and
  where boards stop at the pass limit with fanout stubs, so B acts too;
- `Issue508-DAC2020_bm02` (no DSN wiring, completes) and `Issue676-ch32v-tx118s` (48 DSN wires, none
  off-centre): cheap smoke boards on which A must be a no-op and B nearly so;
- `Issue093-interf_u`: GND-plane board with 653 DSN wires and none off-centre; regression guard for
  the plane-routing path;
- `Issue209-split05` and `Issue039-bug-design`: no off-centre ends, but head leaves dangling vias or
  tracks (camp3: 1+4 and 12), so they show B alone on unaffected input.

## Variants

`variants.txt`, first line is the control:

| name | `wire_snap` | `final_tail_cleanup` |
|---|---|---|
| `control` | false | false |
| `a_snap` | true | false |
| `b_tail` | false | true |
| `ab` | true | true |

All use the `head` jar = d4383b44 + `patch.diff`. Optional lines (commented out): `plain`, a jar
built from d4383b44 without the patch, to confirm the control is byte-identical at scale; and
`ab_rerun`, a second `ab` arm for the determinism check above.

## How to run

From the repository root on a checkout that contains `scripts/experiments/` and this directory:

```
E=docs/research/experiments/E01-wire-snap-tail-cleanup
mkdir -p runs/E01/jars
# Evaluator (and optional 'plain' arm): plain d4383b44.
scripts/experiments/build_variant.sh d4383b44 - runs/E01/jars/E01-plain.jar
# Experiment jar: d4383b44 + patch.diff.
scripts/experiments/build_variant.sh d4383b44 $E/patch.diff runs/E01/jars/E01-exp.jar

python3 scripts/experiments/paired_campaign.py --out runs/E01 \
  --boards $E/boards.txt --variants $E/variants.txt \
  --head-jar runs/E01/jars/E01-exp.jar --evaluator runs/E01/jars/E01-plain.jar \
  --flags "-mt 1 -mp 25 --router.optimizer.enabled=false" --timeout 00:20:00 --parallel 8
```

Use `--filter Issue269-caniot` for a single-board check first; `--dry-run --filter caniot` with
the harness copy of 2026-10-24 printed the expected four route and four reload-DRC commands. To enable the optional arms,
uncomment them in a copy of `variants.txt` (`plain` resolves `jars/E01-plain.jar` relative to the
current directory, the variants file or the repository; give an absolute path if needed).

The evaluator must be the plain jar: the reload DRC (`-de board.dsn+out.ses -drc`) replaces the DSN
wiring with the session's, so it does not depend on A, but a fixed evaluator keeps all arms judged by
the same code.

## Metrics and decision rule

Per board and variant from `summary.jsonl`: reload-DRC unconnected items, reload-DRC violations by
type (`clearance`, `holeClearance`, `via_dangling`, `track_dangling`, ...), router unrouted count,
vias, wire length, CPU seconds, and SES sha1. Runs that hit the safety timeout are reported
separately and excluded from the comparison.

Each fix is judged against the control on its own (`a_snap` vs `control`, `b_tail` vs `control`)
and together (`ab` vs `control`). Adopt a fix if, over the boards that finish in both arms:

1. reload-DRC clearance-type violations (`clearance` + `holeClearance`) never increase on any board;
2. completion never drops on any board (reload-DRC unconnected items never increase);
3. and the total reload-DRC unconnected items or the total `via_dangling` decreases.

Also expected, but not adoption criteria: `b_tail` has `via_dangling` = 0 on every board; on
`Issue508-DAC2020_bm02` and `Issue676-ch32v-tx118s` the `a_snap` SES equals the control's; the
`plain` arm (if run) is identical to `control` everywhere; `ab_rerun` (if run) is identical to `ab`.
A failure of one of these is a bug to investigate before adopting.

## Cost estimate

Rough, from single-threaded runs on a loaded 4-core machine (wall time roughly equals CPU time
with `-mt 1`; the reload DRC adds about 5-30 s per run):

- Boards with a known `-mp 25` time on head-like master (`camp3/run1`, investigator runs):
  `Issue015` 151 s, `Issue054` 27 s, `Issue153` 6 s, `Issue157` 321 s, `Issue367` 45 s,
  `Issue957` 7 s, `Issue269-caniot` 250 s, `bm02` 8 s, `Issue676` 9 s, `Issue093` 37 s,
  `Issue209-split05` 41 s, `Issue039` 311 s: about 20 min together.
- The five `Issue756-tomu-fpga*` boards took 4.5 min to the full 20-min safety stop in earlier runs;
  assume about 10 min each, 50 min together.
- The other 15 affected boards have no recorded time; assume 1-20 min each (15 min to 5 h).

Per variant: about 1.5-6.5 CPU-hours; four variants: about 6-26 CPU-hours (each optional arm adds a
quarter). With `--parallel 8` on a 12-core machine that is roughly 1-3.5 hours of wall time. The
smoke run of `Issue269-caniot-tiny-arm` at `-mp 5` took 121 s wall / 81 s CPU with the head jar on
the loaded machine, consistent with the 250 s recorded for 25 passes on a quieter one.

## Smoke-test results

Jar: d4383b44 + `patch.diff` (`E01.jar`); `head` = `SP/ab/jars/fix9.jar` (head behaviour). Common
flags `-mt 1 --router.optimizer.enabled=false --router.job_timeout=00:20:00`, `-mp 5` for caniot
and `-mp 25` for the others; reload DRC with the head jar. Wall and CPU seconds are from a shared,
loaded 4-core machine. Build: `spotlessCheck checkstyleMain checkstyleTest` clean for the changed
files, `WireEndPinSnapTest` 4/4 and `ExperimentFlagsTest` 4/4 pass (none skipped, so the child-JVM
switch-off test ran).

| board | variant | state | router unrouted | DRC unconnected | DRC violations | vias | wall s | CPU s | SES sha1 |
|---|---|---|---|---|---|---|---|---|---|
| Issue269-caniot-tiny-arm | head | COMPLETED | 28 | 19 | track_dangling 8, via_dangling 6 | 43 | 121 | 81 | `3f5775ece4be` |
| Issue269-caniot-tiny-arm | control | COMPLETED | 28 | 19 | track_dangling 8, via_dangling 6 | 43 | 90 | 104 | `3f5775ece4be` |
| Issue269-caniot-tiny-arm | a_snap | COMPLETED | 24 | 20 | via_dangling 13 | 51 | 94 | 108 | `8d1f7195f6e0` |
| Issue269-caniot-tiny-arm | b_tail | COMPLETED | 28 | 19 | track_dangling 8 | 37 | 85 | 105 | `faffd5396501` |
| Issue269-caniot-tiny-arm | ab | COMPLETED | 24 | 20 | none | 38 | 93 | 115 | `8b7d45c4578e` |
| Issue676-ch32v-tx118s | head | COMPLETED | 0 | 0 | none | 6 | 7 | 5 | `7df61179da5b` |
| Issue676-ch32v-tx118s | control | COMPLETED | 0 | 0 | none | 6 | 8 | 6 | `7df61179da5b` |
| Issue676-ch32v-tx118s | ab | COMPLETED | 0 | 0 | none | 6 | 8 | 7 | `7df61179da5b` |
| Issue957-smoke-ldo-led | head | COMPLETED | 0 | 0 | track_dangling 1 | 14 | 7 | 6 | `680f01cc5612` |
| Issue957-smoke-ldo-led | control | COMPLETED | 0 | 0 | track_dangling 1 | 14 | 9 | 6 | `680f01cc5612` |
| Issue957-smoke-ldo-led | a_snap | COMPLETED | 0 | 0 | none | 14 | 7 | 6 | `b3e4419b6613` |
| Issue957-smoke-ldo-led | b_tail | COMPLETED | 0 | 0 | track_dangling 1 | 14 | 8 | 7 | `680f01cc5612` |
| Issue957-smoke-ldo-led | ab | COMPLETED | 0 | 0 | none | 14 | 7 | 6 | `b3e4419b6613` |

- **Byte identity:** `cmp` of the SES files: `control` = `head` on all three boards
  (Issue676, Issue957, caniot at `-mp 5`). On Issue676 (no off-centre ends, completes) `ab` is also
  identical to `head`, so both fixes are no-ops there. On Issue957 `b_tail` = `head` (the board
  completes, so the pass loop already cleaned up).
- **Fix A** on Issue957: two wire ends move by 0.5 um onto pin centres, one leftover router stub
  disappears, and the reload DRC's one `track_dangling` goes away. On caniot the router starts
  with 89 instead of 99 unrouted nets and the 8 `track_dangling` of the control are gone. The same
  8 tracks (0.02-2.9 mm long, F.Cu) remain in `b_tail`, so B does not remove them; most likely they
  are the user-fixed DSN wires whose ends miss the pad centres (inferred, not traced item by item).
- **Fix B** on caniot: `via_dangling` 6 -> 0 and vias 43 -> 37 with the same unconnected count; with
  A, `via_dangling` 13 -> 0 and vias 51 -> 38.
- **Caution, caniot at `-mp 5`:** `a_snap` and `ab` end with 24 router-unrouted connections instead
  of 28, but with 20 reload-DRC unconnected items instead of 19. After 5 passes the board is far
  from finished and the two arms route different nets (the sets of unconnected pairs differ almost
  completely), so this is trajectory divergence rather than a connection the fix breaks; at
  `-mp 25` the investigator measured 11 -> 9. It does show that the per-board rule "unconnected
  never increases" can be tripped by divergence on unfinished boards; see Risks.

## Risks and follow-ups

- The reload DRC uses Freerouting's exact-centre connectivity model. KiCad connects a track to a
  pad by copper overlap, so KiCad probably already treats the control's 0.3 um-off ends as
  connected; part of A's unconnected-count gain is Freerouting's own accounting. The router-side
  gain (fewer vias and detours spent on connections that exist) is real either way; checking a few
  `ab` results in KiCad's DRC would confirm that the moved ends (by at most 1 um) raise nothing new.
- A moves only the first and last corner; a wire that passes through a pad without ending there,
  or an end within 1 um of a centre but outside the pad shape, is unchanged. A snap of up to 1 um
  can in principle bring copper 1 um closer to a neighbour; at KiCad clearances (>= 100 um) and
  inside the pin's own pad this is negligible, and criterion 1 checks it.
- B also runs after a job timeout. The pull-tight after tail removal uses the same step budget as
  every tail removal (see Determinism), so it does not depend on machine load.
- The investigator's B-only caniot run hit its 5-minute job timeout (TIMED_OUT, 12 unconnected), so
  that number is load-dependent; this experiment's 20-minute safety stop avoids it.
- The per-board completion rule is strict. Any change of the routing input (A starts the router
  with fewer unrouted nets) sends unfinished boards down a different trajectory, and the smoke run
  showed a +1 unconnected item on caniot at `-mp 5` although the router's own count fell by 4.
  If a board fails the rule by a small margin, rerun that board at a second pass limit (for
  example `-mp 15` and `-mp 40`) before rejecting the fix, and report it separately from boards
  that finish in both arms.
- Follow-up after adoption: remove the switches and `ExperimentFlags` if no other experiment uses
  it, keeping the fixes unconditional.
