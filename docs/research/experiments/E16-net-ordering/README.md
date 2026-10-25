# E16 net ordering: deterministic alternative orders for the autoroute pass

Seven deterministic orders for the signal items of each autoroute pass (shortest airline first,
longest first, most constrained first, fine-pitch nets first, failed-last-pass first, learned
difficulty, and a plain net/id calibration order), selected with
`-Dfreerouting.exp.route_order=<name>`; unset means the router's own order, byte-identical to head.

## 1. Status

Designed; smoke-tested on Issue508-DAC2020_bm02 and Issue269-caniot-tiny-arm at `-mp 5`; not yet
run at scale.

## 2. Hypothesis

Connection order is a first-order lever on completion in rip-up-and-reroute: whatever routes
first takes the free space, and the ripup cost ramp (`ripup_costs = start * pass`) makes late
items pay for early choices. The current order is the board item-list order in pass 1 and
"nets incomplete after the previous pass first, then net number and item id" afterwards. Expected
effects, to be measured:

- `constrained_first` and `fine_pitch_first`: fewer open connections on boards whose leftovers end
  on dense parts (caniot, Issue157-TeamAdapt, Issue297-myboard, Issue732-CM5_MINIMA_3), because
  pins with few escape directions get their space before long nets cross their escape area.
  Target: at least 25% fewer reload-DRC unconnected items summed over the open-connection boards.
- `short_first`: fewer vias and shorter total length, near-neutral completion (classic
  heuristic: short connections have little choice, long ones can detour).
- `long_first`: the opposite control; expected worse completion, kept to bound the effect size.
- `failed_first`: close to the default from pass 2 on (the default already moves incomplete nets
  to the front); measures whether ordering by failure count adds anything.
- `learned`: fewer passes to converge on boards that oscillate (bm06, Issue313, Issue367), because
  nets that keep failing or keep getting ripped move to the front of the next pass while the
  board is emptiest. This is the order-level analogue of negotiated-congestion history costs.
- `net_id`: isolates the effect of replacing the pass-1 item-list order and the
  incomplete-first rule by a plain net/id sort; every other order uses this as its tie break.

## 3. Mechanism (code at d4383b44)

- Pass items come from `BatchAutorouter.getAutorouteItems`
  (`src/main/java/app/freerouting/autoroute/pipeline/BatchAutorouter.java:457-543`): one
  representative per connected group that is not yet connected to the whole net, in board item-list
  order, plane-net items first (line 540).
- `AutoroutePassRunner.runSingleThread` routes them in that order in pass 1 and calls
  `reorderSingleThreadItems` from pass 2 on
  (`src/main/java/app/freerouting/autoroute/pipeline/AutoroutePassRunner.java:66-68`).
- `reorderSingleThreadItems` (`AutoroutePassRunner.java:257-322`) keeps plane items first; with
  stagnation (2 passes without fewer incomplete connections) or an `item_order_seed` it applies a
  seeded shuffle (anti-oscillation, line 278-292); otherwise it puts items of nets incomplete after
  the previous pass first, each group sorted by first net number then item id (lines 293-317).
- Each item routes its group against the rest of its net (`AutorouteConnectionRouter`,
  `src/main/java/app/freerouting/autoroute/pipeline/AutorouteConnectionRouter.java:120-140`),
  with ripup allowed at `start_ripup_costs * pass` (line 113), so earlier items in a pass see a
  less occupied board and later items pay more to displace them.
- `ItemSelectionStrategy` (`src/main/java/app/freerouting/autoroute/ItemSelectionStrategy.java`)
  is the optimizer's order, not the router's; it is not touched.

## 4. Change

Code: `patch.diff` (applies with `git apply` on d4383b44).

- New `src/main/java/app/freerouting/autoroute/pipeline/RouteOrder.java`: reads
  `freerouting.exp.route_order` once into a `static final` mode; unknown values log a warning
  and fall back to the router's order. Orders (keys sort ascending; ties by first net number,
  then item id):
  - `net_id`: key 0 (pure net/id order).
  - `short_first` / `long_first`: squared airline from the item's connected group to the nearest
    item of the rest of the net (pins and vias by centre, traces by both end points), ascending
    / descending.
  - `constrained_first`: the largest local pin density of a pin in the item's group (number of
    other pin centres within `freerouting.exp.route_order.density_radius_um`, default 1500 um,
    computed with a uniform grid), descending.
  - `fine_pitch_first`: nets that touch a component whose pin pitch (smallest centre-to-centre
    distance) is at most `freerouting.exp.route_order.fine_pitch_um` (default 650 um) first,
    finest pitch first; other nets after.
  - `failed_first`: number of failed connection attempts of the item's net in the previous pass,
    descending.
  - `learned`: per-net difficulty, descending: each pass adds 8 per failed attempt, plus the
    number of items the net ripped, plus 1 to the net of every ripped item; the accumulated
    value halves at every pass end (integer arithmetic).
