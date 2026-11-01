# bishboria_ErgoDox

Tier **C** (complex). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 10 |
| of which router-introduced | 0 |
| of which pre-existing in input | 10 (unfixable: 10) |
| Worst violation depth | 76.2 µm |
| Quality score | 999.7732 |
| Wall clock | 3.1 min |
| Connections (total / still open) | 360 / 0 |
| Autorouter stop reason | completed after 4 passes |
| Optimizer | no_improvement |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 84 | 0 |
| clearance | 273 | 478 |
| hole_clearance | 12 | 113 |
| shorting_items | 199 | 199 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 28 | 35 |

Open connections reported by KiCad:

- PTH pad 0 [GND] of SW0:7 @ (30.09, 145.42) ↔ PTH pad 0 [GND] of SW0:7 @ (39.30, 141.12)
- PTH pad 0 [GND] of SW0:7 @ (30.09, 145.42) ↔ PTH pad 0 [GND] of SW0:9 @ (38.14, 162.68)
- PTH pad 0 [GND] of SW0:9 @ (47.35, 158.39) ↔ PTH pad 0 [GND] of SW0:9 @ (38.14, 162.68)
- PTH pad 0 [GND] of SW0:8 @ (47.36, 137.37) ↔ PTH pad 0 [GND] of SW0:7 @ (39.30, 141.12)
- PTH pad 0 [GND] of SW0:8 @ (47.36, 137.37) ↔ PTH pad 0 [GND] of SW0:8 @ (56.57, 133.07)
- PTH pad 0 [GND] of SW0:12 @ (55.40, 175.65) ↔ PTH pad 0 [GND] of SW0:12 @ (46.20, 179.95)
- PTH pad 0 [GND] of SX1:7 @ (55.41, 154.63) ↔ PTH pad 0 [GND] of SW0:11 @ (61.89, 156.51)
- PTH pad 0 [GND] of SX1:7 @ (55.41, 154.63) ↔ PTH pad 0 [GND] of SW0:9 @ (47.35, 158.39)
- PTH pad 0 [GND] of SX0:11 @ (63.46, 171.89) ↔ PTH pad 0 [GND] of SW0:12 @ (55.40, 175.65)
- PTH pad 0 [GND] of SX1:7 @ (64.62, 150.34) ↔ PTH pad 0 [GND] of SW0:11 @ (61.89, 156.51)
- PTH pad 0 [GND] of SW0:11 @ (66.19, 165.72) ↔ PTH pad 0 [GND] of SX0:11 @ (63.46, 171.89)
- PTH pad 0 [GND] of SW0:11 @ (66.19, 165.72) ↔ PTH pad 0 [GND] of SX0:11 @ (72.67, 167.60)
- PTH pad 0 [GND] of SW5:7 @ (67.31, 65.10) ↔ Track [GND] on Back, length 2.5766 mm @ (75.13, 64.01)
- PTH pad 0 [GND] of SW4:7 @ (72.39, 83.83) ↔ Track [GND] on Back, length 4.1149 mm @ (75.89, 81.94)
- PTH pad 0 [GND] of SW2:7 @ (72.39, 112.41) ↔ Track [GND] on Back, length 25.2813 mm @ (75.04, 115.36)
- PTH pad 0 [GND] of SW2:7 @ (72.39, 122.56) ↔ PTH pad 0 [GND] of SW0:8 @ (56.57, 133.07)
- PTH pad 0 [GND] of SX1:8 @ (72.68, 146.58) ↔ PTH pad 0 [GND] of SW0:10 @ (79.16, 148.46)
- PTH pad 0 [GND] of SX1:8 @ (72.68, 146.58) ↔ PTH pad 0 [GND] of SX1:7 @ (64.62, 150.34)
- Track [GND] on Back, length 25.2813 mm @ (75.04, 115.36) ↔ PTH pad 0 [GND] of SW4:7 @ (72.39, 93.99)
- Track [GND] on Front, length 1.2155 mm @ (76.19, 116.22) ↔ PTH pad 0 [GND] of SW2:7 @ (72.39, 122.56)
- PTH pad 0 [GND] of SW5:7 @ (77.47, 65.10) ↔ Track [GND] on Back, length 3.4163 mm @ (75.13, 67.43)
- PTH pad 0 [GND] of SW5:7 @ (77.47, 65.10) ↔ PTH pad 0 [GND] of SW5:8 @ (86.36, 65.10)
- Track [GND] on Front, length 1.2844 mm @ (77.47, 116.22) ↔ PTH pad 0 [GND] of SW2:8 @ (86.36, 122.25)
- PTH pad 0 [GND] of SW0:10 @ (79.16, 148.46) ↔ PTH pad 0 [GND] of SX1:8 @ (81.88, 142.29)
- PTH pad 0 [GND] of SW0:10 @ (79.16, 148.46) ↔ PTH pad 0 [GND] of SW0:10 @ (83.45, 157.67)
- PTH pad 0 [GND] of SX0:10 @ (80.73, 163.85) ↔ PTH pad 0 [GND] of SX0:11 @ (72.67, 167.60)
- PTH pad 0 [GND] of SW0:10 @ (83.45, 157.67) ↔ PTH pad 0 [GND] of SX0:10 @ (80.73, 163.85)
- PTH pad 0 [GND] of SW5:8 @ (86.36, 65.10) ↔ PTH pad 0 [GND] of SW5:8 @ (96.52, 65.10)
- PTH pad 0 [GND] of SW4:8 @ (86.36, 84.15) ↔ Track [GND] on Back, length 3.4422 mm @ (75.04, 90.07)
- PTH pad 0 [GND] of SW3:8 @ (86.36, 103.20) ↔ Track [GND] on Front, length 1.2844 mm @ (77.47, 116.22)
- PTH pad 0 [GND] of SW2:8 @ (86.36, 122.25) ↔ PTH pad 0 [GND] of SW2:8 @ (96.52, 122.25)
- PTH pad 0 [GND] of SX0:10 @ (89.94, 159.55) ↔ PTH pad 0 [GND] of SW0:10 @ (83.45, 157.67)
- PTH pad 0 [GND] of SW4:8 @ (96.52, 84.15) ↔ PTH pad 0 [GND] of SW4:8 @ (86.36, 84.15)
- PTH pad 0 [GND] of SW3:8 @ (96.52, 103.20) ↔ PTH pad 0 [GND] of SW3:8 @ (86.36, 103.20)
- PTH pad 0 [GND] of SW2:8 @ (96.52, 122.25) ↔ PTH pad 0 [GND] of SW2:9 @ (105.41, 120.98)
- PTH pad 0 [GND] of SW5:9 @ (105.41, 63.83) ↔ PTH pad 0 [GND] of SW5:8 @ (96.52, 65.10)
- PTH pad 0 [GND] of SW4:9 @ (105.41, 82.88) ↔ PTH pad 0 [GND] of SW4:8 @ (96.52, 84.15)
- PTH pad 0 [GND] of SW4:9 @ (105.41, 82.88) ↔ PTH pad 0 [GND] of SW4:9 @ (115.57, 82.88)
- PTH pad 0 [GND] of SW3:9 @ (105.41, 101.93) ↔ PTH pad 0 [GND] of SW3:8 @ (96.52, 103.20)
- PTH pad 0 [GND] of SW5:9 @ (115.57, 63.83) ↔ PTH pad 0 [GND] of SW5:10 @ (124.46, 62.23)
- … 44 more in `../../drc-summary.json`

## Board

- Source: https://github.com/bishboria/ErgoDox (author , license )
- 2 layers, 0 nets, 65 components, 182.84 × 158.87 mm, 2 zones
- Human reference layout: 1174 segments, 6 vias, 5338.3 mm of track, 1395 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/bishboria_ErgoDox/`](../../../../fixtures/PCBench/bishboria_ErgoDox/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 19 | None | 34 | 13.5 min | FAIL |
| 2.4.1 | COMPLETED | 24 | None | 36 | 15.1 min | FAIL |
| 2.5.0 | COMPLETED | 36 | 0 | 20 | 13.0 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 21 | 0 | 10 | 20.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 21 | 0 | 10 | 63.3 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 16 | 0 | 10 | 78.0 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 0 | 0 | 10 | 3.1 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `bishboria_ErgoDox--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
