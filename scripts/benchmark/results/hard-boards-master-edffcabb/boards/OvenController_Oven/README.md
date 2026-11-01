# OvenController_Oven

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 17 |
| Clearance violations (total) | 60 |
| of which router-introduced | 0 |
| of which pre-existing in input | 60 (unfixable: 60) |
| Worst violation depth | 2059.9365234375 µm |
| Quality score | 824.0014 |
| Wall clock | 0.5 min |
| Connections (total / still open) | 170 / 17 |
| Autorouter stop reason | stagnation after 25 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 17 | 0 | 824.0 | stagnation | 0.1 min |
| direct-first | 17 | 0 | 824.0 | stagnation | 0.1 min |
| fanout-retry | 104 | 0 | 216.2 | repeated | 0.0 min |
| short-escape | 17 | 0 | 824.0 | stagnation | 0.1 min |
| open-escape | 17 | 0 | 824.0 | stagnation | 0.1 min |
| no-fanout | 17 | 0 | 824.0 | stagnation | 0.1 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 17 | 0 |
| clearance | 46 | 76 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 3 |

Open connections reported by KiCad:

- PTH pad 1 [/PWR+] of RC4 @ (102.88, 104.90) ↔ PTH pad 1 [/PWR+] of R17 @ (105.92, 110.24)
- PTH pad 1 [/PWR+] of RC4 @ (102.88, 104.90) ↔ PTH pad 1 [/PWR+] of R11 @ (105.92, 91.19)
- Track [/PWR+] on B.Cu, length 10.6780 mm @ (105.92, 53.09) ↔ PTH pad 2 [/PWR+] of U1 @ (119.38, 53.34)
- PTH pad 1 [/PWR+] of R11 @ (105.92, 91.19) ↔ PTH pad 2 [/PWR+] of U6 @ (119.38, 91.44)
- Track [/PWR+] on B.Cu, length 6.1066 mm @ (110.24, 114.55) ↔ PTH pad 2 [/PWR+] of U9 @ (119.38, 110.49)
- PTH pad 2 [/PWR+] of U3 @ (119.38, 72.39) ↔ PTH pad 1 [/PWR+] of R7 @ (105.92, 72.14)
- PTH pad 2 [/PWR+] of P20 @ (136.65, 120.52) ↔ PTH pad 2 [/PWR+] of U9 @ (119.38, 110.49)
- PTH pad 2 [Net-(R1-Pad2)] of R1 @ (95.76, 53.09) ↔ PTH pad 6 [Net-(R1-Pad2)] of U2 @ (95.76, 57.40)
- PTH pad 2 [Net-(R7-Pad2)] of R7 @ (95.76, 72.14) ↔ PTH pad 6 [Net-(R7-Pad2)] of U4 @ (95.76, 76.45)
- PTH pad 2 [Net-(R11-Pad2)] of R11 @ (95.76, 91.19) ↔ PTH pad 6 [Net-(R11-Pad2)] of U7 @ (95.76, 95.50)
- PTH pad 2 [Net-(R17-Pad2)] of R17 @ (95.76, 110.24) ↔ PTH pad 6 [Net-(R17-Pad2)] of U10 @ (95.76, 114.55)
- PTH pad 4 [Net-(U1-Pad3)] of U2 @ (95.76, 62.48) ↔ PTH pad 3 [Net-(U1-Pad3)] of U1 @ (119.38, 58.93)
- PTH pad 4 [Net-(U3-Pad3)] of U4 @ (95.76, 81.53) ↔ PTH pad 3 [Net-(U3-Pad3)] of U3 @ (119.38, 77.98)
- PTH pad 4 [Net-(U6-Pad3)] of U7 @ (95.76, 100.58) ↔ PTH pad 3 [Net-(U6-Pad3)] of U6 @ (119.38, 97.03)
- PTH pad 1 [/PWR-] of U8 @ (95.76, 133.10) ↔ PTH pad 2 [/PWR-] of P13 @ (125.86, 130.56)
- PTH pad 2 [Net-(R19-Pad1)] of U8 @ (95.76, 130.56) ↔ PTH pad 1 [Net-(R19-Pad1)] of R19 @ (103.64, 130.56)
- PTH pad 4 [Net-(U10-Pad4)] of U10 @ (95.76, 119.63) ↔ PTH pad 3 [Net-(U10-Pad4)] of U9 @ (119.38, 116.08)

## Board

- Source: https://github.com/SteelLiras/OvenController (author SteelLiras, license MIT)
- 2 layers, 31 nets, 83 components, 100.08 × 100.08 mm, 7 zones
- Human reference layout: 488 segments, 1 vias, 2041.17 mm of track, 298 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/OvenController_Oven/`](../../../../fixtures/PCBench/OvenController_Oven/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 28 | None | 96 | 0.3 min | FAIL |
| 2.4.1 | COMPLETED | 28 | None | 76 | 2.7 min | FAIL |
| 2.5.0 | COMPLETED | 22 | 0 | 60 | 1.3 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 18 | 0 | 60 | 3.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 18 | 0 | 60 | 2.5 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 16 | 0 | 60 | 1.6 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 17 | 0 | 60 | 2.1 min | FAIL |
| master-edffcabb | COMPLETED | 17 | 0 | 60 | 0.5 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `OvenController_Oven--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
