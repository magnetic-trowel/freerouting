# iso50_pcb

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 6 |
| Clearance violations (total) | 3 |
| of which router-introduced | 0 |
| of which pre-existing in input | 3 (unfixable: 3) |
| Worst violation depth | 102.1026611328125 µm |
| Quality score | 962.9607 |
| Wall clock | 9.4 min |
| Connections (total / still open) | 217 / 6 |
| Autorouter stop reason | stagnation after 21 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 6 | 0 | 963.0 | stagnation | 1.2 min |
| direct-first | 7 | 0 | 956.8 | stagnation | 1.5 min |
| fanout-retry | 7 | 0 | 956.8 | stagnation | 1.4 min |
| short-escape | 9 | 0 | 944.5 | stagnation | 2.3 min |
| open-escape | 7 | 0 | 956.8 | stagnation | 1.1 min |
| no-fanout | 10 | 0 | 938.4 | stagnation | 1.8 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 6 | 0 |
| clearance | 10 | 14 |
| hole_clearance | 58 | 14 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 9 | 9 |

Open connections reported by KiCad:

- Pad 33 [Net-(R3-Pad1)] of U1 on F.Cu @ (188.36, 178.62) ↔ PTH pad 1 [Net-(R3-Pad1)] of R3 @ (190.50, 149.51)
- Track [GND] on B.Cu, length 8.8266 mm @ (103.24, 149.93) ↔ Pad 5 [GND] of P1 on F.Cu @ (105.89, 150.93)
- Pad 1 [VCC] of P1 on F.Cu @ (108.49, 150.93) ↔ Pad 1 [VCC] of C4 on F.Cu @ (108.74, 153.32)
- Pad 1 [VCC] of C4 on F.Cu @ (108.74, 153.32) ↔ Track [VCC] on F.Cu, length 1.8303 mm @ (143.53, 169.68)
- Track [VCC] on F.Cu, length 1.2517 mm @ (180.82, 170.87) ↔ Pad 14 [VCC] of U1 on F.Cu @ (182.54, 173.79)
- Pad 24 [VCC] of U1 on F.Cu @ (183.87, 178.62) ↔ Pad 14 [VCC] of U1 on F.Cu @ (182.54, 173.79)

## Board

- Source: https://github.com/trebb/iso50 (author trebb, license NOASSERTION)
- 2 layers, 70 nets, 105 components, 274.92 × 71.44 mm, 2 zones
- Human reference layout: 1534 segments, 132 vias, 7060.27 mm of track, 686 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/iso50_pcb/`](../../../../fixtures/PCBench/iso50_pcb/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 217 | None | 10 | 0.1 min | FAIL |
| 2.4.1 | COMPLETED | 217 | None | 3 | 1.1 min | FAIL |
| 2.5.0 | COMPLETED | 29 | 0 | 3 | 6.3 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 23 | 0 | 3 | 20.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 20 | 0 | 3 | 32.9 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 4 | 0 | 3 | 33.7 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 6 | 0 | 3 | 9.4 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `iso50_pcb--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
