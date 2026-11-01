# HW-ESP8266_Roomba_ESP8266_Roomba_panel_x_3

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 4 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 970.696 |
| Wall clock | 1.5 min |
| Connections (total / still open) | 182 / 4 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 4 | 0 | 970.7 | stagnation | 0.2 min |
| continue | 4 | 0 | 970.7 | stagnation | 0.1 min |
| direct-first | 7 | 0 | 948.7 | stagnation | 0.4 min |
| fanout-retry | 4 | 0 | 970.7 | stagnation | 0.2 min |
| short-escape | 6 | 0 | 956.0 | stagnation | 0.3 min |
| open-escape | 4 | 0 | 970.7 | stagnation | 0.2 min |
| no-fanout | 4 | 0 | 970.7 | stagnation | 0.1 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 4 | 0 |
| clearance | 0 | 0 |
| hole_clearance | 2 | 3 |
| shorting_items | 0 | 1 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 115 | 103 |

Open connections reported by KiCad:

- Track [Net-(R18-Pad1)] on F.Cu, length 2.2233 mm @ (163.67, 136.49) ↔ Pad 1 [Net-(R18-Pad1)] of SW1 on F.Cu @ (166.67, 133.99)
- Pad 2 [GND] of SW1 on F.Cu @ (174.67, 133.99) ↔ Track [GND] on F.Cu, length 1.9267 mm @ (170.67, 139.69)
- Pad 3 [Net-(P1-Pad3)] of P1 on B.Cu @ (96.80, 143.84) ↔ Pad 4 [Net-(P1-Pad3)] of U2 on F.Cu @ (96.85, 138.81)
- Pad 4 [Net-(LED1-Pad4)] of LED1 on B.Cu @ (166.39, 132.02) ↔ Pad 1 [Net-(LED1-Pad4)] of R25 on F.Cu @ (149.64, 139.52)

## Board

- Source: https://github.com/linlun/HW-ESP8266_Roomba (author linlun, license None)
- 2 layers, 31 nets, 68 components, 100.0 × 30.03 mm, 4 zones
- Human reference layout: 835 segments, 101 vias, 1709.86 mm of track, 799 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/HW-ESP8266_Roomba_ESP8266_Roomba_panel_x_3/`](../../../../fixtures/PCBench/HW-ESP8266_Roomba_ESP8266_Roomba_panel_x_3/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 6 | None | 7 | 1.4 min | FAIL |
| 2.4.1 | COMPLETED | 3 | None | 7 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 15 | 0 | 0 | 2.1 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 15 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 15 | 0 | 0 | 10.5 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 4 | 0 | 0 | 12.8 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 4 | 0 | 0 | 8.4 min | FAIL |
| master-edffcabb | COMPLETED | 4 | 0 | 0 | 1.5 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `HW-ESP8266_Roomba_ESP8266_Roomba_panel_x_3--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
