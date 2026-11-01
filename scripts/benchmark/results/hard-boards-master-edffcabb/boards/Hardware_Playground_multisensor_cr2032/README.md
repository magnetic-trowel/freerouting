# Hardware_Playground_multisensor_cr2032

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 1 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 993.4959 |
| Wall clock | 2.8 min |
| Connections (total / still open) | 205 / 1 |
| Autorouter stop reason | stagnation after 28 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **open-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 6 | 0 | 961.0 | stagnation | 0.4 min |
| direct-first | 7 | 0 | 954.5 | stagnation | 0.4 min |
| fanout-retry | 4 | 0 | 974.0 | stagnation | 0.4 min |
| short-escape | 2 | 0 | 987.0 | stagnation | 0.4 min |
| open-escape | 1 | 0 | 993.5 | stagnation | 0.4 min |
| no-fanout | 3 | 0 | 980.5 | stagnation | 0.5 min |
| continue | 1 | 0 | 993.5 | stagnation | 0.1 min |

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
| clearance | 26 | 20 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 17 | 23 |

Open connections reported by KiCad:

- Pad 32 [/RFM69W_INT] of IC1 on F.Cu @ (119.49, 72.52) ↔ Pad 2 [/RFM69W_INT] of U4 on F.Cu @ (126.75, 94.11)

## Board

- Source: https://github.com/mtiutiu/Hardware_Playground (author mtiutiu, license None)
- 2 layers, 6 nets, 147 components, 14.99 × 9.91 mm, 7 zones
- Human reference layout: 417 segments, 39 vias, 727.64 mm of track, 558 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/Hardware_Playground_multisensor_cr2032/`](../../../../fixtures/PCBench/Hardware_Playground_multisensor_cr2032/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 22 | None | 29 | 2.0 min | FAIL |
| 2.4.1 | COMPLETED | 10 | None | 19 | 4.9 min | FAIL |
| 2.5.0 | COMPLETED | 4 | 0 | 0 | 2.2 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 2 | 0 | 0 | 8.8 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 2 | 0 | 0 | 7.5 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 4 | 0 | 0 | 16.2 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 2.8 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `Hardware_Playground_multisensor_cr2032--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
