# karabas-nano_karabas-nano-revG

Tier **D** (diagnostic). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 13 |
| Clearance violations (total) | 1 |
| of which router-introduced | 0 |
| of which pre-existing in input | 1 (unfixable: 1) |
| Worst violation depth | 50.0 µm |
| Quality score | 972.67993 |
| Wall clock | 186.4 min |
| Connections (total / still open) | 635 / 13 |
| Autorouter stop reason | stagnation after 35 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **no-fanout**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 20 | 0 | 958.0 | stagnation | 35.4 min |
| direct-first | 20 | 0 | 958.0 | stagnation | 28.5 min |
| fanout-retry | 18 | 0 | 962.2 | stagnation | 34.6 min |
| short-escape | 18 | 0 | 962.2 | stagnation | 31.0 min |
| open-escape | 15 | 0 | 968.5 | stagnation | 27.7 min |
| no-fanout | 13 | 0 | 972.7 | stagnation | 29.1 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 13 | 0 |
| clearance | 0 | 16 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 15 | 1 |

Open connections reported by KiCad:

- Track [VCC] on F.Cu, length 2.1131 mm @ (168.15, 69.69) ↔ Track [VCC] on F.Cu, length 9.7576 mm @ (169.31, 68.37)
- Pad 4 [/D5] of U1 on F.Cu @ (141.75, 75.87) ↔ Pad 20 [/D5] of U2 on F.Cu @ (170.69, 84.05)
- Track [/D6] on F.Cu, length 0.7927 mm @ (166.36, 100.08) ↔ Track [/D6] on F.Cu, length 0.6767 mm @ (168.34, 97.56)
- Pad 96 [/D2] of U6 on F.Cu @ (136.39, 77.89) ↔ Pad 7 [/D2] of U1 on F.Cu @ (141.75, 78.27)
- Track [/D2] on F.Cu, length 1.1687 mm @ (164.34, 84.05) ↔ Pad 7 [/D2] of U1 on F.Cu @ (141.75, 78.27)
- Pad 81 [/~{HALT}] of U6 on F.Cu @ (136.39, 85.39) ↔ Pad 14 [/~{HALT}] of U1 on F.Cu @ (145.56, 83.67)
- PTH pad 20 [/A0] of J6 @ (146.05, 53.98) ↔ Track [/A0] on B.Cu, length 5.9773 mm @ (146.84, 77.67)
- Pad 100 [/A2] of U6 on F.Cu @ (136.39, 75.89) ↔ Track [/A2] on F.Cu, length 7.1569 mm @ (154.16, 76.67)
- Pad 28 [/A9] of U2 on F.Cu @ (172.44, 73.41) ↔ Track [/A9] on F.Cu, length 0.5943 mm @ (159.34, 69.47)
- PTH pad 12 [/A11] of J6 @ (135.89, 53.98) ↔ Track [/A11] on B.Cu, length 0.1745 mm @ (130.47, 66.22)
- Pad 2 [/AY_BDIR] of U6 on F.Cu @ (115.07, 72.39) ↔ Pad 1 [/AY_BDIR] of U4 on B.Cu @ (176.34, 96.89)
- PTH pad 9 [/TDI] of J7 @ (184.40, 109.22) ↔ Pad 4 [/TDI] of U6 on F.Cu @ (115.07, 73.39)
- Pad 2 [/AY_OUT_R] of R29 on B.Cu @ (97.49, 63.50) ↔ Pad 14 [/AY_OUT_R] of U4 on B.Cu @ (170.88, 103.94)

## Board

- Source: https://github.com/andykarpov/karabas-nano (author andykarpov, license WTFPL)
- 2 layers, 70 nets, 157 components, 99.57 × 99.57 mm, 3 zones
- Human reference layout: 4082 segments, 776 vias, 10188.67 mm of track, 875 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/karabas-nano_karabas-nano-revG/`](../../../../fixtures/PCBench/karabas-nano_karabas-nano-revG/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | FAILED | 33 | None | 141 | 17.0 min | FAIL |
| 2.4.1 | TIMED_OUT | 123 | None | 10 | 15.3 min | FAIL |
| 2.5.0 | TIMED_OUT | 53 | 0 | 1 | 20.1 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 27 | 0 | 1 | 20.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 13 | 0 | 1 | 331.6 min | FAIL |
| 2.6.0-RC3 | TIMED_OUT | 14 | 0 | 1 | 360.8 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 13 | 0 | 1 | 186.4 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `karabas-nano_karabas-nano-revG--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
