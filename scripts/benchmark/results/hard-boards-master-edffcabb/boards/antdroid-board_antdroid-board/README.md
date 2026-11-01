# antdroid-board_antdroid-board

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
| Quality score | 990.40765 |
| Wall clock | 0.7 min |
| Connections (total / still open) | 139 / 1 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **direct-first**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 2 | 0 | 980.8 | stagnation | 0.1 min |
| continue | 2 | 0 | 980.8 | stagnation | 0.1 min |
| direct-first | 1 | 0 | 990.4 | stagnation | 0.1 min |
| fanout-retry | 111 | 0 | 134.3 | repeated | 0.0 min |
| short-escape | 2 | 0 | 980.8 | stagnation | 0.1 min |
| open-escape | 2 | 0 | 980.8 | stagnation | 0.1 min |
| no-fanout | 1 | 0 | 990.4 | stagnation | 0.1 min |
| continue | 1 | 0 | 990.4 | stagnation | 0.1 min |

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
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Pad 1 [+5VA] of R7 on F.Cu @ (188.00, 101.85) ↔ Track [+5VA] on F.Cu, length 3.5560 mm @ (188.85, 96.01)

## Board

- Source: https://github.com/antdroid-hexapod/antdroid-board (author antdroid-hexapod, license CC-BY-SA-4.0)
- 2 layers, 62 nets, 60 components, 99.45 × 53.72 mm, 2 zones
- Human reference layout: 275 segments, 2 vias, 1110.04 mm of track, 268 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/antdroid-board_antdroid-board/`](../../../../fixtures/PCBench/antdroid-board_antdroid-board/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 12 | None | 0 | 0.5 min | FAIL |
| 2.4.1 | COMPLETED | 22 | None | 0 | 2.0 min | FAIL |
| 2.5.0 | COMPLETED | 3 | 0 | 0 | 1.2 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 2 | 0 | 0 | 2.7 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 2 | 0 | 0 | 2.1 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 0 | 2.0 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 0 | 1.7 min | FAIL |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 0.7 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `antdroid-board_antdroid-board--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
