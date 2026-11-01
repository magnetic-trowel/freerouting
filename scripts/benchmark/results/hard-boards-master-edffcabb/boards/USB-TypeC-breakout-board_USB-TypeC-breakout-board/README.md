# USB-TypeC-breakout-board_USB-TypeC-breakout-board

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
| Quality score | 960.7843 |
| Wall clock | 0.7 min |
| Connections (total / still open) | 34 / 1 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **open-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 882.4 | stagnation | 0.2 min |
| continue | 3 | 0 | 882.4 | stagnation | 0.1 min |
| direct-first | 5 | 0 | 803.9 | stagnation | 0.1 min |
| fanout-retry | 25 | 0 | 176.5 | repeated | 0.0 min |
| short-escape | 4 | 0 | 843.1 | stagnation | 0.1 min |
| open-escape | 1 | 0 | 960.8 | stagnation | 0.1 min |
| no-fanout | 4 | 0 | 843.1 | stagnation | 0.1 min |
| continue | 1 | 0 | 960.8 | stagnation | 0.0 min |

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
| hole_clearance | 2 | 3 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Pad B11 [Net-(P1-Pad4)] of P2 on F.Cu @ (135.10, 95.22) ↔ PTH pad 4 [Net-(P1-Pad4)] of P1 @ (160.02, 85.09)

## Board

- Source: https://github.com/perillamint/USB-TypeC-breakout-board (author perillamint, license NOASSERTION)
- 2 layers, 16 nets, 2 components, 50.0 × 50.0 mm, 2 zones
- Human reference layout: 115 segments, 4 vias, 537.74 mm of track, 86 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/USB-TypeC-breakout-board_USB-TypeC-breakout-board/`](../../../../fixtures/PCBench/USB-TypeC-breakout-board_USB-TypeC-breakout-board/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 4 | None | 0 | 0.4 min | FAIL |
| 2.4.1 | COMPLETED | 3 | None | 0 | 1.1 min | FAIL |
| 2.5.0 | COMPLETED | 0 | 0 | 0 | 0.7 min | clean |
| 2.6.0-RC1 | COMPLETED | 0 | 0 | 0 | 0.9 min | clean |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 0 | 0.5 min | clean |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 0 | 3.0 min | clean |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 0 | 1.8 min | FAIL |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 0.7 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `USB-TypeC-breakout-board_USB-TypeC-breakout-board--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
