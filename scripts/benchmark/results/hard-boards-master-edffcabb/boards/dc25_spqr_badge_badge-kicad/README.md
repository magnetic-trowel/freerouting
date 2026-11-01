# dc25_spqr_badge_badge-kicad

Tier **C** (complex). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 4 |
| of which router-introduced | 0 |
| of which pre-existing in input | 4 (unfixable: 4) |
| Worst violation depth | 200.0 µm |
| Quality score | 999.6163 |
| Wall clock | 9.6 min |
| Connections (total / still open) | 281 / 0 |
| Autorouter stop reason | completed after 9 passes |
| Optimizer | no_improvement |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **direct-first**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 2 | 0 | 990.1 | stagnation | 1.7 min |
| continue | 2 | 0 | 990.1 | stagnation | 0.8 min |
| direct-first | 0 | 0 | 999.6 | completed | 0.7 min |

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
| clearance | 126 | 186 |
| hole_clearance | 0 | 3 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 7 | 16 |

## Board

- Source: https://github.com/netik/dc25_spqr_badge (author netik, license Apache-2.0)
- 2 layers, 124 nets, 129 components, 152.67 × 76.68 mm, 2 zones
- Human reference layout: 1419 segments, 96 vias, 4462.82 mm of track, 783 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/dc25_spqr_badge_badge-kicad/`](../../../../fixtures/PCBench/dc25_spqr_badge_badge-kicad/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 2 | None | 60 | 4.2 min | FAIL |
| 2.4.1 | COMPLETED | 19 | None | 4 | 5.2 min | FAIL |
| 2.5.0 | TIMED_OUT | 2 | 0 | 4 | 10.2 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 5 | 0 | 4 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 4 | 49.7 min | clean |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 4 | 68.3 min | clean |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 0 | 0 | 4 | 9.6 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `dc25_spqr_badge_badge-kicad--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
