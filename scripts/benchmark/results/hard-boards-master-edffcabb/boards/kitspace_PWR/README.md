# kitspace_PWR

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 2 |
| Clearance violations (total) | 4 |
| of which router-introduced | 0 |
| of which pre-existing in input | 4 (unfixable: 4) |
| Worst violation depth | 364.9139404296875 µm |
| Quality score | 907.3231 |
| Wall clock | 0.2 min |
| Connections (total / still open) | 30 / 2 |
| Autorouter stop reason | stagnation after 25 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **continue**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 4 | 0 | 818.4 | stagnation | 0.1 min |
| continue | 2 | 0 | 907.3 | stagnation | 0.0 min |

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
| clearance | 6 | 8 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 3 | 0 |

Open connections reported by KiCad:

- PTH pad 14 [GND] of U3 @ (128.27, 88.90) ↔ PTH pad 10 [GND] of U3 @ (128.27, 99.06)
- PTH pad 10 [GND] of U3 @ (128.27, 99.06) ↔ Track [GND] on F.Cu, length 4.0294 mm @ (139.70, 103.64)

## Board

- Source: https://github.com/nerdyscout/NodeMCU (author , license [{'name': 'Creative Commons', 'link': 'https://raw.githubusercontent.com/nerdyscout/NodeMCU/master/lib/LICENSE.md'}, {'name': 'CERN', 'link': 'https://raw.githubusercontent.com/nerdyscout/NodeMCU/master/CO2/LICENSE.TXT'}])
- 2 layers, 10 nets, 19 components, 26.67 × 53.34 mm, 1 zones
- Human reference layout: 59 segments, 0 vias, 260.43 mm of track, 245 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_PWR/`](../../../../fixtures/PCBench/kitspace_PWR/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 4 | None | 6 | 0.3 min | FAIL |
| 2.4.1 | COMPLETED | 5 | None | 6 | 0.4 min | FAIL |
| 2.5.0 | COMPLETED | 5 | 0 | 4 | 0.5 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 5 | 0 | 4 | 0.4 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 5 | 0 | 4 | 0.6 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 4 | 0 | 4 | 0.3 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 2 | 0 | 4 | 0.3 min | FAIL |
| master-edffcabb | COMPLETED | 2 | 0 | 4 | 0.2 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_PWR--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