- `AutoroutePassRunner`: when a mode is set, the order replaces the pass-1 item-list order and the
  incomplete-first rule in every pass; the stagnation / `item_order_seed` shuffle is kept
  unchanged (the order is skipped in exactly the passes where the shuffle runs). Plane items stay
  first in their current order. The runner reports each attempt's outcome and ripped items to the
  order and closes the pass before the tail removal. With the property unset the field is null
  and every changed line is behind `routeOrder != null`, so the off arm runs head's code.
- New `src/test/java/app/freerouting/autoroute/pipeline/RouteOrderTest.java` (16 tests): parsing,
  key/net/id tie breaks, history and decay, grid density against brute force, pitch, and on the
  caniot board: every order is a permutation independent of the input order, plane items stay in
  front, and `fine_pitch_first` starts with a U1 (LQFP-64) net.

The history of `failed_first` and `learned` lives in the pass runner, so it starts empty in every
new pass loop (each routing fallback attempt and each optimizer candidate creates its own
`BatchAutorouter`).

## 5. Determinism and time-budget check

Deterministic: keys are computed from board geometry and from counts of the pass's own
outcomes; the sort is a total order (key, net, id); maps are `TreeMap` where iterated; the grid
`HashMap` is only used for lookups. Results do not depend on the input list order (unit test).
Time-budget agnostic: no key uses elapsed time, remaining budget or a timeout. The keys are
recomputed every pass (cost is in section 11). Note that the maze search's own per-connection
`TimeLimit` (`AutorouteConnectionRouter.java:142-144`) is unchanged head behaviour.

## 6. Boards

`boards.txt`:

- Smoke: bm02 (quick, clean), caniot (dense LQFP-64; all open connections end on U1).
- Open connections at head in `SP/camp3/run1/summary.jsonl` (router unrouted > 0 or reload-DRC
  unconnected > 0): Issue313, Issue157, Issue110-Pajalnaja, Issue297, bm04, bm05, Issue684,
  Issue039, Issue367, Issue093, Issue153, Issue054, Issue209-split05, Issue690, Issue957, Issue676.
  These should benefit.
- Dense: Issue732-CM5_MINIMA_3 (also Issue157, Issue297 above).
- Must not regress (clean at head and v2.5.0): bm06, bm07, bm09, bm10, bm11, Issue034, Issue155,
  Issue558, Issue733, Issue217, Issue107, Issue110-RelayModule, PCBench-Box0, Issue102, bm01.
- Issue732-RoyalBlue54L-Feather times out at head; kept last and reported separately.

## 7. Variants

`variants.txt`: `control` (E16 jar, property off), `head-unpatched` (a jar built from d4383b44
without the patch: its SES must equal `control` byte for byte), then one arm per order.

## 8. How to run

```
scripts/experiments/build_variant.sh d4383b44 - jars/head.jar
scripts/experiments/build_variant.sh d4383b44 docs/research/experiments/E16-net-ordering/patch.diff jars/e16.jar
python3 scripts/experiments/paired_campaign.py --out runs/E16 \
  --boards docs/research/experiments/E16-net-ordering/boards.txt \
  --variants docs/research/experiments/E16-net-ordering/variants.txt \
  --flags "-mt 1 -mp 25 --router.optimizer.enabled=false" --parallel 8 --timeout 00:20:00
```

