# E10 Per-board adaptive parameters: can a board-feature rule pick better settings?

An analysis plan, with a feature extractor, a pre-registered hold-out split and a fitting script,
for testing whether a simple deterministic rule computed from the DSN (layer count, pin density,
net count, SMD share, area per pin, routing demand) can choose router parameters per board better
than the best single global default from E09.

## 1. Status

Designed; feature extractor run on all 120 fixtures (`features-fixtures.jsonl`); fitting script
tested on synthetic data only; no routing runs of its own yet (it reuses E09 stage 1 and needs one
campaign, below). Not yet run at scale.

## 2. Hypothesis

Freerouting already adapts two costs to the board (`RouterSettings.java:369-505`: the
against-direction cost grows with the aspect ratio, outer layers cost more on boards with more
than two signal layers), which shows the authors expected board-dependent optima. Hypotheses,
each tied to a mechanism, to be tested in the order given:

- H1 (pull-tight in physical units): `trace_pull_tight_accuracy` is a board-unit distance
  (`TraceTightener.java:123`), so the default 500 means 50 um on 108 of the 120 fixtures
  (KiCad, `resolution um 10`), 12.7 um on 7 EasyEDA boards (`resolution mil 1000`), 5 um on 3 boards
  (`mil 2540`) and 0.5 um on Issue676 (`mm 1000000`, clamped at 100 units = 0.1 um). The rule
  "accuracy = 50 um in board units" should cut pull-tight CPU on the fine-resolution boards
  (Issue684 spends 322 CPU s on 133 pins) without changing KiCad boards at all. Expected: CPU
  -10 % or more on those boards, completion and vias unchanged within noise.
- H2 (via cost and SMD share): on two-layer boards with a high pure-SMD net share, cheaper vias
  (or a lower `smd_net_via_cost_factor`) help completion; on through-hole boards dearer vias
  reduce vias at no completion cost.
- H3 (rip-up cost and demand): boards with a high routing demand ratio (airline length over
  track capacity, above about 0.1) benefit from a different `start_ripup_costs` than sparse boards.
- H4 (fanout and pin density): the fanout pre-pass pays off on dense fine-pitch SMD boards and
  only adds vias on sparse boards.

A rule is worth adopting only if it beats the best global default of E09 on hold-out boards.

## 3. Features (`dsn_features.py`, from the DSN only, before routing)

| Feature | Computation |
|---|---|
| `signal_layers`, `power_layers` | `(structure (layer N (type signal|power)))`; a `power` layer counts as power only if a `(plane ...)` lies on it, because Freerouting routes a plane-less power layer as a signal layer (the reader warns "It will be treated as a signal layer"; caniot-tiny-arm routed 87 of 484 wires on such a B.Cu in the E09 smoke). |
| `plane_scopes` | count of `(structure (plane NET ...))`; plane nets switch to plane routing and F of E09. |
| `nets`, `pins`, `connections`, `max_net_pins` | `(network (net NAME (pins ...)))`; connections = sum over nets of pins - 1. |
| `smd_pin_fraction` | pins whose padstack has shapes on exactly one named layer (no `signal` wildcard), from `(library (padstack ...))` and the image pin's padstack. |
| `pure_smd_net_fraction` | nets whose pins are all SMD: the nets where `smd_net_via_cost_factor` applies (`AutorouteControl.java:340-351`). |
| `board_area_mm2`, `aspect_ratio` | area of the largest `(boundary (path|polygon|rect ...))`, its bounding box ratio. |
| `area_per_pin_mm2`, `pins_per_cm2` | area / pins, pins / area. |
| `trace_width_mm`, `clearance_mm` | first `(rule (width ...))` and plain `(clearance ...)` (EasyEDA writes one `(rule (clear ...))` per entry). |
| `airline_total_mm`, `airline_max_mm` | per non-plane net, the Euclidean minimum spanning tree of its pin positions (component place, rotation and side applied to the image pin offset); total and largest. |
| `routing_demand_ratio` | `airline_total / (area * signal_layers / (width + clearance))`: demanded track length over the track length that fits at one pitch. |
| `resolution_per_unit`, `pull_tight_500_units_mm` | `(resolution unit N)`; the physical size of the default pull-tight accuracy. |
| `host_cad` | `(parser (host_cad ...))`, used for the source groups. |

