# SpindleController_spindle_controller

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 1 |
| Clearance violations (total) | 4 |
| of which router-introduced | 0 |
| of which pre-existing in input | 4 (unfixable: 4) |
| Worst violation depth | 759.9105834960938 µm |
| Quality score | 988.18365 |
| Wall clock | 0.2 min |
| Connections (total / still open) | 143 / 1 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 1 | 0 | 988.2 | stagnation | 0.0 min |
| continue | 1 | 0 | 988.2 | stagnation | 0.0 min |
| direct-first | 1 | 0 | 988.2 | stagnation | 0.0 min |
| fanout-retry | 137 | 0 | 25.5 | repeated | 0.0 min |
| short-escape | 137 | 0 | 25.5 | repeated | 0.0 min |
| open-escape | 1 | 0 | 988.2 | stagnation | 0.0 min |
| no-fanout | 1 | 0 | 988.2 | stagnation | 0.0 min |

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
| clearance | 4 | 24 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Track [/VAC_L] on F.Cu, length 4.0640 mm @ (92.33, 145.67) ↔ PTH pad 2 [/VAC_L] of TRIAC1 @ (87.50, 140.02)

## Board

- Source: https://github.com/eflukx/SpindleController (author eflukx, license MIT)
- 2 layers, 28 nets, 69 components, 66.26 × 114.35 mm, 2 zones
- Human reference layout: 567 segments, 0 vias, 1790.12 mm of track, 157 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/SpindleController_spindle_controller/`](../../../../fixtures/PCBench/SpindleController_spindle_controller/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 1 | None | 5 | 0.4 min | FAIL |
| 2.4.1 | COMPLETED | 1 | None | 4 | 0.9 min | FAIL |
| 2.5.0 | COMPLETED | 1 | 0 | 4 | 0.5 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 1 | 0 | 4 | 2.5 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 1 | 0 | 4 | 2.1 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 4 | 0.4 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 4 | 0.5 min | FAIL |
| master-edffcabb | COMPLETED | 1 | 0 | 4 | 0.2 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `SpindleController_spindle_controller--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
