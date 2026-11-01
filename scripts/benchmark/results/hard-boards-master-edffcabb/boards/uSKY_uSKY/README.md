# uSKY_uSKY

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 10 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 784.9462 |
| Wall clock | 1.0 min |
| Connections (total / still open) | 62 / 10 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **short-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 13 | 0 | 720.4 | stagnation | 0.2 min |
| direct-first | 12 | 0 | 741.9 | stagnation | 0.1 min |
| fanout-retry | 14 | 0 | 698.9 | stagnation | 0.2 min |
| short-escape | 10 | 0 | 784.9 | stagnation | 0.2 min |
| open-escape | 10 | 0 | 784.9 | stagnation | 0.2 min |
| no-fanout | 14 | 0 | 698.9 | stagnation | 0.1 min |

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
| clearance | 0 | 0 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 58 | 112 |

Open connections reported by KiCad:

- Pad 12 [/ADC0] of U1 on B.Cu @ (24.62, 24.72) ↔ Pad 1 [/ADC0] of ADC0 on F.Cu @ (26.90, 22.45)
- Track [GND] on F.Cu, length 0.5553 mm @ (20.39, 24.70) ↔ Track [GND] on B.Cu, length 0.0096 mm @ (19.21, 24.75)
- Track [GND] on F.Cu, length 0.7303 mm @ (19.86, 18.28) ↔ Track [GND] on B.Cu, length 0.2461 mm @ (18.02, 19.40)
- Pad 20 [Net-(C2-Pad1)] of U1 on B.Cu @ (20.62, 23.22) ↔ Pad 1 [Net-(C2-Pad1)] of X1 on F.Cu @ (21.49, 24.14)
- Pad 1 [Net-(D1-Pad1)] of D1 on F.Cu @ (25.25, 20.35) ↔ Pad 3 [Net-(D1-Pad1)] of U1 on B.Cu @ (26.62, 20.72)
- Pad 11 [Net-(R1-Pad1)] of U1 on B.Cu @ (25.12, 24.72) ↔ Pad 1 [Net-(R1-Pad1)] of R2 on F.Cu @ (25.40, 24.49)
- Pad 27 [Net-(R3-Pad2)] of U1 on B.Cu @ (20.62, 19.72) ↔ Pad 2 [Net-(R3-Pad2)] of R3 on F.Cu @ (21.26, 19.92)
- Pad 31 [/~{RESET}] of U1 on B.Cu @ (23.12, 18.72) ↔ Track [/~{RESET}] on F.Cu, length 1.5377 mm @ (24.17, 19.90)
- Pad 9 [/SBUS_PPM] of U1 on B.Cu @ (26.62, 23.72) ↔ Pad 1 [/SBUS_PPM] of SBUS_PPM1 on F.Cu @ (26.90, 23.76)
- Pad 1 [Net-(D2-Pad1)] of D2 on B.Cu @ (18.53, 18.15) ↔ Pad 17 [Net-(D2-Pad1)] of U1 on B.Cu @ (22.12, 24.72)

## Board

- Source: https://github.com/fishpepper/uSKY (author fishpepper, license NOASSERTION)
- 2 layers, 26 nets, 36 components, 11.16 × 8.39 mm, 2 zones
- Human reference layout: 292 segments, 23 vias, 174.59 mm of track, 386 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/uSKY_uSKY/`](../../../../fixtures/PCBench/uSKY_uSKY/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 19 | None | 2 | 0.9 min | FAIL |
| 2.4.1 | COMPLETED | 19 | None | 28 | 2.0 min | FAIL |
| 2.5.0 | COMPLETED | 18 | 0 | 0 | 1.0 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 10 | 0 | 0 | 3.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 10 | 0 | 0 | 2.3 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 12 | 0 | 0 | 3.4 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 11 | 0 | 0 | 2.6 min | FAIL |
| master-edffcabb | COMPLETED | 10 | 0 | 0 | 1.0 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `uSKY_uSKY--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
