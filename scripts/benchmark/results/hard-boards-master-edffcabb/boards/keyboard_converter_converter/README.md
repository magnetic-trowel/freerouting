# keyboard_converter_converter

Tier **B** (routine). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 19 |
| of which router-introduced | 0 |
| of which pre-existing in input | 19 (unfixable: 19) |
| Worst violation depth | 53.9036865234375 µm |
| Quality score | 997.6762 |
| Wall clock | 1.7 min |
| Connections (total / still open) | 112 / 0 |
| Autorouter stop reason | completed after 22 passes |
| Optimizer | no_improvement |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **short-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 1 | 0 | 985.8 | stagnation | 0.2 min |
| continue | 1 | 0 | 985.8 | stagnation | 0.0 min |
| direct-first | 1 | 0 | 985.8 | stagnation | 0.1 min |
| fanout-retry | 1 | 0 | 985.8 | stagnation | 0.3 min |
| short-escape | 0 | 0 | 997.7 | completed | 0.2 min |

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
| clearance | 0 | 1 |
| hole_clearance | 0 | 0 |
| shorting_items | 1 | 1 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 3 | 4 |

## Board

- Source: https://github.com/tmk/keyboard_converter (author tmk, license None)
- 2 layers, 9 nets, 61 components, 26.0 × 37.0 mm, 3 zones
- Human reference layout: 497 segments, 26 vias, 726.51 mm of track, 406 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/keyboard_converter_converter/`](../../../../fixtures/PCBench/keyboard_converter_converter/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 4 | None | 36 | 0.5 min | FAIL |
| 2.4.1 | COMPLETED | 5 | None | 23 | 2.7 min | FAIL |
| 2.5.0 | COMPLETED | 2 | 0 | 19 | 1.0 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 1 | 0 | 19 | 6.6 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 1 | 0 | 19 | 4.4 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 19 | 4.2 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 19 | 3.9 min | clean |
| master-edffcabb | COMPLETED | 0 | 0 | 19 | 1.7 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `keyboard_converter_converter--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
