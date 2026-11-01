# esp32-iot-uno_esp32-iot-uno-hw

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 1 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 993.9668 |
| Wall clock | 103.5 min |
| Connections (total / still open) | 221 / 1 |
| Autorouter stop reason | stagnation after 40 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **open-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 981.9 | stagnation | 1.9 min |
| continue | 3 | 0 | 981.9 | stagnation | 0.7 min |
| direct-first | 3 | 0 | 981.9 | stagnation | 14.6 min |
| fanout-retry | 2 | 0 | 987.9 | stagnation | 1.6 min |
| short-escape | 2 | 0 | 987.9 | stagnation | 81.5 min |
| open-escape | 1 | 0 | 994.0 | stagnation | 1.8 min |
| no-fanout | 4 | 0 | 975.9 | stagnation | 1.3 min |
| continue | 1 | 0 | 994.0 | stagnation | 0.1 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 1 | 0 |
| clearance | 36 | 60 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 30 | 2 |

Open connections reported by KiCad:

- Pad 2 [+3V3] of R26 on F.Cu @ (158.22, 83.95) ↔ Pad 4 [+3V3] of S1 on F.Cu @ (163.49, 84.21)

## Board

- Source: https://github.com/esp32vn/esp32-iot-uno (author esp32vn, license None)
- 2 layers, 38 nets, 105 components, 68.53 × 53.3 mm, 2 zones
- Human reference layout: 1316 segments, 117 vias, 3053.1 mm of track, 690 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/esp32-iot-uno_esp32-iot-uno-hw/`](../../../../fixtures/PCBench/esp32-iot-uno_esp32-iot-uno-hw/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 17 | None | 44 | 5.1 min | FAIL |
| 2.4.1 | TIMED_OUT | 20 | None | 12 | 5.2 min | FAIL |
| 2.5.0 | COMPLETED | 4 | 0 | 4 | 7.6 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 2 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 0 | 23.0 min | clean |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 0 | 72.2 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 0 | 35.9 min | FAIL |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 103.5 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `esp32-iot-uno_esp32-iot-uno-hw--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
