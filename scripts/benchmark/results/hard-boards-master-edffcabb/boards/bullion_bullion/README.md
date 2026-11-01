# bullion_bullion

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 20 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 420.28983 |
| Wall clock | 0.9 min |
| Connections (total / still open) | 46 / 20 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 20 | 0 | 420.3 | stagnation | 0.2 min |
| direct-first | 23 | 0 | 333.3 | stagnation | 0.1 min |
| fanout-retry | 20 | 0 | 420.3 | stagnation | 0.2 min |
| short-escape | 23 | 0 | 333.3 | stagnation | 0.1 min |
| open-escape | 22 | 0 | 362.3 | stagnation | 0.1 min |
| no-fanout | 23 | 0 | 333.3 | stagnation | 0.1 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 20 | 0 |
| clearance | 29 | 63 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 6 |

Open connections reported by KiCad:

- Pad 1 [+5V] of P1 on F.Cu @ (126.79, 109.59) ↔ Track [+5V] on F.Cu, length 0.9325 mm @ (124.59, 104.86)
- Pad 1 [+5V] of P1 on F.Cu @ (126.79, 109.59) ↔ Track [+5V] on B.Cu, length 5.9644 mm @ (138.76, 109.59)
- Pad 18 [+5V] of IC1 on F.Cu @ (142.09, 102.75) ↔ Track [+5V] on B.Cu, length 4.1326 mm @ (145.90, 102.45)
- Pad 4 [+5V] of IC1 on F.Cu @ (144.49, 94.25) ↔ Track [+5V] on B.Cu, length 3.7718 mm @ (143.23, 90.82)
- Pad 4 [+5V] of IC1 on F.Cu @ (144.49, 94.25) ↔ Pad 6 [+5V] of IC1 on F.Cu @ (142.89, 94.25)
- Pad 29 [Net-(C2-Pad1)] of IC1 on F.Cu @ (148.34, 98.10) ↔ Pad 1 [Net-(C2-Pad1)] of C2 on F.Cu @ (123.14, 95.09)
- Track [Net-(C2-Pad1)] on F.Cu, length 14.6565 mm @ (151.46, 113.23) ↔ Pad 29 [Net-(C2-Pad1)] of IC1 on F.Cu @ (148.34, 98.10)
- Pad 2 [RXD] of U2 on F.Cu @ (131.10, 101.55) ↔ Pad 30 [RXD] of IC1 on F.Cu @ (148.34, 97.30)
- Pad 3 [TXD] of U2 on F.Cu @ (131.10, 100.28) ↔ Pad 31 [TXD] of IC1 on F.Cu @ (148.34, 96.50)
- Pad 3 [Net-(P1-Pad3)] of P1 on F.Cu @ (128.09, 109.59) ↔ Pad 5 [Net-(P1-Pad3)] of U2 on F.Cu @ (131.10, 97.74)
- Pad 2 [Net-(P1-Pad2)] of P1 on F.Cu @ (127.44, 109.59) ↔ Pad 6 [Net-(P1-Pad2)] of U2 on F.Cu @ (131.10, 96.47)
- Pad 7 [CLKOUT] of U2 on F.Cu @ (131.10, 95.20) ↔ Pad 12 [CLKOUT] of IC1 on F.Cu @ (139.84, 98.10)
- Pad 2 [Net-(C4-Pad1)] of Y1 on F.Cu @ (149.83, 86.44) ↔ Pad 7 [Net-(C4-Pad1)] of IC1 on F.Cu @ (142.09, 94.25)
- Pad 8 [Net-(C3-Pad1)] of IC1 on F.Cu @ (141.29, 94.25) ↔ Pad 1 [Net-(C3-Pad1)] of Y1 on F.Cu @ (140.13, 86.44)
- Pad 1 [Net-(IC1-Pad9)] of R4 on F.Cu @ (138.36, 91.01) ↔ Pad 9 [Net-(IC1-Pad9)] of IC1 on F.Cu @ (139.84, 95.70)
- Pad 14 [Net-(IC1-Pad14)] of IC1 on F.Cu @ (139.84, 99.70) ↔ PTH pad 3 [Net-(IC1-Pad14)] of P2 @ (141.30, 113.23)
- Pad 15 [Net-(IC1-Pad15)] of IC1 on F.Cu @ (139.84, 100.50) ↔ PTH pad 4 [Net-(IC1-Pad15)] of P2 @ (143.84, 113.23)
- Pad 16 [Net-(IC1-Pad16)] of IC1 on F.Cu @ (139.84, 101.30) ↔ PTH pad 5 [Net-(IC1-Pad16)] of P2 @ (146.38, 113.23)
- Pad 17 [Net-(IC1-Pad17)] of IC1 on F.Cu @ (141.29, 102.75) ↔ PTH pad 6 [Net-(IC1-Pad17)] of P2 @ (148.92, 113.23)
- Track [MM_TXD] on F.Cu, length 1.4017 mm @ (163.25, 97.89) ↔ Pad 32 [MM_TXD] of IC1 on F.Cu @ (148.34, 95.70)

## Board

- Source: https://github.com/coddingtonbear/bullion (author coddingtonbear, license None)
- 2 layers, 38 nets, 21 components, 45.14 × 33.54 mm, 3 zones
- Human reference layout: 226 segments, 22 vias, 545.86 mm of track, 199 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/bullion_bullion/`](../../../../fixtures/PCBench/bullion_bullion/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 20 | None | 32 | 0.7 min | FAIL |
| 2.4.1 | COMPLETED | 18 | None | 0 | 2.1 min | FAIL |
| 2.5.0 | COMPLETED | 22 | 0 | 0 | 1.1 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 20 | 0 | 0 | 4.0 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 20 | 0 | 0 | 4.2 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 19 | 0 | 0 | 2.9 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 19 | 0 | 0 | 2.4 min | FAIL |
| master-edffcabb | COMPLETED | 20 | 0 | 0 | 0.9 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `bullion_bullion--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
