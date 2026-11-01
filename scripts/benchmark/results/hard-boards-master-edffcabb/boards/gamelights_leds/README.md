# gamelights_leds

Tier **B** (routine). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 2 |
| of which router-introduced | 0 |
| of which pre-existing in input | 2 (unfixable: 2) |
| Worst violation depth | 0.0 µm |
| Quality score | 999.5763 |
| Wall clock | 0.1 min |
| Connections (total / still open) | 30 / 0 |
| Autorouter stop reason | completed after 2 passes |
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
| clearance | 0 | 0 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 19 | 17 |

## Board

- Source: https://github.com/skorokithakis/gamelights (author skorokithakis, license BSD-2-Clause)
- 2 layers, 3 nets, 15 components, 24.3 × 24.0 mm, 0 zones
- Human reference layout: 125 segments, 10 vias, 239.4 mm of track, 66 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/gamelights_leds/`](../../../../fixtures/PCBench/gamelights_leds/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 0 | None | 14 | 0.5 min | clean |
| 2.4.1 | COMPLETED | 1 | None | 24 | 0.5 min | FAIL |
| 2.5.0 | COMPLETED | 1 | 0 | 0 | 0.1 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 0 | 0 | 0 | 0.2 min | clean |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 0 | 0.3 min | clean |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 2 | 0.3 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 2 | 0.1 min | clean |
| master-edffcabb | COMPLETED | 0 | 0 | 2 | 0.1 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `gamelights_leds--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
