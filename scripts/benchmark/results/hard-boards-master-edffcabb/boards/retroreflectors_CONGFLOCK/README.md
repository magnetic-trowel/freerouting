# retroreflectors_CONGFLOCK

Tier **B** (routine). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 4 |
| of which router-introduced | 0 |
| of which pre-existing in input | 4 (unfixable: 4) |
| Worst violation depth | 50.70233154296875 µm |
| Quality score | 995.26935 |
| Wall clock | 0.1 min |
| Connections (total / still open) | 14 / 0 |
| Autorouter stop reason | completed after 17 passes |
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
| clearance | 11 | 10 |
| hole_clearance | 0 | 0 |
| shorting_items | 6 | 4 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 10 | 9 |

## Board

- Source: https://github.com/mossmann/retroreflectors (author , license [{'name': 'GNU General Public License v2.0', 'link': 'https://api.github.com/licenses/gpl-2.0'}])
- 2 layers, 0 nets, 7 components, 6.35 × 6.35 mm, 0 zones
- Human reference layout: 21 segments, 0 vias, 12.23 mm of track, 63 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/retroreflectors_CONGFLOCK/`](../../../../fixtures/PCBench/retroreflectors_CONGFLOCK/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 5 | None | 12 | 0.1 min | FAIL |
| 2.4.1 | COMPLETED | 6 | None | 17 | 0.1 min | FAIL |
| 2.5.0 | COMPLETED | 4 | 0 | 8 | 0.1 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 0 | 0 | 4 | 0.1 min | clean |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 4 | 0.1 min | clean |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 4 | 0.6 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 4 | 0.1 min | clean |
| master-edffcabb | COMPLETED | 0 | 0 | 4 | 0.1 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `retroreflectors_CONGFLOCK--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
