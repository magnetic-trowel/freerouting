# KiCad-Library_Teensy_test_layout

Tier **D** (diagnostic). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 1 |
| Clearance violations (total) | 79 |
| of which router-introduced | 0 |
| of which pre-existing in input | 79 (unfixable: 79) |
| Worst violation depth | 76.10000610351562 µm |
| Quality score | 998.4137 |
| Wall clock | 116.3 min |
| Connections (total / still open) | 1276 / 1 |
| Autorouter stop reason | stagnation after 34 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **direct-first**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 12 | 0 | 986.9 | stagnation | 19.5 min |
| direct-first | 1 | 0 | 998.4 | stagnation | 13.3 min |
| fanout-retry | 9 | 0 | 990.1 | stagnation | 18.0 min |
| short-escape | 10 | 0 | 989.0 | stagnation | 25.4 min |
| open-escape | 3 | 0 | 996.3 | stagnation | 13.2 min |
| no-fanout | 12 | 0 | 986.9 | stagnation | 24.5 min |
| continue | 1 | 0 | 998.4 | stagnation | 2.1 min |

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
| clearance | 79 | 86 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Pad 10 [Net-(C14-Pad1)] of U5 on F.Cu @ (158.09, 90.94) ↔ Pad 1 [Net-(C14-Pad1)] of C14 on F.Cu @ (159.91, 89.53)

## Board

- Source: https://github.com/marshalltaylorSFE/KiCad-Library (author marshalltaylorSFE, license None)
- 2 layers, 248 nets, 1441 components, 254.0 × 127.0 mm, 19 zones
- Human reference layout: 4357 segments, 374 vias, 11571.69 mm of track, 1413 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/KiCad-Library_Teensy_test_layout/`](../../../../fixtures/PCBench/KiCad-Library_Teensy_test_layout/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | FAILED | 19 | None | 111 | 33.0 min | FAIL |
| 2.4.1 | TIMED_OUT | 85 | None | 111 | 30.4 min | FAIL |
| 2.5.0 | TIMED_OUT | 16 | 0 | 79 | 45.3 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 42 | 0 | 79 | 45.3 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 25 | 0 | 79 | 324.7 min | FAIL |
| 2.6.0-RC3 | TIMED_OUT | 3 | 0 | 79 | 360.6 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 1 | 0 | 79 | 116.3 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `KiCad-Library_Teensy_test_layout--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
