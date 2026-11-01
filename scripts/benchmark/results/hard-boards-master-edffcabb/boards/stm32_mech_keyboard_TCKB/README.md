# stm32_mech_keyboard_TCKB

Tier **D** (diagnostic). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 35 |
| Clearance violations (total) | 4 |
| of which router-introduced | 0 |
| of which pre-existing in input | 4 (unfixable: 4) |
| Worst violation depth | 0.0 µm |
| Quality score | 893.61304 |
| Wall clock | 163.8 min |
| Connections (total / still open) | 439 / 35 |
| Autorouter stop reason | stagnation after 37 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **open-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 47 | 0 | 857.2 | stagnation | 24.3 min |
| direct-first | 50 | 0 | 848.1 | stagnation | 26.7 min |
| fanout-retry | 43 | 0 | 869.3 | stagnation | 26.6 min |
| short-escape | 47 | 0 | 857.2 | stagnation | 30.4 min |
| open-escape | 35 | 0 | 893.6 | stagnation | 43.5 min |
| no-fanout | 41 | 0 | 875.4 | stagnation | 12.2 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 35 | 0 |
| clearance | 0 | 0 |
| hole_clearance | 22 | 23 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 8 | 10 |

Open connections reported by KiCad:

- PTH pad 1 [LED_LINE_1] of LED7 @ (101.28, 87.79) ↔ PTH pad 1 [LED_LINE_1] of LED2 @ (77.47, 87.79)
- Pad 3 [LED_LINE_1] of U5 on B.Cu @ (230.30, 150.62) ↔ Track [LED_LINE_1] on F.Cu, length 16.3544 mm @ (216.93, 89.14)
- PTH pad 1 [LED_LINE_4] of LED5 @ (75.09, 144.94) ↔ Track [LED_LINE_4] on F.Cu, length 12.9729 mm @ (111.87, 144.94)
- Pad 7 [LED_LINE_4] of U5 on B.Cu @ (230.30, 149.03) ↔ Track [LED_LINE_4] on B.Cu, length 6.5945 mm @ (237.06, 153.73)
- PTH pad 2 [LED_COL_1] of LED7 @ (98.74, 87.79) ↔ Track [LED_COL_1] on F.Cu, length 13.8417 mm @ (103.50, 106.84)
- PTH pad 2 [LED_COL_1] of LED7 @ (98.74, 87.79) ↔ PTH pad 2 [LED_COL_1] of LED6 @ (89.22, 68.74)
- Pad 17 [LED_COL_1] of U5 on B.Cu @ (232.70, 146.22) ↔ Track [LED_COL_1] on B.Cu, length 1.5150 mm @ (101.86, 131.61)
- PTH pad 1 [LED_LINE_2] of LED8 @ (106.05, 106.84) ↔ PTH pad 1 [LED_LINE_2] of LED65 @ (79.88, 106.81)
- Track [LED_LINE_2] on F.Cu, length 1.9057 mm @ (125.09, 106.84) ↔ PTH pad 1 [LED_LINE_2] of LED8 @ (106.05, 106.84)
- Pad 4 [LED_LINE_2] of U5 on B.Cu @ (230.30, 150.22) ↔ Track [LED_LINE_2] on F.Cu, length 19.0500 mm @ (220.34, 108.19)
- Pad 19 [LED_COL_3] of U5 on B.Cu @ (233.50, 146.22) ↔ Track [LED_COL_3] on B.Cu, length 11.1732 mm @ (144.73, 102.36)
- PTH pad 2 [LED_COL_4] of LED20 @ (146.37, 68.74) ↔ PTH pad 2 [LED_COL_4] of LED21 @ (155.89, 87.79)
- Pad 21 [LED_COL_4] of U5 on B.Cu @ (234.30, 146.22) ↔ Track [LED_COL_4] on B.Cu, length 10.1835 mm @ (163.74, 92.21)
- Pad 27 [LED_COL_10] of U5 on B.Cu @ (236.30, 147.82) ↔ Track [LED_COL_10] on B.Cu, length 8.4501 mm @ (262.62, 134.70)
- Pad 28 [LED_COL_11] of U5 on B.Cu @ (236.30, 148.22) ↔ Track [LED_COL_11] on B.Cu, length 2.9734 mm @ (281.67, 135.41)
- Track [LED_COL_11] on F.Cu, length 5.4080 mm @ (294.00, 106.84) ↔ PTH pad 2 [LED_COL_11] of LED51 @ (289.24, 87.79)
- PTH pad 2 [LED_COL_11] of LED51 @ (289.24, 87.79) ↔ PTH pad 2 [LED_COL_11] of LED50 @ (279.72, 68.74)
- Pad 31 [LED_COL_13] of U5 on B.Cu @ (236.30, 149.43) ↔ Track [LED_COL_13] on B.Cu, length 6.6096 mm @ (317.33, 134.87)
- PTH pad 1 [+5V] of P2 @ (78.23, 83.44) ↔ Track [+5V] on F.Cu, length 0.8564 mm @ (169.02, 146.69)
- PTH pad VBUS [+5V] of J1 @ (79.40, 74.10) ↔ PTH pad 1 [+5V] of P2 @ (78.23, 83.44)
- Pad 20 [+5V] of U5 on B.Cu @ (233.90, 146.22) ↔ Track [+5V] on F.Cu, length 0.7814 mm @ (233.68, 144.08)
- Pad 24 [+3V3] of U1 on B.Cu @ (148.05, 146.47) ↔ Track [+3V3] on F.Cu, length 0.7814 mm @ (150.37, 147.13)
- Pad 48 [+3V3] of U1 on B.Cu @ (156.75, 151.97) ↔ Track [+3V3] on F.Cu, length 0.7814 mm @ (155.83, 151.32)
- Pad 16 [LED_INT] of U1 on B.Cu @ (148.05, 150.47) ↔ Pad 2 [LED_INT] of R5 on F.Cu @ (227.08, 153.88)
- Pad 2 [LED_INT] of R5 on F.Cu @ (227.08, 153.88) ↔ Pad 45 [LED_INT] of U5 on B.Cu @ (232.30, 152.22)
- Track [SCL] on B.Cu, length 1.2564 mm @ (208.15, 150.62) ↔ Pad 42 [SCL] of U1 on B.Cu @ (156.75, 148.97)
- Track [SCL] on F.Cu, length 0.8564 mm @ (224.28, 153.88) ↔ Pad 42 [SCL] of U5 on B.Cu @ (233.50, 152.22)
- Pad 34 [SWDIO] of U1 on B.Cu @ (154.15, 144.88) ↔ PTH pad 3 [SWDIO] of DBG1 @ (170.81, 150.50)
- Pad 1 [Col_3_] of D6 on B.Cu @ (127.00, 89.38) ↔ Pad 1 [Col_3_] of D20 on B.Cu @ (129.06, 97.79)
- Track [Col_3_] on B.Cu, length 1.3064 mm @ (138.91, 137.16) ↔ Pad 39 [Col_3_] of U1 on B.Cu @ (156.75, 147.47)
- Pad 1 [Col_3_] of D34 on B.Cu @ (139.70, 127.48) ↔ Pad 1 [Col_3_] of D20 on B.Cu @ (129.06, 97.79)
- Pad 21 [Col_8_] of U1 on B.Cu @ (148.05, 147.97) ↔ Track [Col_8_] on B.Cu, length 1.3064 mm @ (234.16, 137.16)
- Pad 22 [Col_9_] of U1 on B.Cu @ (148.05, 147.47) ↔ Track [Col_9_] on B.Cu, length 1.3064 mm @ (253.21, 137.16)
- Pad 1 [Col_9_] of D26 on B.Cu @ (244.00, 95.89) ↔ Pad 1 [Col_9_] of D12 on B.Cu @ (244.47, 89.38)
- Pad 1 [Col_9_] of D40 on B.Cu @ (253.37, 127.48) ↔ Pad 1 [Col_9_] of D26 on B.Cu @ (244.00, 95.89)

## Board

- Source: https://github.com/julbouln/stm32_mech_keyboard (author julbouln, license None)
- 2 layers, 66 nets, 242 components, 285.0 × 94.6 mm, 2 zones
- Human reference layout: 2435 segments, 220 vias, 11185.34 mm of track, 907 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/stm32_mech_keyboard_TCKB/`](../../../../fixtures/PCBench/stm32_mech_keyboard_TCKB/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | FAILED | 33 | None | 46 | 17.0 min | FAIL |
| 2.4.1 | TIMED_OUT | 52 | None | 46 | 15.2 min | FAIL |
| 2.5.0 | TIMED_OUT | 76 | 0 | 0 | 20.1 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 76 | 0 | 0 | 20.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 72 | 0 | 0 | 244.6 min | FAIL |
| 2.6.0-RC3 | TIMED_OUT | 35 | 0 | 4 | 360.2 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 35 | 0 | 4 | 163.8 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `stm32_mech_keyboard_TCKB--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
