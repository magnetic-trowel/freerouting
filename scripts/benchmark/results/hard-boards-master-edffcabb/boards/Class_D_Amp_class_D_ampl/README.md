# Class_D_Amp_class_D_ampl

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 14 |
| Clearance violations (total) | 8 |
| of which router-introduced | 0 |
| of which pre-existing in input | 8 (unfixable: 8) |
| Worst violation depth | 229.90264892578125 µm |
| Quality score | 891.85345 |
| Wall clock | 1.1 min |
| Connections (total / still open) | 175 / 14 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **open-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 15 | 0 | 884.2 | stagnation | 0.2 min |
| direct-first | 17 | 0 | 869.0 | stagnation | 0.2 min |
| fanout-retry | 18 | 0 | 861.4 | stagnation | 0.1 min |
| short-escape | 15 | 0 | 884.2 | stagnation | 0.1 min |
| open-escape | 14 | 0 | 891.9 | stagnation | 0.1 min |
| no-fanout | 20 | 0 | 846.1 | stagnation | 0.2 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 14 | 0 |
| clearance | 10 | 60 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Track [Net-(C4-Pad1)] on F.Cu, length 1.8990 mm @ (126.40, 124.56) ↔ Pad 7 [Net-(C4-Pad1)] of U5 on F.Cu @ (122.97, 124.47)
- Pad 6 [Net-(C4-Pad2)] of U5 on F.Cu @ (122.97, 125.74) ↔ Pad 2 [Net-(C4-Pad2)] of C4 on F.Cu @ (129.92, 128.78)
- Track [VCC] on B.Cu, length 1.5517 mm @ (118.93, 124.48) ↔ Pad 3 [VCC] of U5 on F.Cu @ (122.97, 129.55)
- Track [VCC] on B.Cu, length 1.5517 mm @ (117.77, 100.31) ↔ Pad 3 [VCC] of U4 on F.Cu @ (122.91, 103.93)
- Pad 3 [VCC] of U4 on F.Cu @ (122.91, 103.93) ↔ Track [VCC] on B.Cu, length 22.4570 mm @ (124.85, 127.56)
- Track [VCC] on B.Cu, length 22.4570 mm @ (124.85, 127.56) ↔ Pad 3 [VCC] of U5 on F.Cu @ (122.97, 129.55)
- Pad 2 [Net-(R16-Pad2)] of R16 on F.Cu @ (122.73, 109.35) ↔ Pad 1 [Net-(R16-Pad2)] of U4 on F.Cu @ (122.91, 106.47)
- Pad 1 [Net-(R18-Pad2)] of U5 on F.Cu @ (122.97, 132.09) ↔ Pad 2 [Net-(R18-Pad2)] of R18 on F.Cu @ (123.04, 135.16)
- Pad 1 [Net-(C9-Pad1)] of C9 on F.Cu @ (118.95, 93.91) ↔ Pad 7 [Net-(C9-Pad1)] of U4 on F.Cu @ (122.91, 98.85)
- Pad 7 [Net-(C9-Pad1)] of U4 on F.Cu @ (122.91, 98.85) ↔ Pad 1 [Net-(C9-Pad1)] of D5 on F.Cu @ (127.35, 100.85)
- Track [GNDPWR] on B.Cu, length 17.1535 mm @ (121.13, 129.88) ↔ Pad 2 [GNDPWR] of U5 on F.Cu @ (122.97, 130.82)
- Pad 2 [GNDPWR] of U4 on F.Cu @ (122.91, 105.20) ↔ Track [GNDPWR] on B.Cu, length 5.8515 mm @ (121.13, 112.72)
- Track [Net-(C9-Pad2)] on F.Cu, length 3.8699 mm @ (114.95, 93.91) ↔ Pad 6 [Net-(C9-Pad2)] of U4 on F.Cu @ (122.91, 100.12)
- Pad 4 [Net-(U6-Pad4)] of U6 on F.Cu @ (90.12, 124.93) ↔ Pad 13 [Net-(U6-Pad4)] of U8 on F.Cu @ (99.83, 125.02)

## Board

- Source: https://github.com/zerberros/Class_D_Amp (author zerberros, license None)
- 2 layers, 59 nets, 89 components, 140.5 × 79.0 mm, 1 zones
- Human reference layout: 736 segments, 39 vias, 2020.2 mm of track, 323 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/Class_D_Amp_class_D_ampl/`](../../../../fixtures/PCBench/Class_D_Amp_class_D_ampl/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 14 | None | 8 | 1.0 min | FAIL |
| 2.4.1 | COMPLETED | 13 | None | 8 | 3.7 min | FAIL |
| 2.5.0 | COMPLETED | 20 | 0 | 8 | 2.5 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 17 | 0 | 8 | 5.7 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 17 | 0 | 8 | 6.5 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 13 | 0 | 8 | 5.3 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 13 | 0 | 8 | 3.2 min | FAIL |
| master-edffcabb | COMPLETED | 14 | 0 | 8 | 1.1 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `Class_D_Amp_class_D_ampl--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
