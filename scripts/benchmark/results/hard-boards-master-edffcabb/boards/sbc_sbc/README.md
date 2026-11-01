# sbc_sbc

Tier **D** (diagnostic). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 12 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 960.9756 |
| Wall clock | 78.4 min |
| Connections (total / still open) | 410 / 12 |
| Autorouter stop reason | stagnation after 26 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **open-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 22 | 0 | 928.5 | stagnation | 17.9 min |
| direct-first | 14 | 0 | 954.5 | stagnation | 13.5 min |
| fanout-retry | 16 | 0 | 948.0 | stagnation | 9.8 min |
| short-escape | 21 | 0 | 931.7 | stagnation | 10.7 min |
| open-escape | 12 | 0 | 961.0 | stagnation | 11.6 min |
| no-fanout | 27 | 0 | 912.2 | stagnation | 14.8 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 12 | 0 |
| clearance | 0 | 72 |
| hole_clearance | 199 | 4 |
| shorting_items | 0 | 1 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 4 |

Open connections reported by KiCad:

- Track [+1.5V] on F.Cu, length 0.2397 mm @ (130.74, 93.52) ↔ Track [+1.5V] on F.Cu, length 0.3558 mm @ (130.24, 94.34)
- Pad F17 [/MMC0_DAT3] of IC1 on F.Cu @ (155.28, 88.41) ↔ Track [/MMC0_DAT3] on F.Cu, length 3.9391 mm @ (172.13, 97.50)
- Pad A7 [/DDR_D12] of DRAM1 on F.Cu @ (125.28, 97.14) ↔ Pad K4 [/DDR_D12] of IC1 on F.Cu @ (144.88, 91.61)
- Pad D3 [/DDR_DQM1] of DRAM1 on F.Cu @ (128.48, 94.74) ↔ Pad J2 [/DDR_DQM1] of IC1 on F.Cu @ (143.28, 90.81)
- Pad E3 [/DDR_D0] of DRAM1 on F.Cu @ (128.48, 93.94) ↔ Pad M3 [/DDR_D0] of IC1 on F.Cu @ (144.08, 93.21)
- Pad F7 [/DDR_D1] of DRAM1 on F.Cu @ (125.28, 93.14) ↔ Pad M4 [/DDR_D1] of IC1 on F.Cu @ (144.88, 93.21)
- Pad J3 [/DDR_RASn] of DRAM1 on F.Cu @ (128.48, 90.74) ↔ Pad G4 [/DDR_RASn] of IC1 on F.Cu @ (144.88, 89.21)
- Pad K9 [/DDR_CKE] of DRAM1 on F.Cu @ (123.68, 89.94) ↔ Pad G3 [/DDR_CKE] of IC1 on F.Cu @ (144.08, 89.21)
- Pad L7 [/DDR_A10] of DRAM1 on F.Cu @ (125.28, 89.14) ↔ Pad F4 [/DDR_A10] of IC1 on F.Cu @ (144.88, 88.41)
- Pad N7 [/DDR_A12] of DRAM1 on F.Cu @ (125.28, 87.54) ↔ Pad E3 [/DDR_A12] of IC1 on F.Cu @ (144.08, 87.61)
- Pad P3 [/DDR_A2] of DRAM1 on F.Cu @ (128.48, 86.74) ↔ Pad E4 [/DDR_A2] of IC1 on F.Cu @ (144.88, 87.61)
- Pad P8 [/DDR_A4] of DRAM1 on F.Cu @ (124.48, 86.74) ↔ Pad C2 [/DDR_A4] of IC1 on F.Cu @ (143.28, 86.01)

## Board

- Source: https://github.com/rithvikp1998/sbc (author rithvikp1998, license None)
- 4 layers, 51 nets, 91 components, 67.9 × 30.4 mm, 1 zones
- Human reference layout: 2020 segments, 213 vias, 2521.02 mm of track, 806 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/sbc_sbc/`](../../../../fixtures/PCBench/sbc_sbc/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 20 | None | 25 | 6.4 min | FAIL |
| 2.4.1 | COMPLETED | 98 | None | 24 | 5.2 min | FAIL |
| 2.5.0 | TIMED_OUT | 81 | 0 | 0 | 10.2 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 43 | 0 | 0 | 10.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 17 | 0 | 0 | 182.0 min | FAIL |
| 2.6.0-RC3 | TIMED_OUT | 13 | 0 | 0 | 360.8 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 12 | 0 | 0 | 78.4 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `sbc_sbc--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
