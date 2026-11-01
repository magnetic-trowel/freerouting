# BLDC-controller_BLDC_controller

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 2 |
| Clearance violations (total) | 1 |
| of which router-introduced | 0 |
| of which pre-existing in input | 1 (unfixable: 1) |
| Worst violation depth | 9.9029541015625 µm |
| Quality score | 993.9256 |
| Wall clock | 3.5 min |
| Connections (total / still open) | 440 / 2 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **direct-first**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 990.9 | stagnation | 0.7 min |
| continue | 3 | 0 | 990.9 | stagnation | 0.3 min |
| direct-first | 2 | 0 | 993.9 | stagnation | 0.3 min |
| fanout-retry | 3 | 0 | 990.9 | stagnation | 0.4 min |
| short-escape | 4 | 0 | 987.9 | stagnation | 0.5 min |
| open-escape | 3 | 0 | 990.9 | stagnation | 0.4 min |
| no-fanout | 7 | 0 | 978.8 | stagnation | 0.7 min |
| continue | 2 | 0 | 993.9 | stagnation | 0.1 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 122 | 0 |
| clearance | 0 | 5 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 10 | 13 |

Open connections reported by KiCad:

- Track [/OUTU] on F.Cu, length 0.9017 mm @ (133.42, 75.07) ↔ Pad 5 [/OUTU] of Q2 on B.Cu @ (133.49, 75.07)
- PTH pad 1 [/OUTU] of REF**_17 @ (135.34, 74.75) ↔ PTH pad 1 [/OUTU] of REF**_18 @ (135.34, 75.55)
- PTH pad 1 [/OUTU] of REF**_18 @ (135.34, 75.55) ↔ PTH pad 1 [/OUTU] of REF**_14 @ (136.14, 75.55)
- PTH pad 1 [/OUTU] of REF**_69 @ (135.34, 76.35) ↔ PTH pad 1 [/OUTU] of REF**_13 @ (136.14, 76.35)
- PTH pad 1 [/OUTU] of REF**_69 @ (135.34, 76.35) ↔ PTH pad 1 [/OUTU] of REF**_42 @ (135.34, 77.15)
- PTH pad 1 [/OUTU] of REF**_42 @ (135.34, 77.15) ↔ PTH pad 1 [/OUTU] of REF**_3 @ (136.14, 77.15)
- PTH pad 1 [/OUTU] of REF** @ (135.34, 77.95) ↔ Track [/OUTU] on In2.Cu, length 9.1956 mm @ (125.84, 78.44)
- PTH pad 1 [/OUTU] of REF**_41 @ (135.34, 78.75) ↔ PTH pad 1 [/OUTU] of REF**_5 @ (136.14, 78.75)
- PTH pad 1 [/OUTU] of REF**_40 @ (135.34, 79.55) ↔ PTH pad 1 [/OUTU] of REF**_41 @ (135.34, 78.75)
- PTH pad 1 [/OUTU] of REF**_9 @ (136.14, 74.75) ↔ PTH pad 1 [/OUTU] of REF**_8 @ (136.94, 74.75)
- PTH pad 1 [/OUTU] of REF**_4 @ (136.14, 77.95) ↔ PTH pad 1 [/OUTU] of REF**_5 @ (136.14, 78.75)
- PTH pad 1 [/OUTU] of REF**_5 @ (136.14, 78.75) ↔ PTH pad 1 [/OUTU] of REF**_79 @ (136.94, 78.75)
- PTH pad 1 [/OUTU] of REF**_2 @ (136.14, 79.55) ↔ PTH pad 1 [/OUTU] of REF**_5 @ (136.14, 78.75)
- PTH pad 1 [/OUTU] of REF**_11 @ (136.94, 75.55) ↔ PTH pad 1 [/OUTU] of REF**_6 @ (137.74, 75.55)
- PTH pad 1 [/OUTU] of REF**_66 @ (136.94, 76.35) ↔ PTH pad 1 [/OUTU] of REF**_13 @ (136.14, 76.35)
- PTH pad 1 [/OUTU] of REF**_12 @ (136.94, 77.15) ↔ PTH pad 1 [/OUTU] of REF**_66 @ (136.94, 76.35)
- PTH pad 1 [/OUTU] of REF**_12 @ (136.94, 77.15) ↔ PTH pad 1 [/OUTU] of REF**_68 @ (136.94, 77.95)
- PTH pad 1 [/OUTU] of REF**_78 @ (136.94, 79.55) ↔ PTH pad 1 [/OUTU] of REF**_2 @ (136.14, 79.55)
- PTH pad 1 [/OUTU] of REF**_10 @ (137.74, 74.75) ↔ PTH pad 1 [/OUTU] of REF**_8 @ (136.94, 74.75)
- PTH pad 1 [/OUTU] of REF**_6 @ (137.74, 75.55) ↔ PTH pad 1 [/OUTU] of REF**_10 @ (137.74, 74.75)
- PTH pad 1 [/OUTU] of REF**_6 @ (137.74, 75.55) ↔ PTH pad 1 [/OUTU] of REF**_65 @ (137.74, 76.35)
- PTH pad 1 [/OUTU] of REF**_64 @ (137.74, 77.15) ↔ PTH pad 1 [/OUTU] of REF**_12 @ (136.94, 77.15)
- PTH pad 1 [/OUTU] of REF**_67 @ (137.74, 77.95) ↔ PTH pad 1 [/OUTU] of REF**_64 @ (137.74, 77.15)
- PTH pad 1 [/OUTU] of REF**_67 @ (137.74, 77.95) ↔ PTH pad 1 [/OUTU] of REF**_81 @ (137.74, 78.75)
- PTH pad 1 [/OUTU] of REF**_81 @ (137.74, 78.75) ↔ PTH pad 1 [/OUTU] of REF**_79 @ (136.94, 78.75)
- PTH pad 1 [/OUTU] of REF**_80 @ (137.74, 79.55) ↔ PTH pad 1 [/OUTU] of REF**_81 @ (137.74, 78.75)
- Track [GND] on In2.Cu, length 0.7459 mm @ (118.59, 88.05) ↔ Track [GND] on In1.Cu, length 2.7854 mm @ (119.39, 88.05)
- Track [GND] on In1.Cu, length 9.1050 mm @ (118.59, 97.95) ↔ PTH pad 1 [GND] of REF**_125 @ (119.39, 88.85)
- Track [GND] on In1.Cu, length 9.1050 mm @ (118.59, 97.95) ↔ Track [GND] on In2.Cu, length 0.7459 mm @ (118.59, 88.05)
- PTH pad 1 [GND] of REF**_106 @ (118.84, 99.00) ↔ Track [GND] on F.Cu, length 2.7473 mm @ (116.90, 96.26)
- PTH pad 1 [GND] of REF**_106 @ (118.84, 99.00) ↔ PTH pad 1 [GND] of REF**_39 @ (119.64, 99.00)
- PTH pad 1 [GND] of REF**_22 @ (119.19, 69.20) ↔ PTH pad 1 [GND] of REF**_27 @ (119.99, 69.20)
- PTH pad 1 [GND] of REF**_37 @ (119.19, 70.80) ↔ PTH pad 1 [GND] of REF**_23 @ (119.19, 70.00)
- Track [GND] on In2.Cu, length 8.7700 mm @ (119.19, 71.60) ↔ PTH pad 1 [GND] of REF**_37 @ (119.19, 70.80)
- PTH pad 1 [GND] of REF**_105 @ (119.64, 98.20) ↔ PTH pad 1 [GND] of REF**_108 @ (120.44, 98.20)
- PTH pad 1 [GND] of REF**_105 @ (119.64, 98.20) ↔ PTH pad 1 [GND] of REF**_39 @ (119.64, 99.00)
- PTH pad 1 [GND] of REF**_27 @ (119.99, 69.20) ↔ PTH pad 1 [GND] of REF**_25 @ (119.99, 70.00)
- PTH pad 1 [GND] of REF**_25 @ (119.99, 70.00) ↔ PTH pad 1 [GND] of REF**_23 @ (119.19, 70.00)
- PTH pad 1 [GND] of REF**_36 @ (119.99, 70.80) ↔ PTH pad 1 [GND] of REF**_34 @ (119.99, 71.60)
- PTH pad 1 [GND] of REF**_36 @ (119.99, 70.80) ↔ PTH pad 1 [GND] of REF**_37 @ (119.19, 70.80)
- … 82 more in `../../drc-summary.json`

## Board

- Source: https://github.com/WarmT/BLDC-controller (author WarmT, license None)
- 4 layers, 20 nets, 262 components, 49.0 × 32.0 mm, 38 zones
- Human reference layout: 929 segments, 75 vias, 1006.41 mm of track, 570 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/BLDC-controller_BLDC_controller/`](../../../../fixtures/PCBench/BLDC-controller_BLDC_controller/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 19 | None | 967 | 4.6 min | FAIL |
| 2.4.1 | TIMED_OUT | 18 | None | 1007 | 15.1 min | FAIL |
| 2.5.0 | COMPLETED | 6 | 0 | 1 | 4.2 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 2 | 0 | 1 | 10.8 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 2 | 0 | 1 | 10.1 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 1 | 23.1 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 2 | 0 | 1 | 3.5 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `BLDC-controller_BLDC_controller--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
