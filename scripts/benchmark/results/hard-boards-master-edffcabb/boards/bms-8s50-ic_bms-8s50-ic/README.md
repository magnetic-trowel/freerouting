# bms-8s50-ic_bms-8s50-ic

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 41 |
| Clearance violations (total) | 44 |
| of which router-introduced | 44 |
| of which pre-existing in input | 0 (unfixable: 44) |
| Worst violation depth | 1.1016845703125 µm |
| Quality score | 877.60046 |
| Wall clock | 85.5 min |
| Connections (total / still open) | 450 / 41 |
| Autorouter stop reason | stagnation after 20 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **fanout-retry**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 48 | 44 | 856.9 | stagnation | 12.3 min |
| direct-first | 46 | 44 | 862.8 | stagnation | 11.3 min |
| fanout-retry | 41 | 44 | 877.6 | stagnation | 12.0 min |
| short-escape | 42 | 44 | 874.6 | stagnation | 23.7 min |
| open-escape | 57 | 44 | 830.2 | stagnation | 12.1 min |
| no-fanout | 51 | 44 | 848.0 | stagnation | 13.9 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 41 | 0 |
| clearance | 0 | 0 |
| hole_clearance | 0 | 2 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 11 | 4 |

Open connections reported by KiCad:

- Track [/BAT+] on F.Cu, length 4.4350 mm @ (120.06, 106.56) ↔ Pad 1 [/BAT+] of D3 on F.Cu @ (113.00, 107.60)
- Track [/BAT+] on F.Cu, length 0.4986 mm @ (142.50, 84.80) ↔ Pad 47 [/BAT+] of U7 on F.Cu @ (145.55, 83.80)
- Track [GND] on F.Cu, length 1.2397 mm @ (141.10, 84.06) ↔ Track [GND] on F.Cu, length 0.4207 mm @ (144.10, 87.30)
- Pad 2 [GND] of C38 on F.Cu @ (147.30, 77.30) ↔ Pad 29 [GND] of U7 on F.Cu @ (149.10, 79.05)
- Pad 49 [GND] of U7 on F.Cu @ (148.50, 82.00) ↔ Pad 18 [GND] of U7 on F.Cu @ (151.45, 82.20)
- Pad 29 [GND] of U7 on F.Cu @ (149.10, 79.05) ↔ Pad 28 [GND] of U7 on F.Cu @ (149.50, 79.05)
- Track [GND] on F.Cu, length 6.8637 mm @ (157.20, 77.34) ↔ Pad 28 [GND] of U7 on F.Cu @ (149.50, 79.05)
- Pad 24 [GND] of U7 on F.Cu @ (151.45, 79.80) ↔ Pad 28 [GND] of U7 on F.Cu @ (149.50, 79.05)
- Pad 24 [GND] of U7 on F.Cu @ (151.45, 79.80) ↔ Pad 18 [GND] of U7 on F.Cu @ (151.45, 82.20)
- Pad 23 [GND] of U3 on F.Cu @ (180.29, 92.25) ↔ Pad 2 [GND] of C32 on F.Cu @ (178.60, 92.50)
- Pad 23 [GND] of U3 on F.Cu @ (180.29, 92.25) ↔ Pad 49 [GND] of U3 on F.Cu @ (183.73, 90.00)
- Track [GND] on F.Cu, length 1.1652 mm @ (182.12, 84.18) ↔ Track [GND] on F.Cu, length 0.8298 mm @ (182.98, 87.39)
- Pad 41 [/MCU/SPI1_MOSI] of U3 on F.Cu @ (187.17, 90.75) ↔ PTH pad 8 [/MCU/SPI1_MOSI] of J2 @ (199.04, 92.04)
- Pad 1 [/ISL94202/VC0] of C20 on F.Cu @ (160.10, 81.40) ↔ Pad 17 [/ISL94202/VC0] of U7 on F.Cu @ (151.45, 82.60)
- Pad 1 [/ISL94202/VC0] of R25 on F.Cu @ (163.80, 83.50) ↔ Pad 1 [/ISL94202/VC0] of C20 on F.Cu @ (160.10, 81.40)
- Pad 1 [/ISL94202/VC4] of C5 on F.Cu @ (158.20, 89.50) ↔ Pad 9 [/ISL94202/VC4] of U7 on F.Cu @ (149.50, 84.95)
- Pad 1 [/ISL94202/VC4] of R21 on F.Cu @ (159.80, 91.00) ↔ Pad 1 [/ISL94202/VC4] of C5 on F.Cu @ (158.20, 89.50)
- Pad 1 [/ISL94202/XT2] of RT2 on F.Cu @ (134.00, 84.20) ↔ Pad 21 [/ISL94202/XT2] of U7 on F.Cu @ (151.45, 81.00)
- Pad 1 [/ISL94202/XT2] of R40 on F.Cu @ (154.00, 79.80) ↔ Pad 21 [/ISL94202/XT2] of U7 on F.Cu @ (151.45, 81.00)
- Pad 2 [/ISL94202/XT2] of R26 on F.Cu @ (155.60, 79.80) ↔ Pad 1 [/ISL94202/XT2] of R40 on F.Cu @ (154.00, 79.80)
- Pad 2 [/ISL94202/PCFET] of R67 on F.Cu @ (138.10, 77.30) ↔ Pad 44 [/ISL94202/PCFET] of U7 on F.Cu @ (145.55, 82.60)
- Pad 1 [/ISL94202/DFET] of R68 on F.Cu @ (134.00, 77.20) ↔ Pad 42 [/ISL94202/DFET] of U7 on F.Cu @ (145.55, 81.80)
- Pad 26 [/MCU/I2C2_SDA] of U7 on F.Cu @ (150.30, 79.05) ↔ Track [/MCU/I2C2_SDA] on F.Cu, length 2.4042 mm @ (174.00, 78.30)
- Pad 1 [Net-(C41-Pad1)] of C41 on F.Cu @ (143.60, 80.50) ↔ Pad 41 [Net-(C41-Pad1)] of U7 on F.Cu @ (145.55, 81.40)
- Pad 2 [Net-(C41-Pad2)] of C41 on F.Cu @ (143.60, 78.90) ↔ Pad 40 [Net-(C41-Pad2)] of U7 on F.Cu @ (145.55, 81.00)
- Pad 1 [/ISL94202/VC7] of C2 on F.Cu @ (146.20, 89.50) ↔ Pad 1 [/ISL94202/VC7] of R18 on F.Cu @ (146.20, 91.00)
- Pad 3 [/ISL94202/VC7] of U7 on F.Cu @ (147.10, 84.95) ↔ Pad 1 [/ISL94202/VC7] of C2 on F.Cu @ (146.20, 89.50)
- Pad 4 [/ISL94202/CB7] of U7 on F.Cu @ (147.50, 84.95) ↔ Pad 1 [/ISL94202/CB7] of R46 on F.Cu @ (147.80, 94.00)
- Pad 6 [/ISL94202/CB6] of U7 on F.Cu @ (148.30, 84.95) ↔ Pad 1 [/ISL94202/CB6] of R47 on F.Cu @ (151.80, 94.00)
- Pad 8 [/ISL94202/CB5] of U7 on F.Cu @ (149.10, 84.95) ↔ Pad 1 [/ISL94202/CB5] of R48 on F.Cu @ (155.80, 94.00)
- Pad 16 [/ISL94202/CB1] of U7 on F.Cu @ (151.45, 83.00) ↔ Pad 1 [/ISL94202/CB1] of R52 on F.Cu @ (171.80, 94.00)
- Pad 2 [/ISL94202/NTC_BAT] of R30 on F.Cu @ (157.20, 79.80) ↔ Pad 20 [/ISL94202/NTC_BAT] of U7 on F.Cu @ (151.45, 81.40)
- Pad 1 [/ISL94202/NTC_BAT] of R29 on F.Cu @ (158.80, 79.80) ↔ Pad 2 [/ISL94202/NTC_BAT] of R30 on F.Cu @ (157.20, 79.80)
- PTH pad 8 [/ISL94202/NTC_BAT] of J1 @ (165.00, 110.00) ↔ Pad 1 [/ISL94202/NTC_BAT] of R29 on F.Cu @ (158.80, 79.80)
- Pad 34 [/ISL94202/ALERT] of U7 on F.Cu @ (147.10, 79.05) ↔ Pad 25 [/ISL94202/ALERT] of U3 on F.Cu @ (180.98, 93.44)
- Pad 2 [Net-(C42-Pad2)] of C42 on F.Cu @ (141.90, 78.90) ↔ Pad 39 [Net-(C42-Pad2)] of U7 on F.Cu @ (145.55, 80.60)
- Pad 1 [/ISL94202/VDD] of D7 on F.Cu @ (137.45, 103.55) ↔ Pad 1 [/ISL94202/VDD] of D5 on F.Cu @ (139.70, 87.50)
- Pad 1 [/ISL94202/VDD] of D5 on F.Cu @ (139.70, 87.50) ↔ Track [/ISL94202/VDD] on F.Cu, length 1.2000 mm @ (141.90, 80.50)
- Pad 43 [/ISL94202/VDD] of U7 on F.Cu @ (145.55, 82.20) ↔ Track [/ISL94202/VDD] on F.Cu, length 1.2000 mm @ (141.90, 80.50)
- Pad 4 [/MCU/CAN_RX] of U2 on F.Cu @ (177.34, 63.05) ↔ Pad 45 [/MCU/CAN_RX] of U3 on F.Cu @ (187.17, 88.75)
- … 1 more in `../../drc-summary.json`

## Board

- Source: https://github.com/LibreSolar/bms-8s50-ic (author LibreSolar, license CC-BY-SA-4.0)
- 2 layers, 66 nets, 168 components, 110.0 × 60.0 mm, 30 zones
- Human reference layout: 1439 segments, 255 vias, 2847.39 mm of track, 344 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/bms-8s50-ic_bms-8s50-ic/`](../../../../fixtures/PCBench/bms-8s50-ic_bms-8s50-ic/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 58 | None | 162 | 16.4 min | FAIL |
| 2.4.1 | COMPLETED | 54 | None | 128 | 15.2 min | FAIL |
| 2.5.0 | TIMED_OUT | 67 | 0 | 44 | 20.1 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 61 | 0 | 44 | 20.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 61 | 0 | 44 | 113.7 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 41 | 0 | 44 | 240.6 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 41 | 44 | 44 | 85.5 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `bms-8s50-ic_bms-8s50-ic--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
