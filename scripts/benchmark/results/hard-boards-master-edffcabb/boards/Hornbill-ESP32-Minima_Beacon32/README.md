# Hornbill-ESP32-Minima_Beacon32

Tier **B** (routine). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 30 |
| of which router-introduced | 0 |
| of which pre-existing in input | 30 (unfixable: 30) |
| Worst violation depth | 0.0 µm |
| Quality score | 997.74097 |
| Wall clock | 0.4 min |
| Connections (total / still open) | 116 / 0 |
| Autorouter stop reason | completed after 4 passes |
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
| clearance | 60 | 60 |
| hole_clearance | 0 | 0 |
| shorting_items | 15 | 30 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 160 | 176 |

## Board

- Source: https://github.com/ExploreEmbedded/Hornbill-ESP32-Minima (author ExploreEmbedded, license None)
- 2 layers, 21 nets, 40 components, 22.86 × 2.79 mm, 1 zones
- Human reference layout: 334 segments, 39 vias, 692.92 mm of track, 585 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/Hornbill-ESP32-Minima_Beacon32/`](../../../../fixtures/PCBench/Hornbill-ESP32-Minima_Beacon32/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 45 | None | 128 | 1.9 min | FAIL |
| 2.4.1 | COMPLETED | 45 | None | 142 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 46 | 0 | 60 | 8.7 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 0 | 0 | 0 | 0.2 min | clean |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 0 | 0.3 min | clean |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 30 | 0.9 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 30 | 0.3 min | clean |
| master-edffcabb | COMPLETED | 0 | 0 | 30 | 0.4 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `Hornbill-ESP32-Minima_Beacon32--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
