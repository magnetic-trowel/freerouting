# kitspace_XassetteAsterisk

Tier **D** (diagnostic). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 17 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 948.8337 |
| Wall clock | 82.7 min |
| Connections (total / still open) | 443 / 17 |
| Autorouter stop reason | stagnation after 45 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **direct-first**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 30 | 0 | 909.7 | stagnation | 9.7 min |
| direct-first | 17 | 0 | 948.8 | stagnation | 19.1 min |
| fanout-retry | 25 | 0 | 924.8 | stagnation | 13.5 min |
| short-escape | 18 | 0 | 945.8 | stagnation | 13.2 min |
| open-escape | 30 | 0 | 909.7 | stagnation | 17.0 min |
| no-fanout | 37 | 0 | 888.6 | stagnation | 10.2 min |

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
| clearance | 0 | 11 |
| hole_clearance | 31 | 28 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 1 |
| copper_edge_clearance | 18 | 2 |

Open connections reported by KiCad:

- Track [/VDD-CORE] on F.Cu, length 1.2200 mm @ (136.66, 132.92) ↔ Track [/VDD-CORE] on B.Cu, length 1.0318 mm @ (137.41, 133.18)
- Pad 72 [/LCD0-D21] of U1 on F.Cu @ (139.22, 126.06) ↔ Pad 26 [/LCD0-D21] of J1 on F.Cu @ (150.45, 125.25)
- Pad 73 [/LCD0-D22] of U1 on F.Cu @ (138.94, 125.78) ↔ Pad 27 [/LCD0-D22] of J1 on F.Cu @ (150.45, 124.75)
- Pad 54 [/LCD0-HSYNC] of U1 on F.Cu @ (138.37, 132.94) ↔ Pad 32 [/LCD0-HSYNC] of J1 on F.Cu @ (150.45, 122.25)
- Pad 53 [/LCD0-VSYNC] of U1 on F.Cu @ (138.09, 133.22) ↔ Pad 33 [/LCD0-VSYNC] of J1 on F.Cu @ (150.45, 121.75)
- Pad 101 [/GPADC0] of U1 on F.Cu @ (129.23, 120.40) ↔ PTH pad 1 [/GPADC0] of J9 @ (140.05, 115.10)
- Pad 104 [/TP_Y1] of U1 on F.Cu @ (128.39, 121.25) ↔ PTH pad 4 [/TP_Y1] of J9 @ (144.29, 119.34)
- Pad 105 [/TP_Y2] of U1 on F.Cu @ (128.10, 121.54) ↔ PTH pad 5 [/TP_Y2] of J9 @ (145.71, 120.76)
- Track [/PF6] on F.Cu, length 0.5800 mm @ (109.42, 137.09) ↔ Track [/PF6] on B.Cu, length 3.0321 mm @ (121.68, 137.14)
- Pad 32 [/TWI2-SCK] of U1 on F.Cu @ (130.37, 138.88) ↔ Pad 3 [/TWI2-SCK] of J2 on F.Cu @ (135.07, 150.43)
- Pad 36 [/NCSI0-D7] of U1 on F.Cu @ (133.28, 138.03) ↔ Pad 15 [/NCSI0-D7] of J2 on F.Cu @ (141.07, 150.43)
- Pad 40 [/NCSI0-D3] of U1 on F.Cu @ (134.41, 136.90) ↔ Pad 19 [/NCSI0-D3] of J2 on F.Cu @ (143.07, 150.43)
- Pad 42 [/NCSI0-D1] of U1 on F.Cu @ (134.98, 136.33) ↔ Pad 21 [/NCSI0-D1] of J2 on F.Cu @ (144.07, 150.43)
- Pad 45 [/NCSI0-VSYNC] of U1 on F.Cu @ (135.83, 135.48) ↔ Pad 8 [/NCSI0-VSYNC] of RN1 on F.Cu @ (137.12, 146.53)
- Pad 87 [/MIC3P] of U1 on F.Cu @ (134.98, 121.82) ↔ Pad 1 [/MIC3P] of C50 on F.Cu @ (137.01, 113.45)
- PTH pad 4 [/FMINR] of J10 @ (126.78, 114.20) ↔ Pad 93 [/FMINR] of U1 on F.Cu @ (133.28, 120.12)
- Pad 98 [/HPOUTR] of U1 on F.Cu @ (130.08, 119.56) ↔ Pad 2 [/HPOUTR] of C42 on F.Cu @ (146.28, 114.64)

## Board

- Source: https://github.com/SdtElectronics/Xassette-Asterisk (author , license [{'name': 'CERN', 'link': 'https://raw.githubusercontent.com/SdtElectronics/Xassette-Asterisk/master/LICENCE'}])
- 2 layers, 46 nets, 145 components, 56.0 × 56.0 mm, 2 zones
- Human reference layout: 2044 segments, 265 vias, 3196.27 mm of track, 472 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_XassetteAsterisk/`](../../../../fixtures/PCBench/kitspace_XassetteAsterisk/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | FAILED | 22 | None | 6 | 17.0 min | FAIL |
| 2.4.1 | COMPLETED | 87 | None | 2 | 15.2 min | FAIL |
| 2.5.0 | TIMED_OUT | 26 | 0 | 0 | 20.1 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 35 | 0 | 0 | 20.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 19 | 0 | 0 | 277.6 min | FAIL |
| 2.6.0-RC3 | TIMED_OUT | 28 | 0 | 0 | 360.3 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 17 | 0 | 0 | 82.7 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_XassetteAsterisk--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
