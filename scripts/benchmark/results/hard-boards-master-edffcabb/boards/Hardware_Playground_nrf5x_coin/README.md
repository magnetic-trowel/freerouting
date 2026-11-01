# Hardware_Playground_nrf5x_coin

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 10 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 806.7633 |
| Wall clock | 1.6 min |
| Connections (total / still open) | 69 / 10 |
| Autorouter stop reason | stagnation after 22 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **open-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 13 | 0 | 748.8 | stagnation | 0.3 min |
| direct-first | 11 | 0 | 787.4 | stagnation | 0.3 min |
| fanout-retry | 12 | 0 | 768.1 | stagnation | 0.2 min |
| short-escape | 14 | 0 | 729.5 | stagnation | 0.3 min |
| open-escape | 10 | 0 | 806.8 | stagnation | 0.2 min |
| no-fanout | 14 | 0 | 729.5 | stagnation | 0.2 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 10 | 0 |
| clearance | 11 | 26 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 3 |

Open connections reported by KiCad:

- Pad P$2 [P0.09] of U1 on F.Cu @ (121.29, 32.26) ↔ Pad 9 [P0.09] of U6 on F.Cu @ (128.68, 41.81)
- PTH pad 1 [P0.09] of P0.09 @ (129.29, 24.38) ↔ Pad P$2 [P0.09] of U1 on F.Cu @ (121.29, 32.26)
- Track [GND] on F.Cu, length 1.7780 mm @ (127.51, 36.89) ↔ Pad 5 [GND] of U6 on F.Cu @ (127.48, 39.98)
- Pad 10 [GND] of U6 on F.Cu @ (129.06, 41.81) ↔ Pad 5 [GND] of U6 on F.Cu @ (127.48, 39.98)
- Pad 2 [GND] of C6 on F.Cu @ (130.24, 43.43) ↔ Pad 10 [GND] of U6 on F.Cu @ (129.06, 41.81)
- Track [+3V] on F.Cu, length 0.9893 mm @ (128.27, 42.80) ↔ Pad 2 [+3V] of U6 on F.Cu @ (128.68, 39.98)
- Track [P0.27] on B.Cu, length 2.1694 mm @ (129.45, 47.12) ↔ Pad 7 [P0.27] of U6 on F.Cu @ (127.86, 41.81)
- Track [P0.28] on B.Cu, length 7.3376 mm @ (115.61, 39.07) ↔ Pad 6 [P0.28] of U6 on F.Cu @ (127.48, 41.81)
- Track [P0.28] on F.Cu, length 1.1712 mm @ (121.29, 47.50) ↔ Pad 1 [P0.28] of U5 on F.Cu @ (129.05, 50.75)
- Pad 6 [P0.28] of U3 on F.Cu @ (132.18, 53.24) ↔ Pad 1 [P0.28] of U5 on F.Cu @ (129.05, 50.75)

## Board

- Source: https://github.com/mtiutiu/Hardware_Playground (author mtiutiu, license None)
- 2 layers, 0 nets, 41 components, 11.31 × 14.33 mm, 4 zones
- Human reference layout: 286 segments, 60 vias, 640.26 mm of track, 262 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/Hardware_Playground_nrf5x_coin/`](../../../../fixtures/PCBench/Hardware_Playground_nrf5x_coin/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 8 | None | 10 | 0.8 min | FAIL |
| 2.4.1 | COMPLETED | 7 | None | 0 | 1.8 min | FAIL |
| 2.5.0 | COMPLETED | 18 | 0 | 0 | 1.5 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 18 | 0 | 0 | 4.9 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 18 | 0 | 0 | 4.5 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 10 | 0 | 0 | 7.3 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 10 | 0 | 0 | 3.6 min | FAIL |
| master-edffcabb | COMPLETED | 10 | 0 | 0 | 1.6 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `Hardware_Playground_nrf5x_coin--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
