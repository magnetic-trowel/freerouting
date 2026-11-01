# kitspace_CH330

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
| Quality score | 944.44446 |
| Wall clock | 0.2 min |
| Connections (total / still open) | 24 / 1 |
| Autorouter stop reason | stagnation after 25 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 1 | 0 | 944.4 | stagnation | 0.0 min |
| continue | 1 | 0 | 944.4 | stagnation | 0.0 min |
| direct-first | 3 | 0 | 833.3 | stagnation | 0.0 min |
| fanout-retry | 3 | 0 | 833.3 | stagnation | 0.0 min |
| short-escape | 1 | 0 | 944.4 | stagnation | 0.0 min |
| open-escape | 3 | 0 | 833.3 | stagnation | 0.0 min |
| no-fanout | 1 | 0 | 944.4 | stagnation | 0.0 min |

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
| clearance | 0 | 0 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 13 | 11 |

Open connections reported by KiCad:

- Pad 2 [VBUS] of D2 on F.Cu @ (186.71, 76.30) ↔ Track [VBUS] on B.Cu, length 1.1775 mm @ (189.21, 81.30)

## Board

- Source: https://github.com/Jana-Marie/CH330_Hardware (author , license [{'name': 'GNU', 'link': 'https://raw.githubusercontent.com/Jana-Marie/CH330_Hardware/master/LICENSE'}])
- 2 layers, 3 nets, 9 components, 10.0 × 10.0 mm, 3 zones
- Human reference layout: 43 segments, 4 vias, 46.01 mm of track, 40 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_CH330/`](../../../../fixtures/PCBench/kitspace_CH330/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 4 | None | 0 | 0.2 min | FAIL |
| 2.4.1 | COMPLETED | 2 | None | 12 | 0.4 min | FAIL |
| 2.5.0 | COMPLETED | 3 | 0 | 0 | 0.2 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 2 | 0 | 0 | 0.5 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 2 | 0 | 0 | 0.4 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 0 | 0.4 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 0 | 0.2 min | clean |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 0.2 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_CH330--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
