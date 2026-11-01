# tinyFISH_tinyFISH

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 15 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 900.0 |
| Wall clock | 4.4 min |
| Connections (total / still open) | 200 / 15 |
| Autorouter stop reason | stagnation after 20 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **no-fanout**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 19 | 0 | 873.3 | stagnation | 0.9 min |
| direct-first | 18 | 0 | 880.0 | stagnation | 0.7 min |
| fanout-retry | 20 | 0 | 866.7 | stagnation | 0.9 min |
| short-escape | 16 | 0 | 893.3 | stagnation | 0.5 min |
| open-escape | 19 | 0 | 873.3 | stagnation | 0.9 min |
| no-fanout | 15 | 0 | 900.0 | stagnation | 0.4 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 15 | 0 |
| clearance | 0 | 5 |
| hole_clearance | 7 | 4 |
| shorting_items | 0 | 1 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 56 | 78 |

Open connections reported by KiCad:

- Track [GND] on F.Cu, length 0.8803 mm @ (224.63, 137.28) ↔ Track [GND] on B.Cu, length 1.0553 mm @ (224.63, 137.08)
- Track [3V] on B.Cu, length 0.7493 mm @ (225.93, 129.72) ↔ Track [3V] on F.Cu, length 0.4446 mm @ (225.96, 128.70)
- Track [3V] on F.Cu, length 0.8803 mm @ (226.23, 132.18) ↔ Track [3V] on B.Cu, length 0.4303 mm @ (228.44, 132.22)
- Pad 1 [3V] of C25 on F.Cu @ (236.36, 136.10) ↔ Track [3V] on F.Cu, length 0.8803 mm @ (233.33, 133.78)
- Track [3V] on B.Cu, length 1.4070 mm @ (236.83, 136.46) ↔ Pad 1 [3V] of C25 on F.Cu @ (236.36, 136.10)
- Pad 1 [ESC_2] of TP_ESC_2 on B.Cu @ (223.13, 130.58) ↔ Pad 13 [ESC_2] of U4 on F.Cu @ (226.23, 140.88)
- Pad 15 [MPU_SPI_SCK] of U4 on F.Cu @ (227.23, 140.88) ↔ Pad 23 [MPU_SPI_SCK] of U2 on B.Cu @ (232.78, 135.08)
- Pad 17 [MPU_SPI_MOSI] of U4 on F.Cu @ (228.23, 140.88) ↔ Pad 24 [MPU_SPI_MOSI] of U2 on B.Cu @ (232.78, 135.58)
- Pad 18 [ADC_CURRENT] of U4 on F.Cu @ (228.73, 140.88) ↔ Track [ADC_CURRENT] on B.Cu, length 0.4303 mm @ (228.36, 137.25)
- Pad 21 [UART3_TX] of U4 on F.Cu @ (230.23, 140.88) ↔ Pad 1 [UART3_TX] of TP_2 on F.Cu @ (238.35, 136.68)
- Pad 32 [Net-(U4-Pad32)] of U4 on F.Cu @ (233.33, 135.78) ↔ Pad 2 [Net-(U4-Pad32)] of U6 on F.Cu @ (235.13, 131.03)
- Pad 33 [Net-(R9-Pad2)] of U4 on F.Cu @ (233.33, 135.28) ↔ Track [Net-(R9-Pad2)] on F.Cu, length 0.5303 mm @ (233.88, 131.55)
- Pad 1 [ESC_0] of TP_ESC_0 on B.Cu @ (220.13, 130.58) ↔ Pad 45 [ESC_0] of U4 on F.Cu @ (227.73, 132.18)
- Pad 1 [ESC_1] of TP_ESC_1 on B.Cu @ (224.63, 130.58) ↔ Pad 46 [ESC_1] of U4 on F.Cu @ (227.23, 132.18)
- Pad 31 [/rx/~{RESET}] of U1 on F.Cu @ (229.43, 129.89) ↔ Track [/rx/~{RESET}] on B.Cu, length 0.5303 mm @ (231.96, 125.28)

## Board

- Source: https://github.com/fishpepper/tinyFISH (author fishpepper, license NOASSERTION)
- 2 layers, 22 nets, 88 components, 20.0 × 20.0 mm, 2 zones
- Human reference layout: 1116 segments, 55 vias, 706.96 mm of track, 611 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/tinyFISH_tinyFISH/`](../../../../fixtures/PCBench/tinyFISH_tinyFISH/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 25 | None | 57 | 2.3 min | FAIL |
| 2.4.1 | COMPLETED | 40 | None | 75 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 32 | 0 | 0 | 10.0 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 19 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 17 | 0 | 0 | 21.3 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 17 | 0 | 0 | 32.2 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 13 | 0 | 0 | 14.5 min | FAIL |
| master-edffcabb | COMPLETED | 15 | 0 | 0 | 4.4 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `tinyFISH_tinyFISH--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
