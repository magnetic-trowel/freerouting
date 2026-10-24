# E17 dense-escape pre-pass for fine-pitch QFP and BGA parts

A deterministic, geometry-only escape pass for fine-pitch peripheral (QFP/QFN/SOIC) and
area-array (BGA) parts that runs before the existing fanout stage, so that every pin of a dense
part gets its escape via in a fixed, collision-free pattern before any maze search runs.

## 1. Status

Designed; prototype of the peripheral (QFP) part implemented (`patch.diff`); smoke-tested on
Issue269-caniot-tiny-arm and Issue508-DAC2020_bm02 (section 11); not yet run at scale. The
prototype implements sections 4.1 (peripheral classification only), 4.2, and 4.3 with the
`stagger` and `out_only` patterns. Not yet implemented: the `alternate` pattern with inside
escapes, the pocket-first order of 4.3, and the BGA dog-bone of 4.4 (any other `pattern` value
behaves as `stagger`). The companion experiment E16 (`../E16-net-ordering/`) supplies the
`fine_pitch_first` and `constrained_first` orders that can be combined with this pre-pass.

## 2. Hypothesis

On boards whose open connections end on a fine-pitch part (caniot: all 6 open nets at
`fix9` + caniot fixes end on U1, an LQFP-64 at 0.5 mm pitch; Issue157-TeamAdapt, Issue297-myboard,
Issue732-CM5_MINIMA_3 have similar parts), a planned escape pattern removes the two failure modes
of the current fanout:

- the first-come via placement of the maze-search fanout puts vias where the next pin's escape
  needs to go (no stagger, no alternation), and
- pocketed pins (pins whose straight outward escape is blocked by a neighbouring part or by the
  vias of their neighbours) are processed last in `outer_first` order and find no via site.

Expected: open connections on those boards drop by at least half (caniot 6 to at most 3) with no
new reload-DRC violations; via count may rise by up to the number of escaped pins; CPU should fall
because the pre-pass replaces maze searches with a few legality checks per pin.

## 3. Mechanism (code at d4383b44)

Current fanout:

- The fanout stage runs before the pass loop when `fanout.enabled` (default true,
  `settings/sources/DefaultSettings.java:222`) and the board has SMD pins
  (`autoroute/pipeline/AutorouteBatchLoop.java:109-158`).
- `BatchFanout` orders components by SMD pin count, descending, then id
  (`autoroute/pipeline/BatchFanout.java:761-772`), and pins by `pin_sorting_order`, default
  `outer_first` = farthest from the component's pin centroid first, ties by pin index
  (`BatchFanout.java:821-857`). There is no cross-component awareness and no notion of rows,
  sides or pockets.
- Each pin is escaped by a maze search, `RoutingBoard.fanout` (`board/facade/RoutingBoard.java:1001`),
  with `AutorouteControl.isFanout = true` (`RoutingBoard.java:1051`). The search stops at the
  first drill it reaches (`autoroute/maze/MazeSearchEngine.java:359-366`); on the start layer it
  only expands within `max_escape_length_mm` (default 4.5 mm) and rejects drills closer than
  `min_escape_length_mm` (default 2.5 mm) (`MazeSearchEngine.java:88-121`). The via therefore
  lands wherever the cost function first reaches a drillable spot at least 2.5 mm away, which on a
  0.5 mm pitch row is typically in front of the neighbouring pins.
- Each pin gets a `TimeLimit` of `max_milliseconds_per_pin * (pass + 1)`
  (`BatchFanout.java:244-245`) and the stage has an optional wall-clock timeout
  (`BatchFanout.java:104-127`), so the current fanout's result can depend on machine speed when
  a search is cut by its time limit.
- A pin already connected to anything off its layer is skipped (`RoutingBoard.java:1017-1023`),
  so vias placed before the fanout stage are respected by it.
- Escape vias are protected as fanout vias (`board/model/items/Item.java:1342`, stub shorter than
  `PROTECT_FANOUT_LENGTH` = 400 half-widths, `Item.java:42`) and survive the per-pass tail removal
  with `StopConnectionOption.FANOUT_VIA` (`autoroute/pipeline/AutoroutePassRunner.java`, the
  `removeTails` call at the end of `runSingleThread`).
- The routing fallback chain re-runs fanout with a relaxed escape window when the default loop
  stalls (`autoroute/pipeline/RoutingFallback.java:57-95`: `fanout-retry`, `short-escape`,
  `open-escape`, `direct-first`, `no-fanout`), which shows that the escape geometry is a known
  lever, but each strategy is a full extra pass loop.

