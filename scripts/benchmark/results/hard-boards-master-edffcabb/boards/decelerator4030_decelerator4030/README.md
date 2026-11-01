# decelerator4030_decelerator4030

Tier **D** (diagnostic). Category: **timeout**: hit the 6 h job budget before the router finished. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | TIMED_OUT |
| Unrouted connections | 11 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 985.4642 |
| Wall clock | 360.1 min |
| Connections (total / still open) | 1009 / 11 |
| Autorouter stop reason | stagnation after 42 passes |
| Optimizer | None |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 11 | 0 | 985.5 | stagnation | 97.3 min |
| direct-first | 19 | 0 | 974.9 | stagnation | 88.6 min |
| fanout-retry | 472 | 0 | 376.3 | timed_out | 174.1 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 11 | 0 |
| clearance | 0 | 10 |
| hole_clearance | 0 | 9 |
| shorting_items | 0 | 0 |
| tracks_crossing | 2 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Pad 90 [GND] of U1 on Gorna @ (104.14, 54.61) ↔ Track [GND] on Dolna, length 6.7781 mm @ (105.74, 61.18)
- Pad C2 [/DRM9] of U16 on Gorna @ (176.25, 66.62) ↔ Track [/DRM9] on Dolna, length 0.4801 mm @ (168.87, 67.72)
- Track [/DRM10] on Dolna, length 0.3503 mm @ (165.20, 67.85) ↔ Pad C1 [/DRM10] of U16 on Gorna @ (177.00, 66.62)
- Pad D2 [/DRM11] of U16 on Gorna @ (176.25, 65.87) ↔ Track [/DRM11] on Dolna, length 5.4943 mm @ (164.55, 73.17)
- Pad 31 [/DRM14] of U3 on Gorna @ (161.89, 63.49) ↔ Pad F2 [/DRM14] of U16 on Gorna @ (176.25, 64.37)
- Track [/DRM5] on Dolna, length 1.0593 mm @ (167.49, 66.05) ↔ Pad F6 [/DRM5] of U16 on Gorna @ (173.25, 64.37)
- Pad 38 [/DRM27] of U4 on Gorna @ (195.98, 67.00) ↔ Pad D2 [/DRM27] of U17 on Gorna @ (209.63, 65.89)
- Pad F1 [/DRM29] of U17 on Gorna @ (210.38, 64.39) ↔ Pad 33 [/DRM29] of U4 on Gorna @ (195.98, 64.50)
- Pad C3 [/ARM7] of U16 on Gorna @ (175.50, 66.62) ↔ Track [/ARM7] on Dolna, length 18.1575 mm @ (176.55, 49.68)
- Pad B3 [/ARM6] of U16 on Gorna @ (175.50, 67.37) ↔ Track [/ARM6] on Dolna, length 2.7206 mm @ (180.51, 69.49)
- Pad B5 [_SRAM_HI_CE] of U17 on Gorna @ (207.38, 67.39) ↔ Track [_SRAM_HI_CE] on Gorna, length 1.1512 mm @ (195.98, 61.00)

## Board

- Source: https://github.com/Sakura-IT/decelerator4030 (author Sakura-IT, license None)
- 2 layers, 0 nets, 180 components, 0.0 × 0.0 mm, 2 zones
- Human reference layout: 7707 segments, 865 vias, 28322.14 mm of track, 953 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/decelerator4030_decelerator4030/`](../../../../fixtures/PCBench/decelerator4030_decelerator4030/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | FAILED | 334 | None | 0 | 17.0 min | FAIL |
| 2.4.1 | COMPLETED | 611 | None | 0 | 15.2 min | FAIL |
| 2.5.0 | TIMED_OUT | 458 | 0 | 0 | 20.2 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 477 | 0 | 0 | 20.1 min | FAIL |
| 2.6.0-RC2 | TIMED_OUT | 108 | 0 | 0 | 480.2 min | FAIL |
| 2.6.0-RC3 | TIMED_OUT | 32 | 0 | 0 | 360.8 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | TIMED_OUT | 11 | 0 | 0 | 360.1 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `decelerator4030_decelerator4030--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
