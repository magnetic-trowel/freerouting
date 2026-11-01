# TCKB_Kicad_TCKB

Tier **D** (diagnostic). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 23 |
| Clearance violations (total) | 6 |
| of which router-introduced | 0 |
| of which pre-existing in input | 6 (unfixable: 6) |
| Worst violation depth | 0.0 µm |
| Quality score | 948.5385 |
| Wall clock | 286.0 min |
| Connections (total / still open) | 597 / 23 |
| Autorouter stop reason | stagnation after 30 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **direct-first**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 29 | 0 | 935.1 | stagnation | 160.2 min |
| direct-first | 23 | 0 | 948.5 | stagnation | 24.7 min |
| fanout-retry | 26 | 0 | 941.8 | stagnation | 30.5 min |
| short-escape | 34 | 0 | 924.0 | stagnation | 23.7 min |
| open-escape | 33 | 0 | 926.2 | stagnation | 20.6 min |
| no-fanout | 33 | 0 | 926.2 | stagnation | 26.0 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 23 | 0 |
| clearance | 35 | 35 |
| hole_clearance | 22 | 32 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 11 | 4 |

Open connections reported by KiCad:

- Pad 44 [BOOT0] of U1 on B.Cu @ (148.05, 148.47) ↔ Pad 1 [BOOT0] of R1 on F.Cu @ (174.31, 144.78)
- Pad 1 [BOOT0] of R1 on F.Cu @ (174.31, 144.78) ↔ Track [BOOT0] on B.Cu, length 1.0410 mm @ (180.54, 143.46)
- Pad 41 [Line_0_] of U1 on B.Cu @ (148.05, 149.97) ↔ Track [Line_0_] on F.Cu, length 18.8258 mm @ (132.51, 76.47)
- PTH pad 2 [Line_2_] of SW31 @ (82.39, 114.46) ↔ Track [Line_2_] on B.Cu, length 5.3346 mm @ (108.41, 114.64)
- Pad 6 [Line_2_] of U1 on B.Cu @ (152.15, 144.88) ↔ Track [Line_2_] on F.Cu, length 18.8152 mm @ (146.80, 114.58)
- PTH pad 2 [Line_2_] of SW45 @ (330.04, 114.46) ↔ PTH pad 2 [Line_2_] of SW43 @ (318.13, 114.46)
- PTH pad 2 [Line_2_] of SW45 @ (330.04, 114.46) ↔ PTH pad 2 [Line_2_] of SW44 @ (338.30, 98.58)
- Track [+3V3_AUX] on F.Cu, length 1.0331 mm @ (231.39, 147.08) ↔ Track [+3V3_AUX] on F.Cu, length 1.5232 mm @ (231.28, 151.26)
- Track [+3V3_AUX] on F.Cu, length 1.6802 mm @ (234.27, 150.18) ↔ Pad 48 [+3V3_AUX] of U4 on B.Cu @ (237.78, 151.97)
- Pad 37 [SWCLK] of U1 on B.Cu @ (148.05, 151.97) ↔ PTH pad 2 [SWCLK] of STRIP1 @ (168.28, 150.50)
- Pad 31 [USART_RX] of U1 on B.Cu @ (152.15, 153.57) ↔ PTH pad 8 [USART_RX] of STRIP1 @ (183.51, 150.50)
- PTH pad 4 [SWO_2] of STRIP2 @ (209.93, 150.50) ↔ Pad 39 [SWO_2] of U4 on B.Cu @ (237.78, 147.47)
- Track [Col_0_] on B.Cu, length 7.9735 mm @ (65.56, 133.99) ↔ Track [Col_0_] on B.Cu, length 1.3081 mm @ (65.56, 153.03)
- Pad 45 [Col_2_] of U1 on B.Cu @ (148.05, 147.97) ↔ Track [Col_2_] on B.Cu, length 3.1008 mm @ (119.86, 138.47)
- Pad 3 [Col_4_] of U1 on B.Cu @ (150.65, 144.88) ↔ Track [Col_4_] on B.Cu, length 2.1438 mm @ (146.61, 137.64)
- Pad 19 [Col_11_] of U1 on B.Cu @ (156.75, 149.47) ↔ Track [Col_11_] on F.Cu, length 0.8832 mm @ (281.58, 127.25)
- Pad 26 [LED_LINE_0] of U4 on B.Cu @ (231.18, 144.88) ↔ Pad 1 [LED_LINE_0] of Q1 on B.Cu @ (279.87, 86.68)
- Track [LED_COL_1] on B.Cu, length 2.0750 mm @ (99.38, 107.48) ↔ Pad 19 [LED_COL_1] of U4 on B.Cu @ (229.08, 148.97)
- Pad 33 [LED_COL_9] of U4 on B.Cu @ (234.68, 144.88) ↔ Track [LED_COL_9] on B.Cu, length 2.0750 mm @ (256.23, 123.83)
- Pad 1 [Net-(R3-Pad1)] of R3 on B.Cu @ (81.92, 77.15) ↔ Pad 33 [Net-(R3-Pad1)] of U1 on B.Cu @ (151.15, 153.57)
- Pad 2 [ALT_LAYOUT] of SW1 on B.Cu @ (95.57, 111.55) ↔ Pad 40 [ALT_LAYOUT] of U1 on B.Cu @ (148.05, 150.47)
- Pad 15 [LIGHT_SCK] of U1 on B.Cu @ (156.75, 147.47) ↔ Pad 15 [LIGHT_SCK] of U4 on B.Cu @ (229.08, 150.97)
- Pad 17 [LIGHT_MOSI] of U1 on B.Cu @ (156.75, 148.47) ↔ Pad 17 [LIGHT_MOSI] of U4 on B.Cu @ (229.08, 149.97)

## Board

- Source: https://github.com/Ced67/TCKB_Kicad (author Ced67, license None)
- 2 layers, 92 nets, 383 components, 285.0 × 94.6 mm, 2 zones
- Human reference layout: 2369 segments, 247 vias, 12296.61 mm of track, 939 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/TCKB_Kicad_TCKB/`](../../../../fixtures/PCBench/TCKB_Kicad_TCKB/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | FAILED | 27 | None | 47 | 33.0 min | FAIL |
| 2.4.1 | TIMED_OUT | 43 | None | 47 | 30.3 min | FAIL |
| 2.5.0 | TIMED_OUT | 44 | 0 | 0 | 45.1 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 67 | 0 | 0 | 45.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 41 | 0 | 0 | 182.8 min | FAIL |
| 2.6.0-RC3 | TIMED_OUT | 21 | 0 | 6 | 360.6 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 23 | 0 | 6 | 286.0 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `TCKB_Kicad_TCKB--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
