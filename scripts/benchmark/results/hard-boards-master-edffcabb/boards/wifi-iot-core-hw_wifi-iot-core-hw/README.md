# wifi-iot-core-hw_wifi-iot-core-hw

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 21 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 797.10144 |
| Wall clock | 7.3 min |
| Connections (total / still open) | 138 / 21 |
| Autorouter stop reason | stagnation after 30 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **fanout-retry**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 25 | 0 | 758.5 | stagnation | 1.0 min |
| direct-first | 22 | 1 | 787.4 | stagnation | 0.9 min |
| fanout-retry | 21 | 0 | 797.1 | stagnation | 1.7 min |
| short-escape | 25 | 0 | 758.5 | stagnation | 1.3 min |
| open-escape | 23 | 0 | 777.8 | stagnation | 1.3 min |
| no-fanout | 25 | 0 | 758.5 | stagnation | 1.1 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 21 | 0 |
| clearance | 47 | 162 |
| hole_clearance | 2 | 2 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 6 | 9 |

Open connections reported by KiCad:

- Track [+3V3] on F.Cu, length 1.2396 mm @ (135.03, 89.40) ↔ Pad 9 [+3V3] of U2 on F.Cu @ (138.22, 89.37)
- Track [+3V3] on F.Cu, length 1.0868 mm @ (144.84, 85.77) ↔ Pad 2 [+3V3] of R9 on F.Cu @ (147.11, 81.64)
- Track [GNDREF] on F.Cu, length 0.6481 mm @ (136.16, 105.70) ↔ Pad 2 [GNDREF] of C20 on F.Cu @ (137.10, 108.24)
- Pad 5 [GNDREF] of P8 on F.Cu @ (143.54, 107.94) ↔ Pad 4 [GNDREF] of P8 on F.Cu @ (142.89, 107.94)
- Pad 2 [Net-(C5-Pad1)] of Y1 on F.Cu @ (135.12, 81.61) ↔ Pad 1 [Net-(C5-Pad1)] of C5 on F.Cu @ (133.45, 82.60)
- Pad 6 [Net-(C5-Pad1)] of U2 on F.Cu @ (139.28, 88.31) ↔ Pad 2 [Net-(C5-Pad1)] of Y1 on F.Cu @ (135.12, 81.61)
- Track [Net-(C7-Pad1)] on F.Cu, length 0.1563 mm @ (140.53, 79.04) ↔ Pad 4 [Net-(C7-Pad1)] of U2 on F.Cu @ (139.98, 87.61)
- Track [/RST] on F.Cu, length 0.6344 mm @ (134.09, 87.92) ↔ Pad 1 [/RST] of RST on F.Cu @ (132.63, 110.62)
- Pad 3 [+5V] of U3 on F.Cu @ (134.31, 104.87) ↔ Track [+5V] on F.Cu, length 0.7707 mm @ (132.40, 104.87)
- Pad 1 [+5V] of C13 on F.Cu @ (135.42, 107.44) ↔ Pad 3 [+5V] of U3 on F.Cu @ (134.31, 104.87)
- Pad 1 [+5V] of C20 on F.Cu @ (137.10, 106.64) ↔ Pad 1 [+5V] of C13 on F.Cu @ (135.42, 107.44)
- Pad 1 [+5V] of P8 on F.Cu @ (140.94, 107.94) ↔ Pad 1 [+5V] of C20 on F.Cu @ (137.10, 106.64)
- PTH pad 13 [PA5] of P1 @ (128.27, 89.66) ↔ Pad 15 [PA5] of U2 on F.Cu @ (137.86, 93.41)
- PTH pad 17 [PB10] of P1 @ (128.27, 99.82) ↔ Pad 21 [PB10] of U2 on F.Cu @ (139.98, 95.53)
- Pad 30 [/ESP_RXD] of U2 on F.Cu @ (145.08, 94.82) ↔ PTH pad 4 [/ESP_RXD] of P2 @ (156.21, 66.80)
- Pad 45 [PB8] of U2 on F.Cu @ (144.37, 87.61) ↔ PTH pad 7 [PB8] of P2 @ (156.21, 74.42)
- Pad 39 [PB3] of U2 on F.Cu @ (146.49, 89.73) ↔ PTH pad 12 [PB3] of P2 @ (156.21, 87.12)
- Pad 29 [PA8] of U2 on F.Cu @ (144.72, 95.17) ↔ PTH pad 13 [PA8] of P2 @ (156.21, 89.66)
- Pad 44 [Net-(R1-Pad1)] of U2 on F.Cu @ (144.72, 87.96) ↔ Pad 1 [Net-(R1-Pad1)] of R1 on F.Cu @ (151.64, 77.70)
- Pad 19 [/CH_PD] of U2 on F.Cu @ (139.28, 94.82) ↔ Pad 3 [/CH_PD] of U1 on F.Cu @ (134.24, 65.21)
- Pad 1 [/USB_DP] of R11 on F.Cu @ (143.89, 103.85) ↔ Pad 33 [/USB_DP] of U2 on F.Cu @ (146.14, 93.76)

## Board

- Source: https://github.com/genuine-engineering/wifi-iot-core-hw (author genuine-engineering, license None)
- 2 layers, 15 nets, 52 components, 31.75 × 58.42 mm, 2 zones
- Human reference layout: 656 segments, 42 vias, 1371.49 mm of track, 612 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/wifi-iot-core-hw_wifi-iot-core-hw/`](../../../../fixtures/PCBench/wifi-iot-core-hw_wifi-iot-core-hw/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 50 | None | 48 | 4.3 min | FAIL |
| 2.4.1 | COMPLETED | 45 | None | 0 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 40 | 0 | 0 | 5.5 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 35 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 35 | 0 | 0 | 22.0 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 16 | 0 | 0 | 61.3 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 19 | 0 | 0 | 30.0 min | FAIL |
| master-edffcabb | COMPLETED | 21 | 0 | 0 | 7.3 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `wifi-iot-core-hw_wifi-iot-core-hw--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
