# domotics_base-board-arranged

Tier **C** (complex). Category: **router violations**: fully routed but the router added clearance violations. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 4 |
| of which router-introduced | 4 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 330.0 µm |
| Quality score | 999.75867 |
| Wall clock | 1.1 min |
| Connections (total / still open) | 457 / 0 |
| Autorouter stop reason | completed after 6 passes |
| Optimizer | no_improvement |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 0 | 0 |
| clearance | 3 | 0 |
| hole_clearance | 0 | 0 |
| shorting_items | 3 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

## Board

- Source: https://github.com/dmachtey/domotics (author dmachtey, license None)
- 2 layers, 99 nets, 115 components, 271.99 × 163.99 mm, 2 zones
- Human reference layout: 1012 segments, 0 vias, 15105.35 mm of track, 481 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/domotics_base-board-arranged/`](../../../../fixtures/PCBench/domotics_base-board-arranged/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 0 | None | 0 | 10.5 min | clean |
| 2.4.1 | COMPLETED | 0 | None | 4 | 3.5 min | clean |
| 2.5.0 | COMPLETED | 0 | 4 | 4 | 2.4 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 0 | 0 | 0 | 4.5 min | clean |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 0 | 4.1 min | clean |
| 2.6.0-RC3 | COMPLETED | 0 | 4 | 4 | 7.7 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 0 | 4 | 4 | 1.1 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `domotics_base-board-arranged--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
