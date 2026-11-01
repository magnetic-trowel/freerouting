# medusa_medusa_cape

Tier **C** (complex). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 59 |
| of which router-introduced | 0 |
| of which pre-existing in input | 59 (unfixable: 59) |
| Worst violation depth | 3.1005859375 µm |
| Quality score | 999.2226 |
| Wall clock | 34.1 min |
| Connections (total / still open) | 329 / 0 |
| Autorouter stop reason | completed after 9 passes |
| Optimizer | no_improvement |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **short-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 987.1 | stagnation | 5.2 min |
| continue | 2 | 0 | 991.1 | stagnation | 2.3 min |
| direct-first | 3 | 0 | 987.1 | stagnation | 3.6 min |
| fanout-retry | 3 | 0 | 987.1 | stagnation | 2.8 min |
| short-escape | 0 | 0 | 999.2 | completed | 1.7 min |

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
| clearance | 137 | 148 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 2 | 8 |

## Board

- Source: https://github.com/sharebrained/medusa (author sharebrained, license GPL-2.0)
- 4 layers, 0 nets, 73 components, 54.61 × 86.36 mm, 15 zones
- Human reference layout: 806 segments, 162 vias, 1934.96 mm of track, 570 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/medusa_medusa_cape/`](../../../../fixtures/PCBench/medusa_medusa_cape/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 3 | None | 175 | 5.8 min | FAIL |
| 2.4.1 | COMPLETED | 70 | None | 97 | 5.1 min | FAIL |
| 2.5.0 | TIMED_OUT | 33 | 0 | 59 | 10.1 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 24 | 1 | 60 | 10.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 1 | 0 | 59 | 69.9 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 59 | 60.4 min | clean |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 0 | 0 | 59 | 34.1 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `medusa_medusa_cape--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
