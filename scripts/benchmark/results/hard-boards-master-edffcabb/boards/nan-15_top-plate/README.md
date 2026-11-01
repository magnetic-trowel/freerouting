# nan-15_top-plate

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 30 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 190.47617 |
| Wall clock | 1.3 min |
| Connections (total / still open) | 42 / 30 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 30 | 0 | 190.5 | stagnation | 0.2 min |
| direct-first | 30 | 0 | 190.5 | stagnation | 0.2 min |
| fanout-retry | 30 | 0 | 190.5 | stagnation | 0.2 min |
| short-escape | 30 | 0 | 190.5 | stagnation | 0.2 min |
| open-escape | 30 | 0 | 190.5 | stagnation | 0.2 min |
| no-fanout | 30 | 0 | 190.5 | stagnation | 0.2 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 30 | 0 |
| clearance | 0 | 0 |
| hole_clearance | 24 | 44 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 35 | 57 |

Open connections reported by KiCad:

- Pad 1 [GND] of D10 on B.Cu @ (108.95, 88.38) ↔ PTH pad 1 [GND] of W14 @ (113.72, 71.13)
- Pad 1 [GND] of D10 on B.Cu @ (108.95, 88.38) ↔ PTH pad 1 [GND] of W13 @ (113.72, 109.23)
- Pad 1 [GND] of D11 on B.Cu @ (108.95, 111.03) ↔ PTH pad 1 [GND] of W13 @ (113.72, 109.23)
- PTH pad 1 [GND] of W14 @ (113.72, 71.13) ↔ Pad 1 [GND] of D9 on B.Cu @ (108.95, 69.33)
- PTH pad 1 [GND] of W14 @ (113.72, 71.13) ↔ Pad 1 [GND] of D6 on B.Cu @ (142.87, 61.61)
- PTH pad 1 [GND] of W13 @ (113.72, 109.23) ↔ PTH pad 1 [GND] of W20 @ (125.62, 118.76)
- PTH pad 1 [GND] of W20 @ (125.62, 118.76) ↔ Pad 1 [GND] of D0 on B.Cu @ (123.82, 125.90)
- Pad 1 [GND] of D5 on B.Cu @ (144.67, 58.65) ↔ Pad 1 [GND] of D6 on B.Cu @ (142.87, 61.61)
- Pad 1 [GND] of D7 on B.Cu @ (144.67, 64.57) ↔ Pad 1 [GND] of D6 on B.Cu @ (142.87, 61.61)
- Pad 1 [GND] of D7 on B.Cu @ (144.67, 64.57) ↔ PTH pad 1 [GND] of W22 @ (144.67, 71.13)
- PTH pad 1 [GND] of W22 @ (144.67, 71.13) ↔ Pad 1 [GND] of D8 on B.Cu @ (144.67, 88.38)
- Pad 1 [GND] of D1 on B.Cu @ (165.52, 125.90) ↔ PTH pad 1 [GND] of W21 @ (163.72, 118.76)
- PTH pad 1 [GND] of W15 @ (175.63, 71.13) ↔ PTH pad 1 [GND] of W22 @ (144.67, 71.13)
- PTH pad 1 [GND] of W12 @ (175.63, 109.23) ↔ Pad 1 [GND] of D2 on B.Cu @ (180.39, 111.03)
- PTH pad 1 [GND] of W12 @ (175.63, 109.23) ↔ PTH pad 1 [GND] of W21 @ (163.72, 118.76)
- Pad 1 [GND] of D4 on B.Cu @ (180.39, 69.33) ↔ PTH pad 1 [GND] of W15 @ (175.63, 71.13)
- Pad 1 [GND] of D3 on B.Cu @ (180.39, 91.98) ↔ PTH pad 1 [GND] of W12 @ (175.63, 109.23)
- Pad 1 [GND] of D3 on B.Cu @ (180.39, 91.98) ↔ PTH pad 1 [GND] of W15 @ (175.63, 71.13)
- Pad 2 [Net-(D0-Pad2)] of D0 on B.Cu @ (127.42, 125.90) ↔ Pad 2 [Net-(D0-Pad2)] of R0 on B.Cu @ (162.27, 109.23)
- Pad 2 [Net-(D11-Pad2)] of D11 on B.Cu @ (108.95, 107.43) ↔ Pad 2 [Net-(D11-Pad2)] of R11 on B.Cu @ (143.22, 109.23)
- Pad 2 [Net-(D10-Pad2)] of D10 on B.Cu @ (108.95, 91.98) ↔ Pad 2 [Net-(D10-Pad2)] of R10 on B.Cu @ (136.08, 109.23)
- Pad 2 [Net-(D9-Pad2)] of D9 on B.Cu @ (108.95, 72.93) ↔ Pad 2 [Net-(D9-Pad2)] of R9 on B.Cu @ (138.98, 90.18)
- Pad 2 [Net-(D8-Pad2)] of D8 on B.Cu @ (144.67, 91.98) ↔ Pad 2 [Net-(D8-Pad2)] of R8 on B.Cu @ (144.67, 95.88)
- Pad 2 [Net-(D7-Pad2)] of R7 on B.Cu @ (138.98, 71.13) ↔ Pad 2 [Net-(D7-Pad2)] of D7 on B.Cu @ (144.67, 68.17)
- Pad 2 [Net-(D6-Pad2)] of D6 on B.Cu @ (146.47, 61.61) ↔ Pad 2 [Net-(D6-Pad2)] of R6 on B.Cu @ (150.37, 90.18)
- Pad 2 [Net-(D5-Pad2)] of D5 on B.Cu @ (144.67, 55.05) ↔ Pad 2 [Net-(D5-Pad2)] of R5 on B.Cu @ (163.72, 57.78)
- Pad 2 [Net-(D4-Pad2)] of R4 on B.Cu @ (163.72, 77.35) ↔ Pad 2 [Net-(D4-Pad2)] of D4 on B.Cu @ (180.39, 72.93)
- Pad 2 [Net-(D3-Pad2)] of R3 on B.Cu @ (163.72, 84.49) ↔ Pad 2 [Net-(D3-Pad2)] of D3 on B.Cu @ (180.39, 88.38)
- Pad 2 [Net-(D2-Pad2)] of R2 on B.Cu @ (164.65, 90.18) ↔ Pad 2 [Net-(D2-Pad2)] of D2 on B.Cu @ (180.39, 107.43)
- Pad 2 [Net-(D1-Pad2)] of D1 on B.Cu @ (161.92, 125.90) ↔ Pad 2 [Net-(D1-Pad2)] of R1 on B.Cu @ (163.72, 103.54)

## Board

- Source: https://github.com/trebb/nan-15 (author trebb, license NOASSERTION)
- 2 layers, 12 nets, 48 components, 77.2 × 77.2 mm, 0 zones
- Human reference layout: 333 segments, 2 vias, 1029.91 mm of track, 219 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/nan-15_top-plate/`](../../../../fixtures/PCBench/nan-15_top-plate/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 30 | None | 0 | 0.6 min | FAIL |
| 2.4.1 | COMPLETED | 30 | None | 0 | 1.7 min | FAIL |
| 2.5.0 | COMPLETED | 30 | 0 | 0 | 1.8 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 30 | 0 | 0 | 6.6 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 30 | 0 | 0 | 8.1 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 30 | 0 | 0 | 7.5 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 30 | 0 | 0 | 5.3 min | FAIL |
| master-edffcabb | COMPLETED | 30 | 0 | 0 | 1.3 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `nan-15_top-plate--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
