# kitspace_OtterPill

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 18 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 829.78723 |
| Wall clock | 5.2 min |
| Connections (total / still open) | 141 / 18 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **direct-first**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 22 | 0 | 792.0 | stagnation | 0.9 min |
| direct-first | 18 | 0 | 829.8 | stagnation | 0.8 min |
| fanout-retry | 20 | 0 | 810.9 | stagnation | 0.9 min |
| short-escape | 18 | 0 | 829.8 | stagnation | 0.9 min |
| open-escape | 20 | 0 | 810.9 | stagnation | 0.9 min |
| no-fanout | 23 | 0 | 782.5 | stagnation | 0.7 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 18 | 0 |
| clearance | 0 | 145 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 71 |

Open connections reported by KiCad:

- Track [+3V3] on F.Cu, length 0.8567 mm @ (58.05, 41.61) ↔ PTH pad 2 [+3V3] of J4 @ (55.69, 49.21)
- Track [+3V3] on B.Cu, length 1.0617 mm @ (70.89, 43.98) ↔ Pad 24 [+3V3] of U2 on F.Cu @ (67.26, 40.70)
- Pad 1 [+3V3] of U2 on F.Cu @ (77.04, 40.70) ↔ Pad 9 [+3V3] of U2 on F.Cu @ (74.21, 37.87)
- Pad 1 [GND] of R1 on F.Cu @ (58.94, 44.11) ↔ Pad 2 [GND] of U1 on F.Cu @ (60.25, 41.61)
- Track [GND] on F.Cu, length 1.1000 mm @ (59.14, 38.01) ↔ Pad 2 [GND] of U1 on F.Cu @ (60.25, 41.61)
- Pad 2 [GND] of U1 on F.Cu @ (60.25, 41.61) ↔ Track [GND] on F.Cu, length 1.2625 mm @ (63.45, 41.61)
- Pad 23 [GND] of U2 on F.Cu @ (67.62, 40.35) ↔ Pad 2 [GND] of C7 on F.Cu @ (65.92, 41.30)
- Pad 2 [GND] of C9 on F.Cu @ (75.99, 37.55) ↔ Track [GND] on F.Cu, length 0.2022 mm @ (77.64, 37.51)
- Pad 2 [VBUS] of U4 on F.Cu @ (63.45, 42.87) ↔ Track [VBUS] on F.Cu, length 0.9293 mm @ (78.65, 38.90)
- Pad 1 [CC1] of R8 on F.Cu @ (60.57, 39.11) ↔ Pad 2 [CC1] of R2 on F.Cu @ (58.17, 39.11)
- Track [USB_P] on F.Cu, length 0.8567 mm @ (58.05, 42.56) ↔ Pad 33 [USB_P] of U2 on F.Cu @ (70.09, 45.53)
- Pad 31 [PA10] of U2 on F.Cu @ (69.38, 44.82) ↔ PTH pad 14 [PA10] of J3 @ (86.17, 34.21)
- Pad 30 [PA9] of U2 on F.Cu @ (69.03, 44.47) ↔ PTH pad 15 [PA9] of J3 @ (88.71, 34.21)
- PTH pad 3 [PA8] of J4 @ (58.23, 49.21) ↔ Pad 29 [PA8] of U2 on F.Cu @ (68.68, 44.11)
- PTH pad 5 [PB8] of J4 @ (63.31, 49.21) ↔ Pad 45 [PB8] of U2 on F.Cu @ (75.98, 43.76)
- PTH pad 9 [PB6] of J4 @ (73.47, 49.21) ↔ Pad 42 [PB6] of U2 on F.Cu @ (74.92, 44.82)
- Track [VIN] on F.Cu, length 1.8101 mm @ (81.65, 39.13) ↔ Pad 2 [VIN] of R16 on F.Cu @ (78.65, 36.72)
- Track [SCL] on F.Cu, length 0.8939 mm @ (65.77, 37.76) ↔ Pad 6 [SCL] of U4 on F.Cu @ (64.71, 41.36)

## Board

- Source: https://github.com/Jana-Marie/OtterPill (author , license [{'name': 'CERN', 'link': 'https://raw.githubusercontent.com/Jana-Marie/OtterPill/main/LICENSE.md'}])
- 2 layers, 57 nets, 45 components, 43.2 × 17.6 mm, 6 zones
- Human reference layout: 519 segments, 144 vias, 651.54 mm of track, 158 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_OtterPill/`](../../../../fixtures/PCBench/kitspace_OtterPill/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 18 | None | 62 | 3.5 min | FAIL |
| 2.4.1 | COMPLETED | 21 | None | 4 | 5.1 min | FAIL |
| 2.5.0 | TIMED_OUT | 17 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 17 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 14 | 0 | 0 | 26.9 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 16 | 0 | 0 | 45.5 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 13 | 0 | 0 | 26.6 min | FAIL |
| master-edffcabb | COMPLETED | 18 | 0 | 0 | 5.2 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_OtterPill--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
