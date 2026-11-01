# kitspace_Unifying

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 6 |
| Clearance violations (total) | 8 |
| of which router-introduced | 0 |
| of which pre-existing in input | 8 (unfixable: 8) |
| Worst violation depth | 49.90234375 µm |
| Quality score | 951.9088 |
| Wall clock | 44.2 min |
| Connections (total / still open) | 167 / 6 |
| Autorouter stop reason | stagnation after 46 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 6 | 0 | 951.9 | stagnation | 9.2 min |
| direct-first | 6 | 0 | 951.9 | stagnation | 9.7 min |
| fanout-retry | 13 | 0 | 896.0 | stagnation | 7.3 min |
| short-escape | 19 | 0 | 848.1 | stagnation | 6.8 min |
| open-escape | 12 | 0 | 904.0 | stagnation | 6.7 min |
| no-fanout | 11 | 0 | 912.0 | stagnation | 4.5 min |

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
| clearance | 14 | 499 |
| hole_clearance | 0 | 4 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 6 | 9 |

Open connections reported by KiCad:

- Pad 5 [Net-(D5-A)] of U9 on F.Cu @ (181.47, 122.42) ↔ Track [Net-(D5-A)] on F.Cu, length 0.3926 mm @ (179.41, 121.71)
- Pad 39 [/CM4_GPIO (GPIO, SDCARD)/GPIO8] of Module1 on B.Cu @ (177.62, 107.22) ↔ PTH pad 24 [/CM4_GPIO (GPIO, SDCARD)/GPIO8] of J17 @ (191.65, 114.11)
- Pad 35 [/CM4_GPIO (GPIO, SDCARD)/ID_SC] of Module1 on B.Cu @ (177.62, 106.42) ↔ PTH pad 28 [/CM4_GPIO (GPIO, SDCARD)/ID_SC] of J17 @ (191.65, 109.03)
- Pad 6 [/USB2-HUB/HD3_N] of U4 on F.Cu @ (167.86, 128.76) ↔ PTH pad 2 [/USB2-HUB/HD3_N] of J6 @ (204.47, 101.11)
- PTH pad 3 [/CM4_GPIO (GPIO, SDCARD)/BCLK] of J21 @ (162.32, 139.50) ↔ Pad 49 [/CM4_GPIO (GPIO, SDCARD)/BCLK] of Module1 on B.Cu @ (177.62, 109.22)
- Pad 15 [/CM4_HighSpeed/HDMI1_SCL] of J11 on B.Cu @ (126.09, 83.02) ↔ Pad 147 [/CM4_HighSpeed/HDMI1_SCL] of Module1 on B.Cu @ (143.70, 108.82)

## Board

- Source: https://github.com/balthazar-space/Unifying-PCB (author , license [{'name': 'CERN', 'link': 'https://raw.githubusercontent.com/balthazar-space/Unifying-PCB/main/LICENSE.md'}])
- 4 layers, 261 nets, 132 components, 0.0 × 0.0 mm, 11 zones
- Human reference layout: 4766 segments, 425 vias, 7392.06 mm of track, 73 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_Unifying/`](../../../../fixtures/PCBench/kitspace_Unifying/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 59 | None | 55 | 3.5 min | FAIL |
| 2.4.1 | COMPLETED | 48 | None | 42 | 15.2 min | FAIL |
| 2.5.0 | TIMED_OUT | 11 | 0 | 8 | 20.1 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 15 | 0 | 8 | 20.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 7 | 0 | 8 | 87.5 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 7 | 0 | 8 | 141.4 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 6 | 0 | 8 | 44.2 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_Unifying--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