(`head` in `variants.txt` is the harness's jar for the commit under test, here `jars/head.jar`.)
Check activation: a mistyped order name logs `Unknown value '...' for freerouting.exp.route_order`
at WARN and runs the control; with `--logging.console.level=DEBUG` every pass logs
`experimental route order '<name>' applied to N items`.

## 9. Metrics and decision rule

From `summary.jsonl`, per board and arm: reload-DRC unconnected items and violations, router
unrouted, vias, wire length, CPU seconds, passes. Comparisons exclude runs that hit the timeout.

- Hard gates for any arm: reload-DRC violations never higher than control on any board;
  reload-DRC unconnected never higher than control on any must-not-regress board; `control` and
  `head-unpatched` SES identical on every board.
- Adopt an order as the new default when, over the open-connection and dense boards, total
  reload-DRC unconnected drops by at least 20% against control, no board gets worse by more
  than 1 unconnected item, and total CPU rises by at most 15%. A tie on completion with at least
  5% fewer vias or 10% less CPU is a weaker adopt (as an opt-in setting first).
- Because single-board order effects are noisy (one different early decision changes the whole
  board), confirm a winner with `item_order_seed` 1..3 as extra arms before adopting.

## 10. Cost estimate

Key computation is small next to routing: the airline key does one connectivity walk per item
(the router does the same walk when it routes the item); density and pitch are O(pins) and
O(pins per component squared) per pass. The caniot smoke timings below show no measurable
overhead. Campaign cost at `-mp 25`, from camp3 run1 master wall times: about 4,700 s per arm
on one worker, including two 20-minute timeouts; 9 arms are about 12 CPU-hours, about 1.5 hours
of wall time with 8 workers.

## 11. Smoke-test results

Machine: shared 4-core VM, one routing JVM at a time per slot (`-Xmx1500m`), flags
`-mt 1 -mp 5 --router.optimizer.enabled=false --router.job_timeout=00:20:00`; reload DRC with
`fix9.jar` (`-de <dsn>+<ses> -drc`). `router` = the router's final "(N unrouted and M
violations)"; `drc unc` = reload-DRC `unconnectedItems`; `drc clr/dang` = reload-DRC violations
that are clearance-type / dangling-track-or-via warnings.

Issue269-caniot-tiny-arm (fanout on, 214 SMD pins, 180 escaped by the fanout stage):

| arm | router | drc unc | drc clr/dang | vias | wall s | CPU s | SES sha1 |
|---|---|---|---|---|---|---|---|
| head (`fix9.jar`) | 28 / 0 | 19 | 0 / 14 | 43 | 92.6 | 72.7 | 3f5775ece4 |
| control (E16 jar, property off) | 28 / 0 | 19 | 0 / 14 | 43 | 92.4 | 74.9 | 3f5775ece4 |
| net_id | 24 / 0 | 21 | 0 / 17 | 49 | 95.0 | 73.5 | b7a7fa8929 |
| short_first | 24 / 0 | 20 | 0 / 13 | 50 | 101.4 | 84.7 | a6e4ad73cb |

Pending (not run before the report; the shared machine's slot queue was full): caniot arms
long_first, constrained_first, fine_pitch_first, failed_first, learned, and all bm02 arms. Command
(one limiter slot, about 10 minutes):

```
SP=/tmp/claude-0/-home-user-freerouting/83886c4b-c5a5-51ea-8db6-8cee185b706d/scratchpad
$SP/bin/limited $SP/agent-exp-ordering/slotA.sh > $SP/agent-exp-ordering/slotA.log 2>&1
python3 $SP/agent-exp-ordering/summ.py $SP/agent-exp-ordering/runs
```

Findings so far: the property-off arm is byte-identical to head (same SES sha1), and each order
is active (different SES). No arm adds a clearance violation. On caniot at 5 passes the
orders lower the router's own unrouted count (28 to 24) but not the reload-DRC unconnected count
(19 to 20-21) and add 6-7 vias; two arms on one board at 5 passes are no evidence either way,
which is why the decision rule needs the full campaign at 25 passes. Wall and CPU include
JVM start and the fanout stage (about 21 s on caniot); the order computation itself is not
visible at this resolution.

## 12. Risks and follow-ups

- Order effects are chaotic per board; a campaign of 35 boards can show wins and losses that
  are noise. The decision rule therefore asks for seed confirmation.
- `constrained_first` and `fine_pitch_first` move many items of a dense part to the front of
  every pass; with the ripup ramp that can make those nets harder to displace and cost
  completion elsewhere. The `long_first` arm bounds the effect size.
- At this commit every autoroute pass goes through `AutoroutePassRunner.runSingleThread`
  (`BatchAutorouter.java:564`), so the order applies to all passes, including the passes of the
  routing fallback attempts (`RoutingFallbackRunner`) and of the optimizer's nested routers. A
  pass loop that stops at `max_passes` does not start the fallback chain
  (`RoutingFallback.isRetryable`, `RoutingFallback.java:166-171`), so at `-mp 5` the smoke runs
  measure the default loop only; at `-mp 25` a stalled loop runs fallback attempts, each with
  a fresh order history.
- Follow-up: combine the best order with E17 (dense-escape pre-pass); make the order a proper
  `router.item_order` setting if an arm wins.
