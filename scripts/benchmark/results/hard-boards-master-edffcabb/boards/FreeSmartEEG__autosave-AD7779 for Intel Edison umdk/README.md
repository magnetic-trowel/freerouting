# FreeSmartEEG__autosave-AD7779 for Intel Edison umdk

Tier **B** (routine). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 4 |
| of which router-introduced | 0 |
| of which pre-existing in input | 4 (unfixable: 4) |
| Worst violation depth | 200.0 µm |
| Quality score | 999.6583 |
| Wall clock | 2.4 min |
| Connections (total / still open) | 236 / 0 |
| Autorouter stop reason | completed after 15 passes |
| Optimizer | no_improvement |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **direct-first**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 2 | 0 | 988.4 | stagnation | 0.9 min |
| continue | 1 | 0 | 994.0 | stagnation | 0.2 min |
| direct-first | 0 | 0 | 999.7 | completed | 0.1 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 29 | 0 |
| clearance | 52 | 74 |
| hole_clearance | 0 | 0 |
| shorting_items | 30 | 125 |
| tracks_crossing | 0 | 8 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- PTH pad 10 [AVSS] of P10 @ (66.04, 124.46) ↔ Track [AVSS] on F.Cu, length 2.3341 mm @ (67.94, 124.46)
- Track [AVSS] on F.Cu, length 2.0043 mm @ (107.31, 124.46) ↔ Track [AVSS] on F.Cu, length 1.1903 mm @ (109.22, 124.46)
- Track [AVSS] on B.Cu, length 1.1903 mm @ (107.31, 129.54) ↔ Track [AVSS] on F.Cu, length 1.3433 mm @ (105.97, 129.54)
- Track [AVSS] on F.Cu, length 1.1903 mm @ (109.22, 129.54) ↔ Track [AVSS] on F.Cu, length 1.3433 mm @ (105.97, 129.54)
- PTH pad 11 [AVDD] of P10 @ (66.04, 127.00) ↔ Track [AVDD] on F.Cu, length 4.5975 mm @ (72.54, 127.00)
- Track [AVDD] on F.Cu, length 4.5975 mm @ (72.54, 127.00) ↔ Track [AVDD] on B.Cu, length 1.1903 mm @ (67.94, 127.00)
- Track [AVDD] on F.Cu, length 2.3833 mm @ (104.93, 127.00) ↔ Track [AVDD] on B.Cu, length 1.1903 mm @ (107.31, 127.00)
- Track [AVDD] on F.Cu, length 2.3833 mm @ (104.93, 127.00) ↔ PTH pad 6 [AVDD] of P8 @ (109.22, 127.00)
- Track [AIN1-] on F.Cu, length 1.1903 mm @ (107.31, 134.62) ↔ PTH pad 3 [AIN1-] of P8 @ (109.22, 134.62)
- Track [AIN1+] on F.Cu, length 1.6396 mm @ (105.68, 132.08) ↔ Track [AIN1+] on B.Cu, length 5.0800 mm @ (109.22, 132.08)
- PTH pad 9 [OUT_VREF] of P10 @ (66.04, 121.92) ↔ Track [OUT_VREF] on B.Cu, length 1.1903 mm @ (67.94, 121.92)
- Track [OUT_VREF] on F.Cu, length 3.3741 mm @ (67.94, 121.92) ↔ Track [OUT_VREF] on B.Cu, length 1.1903 mm @ (67.94, 121.92)
- Track [OUT_VREF] on F.Cu, length 3.0443 mm @ (107.31, 121.92) ↔ Track [OUT_VREF] on B.Cu, length 1.1903 mm @ (107.31, 121.92)
- Track [OUT_VREF] on B.Cu, length 1.1903 mm @ (107.31, 121.92) ↔ PTH pad 8 [OUT_VREF] of P8 @ (109.22, 121.92)
- Track [AIN2-] on B.Cu, length 5.0800 mm @ (109.22, 119.38) ↔ Track [AIN2-] on F.Cu, length 1.1903 mm @ (107.31, 119.38)
- Track [AIN2+] on B.Cu, length 5.0800 mm @ (109.22, 116.84) ↔ Track [AIN2+] on F.Cu, length 1.1903 mm @ (107.31, 116.84)
- Track [AIN3-] on B.Cu, length 5.0800 mm @ (109.22, 114.30) ↔ Track [AIN3-] on F.Cu, length 7.2125 mm @ (107.31, 114.30)
- Track [AIN3+] on B.Cu, length 5.0800 mm @ (109.22, 111.76) ↔ Track [AIN3+] on F.Cu, length 10.0975 mm @ (107.31, 111.76)
- PTH pad 13 [GPIO0] of P8 @ (109.22, 109.22) ↔ Track [GPIO0] on F.Cu, length 12.9825 mm @ (107.31, 109.22)
- PTH pad 14 [GPIO1] of P8 @ (109.22, 106.68) ↔ Track [GPIO1] on F.Cu, length 1.1903 mm @ (107.31, 106.68)
- Track [SYNC_IN] on F.Cu, length 1.3427 mm @ (67.94, 106.68) ↔ PTH pad 3 [SYNC_IN] of P10 @ (66.04, 106.68)
- Track [ADC_RESET] on F.Cu, length 1.1903 mm @ (67.94, 109.22) ↔ Track [ADC_RESET] on B.Cu, length 1.1903 mm @ (67.94, 109.22)
- Track [ADC_RESET] on F.Cu, length 1.1903 mm @ (67.94, 109.22) ↔ PTH pad 4 [ADC_RESET] of P10 @ (66.04, 109.22)
- Track [AIN7+] on F.Cu, length 1.1903 mm @ (67.94, 111.76) ↔ PTH pad 5 [AIN7+] of P10 @ (66.04, 111.76)
- Track [AIN7-] on F.Cu, length 1.1903 mm @ (67.94, 114.30) ↔ PTH pad 6 [AIN7-] of P10 @ (66.04, 114.30)
- Track [AIN6+] on F.Cu, length 1.1903 mm @ (67.94, 116.84) ↔ PTH pad 7 [AIN6+] of P10 @ (66.04, 116.84)
- Track [AIN6-] on F.Cu, length 1.1903 mm @ (67.94, 119.38) ↔ PTH pad 8 [AIN6-] of P10 @ (66.04, 119.38)
- PTH pad 13 [AIN5+] of P10 @ (66.04, 132.08) ↔ Track [AIN5+] on F.Cu, length 3.4293 mm @ (71.37, 132.08)
- PTH pad 14 [AIN5-] of P10 @ (66.04, 134.62) ↔ Track [AIN5-] on F.Cu, length 1.8488 mm @ (67.94, 134.62)

## Board

- Source: https://github.com/neuroidss/FreeSmartEEG (author neuroidss, license GPL-3.0)
- 2 layers, 15 nets, 83 components, 70.91 × 127.03 mm, 0 zones
- Human reference layout: 507 segments, 0 vias, 3845.03 mm of track, 861 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/FreeSmartEEG__autosave-AD7779 for Intel Edison umdk/`](../../../../fixtures/PCBench/FreeSmartEEG__autosave-AD7779%20for%20Intel%20Edison%20umdk/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 0 | None | 94 | 5.2 min | clean |
| 2.4.1 | COMPLETED | 5 | None | 94 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 6 | 0 | 52 | 3.6 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 1 | 0 | 4 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 1 | 0 | 4 | 16.2 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 4 | 23.1 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 4 | 9.5 min | clean |
| master-edffcabb | COMPLETED | 0 | 0 | 4 | 2.4 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `FreeSmartEEG__autosave-AD7779 for Intel Edison umdk--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