Run: `python3 -I dsn_features.py fixtures/*.dsn > features.jsonl` (3.5 s for 120 fixtures).
Spot checks: bm02 34 nets / 68 pins / 2 signal layers / SMD 0.54; caniot 2 signal layers (B.Cu is
a plane-less `power` layer) / 286 pins / demand about 0.1. Known limitation: pins of padstacks with only a `signal`
wildcard shape count as through-hole; boards without pins (schematic-only fixtures) are skipped by
the split.

## 4. Mechanism and rule class

The rule must be computable inside Freerouting from the loaded board, deterministic, and simple
enough to explain in a settings doc: a threshold tree of depth at most 2 (at most 3 leaves) over
the pre-registered features above, each leaf choosing one of K candidate settings (K <= 4), or for
H1 a closed-form unit conversion. That is at most 2 thresholds + 3 leaf choices, which keeps the
number of fitted parameters small against about 70 fitting boards.

## 5. Change

Analysis only. If a rule is adopted, it becomes a code change in
`RouterSettings.applyBoardSpecificOptimizations` (where the existing aspect-ratio rule lives),
switchable with `-Dfreerouting.exp.board_rule=<name>` for the confirmation run, and it applies
only to fields the user did not set (a CLI or GUI value must keep winning, as the
`boardSpecificTraceCostsApplied` logic does for trace costs today).

## 6. Determinism and time-budget check

Features are pure functions of the DSN; thresholds are fixed constants after fitting. Nothing
depends on time or budget. The fitting script uses a fixed permutation seed (20261024).

## 7. Boards and validation (anti-overfitting)

- Corpus: every fixture with at least 10 pins (104 boards, `features-fixtures.jsonl`).
- Groups (`make_split.py`): design sources, with KiCad split by layer class and SMD share:
  kicad-2L-smd 35, kicad-2L-mix 34, kicad-ML 10, easyeda-mil 10, dac2020 10, kicad-1L 1, pcbench 2,
  other 2.
- Hold-out (`split.json`, pre-registered now): every fourth board by SHA-1 of its name within each
  group, 30 boards. They are not used for fitting, for choosing the candidate settings, or for
  choosing the rule class; they are evaluated once, at the end.
- Leakage guard: 10 of the 30 hold-out boards are also E09 boards (bm05, bm07, bm09,
  Issue157-TeamAdapt, Issue153, Issue209, Issue110-RelayModule, Issue102-Mars-64, Issue217,
  PCBench-Box0). When E09 results are used to pick E10's candidate settings, rerun
  `analyze_e09.py` on E09's `boards.txt` without these 10 boards.
- Cross-validation on the fitting boards: leave one group out (the rule is fitted on the other
  groups and scored on the left-out one), so a rule must transfer to a design source it has not
  seen.
- Permutation test: features shuffled across fitting boards 200 times and the rule refitted; the
  real in-sample gain must exceed the 95th percentile of the shuffled gains, otherwise the rule
  is fitting noise.
- Noise: routing is chaotic, so every (board, setting) is routed with no seed and with
  `item_order_seed` 1 and 2; the loss is the mean of the three. A per-board "best setting" whose
  margin is inside the seed spread is treated as a tie (the fitting then prefers the default).

## 8. Variants (the E10 campaign)

Stage 0 (H1, cheap, settings-only): the 11 corpus boards whose resolution is not 0.1 um, in three
resolution classes, each with `control` and `pt50um` (`trace_pull_tight_accuracy` = 50 um in board
units): `boards-h1-mil1000.txt` / `variants-h1-mil1000.txt` (7 EasyEDA boards, 1969 units),
`boards-h1-mil2540.txt` / `variants-h1-mil2540.txt` (3 boards, 5000 units),
`boards-h1-mm1e6.txt` / `variants-h1-mm1e6.txt` (Issue676, 50000 units). Accept H1 if CPU drops on
these boards and completion and violations are unchanged on every board; it then becomes a unit
fix (accuracy defined in micrometres), not a fitted rule.

Stage 1: K = 4 candidate settings: `control` plus the three best distinct rows of E09 stage 1
(ranked by `analyze_e09.py` without the hold-out boards), each with seeds none/1/2, on all 104
corpus boards: 4 x 3 = 12 runs per board.