caniot evidence (from the caniot investigation in `SP/agent-caniot/`): U1 pins sit on four rows,
x = 132.325 / 143.675 mm and y = -62.675 / -51.325 mm, pitch 0.5 mm, pad 0.775 mm. The six
connections still open after the caniot fixes all start on U1 (U1-4 /GPIO_PC15, U1-36 /USER_LED,
U1-45 /USB_D+, U1-51 /USART4_TX, U1-53 /GPIO_PC12, U1-57 /SPI1_MOSI); two of them are pins in a
pocket where no via fits within the escape window (`fixC-via-fitting-drills.patch` adds drill
candidates for narrow pockets for exactly that reason).

## 4. Change (algorithm to implement)

Switch: `-Dfreerouting.exp.dense_escape=true` (read once into a `static final boolean` in the
new package-private class `autoroute/pipeline/DenseEscape`). Optional tuning properties, all read
once: `freerouting.exp.dense_escape.fine_pitch_um` (default 650),
`freerouting.exp.dense_escape.min_pins` (default 16), `freerouting.exp.dense_escape.max_escape_um`
(default 3000), `freerouting.exp.dense_escape.pattern` (design default `alternate`; the prototype
default is `stagger`, and it also implements `out_only`). When the switch is off nothing runs, so
the off arm executes head's code.

Prototype (`patch.diff`, standalone on d4383b44): `DenseEscape.java` (classification, escape
selection, insertion with snapshot/undo and the strict-DRC check), a 3-line hook in
`AutorouteBatchLoop` before `BatchFanout.fanoutBoard`, and `DenseEscapeTest` (U1 of caniot
classifies into four sides of 16 pins; a pre-pass on caniot escapes pins, leaves every via free
of clearance violations, and gives the same counts on a second load). The prototype inserts with
`BasicBoard.insertVia` / `insertTrace` and accepts the escape only when
`BatchAutorouter.strictDrcRejection` finds no clearance violation on the new items and the pin is
then connected off its layer; otherwise `undo` restores the snapshot. This is the same
acceptance test as the fanout's (`BatchFanout.java:331-333`) and is equivalent to the
depth-0 `ForcedViaInserter.check` described below, at the cost of an insert per rejected site.

Hook: in `AutorouteBatchLoop`, immediately before the `BatchFanout.fanoutBoard` call (line 158)
and inside the same `isFanoutEnabled()` branch, call `DenseEscape.run(router.board,
router.settings, router.thread)` when enabled. Log one INFO line with the counts.

### 4.1 Component classification (deterministic, geometry only)

For each component, in ascending component id order, take its SMD pins that have a net that is
not ignored. Skip components with fewer than `min_pins` such pins.

1. Pitch `p` = smallest centre-to-centre distance between two distinct pins (same helper as
   `RouteOrder.minPitch`). Skip if `p > fine_pitch_um` for peripheral parts or `p > 1000 um`
   for area arrays.
2. Bounding box `B` of the pin centres. A pin is on side S (W, E, S, N) if its centre is within
   `p/2` of that edge of `B`. If every pin is on a side, the part is peripheral; corner pins that
   match two sides go to the side along which their pad is longer (pad bounding box aspect), ties
   to the side first in W, S, E, N order.
3. Otherwise, if the pin centres snap to a grid of step `p` (every centre within `p/10` of
   `B.ll + (i*p, j*p)`) with at least 3 rows and 3 columns, the part is an area array; ring
   number `r(i,j) = min(i, j, cols-1-i, rows-1-j)`.
4. Anything else is left to the current fanout.

### 4.2 Which pins are escaped

A pin is escaped by the pre-pass when all of the following hold:

- its net is not a plane net with the pin already touching the plane, and the pin is not already
  connected to an item off its layer (same test as `RoutingBoard.fanout`, lines 1017-1023);
- its net's nearest other terminal (pin centre) is farther than `3 * p` from the pin, or lies on
  the far side of `B` (the connection would have to cross the part); pins whose partner is a
  nearby same-layer pad (decoupling capacitor, series resistor next to the pin) are left to the
  router, which connects them on the surface;
- the net class has a via rule (same check as `BatchFanout.java:256-282`).

### 4.3 Peripheral pattern (QFP/QFN/SOIC)

