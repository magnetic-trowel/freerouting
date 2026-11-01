# MonApollo_digital-board

Tier **D** (diagnostic). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 9 |
| of which router-introduced | 0 |
| of which pre-existing in input | 9 (unfixable: 9) |
| Worst violation depth | 25.393798828125 µm |
| Quality score | 999.86975 |
| Wall clock | 58.6 min |
| Connections (total / still open) | 632 / 0 |
| Autorouter stop reason | completed after 17 passes |
| Optimizer | no_improvement |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **direct-first**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 1 | 0 | 997.8 | stagnation | 7.9 min |
| continue | 1 | 0 | 997.8 | stagnation | 1.6 min |
| direct-first | 0 | 0 | 999.9 | completed | 6.8 min |

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
| clearance | 10 | 24 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 6 | 6 |

## Board

- Source: https://github.com/minisystem/MonApollo (author minisystem, license None)
- 2 layers, 227 nets, 240 components, 200.0 × 150.0 mm, 2 zones
- Human reference layout: 3340 segments, 165 vias, 16294.96 mm of track, 1297 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/MonApollo_digital-board/`](../../../../fixtures/PCBench/MonApollo_digital-board/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 5 | None | 9 | 15.7 min | FAIL |
| 2.4.1 | TIMED_OUT | 85 | None | 21 | 15.2 min | FAIL |
| 2.5.0 | TIMED_OUT | 66 | 0 | 9 | 20.2 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 48 | 0 | 9 | 20.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 9 | 181.1 min | clean |
| 2.6.0-RC3 | TIMED_OUT | 0 | 0 | 9 | 361.1 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 0 | 0 | 9 | 58.6 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `MonApollo_digital-board--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
