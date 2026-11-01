# zx-tsid_zx-tsid

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 23 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 853.96826 |
| Wall clock | 41.6 min |
| Connections (total / still open) | 210 / 23 |
| Autorouter stop reason | stagnation after 28 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **fanout-retry**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 24 | 0 | 847.6 | stagnation | 8.5 min |
| direct-first | 26 | 0 | 834.9 | stagnation | 7.9 min |
| fanout-retry | 23 | 0 | 854.0 | stagnation | 6.8 min |
| short-escape | 28 | 0 | 822.2 | stagnation | 5.4 min |
| open-escape | 26 | 0 | 834.9 | stagnation | 9.3 min |
| no-fanout | 28 | 0 | 822.2 | stagnation | 3.5 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 23 | 0 |
| clearance | 0 | 1 |
| hole_clearance | 0 | 3 |
| shorting_items | 2 | 2 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 15 | 17 |

Open connections reported by KiCad:

- Pad 15 [MOSI] of U1 on F.Cu @ (152.15, 97.05) ↔ Pad 4 [MOSI] of J6 on F.Cu @ (165.70, 98.62)
- PTH pad A4 [+9V] of J2 @ (121.90, 118.94) ↔ PTH pad 1 [+9V] of JP3 @ (183.00, 103.70)
- PTH pad A8 [~{CLKCPU}] of J2 @ (132.06, 118.94) ↔ Pad 15 [~{CLKCPU}] of U6 on F.Cu @ (137.56, 98.75)
- Pad 40 [A0] of U6 on F.Cu @ (126.24, 101.75) ↔ PTH pad A9 [A0] of J2 @ (134.60, 118.94)
- Pad 43 [A2] of U6 on F.Cu @ (126.24, 103.25) ↔ PTH pad A11 [A2] of J2 @ (139.68, 118.94)
- Pad 51 [A3] of U6 on F.Cu @ (129.15, 107.66) ↔ PTH pad A12 [A3] of J2 @ (142.22, 118.94)
- Pad 36 [A6] of U6 on F.Cu @ (126.24, 99.75) ↔ PTH pad A22 [A6] of J2 @ (167.62, 118.94)
- Pad 39 [A4] of U6 on F.Cu @ (126.24, 101.25) ↔ PTH pad A24 [A4] of J2 @ (172.70, 118.94)
- Pad 45 [D1] of U6 on F.Cu @ (126.24, 104.25) ↔ PTH pad B7 [D1] of J2 @ (129.52, 113.86)
- Pad 58 [D4] of U6 on F.Cu @ (132.65, 107.66) ↔ PTH pad B12 [D4] of J2 @ (142.22, 113.86)
- Pad 50 [~{IORQ}] of U6 on F.Cu @ (128.65, 107.66) ↔ PTH pad B17 [~{IORQ}] of J2 @ (154.92, 113.86)
- Pad 63 [~{RD}] of U6 on F.Cu @ (135.15, 107.66) ↔ PTH pad B18 [~{RD}] of J2 @ (157.46, 113.86)
- Pad 62 [~{WR}] of U6 on F.Cu @ (134.65, 107.66) ↔ PTH pad B19 [~{WR}] of J2 @ (160.00, 113.86)
- Pad 11 [SID_D7] of U6 on F.Cu @ (137.56, 100.75) ↔ Pad 11 [SID_D7] of U1 on F.Cu @ (155.35, 97.05)
- Pad 28 [SID_D2] of U1 on F.Cu @ (153.75, 105.55) ↔ Track [SID_D2] on F.Cu, length 1.1267 mm @ (151.41, 93.56)
- PTH pad 7 [~{SID_WR}] of U5 @ (164.11, 108.80) ↔ Pad 1 [~{SID_WR}] of R2 on F.Cu @ (164.18, 95.50)
- Pad 1 [~{SID_WR}] of R2 on F.Cu @ (164.18, 95.50) ↔ Pad 13 [~{SID_WR}] of U6 on F.Cu @ (137.56, 99.75)
- Track [SID_D4] on B.Cu, length 1.1267 mm @ (156.49, 93.56) ↔ Pad 18 [SID_D4] of U6 on F.Cu @ (135.15, 96.34)
- Track [SID_D3] on B.Cu, length 1.1267 mm @ (153.95, 93.56) ↔ Pad 19 [SID_D3] of U6 on F.Cu @ (134.65, 96.34)
- Pad 28 [TDI] of U6 on F.Cu @ (130.15, 96.34) ↔ Pad 9 [TDI] of J1 on F.Cu @ (144.28, 91.42)
- Pad 30 [TCK] of U6 on F.Cu @ (129.15, 96.34) ↔ Pad 1 [TCK] of J1 on F.Cu @ (134.12, 91.42)
- Pad 53 [TDO] of U6 on F.Cu @ (130.15, 107.66) ↔ Pad 3 [TDO] of J1 on F.Cu @ (136.66, 91.42)
- Pad 1 [Net-(R6-Pad1)] of R6 on B.Cu @ (142.18, 102.50) ↔ Pad 13 [Net-(R6-Pad1)] of U1 on F.Cu @ (153.75, 97.05)

## Board

- Source: https://github.com/UzixLS/zx-tsid (author UzixLS, license MIT)
- 2 layers, 30 nets, 67 components, 73.8 × 37.3 mm, 2 zones
- Human reference layout: 1555 segments, 144 vias, 3420.44 mm of track, 371 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/zx-tsid_zx-tsid/`](../../../../fixtures/PCBench/zx-tsid_zx-tsid/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 37 | None | 59 | 5.4 min | FAIL |
| 2.4.1 | COMPLETED | 42 | None | 0 | 5.3 min | FAIL |
| 2.5.0 | TIMED_OUT | 30 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 29 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 27 | 0 | 0 | 68.7 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 25 | 0 | 0 | 111.5 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 23 | 0 | 0 | 41.6 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `zx-tsid_zx-tsid--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