Per side, sort the side's pins by their coordinate along the side (ties by pin index). Let `n` be
the outward normal of the side, `t` the side direction, `e` the distance from the pin centre to
the outer end of its pad along `n`, `d` the via pad diameter of the net's first via, `c` the
via-to-trace clearance of the net class, and `w` the net's trace half width.

Candidate via sites for the pin at index `k` of its side:

- `out_near`: centre + `(e + c + d/2) * n`
- `out_far`:  centre + `(e + c + d/2 + d + c) * n`  (second, staggered row)
- `in`:       centre - `(e_in + c + d/2) * n`, where `e_in` is the distance to the inner pad end,
  only when the region strictly inside the pin ring on the pin's layer contains no copper of
  another net (one `ShapeSearchTree` overlap query on the inner box shrunk by `c`).

Pattern `alternate` (default): pins with even `k` try `out_near`, then `out_far`, then `in`; pins
with odd `k` try `in`, then `out_far`, then `out_near`. Pattern `stagger`: even `k` -> `out_near`,
odd `k` -> `out_far`, no `in`. Pattern `out_only`: `out_near`, `out_far`. Within a candidate,
if the site is not legal, slide it along `n` (away from the pad) in steps of `d/2` up to
`max_escape_length_mm`; never along `t`, so the vias of neighbouring pins cannot collide.

Legality of a site: `ForcedViaInserter.check(...)` (`board/actions/ForcedViaInserter.java:133`)
with `maxRecursionDepth = 0` and `maxViaRecursionDepth = 0` (place only into free space, never
shove), and `RoutingBoard.checkForcedTracePolyline` (`board/facade/RoutingBoard.java:419`) with
depth 0 for the straight stub from the pin centre to the site, width `2w`.

Insertion: `ForcedViaInserter.insert` (line 253) and `RoutingBoard.insertForcedTracePolyline`
(line 467) with depth 0, both inside one `generateSnapshot()`; then
`BatchAutorouter.enforceStrictDrc(board, net, maxItemIdBefore)` exactly as the fanout does
(`BatchFanout.java:331-333`); on any rejection `undo` the snapshot and record the pin as
`DENSE_ESCAPE_FAILED` for the log. The current fanout then retries such pins with its normal
maze search.

Pocket handling: before placing any via of a part, compute for every candidate pin whether
`out_near`, `out_far` and `in` are all illegal on the empty-of-escapes board ("pocketed"). Pins
are processed in this order: pocketed pins first (they get the remaining in-body sites before the
neighbours take them), then the remaining pins by side W, S, E, N and index `k`. The order
depends only on geometry.

### 4.4 Area-array pattern (BGA dog-bone)

- Ring 0 (outer row and column) and, when `p >= 0.8 mm`, ring 1 escape on the surface and are
  left to the router.
- Every other ball (i, j) gets one via at the centre of the 2x2 ball quad in the direction of its
  quadrant: `dx = i < cols/2 ? -1 : +1` and `dy = j < rows/2 ? -1 : +1` (ties at the exact centre
  go to -1), site = centre + `(dx * p/2, dy * p/2)`; stub straight from ball centre to site.
- A quad centre that another ball of the same part already uses goes to the other diagonal of
  the same quadrant pair (flip `dx`), then is skipped. Legality and insertion as in 4.3.
- If `d + 2c > p * sqrt(2) - ball_diameter`, the part needs via-in-pad; skip it and log.

### 4.5 Order and determinism

Components in descending pin count, then ascending id (the same order as `BatchFanout`); pins
as above. No maze search, no `TimeLimit`, no elapsed-time check: the pre-pass is a fixed
sequence of geometric checks, so it is deterministic and time-budget agnostic by construction.
The job's stop request may end it between pins (a stop only stops work).

## 5. Determinism and time-budget check

Deterministic: every iteration is over id- or coordinate-sorted lists; all arithmetic is on the
board's integer coordinates and doubles from them; no hash-order iteration decides anything.
Time-budget agnostic: there is no time limit in the pre-pass. Note that the existing fanout that
runs afterwards keeps its per-pin `TimeLimit` (`BatchFanout.java:244-245`); the pre-pass reduces
how many pins reach it, it does not change it.

## 6. Boards

`boards.txt`: caniot (U1 LQFP-64; the target), Issue157-TeamAdapt-LinePCB, Issue297-myboard,
Issue732-CM5_MINIMA_3 (dense), the open-connection boards from camp3 run1, and the clean
regression boards. bm02 and caniot are the smoke boards.

## 7. Variants

