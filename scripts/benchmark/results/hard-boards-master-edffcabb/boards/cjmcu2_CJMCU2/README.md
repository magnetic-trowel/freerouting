# cjmcu2_CJMCU2

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 21 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 795.6204 |
| Wall clock | 14.3 min |
| Connections (total / still open) | 137 / 21 |
| Autorouter stop reason | stagnation after 22 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **short-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 22 | 0 | 785.9 | stagnation | 2.6 min |
| direct-first | 22 | 0 | 785.9 | stagnation | 2.4 min |
| fanout-retry | 27 | 0 | 737.2 | stagnation | 2.7 min |
| short-escape | 21 | 0 | 795.6 | stagnation | 2.0 min |
| open-escape | 22 | 0 | 785.9 | stagnation | 2.0 min |
| no-fanout | 23 | 0 | 776.2 | stagnation | 2.5 min |

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
| clearance | 4 | 7 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 2 |
| copper_edge_clearance | 3 | 9 |

Open connections reported by KiCad:

- Track [GND] on B.Cu, length 7.3126 mm @ (147.01, 111.72) ↔ Pad 23 [GND] of U2 on F.Cu @ (145.31, 107.76)
- Pad 11 [GND] of U4 on F.Cu @ (143.64, 85.03) ↔ Pad 9 [GND] of U4 on F.Cu @ (144.65, 85.03)
- Pad 11 [GND] of U4 on F.Cu @ (143.64, 85.03) ↔ Track [GND] on B.Cu, length 1.1455 mm @ (143.45, 87.70)
- Pad 1 [Net-(C2-Pad2)] of Y1 on B.Cu @ (135.13, 100.39) ↔ Pad 2 [Net-(C2-Pad2)] of C2 on F.Cu @ (129.79, 100.45)
- Pad 5 [Net-(C2-Pad2)] of U2 on F.Cu @ (138.79, 102.93) ↔ Pad 1 [Net-(C2-Pad2)] of Y1 on B.Cu @ (135.13, 100.39)
- Pad 9 [+3V3] of U2 on F.Cu @ (138.79, 105.12) ↔ Track [+3V3] on F.Cu, length 0.0931 mm @ (138.79, 100.61)
- Pad 12 [/SETC] of U3 on F.Cu @ (149.16, 95.50) ↔ Pad 2 [/SETC] of C12 on F.Cu @ (155.73, 95.81)
- Track [/SDA] on F.Cu, length 1.6877 mm @ (147.67, 97.49) ↔ Pad 43 [/SDA] of U2 on F.Cu @ (142.62, 99.63)
- Pad 1 [/PC14] of R2 on F.Cu @ (135.57, 90.73) ↔ Pad 3 [/PC14] of U2 on F.Cu @ (138.79, 101.82)
- Pad 2 [/PC15] of R3 on F.Cu @ (132.84, 90.73) ↔ Pad 4 [/PC15] of U2 on F.Cu @ (138.79, 102.38)
- Pad 44 [Net-(R7-Pad1)] of U2 on F.Cu @ (142.06, 99.63) ↔ Pad 1 [Net-(R7-Pad1)] of R7 on F.Cu @ (145.35, 95.68)
- Pad 34 [/PA13_JTMS] of U2 on F.Cu @ (146.94, 101.79) ↔ Track [/PA13_JTMS] on F.Cu, length 0.9500 mm @ (152.24, 104.58)
- Pad 37 [/PA14_JTCK] of U2 on F.Cu @ (145.87, 99.61) ↔ Pad 2 [/PA14_JTCK] of R11 on F.Cu @ (152.07, 102.04)
- Pad 7 [/RESET] of U2 on F.Cu @ (138.79, 104.03) ↔ Track [/RESET] on B.Cu, length 1.3000 mm @ (132.02, 107.70)
- PTH pad 7 [/PA5/SP11_SCK] of W2 @ (118.22, 118.27) ↔ Pad 15 [/PA5/SP11_SCK] of U2 on F.Cu @ (141.00, 107.79)
- PTH pad 2 [/PA7/SP11_MOSI] of W2 @ (119.55, 118.33) ↔ Pad 17 [/PA7/SP11_MOSI] of U2 on F.Cu @ (142.11, 107.79)
- Pad 1 [/PB1] of R20 on B.Cu @ (142.95, 111.58) ↔ Pad 19 [/PB1] of U2 on F.Cu @ (143.18, 107.79)
- Pad 30 [/PA9/USART1__TX] of U2 on F.Cu @ (146.94, 104.00) ↔ PTH pad 5 [/PA9/USART1__TX] of P3 @ (158.31, 118.75)
- PTH pad 3 [/PA11] of W2 @ (116.95, 118.27) ↔ Pad 32 [/PA11] of U2 on F.Cu @ (146.94, 102.91)
- Pad 1 [/PB9] of R19 on B.Cu @ (135.62, 98.11) ↔ Pad 46 [/PB9] of U2 on F.Cu @ (140.94, 99.63)
- Pad 10 [Net-(C16-Pad2)] of U3 on F.Cu @ (150.18, 95.50) ↔ Pad 2 [Net-(C16-Pad2)] of C16 on B.Cu @ (154.18, 93.73)

## Board

- Source: https://github.com/aeracoop/cjmcu2 (author aeracoop, license GPL-3.0)
- 2 layers, 46 nets, 68 components, 57.78 × 57.15 mm, 3 zones
- Human reference layout: 885 segments, 45 vias, 1902.79 mm of track, 688 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/cjmcu2_CJMCU2/`](../../../../fixtures/PCBench/cjmcu2_CJMCU2/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 17 | None | 4 | 5.4 min | FAIL |
| 2.4.1 | COMPLETED | 30 | None | 0 | 5.2 min | FAIL |
| 2.5.0 | COMPLETED | 39 | 0 | 0 | 9.0 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 36 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 36 | 0 | 0 | 33.6 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 16 | 0 | 0 | 204.8 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 21 | 0 | 0 | 14.3 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `cjmcu2_CJMCU2--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
