# freeDSPx-AMP-x4_freeDSPx-AMPx4

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 21 |
| Clearance violations (total) | 64 |
| of which router-introduced | 0 |
| of which pre-existing in input | 64 (unfixable: 64) |
| Worst violation depth | 177.8 µm |
| Quality score | 935.0866 |
| Wall clock | 11.9 min |
| Connections (total / still open) | 462 / 21 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **open-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 31 | 0 | 906.2 | stagnation | 2.2 min |
| direct-first | 32 | 0 | 903.3 | stagnation | 2.1 min |
| fanout-retry | 27 | 0 | 917.8 | stagnation | 2.2 min |
| short-escape | 27 | 0 | 917.8 | stagnation | 2.2 min |
| open-escape | 21 | 0 | 935.1 | stagnation | 1.7 min |
| no-fanout | 35 | 0 | 894.7 | stagnation | 1.5 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 21 | 0 |
| clearance | 64 | 68 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Track [VCC] on F.Cu, length 0.4000 mm @ (113.10, 86.20) ↔ Pad 14 [VCC] of U3 on F.Cu @ (113.10, 90.20)
- Pad 14 [VCC] of U3 on F.Cu @ (113.10, 90.20) ↔ Pad 15 [VCC] of U3 on F.Cu @ (113.50, 90.20)
- Pad 14 [VCC] of U4 on F.Cu @ (113.10, 113.80) ↔ Track [VCC] on F.Cu, length 0.4000 mm @ (113.10, 109.80)
- Pad 1 [VCC] of C22 on F.Cu @ (113.40, 91.70) ↔ Pad 15 [VCC] of U3 on F.Cu @ (113.50, 90.20)
- Pad 1 [VCC] of C34 on F.Cu @ (113.40, 108.30) ↔ PTH pad 1 [VCC] of C35 @ (116.60, 105.10)
- Track [VCC] on F.Cu, length 1.3500 mm @ (113.50, 60.95) ↔ Track [VCC] on B.Cu, length 2.3295 mm @ (116.60, 57.60)
- Track [VCC] on F.Cu, length 1.4000 mm @ (113.50, 67.70) ↔ Pad 14 [VCC] of U2 on F.Cu @ (113.10, 66.30)
- Track [VCC] on F.Cu, length 0.1414 mm @ (113.50, 67.70) ↔ Track [VCC] on B.Cu, length 1.1648 mm @ (116.65, 71.00)
- Track [VCC] on F.Cu, length 0.1414 mm @ (113.50, 84.80) ↔ Track [VCC] on B.Cu, length 2.3295 mm @ (116.60, 81.50)
- Pad 15 [VCC] of U4 on F.Cu @ (113.50, 113.80) ↔ Pad 14 [VCC] of U4 on F.Cu @ (113.10, 113.80)
- Pad 15 [VCC] of U4 on F.Cu @ (113.50, 113.80) ↔ Pad 1 [VCC] of C33 on F.Cu @ (113.40, 115.30)
- Track [VCC] on F.Cu, length 1.4000 mm @ (113.50, 132.40) ↔ Track [VCC] on B.Cu, length 2.3295 mm @ (116.60, 129.00)
- Track [VCC] on F.Cu, length 0.2121 mm @ (116.60, 118.50) ↔ Pad 1 [VCC] of C33 on F.Cu @ (113.40, 115.30)
- Track [VCC] on B.Cu, length 2.3295 mm @ (116.60, 142.60) ↔ Track [VCC] on F.Cu, length 1.4000 mm @ (113.50, 139.20)
- Track [MCLK] on F.Cu, length 29.8510 mm @ (102.99, 103.85) ↔ Pad 5 [MCLK] of U4 on F.Cu @ (110.50, 112.00)
- Track [MCLK] on B.Cu, length 2.4830 mm @ (107.68, 90.80) ↔ Pad 5 [MCLK] of U3 on F.Cu @ (110.50, 88.40)
- Pad 4 [LRCLK] of U4 on F.Cu @ (110.50, 111.60) ↔ Track [LRCLK] on F.Cu, length 0.6795 mm @ (110.50, 88.00)
- Pad 4 [LRCLK] of U5 on F.Cu @ (110.50, 135.60) ↔ Pad 4 [LRCLK] of U4 on F.Cu @ (110.50, 111.60)
- PTH pad 2 [ADRESS] of JP1 @ (100.84, 55.78) ↔ Pad 2 [ADRESS] of R8 on F.Cu @ (100.84, 58.61)
- Track [ADRESS] on B.Cu, length 1.6283 mm @ (111.97, 114.78) ↔ Pad 12 [ADRESS] of U5 on F.Cu @ (112.30, 137.80)
- Pad 12 [ADRESS] of U2 on F.Cu @ (112.30, 66.30) ↔ Pad 2 [ADRESS] of R8 on F.Cu @ (100.84, 58.61)

## Board

- Source: https://github.com/freeDSP/freeDSPx-AMP-x4 (author freeDSP, license None)
- 2 layers, 72 nets, 283 components, 60.0 × 100.0 mm, 37 zones
- Human reference layout: 663 segments, 51 vias, 1433.82 mm of track, 777 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/freeDSPx-AMP-x4_freeDSPx-AMPx4/`](../../../../fixtures/PCBench/freeDSPx-AMP-x4_freeDSPx-AMPx4/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 16 | None | 168 | 6.3 min | FAIL |
| 2.4.1 | COMPLETED | 14 | None | 168 | 15.2 min | FAIL |
| 2.5.0 | COMPLETED | 29 | 0 | 64 | 8.6 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 32 | 0 | 64 | 20.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 31 | 0 | 64 | 31.4 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 25 | 0 | 64 | 55.9 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 21 | 0 | 64 | 11.9 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `freeDSPx-AMP-x4_freeDSPx-AMPx4--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
