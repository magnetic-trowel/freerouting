# BeaconBuddy_BeaconBuddy

Tier **B** (routine). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 1 |
| of which router-introduced | 0 |
| of which pre-existing in input | 1 (unfixable: 1) |
| Worst violation depth | 38.2 µm |
| Quality score | 999.9227 |
| Wall clock | 0.4 min |
| Connections (total / still open) | 184 / 0 |
| Autorouter stop reason | completed after 7 passes |
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
| clearance | 0 | 4 |
| hole_clearance | 0 | 6 |
| shorting_items | 1 | 1 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 20 | 36 |

## Board

- Source: https://github.com/angst7/BeaconBuddy (author angst7, license None)
- 2 layers, 37 nets, 112 components, 25.4 × 27.94 mm, 2 zones
- Human reference layout: 256 segments, 8 vias, 339.62 mm of track, 244 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/BeaconBuddy_BeaconBuddy/`](../../../../fixtures/PCBench/BeaconBuddy_BeaconBuddy/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 5 | None | 111 | 1.1 min | FAIL |
| 2.4.1 | COMPLETED | 1 | None | 141 | 3.8 min | FAIL |
| 2.5.0 | COMPLETED | 2 | 0 | 1 | 1.3 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 0 | 0 | 1 | 1.2 min | clean |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 1 | 0.5 min | clean |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 1 | 3.3 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 1 | 0.5 min | clean |
| master-edffcabb | COMPLETED | 0 | 0 | 1 | 0.4 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `BeaconBuddy_BeaconBuddy--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
