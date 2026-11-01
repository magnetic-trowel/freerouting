# reach-nrf51822-hardware_reach-target-hw

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 8 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 929.3598 |
| Wall clock | 8.0 min |
| Connections (total / still open) | 151 / 8 |
| Autorouter stop reason | stagnation after 30 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 8 | 0 | 929.4 | stagnation | 1.6 min |
| direct-first | 8 | 0 | 929.4 | stagnation | 1.3 min |
| fanout-retry | 8 | 0 | 929.4 | stagnation | 1.2 min |
| short-escape | 8 | 0 | 929.4 | stagnation | 1.4 min |
| open-escape | 11 | 0 | 902.9 | stagnation | 1.3 min |
| no-fanout | 11 | 0 | 902.9 | stagnation | 1.0 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 8 | 0 |
| clearance | 0 | 37 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 24 | 23 |

Open connections reported by KiCad:

- Track [/G1] on B.Cu, length 4.4482 mm @ (86.00, 108.31) ↔ Pad 6 [/G1] of U3 on F.Cu @ (73.25, 113.28)
- Pad 22 [/P0.16] of U3 on F.Cu @ (68.10, 109.63) ↔ Track [/P0.16] on F.Cu, length 12.2054 mm @ (75.59, 120.57)
- Pad 27 [/P0.19] of U3 on F.Cu @ (66.45, 111.28) ↔ PTH pad 9 [/P0.19] of J5 @ (81.41, 122.94)
- Pad 1 [/ANT1] of L2 on F.Cu @ (64.77, 113.09) ↔ Pad 2 [/ANT1] of L1 on F.Cu @ (63.75, 112.98)
- Pad 31 [/ANT1] of U3 on F.Cu @ (66.45, 113.28) ↔ Pad 1 [/ANT1] of L2 on F.Cu @ (64.77, 113.09)
- Pad 2 [/ANT2] of L2 on F.Cu @ (64.77, 113.99) ↔ Pad 2 [/ANT2] of C14 on F.Cu @ (64.77, 114.74)
- Pad 32 [/ANT2] of U3 on F.Cu @ (66.45, 113.78) ↔ Pad 2 [/ANT2] of L2 on F.Cu @ (64.77, 113.99)
- PTH pad 4 [/P0.22] of J6 @ (68.71, 125.48) ↔ Pad 41 [/P0.22] of U3 on F.Cu @ (69.10, 116.43)

## Board

- Source: https://github.com/tessel/reach-nrf51822-hardware (author tessel, license None)
- 2 layers, 7 nets, 62 components, 41.91 × 27.94 mm, 5 zones
- Human reference layout: 1092 segments, 124 vias, 1251.17 mm of track, 699 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/reach-nrf51822-hardware_reach-target-hw/`](../../../../fixtures/PCBench/reach-nrf51822-hardware_reach-target-hw/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 7 | None | 15 | 2.7 min | FAIL |
| 2.4.1 | COMPLETED | 32 | None | 15 | 5.2 min | FAIL |
| 2.5.0 | COMPLETED | 29 | 0 | 0 | 4.5 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 6 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 6 | 0 | 0 | 31.9 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 4 | 0 | 0 | 23.5 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 8 | 0 | 0 | 8.0 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `reach-nrf51822-hardware_reach-target-hw--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