## 9. How to run

```
cd docs/research/experiments/E10-per-board-adaptive
python3 -I dsn_features.py ../../../../fixtures/*.dsn > features.jsonl
python3 -I make_split.py features.jsonl > split.json   # must equal the committed split.json
# Stage 0, one campaign per resolution class
for c in mil1000 mil2540 mm1e6; do
  python3 ../../../../scripts/experiments/paired_campaign.py --out ../../../../logs/E10/h1-$c \
    --boards boards-h1-$c.txt --variants variants-h1-$c.txt --parallel 8 --timeout 00:20:00
done
# Stage 1: write variants-e10.txt from E09 (see section 8), boards-e10.txt = all corpus boards
python3 ../../../../scripts/experiments/paired_campaign.py --out ../../../../logs/E10/s1 \
  --boards boards-e10.txt --variants variants-e10.txt --parallel 8 --timeout 00:20:00
python3 -I fit_rule.py features.jsonl ../../../../logs/E10/s1/summary.jsonl split.json
```

`fit_rule.py` averages nothing itself: give it one record per (board, setting) with the seed
mean, or name the seeded arms `<setting>` / `<setting>@1` / `<setting>@2` and pre-average them
(a ten-line jq or Python step).

## 10. Metrics and decision rule

Loss per (board, setting): `1e6 * reload-DRC unconnected + 1e5 * reload-DRC violations +
100 * vias/vias(control) + 10 * length/length(control) + cpu/cpu(control)`, i.e. completion
first, then DRC, then vias, length and CPU.

Adopt a rule only if all hold:
1. Leave-one-group-out: the rule beats the best global setting in every group with at least 3
   boards (ties allowed for groups where the rule picks the global setting).
2. Permutation test passes (gain above the 95th percentile of shuffled features).
3. Hold-out (evaluated once): total loss below the best global setting, no hold-out board loses
   completion or gains violations against the control, and the gain is larger than the seed
   noise of the hold-out boards.
4. The rule has at most 3 leaves and every threshold has a mechanistic reading (section 2); a
   rule that splits on a feature with no mechanism is reported but not adopted.

## 11. Cost estimate

Stage 0: 11 boards x 2 arms; from `SP/camp3/run1`, Issue313 40 s and Issue684 322 s CPU; the two
Issue289 boards (807 and 989 pins) were not in camp3 and may take 20 minutes each: 1-3 CPU-hours. Stage 1: 104 boards x 12 runs. At the camp3 mean of about
60 CPU s per board (`-mp 25`, without the two timed-out boards), about 21 CPU-hours, plus the
large boards of the corpus that camp3 did not route (Issue289 FHT-VGA 989 pins, FHT-8086 807 pins):
budget 30 CPU-hours, or about 4-5 hours of wall time at 8 parallel jobs. Fitting: seconds (5 s on
the synthetic test with 200 permutations).

## 12. Smoke-test results

`dsn_features.py` on all 120 fixtures: 120 parsed, 0 errors, 3.5 s. `make_split.py`: 104 boards with
at least 10 pins, 30 in the hold-out. `fit_rule.py` on synthetic data (vias depend on the SMD share
by construction, plus noise): it recovered the planted split `smd_pin_fraction <= 0.64` with a
hold-out loss of 2465 against 2589 for the best global setting and 2447 for the oracle, and the
permutation test passed (gain 488 against a shuffled 95th percentile of 128). No routing runs were
needed for E10 itself; the routing smoke of the flags it uses is in E09.

## 13. Risks and follow-ups

- 70 fitting boards is small; the depth limit, the leave-one-group-out rule and the permutation
  test exist for that reason. Expect the most likely honest result to be "no rule beats the
  global default", except for H1, which is a units bug rather than a learned rule.
- The corpus is dominated by KiCad 2-layer hobby boards; multi-layer and BGA designs are rare
  (10 kicad-ML, 2 boards with 16 layers), so any rule about layer count rests on few boards.
- The features use the DSN as exported; KiCad omits copper-to-edge clearance (see the AGENTS.md
  notes) and marks some copper layers `power` without a plane, which Freerouting routes as signal.
- If H1 is accepted, re-run E09's factor E afterwards: its levels are in board units and mean
  different physical distances on non-KiCad boards.
