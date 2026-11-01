# open-source-multimeter_board

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
| Quality score | 987.6543 |
| Wall clock | 2.6 min |
| Connections (total / still open) | 108 / 1 |
| Autorouter stop reason | stagnation after 29 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **continue**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 963.0 | stagnation | 0.6 min |
| continue | 1 | 0 | 987.7 | stagnation | 0.1 min |
| direct-first | 1 | 0 | 987.7 | stagnation | 0.1 min |
| fanout-retry | 5 | 0 | 938.3 | stagnation | 0.6 min |
| short-escape | 3 | 0 | 963.0 | stagnation | 0.6 min |
| open-escape | 6 | 0 | 925.9 | stagnation | 0.3 min |
| no-fanout | 6 | 0 | 925.9 | stagnation | 0.2 min |

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
| clearance | 0 | 1 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 1 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- PTH pad 20 [/PA4] of P102 @ (130.00, 115.72) ↔ Pad 20 [/PA4] of U101 on F.Cu @ (137.08, 106.73)

## Board

- Source: https://github.com/PreferLinux/open-source-multimeter (author PreferLinux, license GPL-3.0)
- 2 layers, 1 nets, 23 components, 29.72 × 83.06 mm, 6 zones
- Human reference layout: 463 segments, 4 vias, 1673.24 mm of track, 320 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/open-source-multimeter_board/`](../../../../fixtures/PCBench/open-source-multimeter_board/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 66 | None | 0 | 0.9 min | FAIL |
| 2.4.1 | COMPLETED | 65 | None | 0 | 1.1 min | FAIL |
| 2.5.0 | COMPLETED | 1 | 0 | 0 | 3.3 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 1 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 1 | 0 | 0 | 10.8 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 0 | 10.8 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 0 | 9.7 min | FAIL |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 2.6 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `open-source-multimeter_board--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
