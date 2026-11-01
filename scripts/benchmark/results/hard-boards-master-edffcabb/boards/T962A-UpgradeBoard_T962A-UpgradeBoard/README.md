# T962A-UpgradeBoard_T962A-UpgradeBoard

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 12 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 849.0566 |
| Wall clock | 89.8 min |
| Connections (total / still open) | 106 / 12 |
| Autorouter stop reason | stagnation after 26 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **direct-first**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 15 | 0 | 811.3 | stagnation | 30.2 min |
| direct-first | 12 | 0 | 849.1 | stagnation | 0.4 min |
| fanout-retry | 15 | 0 | 811.3 | stagnation | 0.4 min |
| short-escape | 14 | 0 | 823.9 | stagnation | 57.3 min |
| open-escape | 13 | 0 | 836.5 | stagnation | 0.4 min |
| no-fanout | 19 | 0 | 761.0 | stagnation | 0.7 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 12 | 0 |
| clearance | 0 | 0 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Track [T3-] on F.Cu, length 2.6046 mm @ (140.51, 103.69) ↔ Pad 2 [T3-] of T3 on F.Cu @ (142.40, 103.94)
- Pad 3 [T3+] of T3 on F.Cu @ (142.40, 104.44) ↔ Track [T3+] on F.Cu, length 1.0771 mm @ (140.51, 105.19)
- Pad 7 [VCC] of T3 on F.Cu @ (146.24, 104.94) ↔ Track [VCC] on F.Cu, length 0.1393 mm @ (146.05, 107.08)
- Pad 2 [T2-] of C5 on F.Cu @ (140.51, 108.77) ↔ PTH pad 4 [T2-] of J1 @ (135.81, 108.25)
- Pad 2 [T2-] of T2 on F.Cu @ (142.40, 109.02) ↔ Pad 2 [T2-] of C5 on F.Cu @ (140.51, 108.77)
- Pad 3 [T2+] of T2 on F.Cu @ (142.40, 109.52) ↔ Track [T2+] on F.Cu, length 2.6046 mm @ (140.51, 110.27)
- Track [T1-] on F.Cu, length 2.6046 mm @ (140.51, 113.85) ↔ Pad 2 [T1-] of T1 on F.Cu @ (142.40, 114.10)
- PTH pad 1 [T1+] of J1 @ (135.81, 115.87) ↔ Pad 1 [T1+] of C7 on F.Cu @ (140.51, 115.35)
- Pad 1 [T1+] of C7 on F.Cu @ (140.51, 115.35) ↔ Pad 3 [T1+] of T1 on F.Cu @ (142.40, 114.60)
- PTH pad 7 [T4+] of J1 @ (135.81, 100.63) ↔ Pad 1 [T4+] of C1 on F.Cu @ (140.51, 100.11)
- Pad 1 [T4+] of C1 on F.Cu @ (140.51, 100.11) ↔ Pad 3 [T4+] of T4 on F.Cu @ (142.40, 99.36)
- Track [T4-] on F.Cu, length 2.6046 mm @ (140.51, 98.61) ↔ Pad 2 [T4-] of T4 on F.Cu @ (142.40, 98.86)

## Board

- Source: https://github.com/rharrison/T962A-UpgradeBoard (author rharrison, license None)
- 2 layers, 12 nets, 37 components, 35.4 × 57.11 mm, 6 zones
- Human reference layout: 488 segments, 72 vias, 728.2 mm of track, 489 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/T962A-UpgradeBoard_T962A-UpgradeBoard/`](../../../../fixtures/PCBench/T962A-UpgradeBoard_T962A-UpgradeBoard/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 19 | None | 6 | 2.0 min | FAIL |
| 2.4.1 | COMPLETED | 11 | None | 6 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 16 | 0 | 0 | 4.9 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 15 | 0 | 0 | 10.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 15 | 0 | 0 | 11.8 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 15 | 0 | 0 | 14.2 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 13 | 0 | 0 | 10.1 min | FAIL |
| master-edffcabb | COMPLETED | 12 | 0 | 0 | 89.8 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `T962A-UpgradeBoard_T962A-UpgradeBoard--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
