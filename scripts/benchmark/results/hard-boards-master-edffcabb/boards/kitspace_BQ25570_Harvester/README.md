# kitspace_BQ25570_Harvester

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 2 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 950.6173 |
| Wall clock | 0.5 min |
| Connections (total / still open) | 54 / 2 |
| Autorouter stop reason | stagnation after 20 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **open-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 5 | 0 | 876.5 | stagnation | 0.1 min |
| direct-first | 8 | 0 | 802.5 | stagnation | 0.1 min |
| fanout-retry | 6 | 0 | 851.9 | stagnation | 0.1 min |
| short-escape | 7 | 0 | 827.2 | stagnation | 0.1 min |
| open-escape | 2 | 0 | 950.6 | stagnation | 0.1 min |
| no-fanout | 2 | 0 | 950.6 | stagnation | 0.0 min |
| continue | 2 | 0 | 950.6 | stagnation | 0.0 min |

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
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 6 | 7 |

Open connections reported by KiCad:

- Pad 3 [Net-(C4-Pad1)] of U1 on F.Cu @ (129.54, 90.55) ↔ Track [Net-(C4-Pad1)] on F.Cu, length 0.7142 mm @ (131.19, 89.90)
- Track [Net-(R3-Pad2)] on F.Cu, length 0.6392 mm @ (133.60, 85.05) ↔ Pad 10 [Net-(R3-Pad2)] of U1 on F.Cu @ (131.19, 87.90)

## Board

- Source: https://github.com/kitspace-forks/BQ25570_Harvester (author , license [{'name': 'License not found', 'link': ''}])
- 2 layers, 9 nets, 20 components, 12.7 × 20.83 mm, 6 zones
- Human reference layout: 176 segments, 10 vias, 193.99 mm of track, 127 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_BQ25570_Harvester/`](../../../../fixtures/PCBench/kitspace_BQ25570_Harvester/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 5 | None | 37 | 0.4 min | FAIL |
| 2.4.1 | COMPLETED | 6 | None | 40 | 1.0 min | FAIL |
| 2.5.0 | COMPLETED | 11 | 0 | 0 | 0.5 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 2 | 0 | 0 | 1.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 2 | 0 | 0 | 1.1 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 0 | 1.9 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 2 | 0 | 0 | 1.3 min | FAIL |
| master-edffcabb | COMPLETED | 2 | 0 | 0 | 0.5 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_BQ25570_Harvester--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
