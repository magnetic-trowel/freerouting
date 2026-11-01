# Hardware_Playground_ws281x_led_strip_controller

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
| Quality score | 992.24805 |
| Wall clock | 1.3 min |
| Connections (total / still open) | 172 / 1 |
| Autorouter stop reason | stagnation after 19 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **short-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 4 | 0 | 969.0 | stagnation | 0.2 min |
| continue | 3 | 0 | 976.7 | stagnation | 0.1 min |
| direct-first | 5 | 0 | 961.2 | stagnation | 0.2 min |
| fanout-retry | 2 | 0 | 984.5 | stagnation | 0.1 min |
| short-escape | 1 | 0 | 992.2 | stagnation | 0.1 min |
| open-escape | 4 | 0 | 969.0 | stagnation | 0.3 min |
| no-fanout | 4 | 0 | 969.0 | stagnation | 0.2 min |
| continue | 1 | 0 | 992.2 | stagnation | 0.0 min |

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
| clearance | 31 | 13 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 3 | 5 |

Open connections reported by KiCad:

- Pad 10 [/SCK] of U3 on B.Cu @ (103.05, 79.65) ↔ Track [/SCK] on B.Cu, length 3.8700 mm @ (110.67, 83.80)

## Board

- Source: https://github.com/mtiutiu/Hardware_Playground (author mtiutiu, license None)
- 2 layers, 8 nets, 118 components, 44.8 × 23.35 mm, 3 zones
- Human reference layout: 315 segments, 32 vias, 561.06 mm of track, 437 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/Hardware_Playground_ws281x_led_strip_controller/`](../../../../fixtures/PCBench/Hardware_Playground_ws281x_led_strip_controller/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 5 | None | 11 | 0.8 min | FAIL |
| 2.4.1 | COMPLETED | 3 | None | 0 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 2 | 0 | 0 | 1.2 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 3 | 0 | 0 | 7.3 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 3 | 0 | 0 | 5.2 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 0 | 6.2 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 0 | 3.8 min | FAIL |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 1.3 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `Hardware_Playground_ws281x_led_strip_controller--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
