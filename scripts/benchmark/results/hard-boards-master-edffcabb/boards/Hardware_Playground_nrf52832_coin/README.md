# Hardware_Playground_nrf52832_coin

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 3 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 826.087 |
| Wall clock | 0.2 min |
| Connections (total / still open) | 23 / 3 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **continue**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 4 | 0 | 768.1 | stagnation | 0.0 min |
| continue | 3 | 0 | 826.1 | stagnation | 0.0 min |
| direct-first | 3 | 0 | 826.1 | stagnation | 0.0 min |
| fanout-retry | 4 | 0 | 768.1 | stagnation | 0.0 min |
| short-escape | 4 | 0 | 768.1 | stagnation | 0.0 min |
| open-escape | 3 | 0 | 826.1 | stagnation | 0.0 min |
| no-fanout | 4 | 0 | 768.1 | stagnation | 0.0 min |

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
| clearance | 8 | 8 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Track [GND] on F.Cu, length 0.3554 mm @ (142.49, 21.59) ↔ Pad 5 [GND] of U6 on F.Cu @ (143.74, 22.20)
- Pad 5 [GND] of U6 on F.Cu @ (143.74, 22.20) ↔ Pad 10 [GND] of U6 on F.Cu @ (145.31, 24.03)
- Pad 1 [GND] of C4 on F.Cu @ (147.07, 24.32) ↔ Pad 10 [GND] of U6 on F.Cu @ (145.31, 24.03)

## Board

- Source: https://github.com/mtiutiu/Hardware_Playground (author mtiutiu, license None)
- 2 layers, 0 nets, 20 components, 12.93 × 12.83 mm, 4 zones
- Human reference layout: 134 segments, 62 vias, 213.86 mm of track, 229 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/Hardware_Playground_nrf52832_coin/`](../../../../fixtures/PCBench/Hardware_Playground_nrf52832_coin/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 3 | None | 2 | 0.2 min | FAIL |
| 2.4.1 | COMPLETED | 3 | None | 1 | 0.3 min | FAIL |
| 2.5.0 | COMPLETED | 3 | 0 | 0 | 0.2 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 3 | 0 | 0 | 0.5 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 3 | 0 | 0 | 0.6 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 3 | 0 | 0 | 0.4 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 3 | 0 | 0 | 0.4 min | FAIL |
| master-edffcabb | COMPLETED | 3 | 0 | 0 | 0.2 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `Hardware_Playground_nrf52832_coin--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
