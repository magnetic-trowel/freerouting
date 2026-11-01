# HW-ESP8266_Roomba_ESP8266_Roomba

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 2 |
| Clearance violations (total) | 17 |
| of which router-introduced | 0 |
| of which pre-existing in input | 17 (unfixable: 17) |
| Worst violation depth | 0.0 µm |
| Quality score | 984.4858 |
| Wall clock | 2.2 min |
| Connections (total / still open) | 181 / 2 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **continue**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 4 | 0 | 969.8 | stagnation | 0.5 min |
| continue | 2 | 0 | 984.5 | stagnation | 0.1 min |
| direct-first | 6 | 0 | 955.0 | stagnation | 0.4 min |
| fanout-retry | 4 | 0 | 969.8 | stagnation | 0.4 min |
| short-escape | 4 | 0 | 969.8 | stagnation | 0.4 min |
| open-escape | 3 | 0 | 977.1 | stagnation | 0.1 min |
| no-fanout | 4 | 0 | 969.8 | stagnation | 0.3 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 2 | 0 |
| clearance | 0 | 0 |
| hole_clearance | 2 | 2 |
| shorting_items | 0 | 1 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 94 | 79 |

Open connections reported by KiCad:

- Pad 4 [Net-(R18-Pad1)] of ROT_ENC1 on F.Cu @ (117.29, 82.58) ↔ Pad 1 [Net-(R18-Pad1)] of SW1 on F.Cu @ (114.29, 85.08)
- Track [GND] on B.Cu, length 7.7529 mm @ (107.28, 75.70) ↔ Pad 2 [GND] of SW1 on F.Cu @ (106.29, 85.08)

## Board

- Source: https://github.com/linlun/HW-ESP8266_Roomba (author linlun, license None)
- 2 layers, 31 nets, 68 components, 100.0 × 30.01 mm, 4 zones
- Human reference layout: 844 segments, 101 vias, 1711.4 mm of track, 779 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/HW-ESP8266_Roomba_ESP8266_Roomba/`](../../../../fixtures/PCBench/HW-ESP8266_Roomba_ESP8266_Roomba/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 5 | None | 58 | 1.5 min | FAIL |
| 2.4.1 | TIMED_OUT | 4 | None | 58 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 20 | 0 | 0 | 2.7 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 17 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 14 | 0 | 0 | 11.9 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 4 | 0 | 17 | 17.5 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 4 | 0 | 17 | 11.1 min | FAIL |
| master-edffcabb | COMPLETED | 2 | 0 | 17 | 2.2 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `HW-ESP8266_Roomba_ESP8266_Roomba--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
