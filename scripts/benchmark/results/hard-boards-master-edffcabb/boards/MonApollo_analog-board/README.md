# MonApollo_analog-board

Tier **D** (diagnostic). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 58 |
| of which router-introduced | 0 |
| of which pre-existing in input | 58 (unfixable: 58) |
| Worst violation depth | 25.30078125 µm |
| Quality score | 999.4207 |
| Wall clock | 144.2 min |
| Connections (total / still open) | 1190 / 0 |
| Autorouter stop reason | completed after 16 passes |
| Optimizer | no_improvement |

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
| clearance | 61 | 66 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

## Board

- Source: https://github.com/minisystem/MonApollo (author minisystem, license None)
- 2 layers, 303 nets, 506 components, 200.0 × 150.0 mm, 3 zones
- Human reference layout: 3858 segments, 262 vias, 16043.41 mm of track, 1346 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/MonApollo_analog-board/`](../../../../fixtures/PCBench/MonApollo_analog-board/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | FAILED | 11 | None | 65 | 33.0 min | FAIL |
| 2.4.1 | COMPLETED | 172 | None | 66 | 30.5 min | FAIL |
| 2.5.0 | TIMED_OUT | 0 | 0 | 58 | 45.5 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 108 | 0 | 58 | 45.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 58 | 365.6 min | clean |
| 2.6.0-RC3 | TIMED_OUT | 0 | 0 | 58 | 363.2 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 0 | 0 | 58 | 144.2 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `MonApollo_analog-board--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
