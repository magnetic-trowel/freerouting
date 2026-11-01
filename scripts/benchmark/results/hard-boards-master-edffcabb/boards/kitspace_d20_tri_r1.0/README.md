# kitspace_d20_tri_r1.0

Tier **D** (diagnostic). Category: **timeout**: hit the 6 h job budget before the router finished. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | TIMED_OUT |
| Unrouted connections | 483 |
| Clearance violations (total) | 11 |
| of which router-introduced | 0 |
| of which pre-existing in input | 11 (unfixable: 11) |
| Worst violation depth | 11.887359619140625 µm |
| Quality score | 83.117134 |
| Wall clock | 360.1 min |
| Connections (total / still open) | 552 / 483 |
| Autorouter stop reason | stagnation after 30 passes |
| Optimizer | None |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **short-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 491 | 0 | 73.5 | stagnation | 86.1 min |
| direct-first | 489 | 0 | 75.9 | stagnation | 50.0 min |
| fanout-retry | 488 | 0 | 77.1 | stagnation | 90.8 min |
| short-escape | 483 | 0 | 83.1 | stagnation | 120.2 min |
| open-escape | 499 | 0 | 63.8 | timed_out | 12.8 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 483 | 499 |
| clearance | 8 | 499 |
| hole_clearance | 0 | 199 |
| shorting_items | 0 | 2 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 178 | 133 |

Open connections reported by KiCad:

