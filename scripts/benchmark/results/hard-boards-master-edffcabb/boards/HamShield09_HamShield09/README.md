# HamShield09_HamShield09

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 17 |
| Clearance violations (total) | 18 |
| of which router-introduced | 0 |
| of which pre-existing in input | 18 (unfixable: 18) |
| Worst violation depth | 53.9036865234375 µm |
| Quality score | 941.4601 |
| Wall clock | 10.8 min |
| Connections (total / still open) | 389 / 17 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 17 | 0 | 941.5 | stagnation | 1.3 min |
| direct-first | 18 | 1 | 938.0 | stagnation | 2.2 min |
| fanout-retry | 18 | 0 | 938.0 | stagnation | 2.7 min |
| short-escape | 15 | 1 | 948.3 | stagnation | 1.4 min |
| open-escape | 18 | 0 | 938.0 | stagnation | 1.3 min |
| no-fanout | 17 | 0 | 941.5 | stagnation | 2.0 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 17 | 0 |
| clearance | 0 | 4 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 14 | 8 |

Open connections reported by KiCad:

- Pad 1 [VCC] of C14 on Front @ (110.72, 130.10) ↔ Pad 1 [VCC] of C15 on Front @ (109.58, 130.10)
- Pad 1 [VCC] of C14 on Front @ (110.72, 130.10) ↔ Pad 16 [VCC] of U1 on Front @ (112.75, 130.10)
- Pad 21 [VCC] of U1 on Front @ (112.00, 132.85) ↔ Pad 16 [VCC] of U1 on Front @ (112.75, 130.10)
- Pad 23 [VCC] of U1 on Front @ (112.00, 133.85) ↔ Pad 21 [VCC] of U1 on Front @ (112.00, 132.85)
- Pad 23 [VCC] of U1 on Front @ (112.00, 133.85) ↔ Track [VCC] on Front, length 1.1000 mm @ (108.90, 134.35)
- Pad 1 [VCC] of C12 on Front @ (114.22, 127.27) ↔ Pad 1 [VCC] of C13 on Front @ (114.22, 126.32)
- Pad 1 [VCC] of C12 on Front @ (114.22, 127.27) ↔ Pad 13 [VCC] of U1 on Front @ (114.25, 130.10)
- Pad 13 [VCC] of U1 on Front @ (114.25, 130.10) ↔ Pad 16 [VCC] of U1 on Front @ (112.75, 130.10)
- Pad 1 [VCC] of U1 on Front @ (117.00, 134.35) ↔ Pad 4 [VCC] of U1 on Front @ (117.00, 132.85)
- Pad 1 [VCC] of U1 on Front @ (117.00, 134.35) ↔ Track [VCC] on Front, length 0.8054 mm @ (117.46, 137.59)
- Pad 1 [VCC] of Q7 on Front @ (131.38, 91.18) ↔ Pad 2 [VCC] of R15 on Front @ (133.28, 93.67)
- Pad 1 [VCC] of Q6 on Front @ (134.85, 93.68) ↔ Pad 2 [VCC] of R15 on Front @ (133.28, 93.67)
- PTH pad 5 [/GPIO3] of P1 @ (107.72, 141.00) ↔ Pad 29 [/GPIO3] of U1 on Front @ (114.75, 135.10)
- Track [VTX] on Front, length 1.1250 mm @ (102.30, 126.65) ↔ Pad 1 [VTX] of C74 on Front @ (103.88, 129.47)
- Pad 1 [VTX] of C74 on Front @ (103.88, 129.47) ↔ Pad 1 [VTX] of U4 on Front @ (105.40, 129.47)
- Track [VTX] on GND.Cu, length 4.3385 mm @ (112.55, 142.83) ↔ Pad 1 [VTX] of U4 on Front @ (105.40, 129.47)
- Track [AVDD] on Front, length 2.0800 mm @ (138.76, 105.97) ↔ Pad 4 [AVDD] of U2 on Front @ (136.68, 109.06)

## Board

- Source: https://github.com/EnhancedRadioDevices/HamShield09 (author EnhancedRadioDevices, license None)
- 4 layers, 85 nets, 173 components, 53.3 × 68.58 mm, 4 zones
- Human reference layout: 1781 segments, 473 vias, 3013.58 mm of track, 965 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/HamShield09_HamShield09/`](../../../../fixtures/PCBench/HamShield09_HamShield09/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 25 | None | 66 | 11.4 min | FAIL |
| 2.4.1 | TIMED_OUT | 27 | None | 58 | 15.3 min | FAIL |
| 2.5.0 | COMPLETED | 35 | 0 | 18 | 14.2 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 19 | 0 | 18 | 20.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 15 | 0 | 18 | 41.9 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 15 | 0 | 18 | 76.5 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 17 | 0 | 18 | 10.8 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `HamShield09_HamShield09--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
