# zx-sizif-xxs_sizif-xxs

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 14 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 933.57056 |
| Wall clock | 18.4 min |
| Connections (total / still open) | 281 / 14 |
| Autorouter stop reason | stagnation after 30 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **short-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 20 | 0 | 905.1 | stagnation | 4.8 min |
| direct-first | 15 | 0 | 928.8 | stagnation | 2.4 min |
| fanout-retry | 16 | 0 | 924.1 | stagnation | 3.4 min |
| short-escape | 14 | 0 | 933.6 | stagnation | 2.9 min |
| open-escape | 17 | 0 | 919.3 | stagnation | 2.1 min |
| no-fanout | 35 | 0 | 833.9 | stagnation | 2.7 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 14 | 0 |
| clearance | 0 | 2 |
| hole_clearance | 0 | 7 |
| shorting_items | 0 | 0 |
| tracks_crossing | 2 | 0 |
| copper_edge_clearance | 43 | 73 |

Open connections reported by KiCad:

- Pad 8 [Net-(J7-Pad8)] of J7 on F.Cu @ (154.93, 65.90) ↔ Pad 2 [Net-(J7-Pad8)] of R32 on F.Cu @ (158.36, 68.07)
- Pad 1 [/VD2] of RN4 on F.Cu @ (181.78, 67.61) ↔ Track [/VD2] on B.Cu, length 1.0694 mm @ (170.59, 67.35)
- Pad 2 [/VD7] of RN4 on F.Cu @ (181.22, 67.05) ↔ Pad 19 [/VD7] of U6 on B.Cu @ (171.86, 67.35)
- Pad 50 [/VD1] of U5 on B.Cu @ (153.92, 67.34) ↔ Pad 20 [/VD1] of U6 on B.Cu @ (173.13, 67.35)
- Pad 20 [/VD1] of U6 on B.Cu @ (173.13, 67.35) ↔ Pad 4 [/VD1] of RN4 on F.Cu @ (180.09, 65.92)
- Pad 8 [/D7] of U4 on F.Cu @ (178.93, 70.81) ↔ Pad 7 [/D7] of RN4 on F.Cu @ (179.95, 68.32)
- Pad 2 [/SD_CS] of J7 on F.Cu @ (148.32, 65.90) ↔ Pad 39 [/SD_CS] of U5 on B.Cu @ (150.04, 71.23)
- Pad 3 [/SD_MOSI] of J7 on F.Cu @ (149.43, 65.90) ↔ Pad 42 [/SD_MOSI] of U5 on B.Cu @ (151.10, 70.17)
- Pad 54 [/~{INT}] of U5 on B.Cu @ (157.34, 68.40) ↔ Pad 12 [/~{INT}] of U4 on F.Cu @ (174.12, 69.11)
- Pad 52 [/~{MREQ}] of U5 on B.Cu @ (156.63, 67.69) ↔ Pad 15 [/~{MREQ}] of U4 on F.Cu @ (172.42, 70.81)
- Pad 78 [/A13] of U5 on B.Cu @ (164.05, 78.88) ↔ Pad 42 [/A13] of U4 on F.Cu @ (181.76, 79.01)
- Pad 62 [/TCK] of U5 on B.Cu @ (160.16, 71.23) ↔ Pad 1 [/TCK] of J6 on F.Cu @ (163.85, 66.00)
- Pad 63 [/TMS] of U5 on B.Cu @ (160.52, 71.58) ↔ Pad 3 [/TMS] of J6 on F.Cu @ (166.39, 66.00)
- Pad 1 [/SND_DAC_L] of R31 on F.Cu @ (130.88, 68.50) ↔ Pad 36 [/SND_DAC_L] of U5 on B.Cu @ (148.97, 72.29)

## Board

- Source: https://github.com/UzixLS/zx-sizif-xxs (author UzixLS, license MIT)
- 2 layers, 23 nets, 95 components, 100.0 × 24.48 mm, 2 zones
- Human reference layout: 1490 segments, 150 vias, 2834.16 mm of track, 484 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/zx-sizif-xxs_sizif-xxs/`](../../../../fixtures/PCBench/zx-sizif-xxs_sizif-xxs/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 40 | None | 98 | 5.6 min | FAIL |
| 2.4.1 | COMPLETED | 52 | None | 43 | 5.2 min | FAIL |
| 2.5.0 | TIMED_OUT | 58 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 78 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 51 | 0 | 0 | 52.1 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 16 | 0 | 0 | 132.4 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 14 | 0 | 0 | 18.4 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `zx-sizif-xxs_sizif-xxs--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