`variants.txt`: `control` = the E17 jar with the property off; `head-unpatched` = d4383b44
without the patch (its SES must equal `control`); `dense` = the prototype's `stagger` pattern;
`dense_out` = `out_only`; `dense_fine_pitch_first` = E17 combined with the E16
`fine_pitch_first` order (jar built with both patches). Add `dense_alternate` once the inside
escapes of 4.3 are implemented.

## 8. How to run

```
scripts/experiments/build_variant.sh d4383b44 - jars/head.jar
scripts/experiments/build_variant.sh d4383b44 docs/research/experiments/E17-dense-escape/patch.diff jars/e17.jar
# jar with both experiments: apply E16's patch.diff and E17's patch.diff (they touch different
# lines; E17 does not depend on E16), e.g. cat both into one file and build it
cat docs/research/experiments/E16-net-ordering/patch.diff \
  docs/research/experiments/E17-dense-escape/patch.diff > /tmp/e16e17.diff
scripts/experiments/build_variant.sh d4383b44 /tmp/e16e17.diff jars/e16e17.jar
python3 scripts/experiments/paired_campaign.py --out runs/E17 \
  --boards docs/research/experiments/E17-dense-escape/boards.txt \
  --variants docs/research/experiments/E17-dense-escape/variants.txt \
  --flags "-mt 1 -mp 25 --router.optimizer.enabled=false" --parallel 8
```

Fanout must stay enabled (the default) in every arm, because the pre-pass runs inside the fanout
stage. The job log shows one INFO line `Dense escape (experimental, pattern ...): ...` per run of
the fanout stage when the property is on.

## 9. Metrics and decision rule

Per board: reload-DRC unconnected items and violations, router unrouted, vias, wire length, CPU
seconds, and from the log the pre-pass counts (`escaped`, `pocketed`, `failed`). Adopt when:
reload-DRC violations never increase on any board; unconnected items never increase on any board;
total unconnected over the dense boards drops by at least 25% (or caniot drops by at least 2);
total CPU does not rise by more than 10%. Additionally check the control arm SES is byte-identical
to a head build (`cmp`).

## 10. Cost estimate

From the camp3 run1 timings at `-mp 25` (master column): the regression and open-connection set
is about 4,700 s of wall time per arm on one worker (Issue732-RoyalBlue54L-Feather and
Issue297-myboard hit the 20-minute safety stop; Issue508-DAC2020_bm01 takes about 475 s). Five
arms are therefore about 6.5 CPU-hours; with 8 workers about 1 hour of wall time.

## 11. Smoke-test results

Unit level (`DenseEscapeTest`, passes): on caniot, U1 classifies as a peripheral part with 16
pins on each side; the pre-pass escapes at least one pin, every via on the board afterwards has no
clearance violation, and a second load of the board gives the same counts and via count.

Routing smoke (pending; the shared machine's slot queue was full): caniot control / `dense` /
`dense_out` and bm02 `dense`, with the jar that holds both patches
(`SP/agent-exp-ordering/e16e17.jar`, E16 off). Command (one limiter slot, about 6 minutes):

```
SP=/tmp/claude-0/-home-user-freerouting/83886c4b-c5a5-51ea-8db6-8cee185b706d/scratchpad
$SP/bin/limited $SP/agent-exp-ordering/smoke_e17.sh > $SP/agent-exp-ordering/smoke_e17.log 2>&1
python3 $SP/agent-exp-ordering/summ.py $SP/agent-exp-ordering/runs
```

The comparison target is the caniot control of E16 (`fix9.jar`, same flags): router 28 unrouted,
reload DRC 19 unconnected, 0 clearance violations, 14 dangling warnings, 43 vias, 92 s wall.

## 12. Risks and follow-ups

- Vias placed early are protected fanout vias and cost more to rip up
  (`autoroute/maze/MazeRipupResolver.java:59-62`); a bad pattern can therefore cost completion
  elsewhere. The `out_only` arm measures this.
- Inside escapes need a free body interior on the pin layer; QFNs with an exposed pad and parts
  with thermal vias have none, and the classification must detect it (the overlap query in 4.3).
- The 0.65 mm threshold classifies SOIC-8 at 1.27 mm as not fine-pitch, which is intended; TSSOP
  at 0.65 mm is included.
- Follow-up: once the pre-pass exists, try it with `fanout.pin_sorting_order=surroundingsDensity`
  for the parts it does not handle, and measure the E16 `constrained_first` order on top.
