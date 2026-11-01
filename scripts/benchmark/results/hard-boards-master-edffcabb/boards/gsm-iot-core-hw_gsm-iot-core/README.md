# gsm-iot-core-hw_gsm-iot-core

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 3 |
| Clearance violations (total) | 1 |
| of which router-introduced | 1 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 100.0 µm |
| Quality score | 983.92664 |
| Wall clock | 338.8 min |
| Connections (total / still open) | 250 / 3 |
| Autorouter stop reason | stagnation after 39 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **open-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 15 | 1 | 919.9 | stagnation | 1.4 min |
| direct-first | 7 | 1 | 962.6 | stagnation | 1.0 min |
| fanout-retry | 76 | 0 | 594.7 | repeated | 0.1 min |
| short-escape | 9 | 0 | 952.0 | stagnation | 332.8 min |
| open-escape | 3 | 1 | 983.9 | stagnation | 1.4 min |
| no-fanout | 13 | 0 | 930.7 | stagnation | 1.3 min |
| continue | 3 | 1 | 983.9 | stagnation | 0.4 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 3 | 0 |
| clearance | 0 | 0 |
| hole_clearance | 3 | 8 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 44 | 42 |

Open connections reported by KiCad:

- Track [GND] on B.Cu, length 1.0478 mm @ (167.77, 113.43) ↔ Pad 2 [GND] of R8 on F.Cu @ (166.37, 112.23)
- Pad 39 [PC8_SDIO_D0] of U3 on F.Cu @ (156.67, 105.05) ↔ Track [PC8_SDIO_D0] on B.Cu, length 1.5192 mm @ (164.42, 113.79)
- Pad 5 [PC12_SDIO_CK] of J1 on B.Cu @ (162.22, 113.79) ↔ Pad 53 [PC12_SDIO_CK] of U3 on F.Cu @ (164.02, 106.82)

## Board

- Source: https://github.com/iotmakervn/gsm-iot-core-hw (author iotmakervn, license None)
- 2 layers, 34 nets, 92 components, 25.4 × 64.77 mm, 2 zones
- Human reference layout: 1116 segments, 114 vias, 1792.36 mm of track, 605 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/gsm-iot-core-hw_gsm-iot-core/`](../../../../fixtures/PCBench/gsm-iot-core-hw_gsm-iot-core/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 5 | None | 7 | 4.4 min | FAIL |
| 2.4.1 | COMPLETED | 33 | None | 76 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 11 | 0 | 0 | 9.1 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 5 | 1 | 1 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 5 | 0 | 0 | 33.3 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 5 | 0 | 0 | 72.5 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 3 | 1 | 1 | 338.8 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `gsm-iot-core-hw_gsm-iot-core--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
