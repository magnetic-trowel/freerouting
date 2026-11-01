# ArduinoDueClone_ATSAM3X8EA

Tier **D** (diagnostic). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 10 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 941.0029 |
| Wall clock | 68.3 min |
| Connections (total / still open) | 226 / 10 |
| Autorouter stop reason | stagnation after 61 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **short-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 14 | 0 | 917.4 | stagnation | 7.4 min |
| direct-first | 11 | 1 | 935.1 | stagnation | 12.9 min |
| fanout-retry | 19 | 0 | 887.9 | stagnation | 8.6 min |
| short-escape | 10 | 0 | 941.0 | stagnation | 19.1 min |
| open-escape | 16 | 0 | 905.6 | stagnation | 9.8 min |
| no-fanout | 23 | 0 | 864.3 | stagnation | 10.4 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 10 | 0 |
| clearance | 140 | 140 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 6 | 0 |

Open connections reported by KiCad:

- PTH pad 9 [/ATSAM3X8EA-AU/PA17] of P9 @ (118.62, 123.44) ↔ Pad 9 [/ATSAM3X8EA-AU/PA17] of U1 on F.Cu @ (131.54, 96.85)
- PTH pad 6 [/ATSAM3X8EA-AU/PD0] of P14 @ (128.52, 59.18) ↔ Pad 13 [/ATSAM3X8EA-AU/PD0] of U1 on F.Cu @ (131.54, 98.85)
- Pad 16 [/ATSAM3X8EA-AU/PD3] of U1 on F.Cu @ (131.54, 100.35) ↔ PTH pad 9 [/ATSAM3X8EA-AU/PD3] of P14 @ (133.60, 61.72)
- PTH pad 4 [/ATSAM3X8EA-AU/PD7] of P9 @ (118.62, 110.74) ↔ Pad 20 [/ATSAM3X8EA-AU/PD7] of U1 on F.Cu @ (131.54, 102.35)
- Pad 24 [/ATSAM3X8EA-AU/PA1] of U1 on F.Cu @ (131.54, 104.35) ↔ PTH pad 7 [/ATSAM3X8EA-AU/PA1] of P10 @ (167.39, 71.37)
- Pad 67 [/ATSAM3X8EA-AU/PC9] of U1 on F.Cu @ (148.49, 112.30) ↔ PTH pad 22 [/ATSAM3X8EA-AU/PC9] of P14 @ (148.84, 59.18)
- Pad 72 [/ATSAM3X8EA-AU/PA20] of U1 on F.Cu @ (150.99, 112.30) ↔ PTH pad 24 [/ATSAM3X8EA-AU/PA20] of P14 @ (151.38, 59.18)
- Pad 97 [/ATSAM3X8EA-AU/PC15] of U1 on F.Cu @ (152.94, 98.35) ↔ PTH pad 29 [/ATSAM3X8EA-AU/PC15] of P14 @ (159.00, 61.72)
- Pad 99 [/ATSAM3X8EA-AU/PC17] of U1 on F.Cu @ (152.94, 97.35) ↔ PTH pad 27 [/ATSAM3X8EA-AU/PC17] of P14 @ (156.46, 61.72)
- PTH pad 7 [/ATSAM3X8EA-AU/PC24] of P13 @ (118.62, 97.03) ↔ Pad 135 [/ATSAM3X8EA-AU/PC24] of U1 on F.Cu @ (137.99, 90.90)

## Board

- Source: https://github.com/digibird1/ArduinoDueClone (author digibird1, license None)
- 2 layers, 18 nets, 75 components, 160.0 × 100.0 mm, 5 zones
- Human reference layout: 1762 segments, 189 vias, 5464.1 mm of track, 768 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/ArduinoDueClone_ATSAM3X8EA/`](../../../../fixtures/PCBench/ArduinoDueClone_ATSAM3X8EA/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 104 | None | 137 | 2.5 min | FAIL |
| 2.4.1 | COMPLETED | 92 | None | 0 | 5.1 min | FAIL |
| 2.5.0 | TIMED_OUT | 70 | 0 | 0 | 10.2 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 40 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 5 | 0 | 0 | 172.3 min | FAIL |
| 2.6.0-RC3 | TIMED_OUT | 14 | 0 | 0 | 360.4 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 10 | 0 | 0 | 68.3 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `ArduinoDueClone_ATSAM3X8EA--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
