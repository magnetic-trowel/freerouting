# pcbproject_opensda_project_opensda

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 15 |
| Clearance violations (total) | 8 |
| of which router-introduced | 0 |
| of which pre-existing in input | 8 (unfixable: 8) |
| Worst violation depth | 96.70257568359375 µm |
| Quality score | 818.7424 |
| Wall clock | 25.0 min |
| Connections (total / still open) | 111 / 15 |
| Autorouter stop reason | stagnation after 25 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **fanout-retry**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 20 | 0 | 758.7 | stagnation | 0.7 min |
| direct-first | 16 | 0 | 806.7 | stagnation | 0.5 min |
| fanout-retry | 15 | 0 | 818.7 | stagnation | 0.9 min |
| short-escape | 21 | 0 | 746.7 | stagnation | 21.7 min |
| open-escape | 19 | 0 | 770.7 | stagnation | 0.5 min |
| no-fanout | 21 | 0 | 746.7 | stagnation | 0.7 min |

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
| clearance | 44 | 44 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Track [VDD] on F.Cu, length 1.9050 mm @ (151.13, 113.34) ↔ Pad 1 [VDD] of U1 on F.Cu @ (154.32, 114.07)
- Pad 7 [VDD] of U1 on F.Cu @ (154.32, 117.07) ↔ Pad 1 [VDD] of U1 on F.Cu @ (154.32, 114.07)
- Pad 4 [VDD] of U2 on F.Cu @ (163.58, 108.58) ↔ Track [VDD] on F.Cu, length 1.1327 mm @ (161.46, 104.78)
- Pad 14 [VDD] of U2 on F.Cu @ (169.16, 106.64) ↔ Track [VDD] on F.Cu, length 3.7355 mm @ (171.42, 104.31)
- Track [VCC] on F.Cu, length 5.8272 mm @ (150.45, 109.45) ↔ Pad 6 [VCC] of U1 on F.Cu @ (154.32, 116.57)
- Pad 1 [Net-(C7-Pad1)] of C7 on F.Cu @ (150.88, 116.78) ↔ Pad 5 [Net-(C7-Pad1)] of U1 on F.Cu @ (154.32, 116.07)
- Pad 21 [/RST_TGTMCU] of U1 on F.Cu @ (159.12, 115.57) ↔ Track [/RST_TGTMCU] on F.Cu, length 1.0517 mm @ (143.72, 129.54)
- PTH pad 5 [/RST_TGTMCU] of P3 @ (182.25, 115.57) ↔ Pad 21 [/RST_TGTMCU] of U1 on F.Cu @ (159.12, 115.57)
- Pad 29 [Net-(D2-Pad1)] of U1 on F.Cu @ (156.47, 113.42) ↔ Pad 1 [Net-(D2-Pad1)] of TP7 on F.Cu @ (158.12, 109.22)
- Pad 1 [Net-(D2-Pad1)] of TP7 on F.Cu @ (158.12, 109.22) ↔ Pad 1 [Net-(D2-Pad1)] of D2 on F.Cu @ (158.12, 107.30)
- Pad 14 [/SDA_JTAG_TDO] of U1 on F.Cu @ (157.47, 118.22) ↔ PTH pad 6 [/SDA_JTAG_TDO] of P2 @ (157.48, 128.27)
- Pad 13 [/SDA_JTAG_TDI] of U1 on F.Cu @ (156.97, 118.22) ↔ PTH pad 8 [/SDA_JTAG_TDI] of P2 @ (160.02, 128.27)
- Pad 30 [/PIN1_TO_MCU] of U1 on F.Cu @ (155.97, 113.42) ↔ PTH pad 9 [/PIN1_TO_MCU] of P3 @ (182.25, 110.49)
- Pad 2 [Net-(R2-Pad2)] of R2 on F.Cu @ (143.34, 116.84) ↔ Pad 4 [Net-(R2-Pad2)] of U1 on F.Cu @ (154.32, 115.57)
- Pad 27 [/SDA_SPI0_SOUT] of U1 on F.Cu @ (157.47, 113.42) ↔ Pad 2 [/SDA_SPI0_SOUT] of U2 on F.Cu @ (163.58, 107.29)

## Board

- Source: https://github.com/HarborZhang/pcbproject_opensda (author HarborZhang, license None)
- 2 layers, 24 nets, 50 components, 66.04 × 38.1 mm, 3 zones
- Human reference layout: 697 segments, 156 vias, 1687.42 mm of track, 509 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/pcbproject_opensda_project_opensda/`](../../../../fixtures/PCBench/pcbproject_opensda_project_opensda/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 41 | None | 54 | 1.2 min | FAIL |
| 2.4.1 | COMPLETED | 30 | None | 15 | 4.1 min | FAIL |
| 2.5.0 | COMPLETED | 24 | 0 | 8 | 2.5 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 19 | 0 | 8 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 19 | 0 | 8 | 9.8 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 15 | 0 | 8 | 22.6 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 12 | 0 | 8 | 12.6 min | FAIL |
| master-edffcabb | COMPLETED | 15 | 0 | 8 | 25.0 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `pcbproject_opensda_project_opensda--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
