# retroreflectors_SALSAFLOCK

Tier **B** (routine). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 2 |
| of which router-introduced | 0 |
| of which pre-existing in input | 2 (unfixable: 2) |
| Worst violation depth | 50.70233154296875 µm |
| Quality score | 999.4087 |
| Wall clock | 0.2 min |
| Connections (total / still open) | 44 / 0 |
| Autorouter stop reason | completed after 5 passes |
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
| unconnected | 1 | 0 |
| clearance | 8 | 9 |
| hole_clearance | 0 | 0 |
| shorting_items | 2 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 7 | 4 |

Open connections reported by KiCad:

- Track [/SOURCE] on F.Cu, length 0.8166 mm @ (136.67, 99.60) ↔ Track [/SOURCE] on F.Cu, length 0.6621 mm @ (137.73, 99.60)

## Board

- Source: https://github.com/mossmann/retroreflectors (author , license [{'name': 'GNU General Public License v2.0', 'link': 'https://api.github.com/licenses/gpl-2.0'}])
- 2 layers, 0 nets, 15 components, 30.8 × 52.8 mm, 2 zones
- Human reference layout: 120 segments, 17 vias, 576.87 mm of track, 112 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/retroreflectors_SALSAFLOCK/`](../../../../fixtures/PCBench/retroreflectors_SALSAFLOCK/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 16 | None | 6 | 0.5 min | FAIL |
| 2.4.1 | COMPLETED | 13 | None | 14 | 1.2 min | FAIL |
| 2.5.0 | COMPLETED | 3 | 0 | 6 | 0.5 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 0 | 0 | 2 | 0.4 min | clean |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 2 | 0.4 min | clean |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 2 | 0.6 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 2 | 0.1 min | clean |
| master-edffcabb | COMPLETED | 0 | 0 | 2 | 0.2 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `retroreflectors_SALSAFLOCK--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