- Pad 2 [vcc] of Q3 on F.Cu @ (114.10, 129.82) ↔ Pad 2 [vcc] of Q2 on F.Cu @ (114.97, 130.60)
- Pad 2 [vcc] of Q3 on F.Cu @ (114.10, 129.82) ↔ Track [vcc] on F.Cu, length 0.9282 mm @ (113.17, 128.75)
- Pad 5 [vcc] of U7 on F.Cu @ (117.10, 138.51) ↔ Track [vcc] on F.Cu, length 0.0878 mm @ (118.47, 135.28)
- Pad 1 [vcc] of J1 on F.Cu @ (121.21, 139.92) ↔ Pad 5 [vcc] of U6 on F.Cu @ (119.30, 143.63)
- Pad 16 [vcc] of U5 on F.Cu @ (122.68, 124.80) ↔ Pad 10 [vcc] of U4 on F.Cu @ (120.86, 124.79)
- Track [vcc] on F.Cu, length 0.4786 mm @ (126.24, 125.67) ↔ Pad 2 [vcc] of Q13 on F.Cu @ (129.56, 127.01)
- Pad 21 [blank] of U1 on F.Cu @ (111.47, 122.64) ↔ Track [blank] on F.Cu, length 0.4060 mm @ (120.92, 122.67)
- Pad 3 [blank] of J2 on F.Cu @ (117.52, 135.78) ↔ Pad 4 [blank] of U6 on F.Cu @ (119.00, 143.46)
- Pad 3 [blank] of J2 on F.Cu @ (117.52, 135.78) ↔ Track [blank] on F.Cu, length 1.0295 mm @ (122.09, 125.40)
- Pad 4 [latch] of J2 on F.Cu @ (117.08, 136.03) ↔ Pad 4 [latch] of U7 on F.Cu @ (117.41, 138.33)
- Pad 4 [latch] of J2 on F.Cu @ (117.08, 136.03) ↔ Pad 12 [latch] of U4 on F.Cu @ (119.56, 124.79)
- Track [latch] on F.Cu, length 0.4546 mm @ (120.92, 117.11) ↔ Pad 12 [latch] of U4 on F.Cu @ (119.56, 124.79)
- Track [latch] on F.Cu, length 0.4546 mm @ (120.92, 117.11) ↔ Pad 4 [latch] of U1 on F.Cu @ (111.47, 117.08)
- Pad 12 [latch] of U5 on F.Cu @ (125.28, 124.80) ↔ Pad 12 [latch] of U4 on F.Cu @ (119.56, 124.79)
- Pad 5 [sclk] of J2 on F.Cu @ (116.65, 136.28) ↔ Pad 6 [sclk] of U7 on F.Cu @ (116.80, 138.68)
- Pad 11 [sclk] of U4 on F.Cu @ (120.21, 124.79) ↔ Pad 5 [sclk] of J2 on F.Cu @ (116.65, 136.28)
- Track [sclk] on F.Cu, length 0.5054 mm @ (121.55, 117.50) ↔ Track [sclk] on F.Cu, length 3.7980 mm @ (119.87, 123.94)
- Pad 11 [sclk] of U5 on F.Cu @ (125.93, 124.80) ↔ Pad 11 [sclk] of U4 on F.Cu @ (120.21, 124.79)
- Pad 1 [dat_i] of U6 on F.Cu @ (119.92, 143.26) ↔ Pad 2 [dat_i] of J1 on F.Cu @ (120.78, 139.67)
- Pad 3 [blank_i] of U6 on F.Cu @ (119.33, 142.89) ↔ Pad 3 [blank_i] of J1 on F.Cu @ (120.35, 139.42)
- Pad 3 [latch_i] of U7 on F.Cu @ (117.74, 138.91) ↔ Pad 4 [latch_i] of J1 on F.Cu @ (119.91, 139.17)
- Pad 1 [sclk_i] of U7 on F.Cu @ (117.12, 139.23) ↔ Pad 5 [sclk_i] of J1 on F.Cu @ (119.48, 138.92)
- Pad 6 [srow_14] of U5 on F.Cu @ (125.93, 130.70) ↔ Pad 1 [srow_14] of Q15 on F.Cu @ (134.27, 117.33)
- Pad 1 [row_5] of D82 on B.Cu @ (116.40, 118.49) ↔ Pad 1 [row_5] of D81 on B.Cu @ (115.28, 116.54)
- Pad 1 [row_5] of D83 on B.Cu @ (117.52, 120.44) ↔ Pad 1 [row_5] of D82 on B.Cu @ (116.40, 118.49)
- Pad 1 [row_5] of D83 on B.Cu @ (117.52, 120.44) ↔ Pad 1 [row_5] of D84 on B.Cu @ (118.65, 122.38)
- Pad 1 [row_5] of D85 on B.Cu @ (119.77, 124.33) ↔ Pad 1 [row_5] of D84 on B.Cu @ (118.65, 122.38)
- Pad 1 [row_5] of D85 on B.Cu @ (119.77, 124.33) ↔ Pad 3 [row_5] of Q6 on F.Cu @ (114.28, 128.32)
- Pad 1 [row_5] of D86 on B.Cu @ (120.89, 126.27) ↔ Pad 1 [row_5] of D85 on B.Cu @ (119.77, 124.33)
- Pad 1 [row_5] of D86 on B.Cu @ (120.89, 126.27) ↔ Pad 1 [row_5] of D87 on B.Cu @ (122.02, 128.22)
- Pad 1 [row_5] of D88 on B.Cu @ (123.14, 130.16) ↔ Pad 1 [row_5] of D87 on B.Cu @ (122.02, 128.22)
- Pad 1 [row_5] of D89 on B.Cu @ (124.27, 132.11) ↔ Pad 1 [row_5] of D88 on B.Cu @ (123.14, 130.16)
- Pad 1 [row_5] of D90 on B.Cu @ (125.39, 134.05) ↔ Pad 1 [row_5] of D89 on B.Cu @ (124.27, 132.11)
- Pad 3 [srow_11] of U5 on F.Cu @ (123.98, 130.70) ↔ Pad 1 [srow_11] of Q12 on F.Cu @ (128.26, 128.35)
- Pad 1 [row_11] of D177 on B.Cu @ (128.76, 116.55) ↔ Pad 1 [row_11] of D178 on B.Cu @ (129.88, 118.49)
- Pad 1 [row_11] of D178 on B.Cu @ (129.88, 118.49) ↔ Pad 1 [row_11] of D179 on B.Cu @ (131.00, 120.44)
- Track [row_11] on B.Cu, length 1.1543 mm @ (132.13, 122.38) ↔ Pad 1 [row_11] of D179 on B.Cu @ (131.00, 120.44)
- Pad 1 [row_10] of D162 on B.Cu @ (127.63, 118.49) ↔ Pad 1 [row_10] of D161 on B.Cu @ (126.51, 116.55)
- Pad 1 [row_10] of D163 on B.Cu @ (128.76, 120.44) ↔ Pad 1 [row_10] of D162 on B.Cu @ (127.63, 118.49)
- Pad 1 [row_10] of D164 on B.Cu @ (129.88, 122.38) ↔ Track [row_10] on B.Cu, length 1.0168 mm @ (131.00, 124.33)
- … 443 more in `../../drc-summary.json`

## Board

- Source: https://github.com/gregdavill/d20-hardware (author , license [{'name': 'BSD', 'link': 'https://raw.githubusercontent.com/gregdavill/d20-hardware/main/LICENSE'}])
- 4 layers, 93 nets, 152 components, 36.07 × 31.24 mm, 4 zones
- Human reference layout: 4396 segments, 262 vias, 2951.98 mm of track, 496 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_d20_tri_r1.0/`](../../../../fixtures/PCBench/kitspace_d20_tri_r1.0/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 522 | None | 68 | 15.2 min | FAIL |
| 2.4.1 | TIMED_OUT | 511 | None | 11 | 15.1 min | FAIL |
| 2.5.0 | TIMED_OUT | 511 | 0 | 11 | 20.1 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 511 | 0 | 11 | 20.1 min | FAIL |
| 2.6.0-RC2 | TIMED_OUT | 504 | 0 | 11 | 480.1 min | FAIL |
| 2.6.0-RC3 | TIMED_OUT | 493 | 0 | 11 | 360.3 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | TIMED_OUT | 483 | 0 | 11 | 360.1 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_d20_tri_r1.0--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
