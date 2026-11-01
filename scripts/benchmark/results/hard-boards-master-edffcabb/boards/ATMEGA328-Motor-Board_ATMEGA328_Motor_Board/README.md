# ATMEGA328-Motor-Board_ATMEGA328_Motor_Board

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 6 |
| Clearance violations (total) | 6 |
| of which router-introduced | 0 |
| of which pre-existing in input | 6 (unfixable: 6) |
| Worst violation depth | 104.70058593750001 µm |
| Quality score | 983.6967 |
| Wall clock | 14.3 min |
| Connections (total / still open) | 499 / 6 |
| Autorouter stop reason | stagnation after 21 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 6 | 0 | 983.7 | stagnation | 2.0 min |
| direct-first | 7 | 0 | 981.0 | stagnation | 2.6 min |
| fanout-retry | 7 | 0 | 981.0 | stagnation | 2.0 min |
| short-escape | 10 | 0 | 973.0 | stagnation | 1.8 min |
| open-escape | 6 | 0 | 983.7 | stagnation | 2.1 min |
| no-fanout | 10 | 0 | 973.0 | stagnation | 3.8 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 6 | 0 |
| clearance | 45 | 50 |
| hole_clearance | 0 | 0 |
| shorting_items | 12 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 1 | 27 |

Open connections reported by KiCad:

- Pad 22 [SERVO4] of U2 on F.Cu @ (133.75, 69.66) ↔ PTH pad 1 [SERVO4] of P8 @ (176.79, 112.89)
- Pad 2 [MOTOR1_SENSE] of U4 on F.Cu @ (128.25, 108.56) ↔ Pad 3 [MOTOR1_SENSE] of U7 on F.Cu @ (142.64, 84.91)
- Track [MOTOR2_SENSE] on F.Cu, length 1.5121 mm @ (145.00, 108.55) ↔ Pad 5 [MOTOR2_SENSE] of U7 on F.Cu @ (148.54, 85.56)
- Pad 1 [MOTOR3_SENSE] of R23 on F.Cu @ (151.45, 108.38) ↔ Pad 2 [MOTOR3_SENSE] of U5 on F.Cu @ (154.50, 108.38)
- Pad 2 [MOTOR3_SENSE] of U5 on F.Cu @ (154.50, 108.38) ↔ Pad 3 [MOTOR3_SENSE] of U8 on F.Cu @ (174.26, 84.91)
- Track [MOTOR4_SENSE] on F.Cu, length 1.5121 mm @ (171.00, 108.40) ↔ Pad 5 [MOTOR4_SENSE] of U8 on F.Cu @ (180.16, 85.56)

## Board

- Source: https://github.com/AntonioMR/ATMEGA328-Motor-Board (author AntonioMR, license None)
- 2 layers, 46 nets, 259 components, 77.0 × 100.0 mm, 4 zones
- Human reference layout: 4134 segments, 136 vias, 4454.57 mm of track, 953 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/ATMEGA328-Motor-Board_ATMEGA328_Motor_Board/`](../../../../fixtures/PCBench/ATMEGA328-Motor-Board_ATMEGA328_Motor_Board/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 20 | None | 128 | 12.5 min | FAIL |
| 2.4.1 | COMPLETED | 13 | None | 122 | 15.3 min | FAIL |
| 2.5.0 | COMPLETED | 43 | 0 | 36 | 17.9 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 4 | 0 | 6 | 20.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 4 | 0 | 6 | 31.9 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 5 | 0 | 6 | 46.6 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 6 | 0 | 6 | 14.3 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `ATMEGA328-Motor-Board_ATMEGA328_Motor_Board--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
