# RGBMatrixPanelCPLD-PhotonBackpack_RGBMatrixPanel_CPLD

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 2 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 986.4636 |
| Wall clock | 2.2 min |
| Connections (total / still open) | 197 / 2 |
| Autorouter stop reason | stagnation after 22 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **open-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 5 | 0 | 966.2 | stagnation | 0.4 min |
| direct-first | 4 | 0 | 972.9 | stagnation | 0.5 min |
| fanout-retry | 67 | 0 | 546.5 | repeated | 0.0 min |
| short-escape | 5 | 0 | 966.2 | stagnation | 0.4 min |
| open-escape | 2 | 0 | 986.5 | stagnation | 0.3 min |
| no-fanout | 2 | 0 | 986.5 | stagnation | 0.3 min |
| continue | 2 | 0 | 986.5 | stagnation | 0.2 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 2 | 88 |
| clearance | 0 | 0 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 4 | 3 |

Open connections reported by KiCad:

- PTH pad 2 [GND] of J1 @ (58.62, 109.22) ↔ Track [GND] on B.Cu, length 1.8760 mm @ (58.74, 102.72)
- Track [VIN] on B.Cu, length 3.2329 mm @ (68.58, 110.73) ↔ PTH pad 1 [VIN] of J1 @ (64.62, 109.22)

## Board

- Source: https://github.com/ReclaimerLabs/RGBMatrixPanelCPLD-PhotonBackpack (author ReclaimerLabs, license None)
- 2 layers, 29 nets, 52 components, 71.12 × 59.69 mm, 11 zones
- Human reference layout: 778 segments, 80 vias, 1870.97 mm of track, 380 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/RGBMatrixPanelCPLD-PhotonBackpack_RGBMatrixPanel_CPLD/`](../../../../fixtures/PCBench/RGBMatrixPanelCPLD-PhotonBackpack_RGBMatrixPanel_CPLD/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 4 | None | 80 | 1.6 min | FAIL |
| 2.4.1 | COMPLETED | 2 | None | 80 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 7 | 0 | 0 | 4.0 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 8 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 8 | 0 | 0 | 12.9 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 2 | 0 | 0 | 11.7 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 2 | 0 | 0 | 9.1 min | FAIL |
| master-edffcabb | COMPLETED | 2 | 0 | 0 | 2.2 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `RGBMatrixPanelCPLD-PhotonBackpack_RGBMatrixPanel_CPLD--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
