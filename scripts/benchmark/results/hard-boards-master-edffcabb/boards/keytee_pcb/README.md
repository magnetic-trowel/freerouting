# keytee_pcb

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 17 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 609.1954 |
| Wall clock | 2.1 min |
| Connections (total / still open) | 58 / 17 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **direct-first**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 19 | 0 | 563.2 | stagnation | 0.4 min |
| direct-first | 17 | 0 | 609.2 | stagnation | 0.3 min |
| fanout-retry | 18 | 0 | 586.2 | stagnation | 0.4 min |
| short-escape | 18 | 0 | 586.2 | stagnation | 0.3 min |
| open-escape | 18 | 0 | 586.2 | stagnation | 0.3 min |
| no-fanout | 20 | 0 | 540.2 | stagnation | 0.3 min |

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
| clearance | 0 | 1 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 65 | 83 |

Open connections reported by KiCad:

- Pad 2 [Net-(C5-Pad1)] of Y1 on F.Cu @ (141.25, 90.35) ↔ Pad 1 [Net-(C5-Pad1)] of C5 on F.Cu @ (140.03, 90.55)
- Pad 2 [Net-(C5-Pad1)] of IC1 on F.Cu @ (147.42, 90.81) ↔ Pad 2 [Net-(C5-Pad1)] of Y1 on F.Cu @ (141.25, 90.35)
- Pad 2 [GND] of C1 on F.Cu @ (134.35, 88.37) ↔ Track [GND] on B.Cu, length 0.6859 mm @ (134.41, 90.78)
- Pad 12 [/D6] of IC1 on F.Cu @ (151.52, 91.87) ↔ Pad 1 [/D6] of W25 on F.Cu @ (157.65, 90.30)
- Pad 14 [/B0] of IC1 on F.Cu @ (152.23, 91.17) ↔ Pad 1 [/B0] of W0 on F.Cu @ (161.80, 90.30)
- Track [VCC] on F.Cu, length 0.6269 mm @ (147.42, 89.19) ↔ Pad 1 [VCC] of R1 on F.Cu @ (154.55, 86.90)
- Pad 4 [VCC] of IC1 on F.Cu @ (148.13, 91.52) ↔ Track [VCC] on F.Cu, length 0.6267 mm @ (147.07, 89.54)
- Pad 1 [/C4] of W27 on F.Cu @ (136.55, 87.20) ↔ Pad 26 [/C4] of IC1 on F.Cu @ (149.19, 87.42)
- Pad 1 [/C2] of W26 on F.Cu @ (136.55, 90.30) ↔ Pad 5 [/C2] of IC1 on F.Cu @ (148.48, 91.87)
- Pad 18 [/B4] of IC1 on F.Cu @ (152.58, 89.19) ↔ Pad 1 [/B4] of W15 on F.Cu @ (161.80, 87.20)
- Pad 1 [Net-(IC1-Pad24)] of W8 on B.Cu @ (157.65, 90.30) ↔ Track [Net-(IC1-Pad24)] on F.Cu, length 0.0336 mm @ (153.49, 86.90)
- Pad 1 [Net-(IC1-Pad30)] of R3 on F.Cu @ (139.63, 89.06) ↔ Pad 30 [Net-(IC1-Pad30)] of IC1 on F.Cu @ (147.77, 88.83)
- Pad 1 [Net-(R3-Pad2)] of W5 on B.Cu @ (132.40, 88.75) ↔ Pad 2 [Net-(R3-Pad2)] of R3 on F.Cu @ (138.57, 89.06)
- Pad 2 [Net-(C6-Pad2)] of C6 on F.Cu @ (138.60, 87.12) ↔ Pad 27 [Net-(C6-Pad2)] of IC1 on F.Cu @ (148.83, 87.77)
- Pad 15 [/B1] of IC1 on F.Cu @ (152.58, 90.81) ↔ Pad 1 [/B1] of W1 on B.Cu @ (161.80, 90.30)
- Pad 17 [/B3] of IC1 on F.Cu @ (152.93, 89.54) ↔ Pad 1 [/B3] of W3 on B.Cu @ (161.80, 88.75)
- Pad 21 [/B7] of IC1 on F.Cu @ (151.52, 88.13) ↔ Pad 1 [/B7] of W18 on B.Cu @ (157.65, 87.20)

## Board

- Source: https://github.com/trebb/keytee (author trebb, license NOASSERTION)
- 2 layers, 9 nets, 38 components, 32.5 × 11.3 mm, 0 zones
- Human reference layout: 416 segments, 36 vias, 387.54 mm of track, 352 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/keytee_pcb/`](../../../../fixtures/PCBench/keytee_pcb/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 21 | None | 16 | 1.0 min | FAIL |
| 2.4.1 | COMPLETED | 21 | None | 58 | 4.8 min | FAIL |
| 2.5.0 | COMPLETED | 21 | 0 | 0 | 1.6 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 17 | 0 | 0 | 7.7 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 17 | 0 | 0 | 6.3 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 17 | 0 | 0 | 7.6 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 18 | 0 | 0 | 5.0 min | FAIL |
| master-edffcabb | COMPLETED | 17 | 0 | 0 | 2.1 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `keytee_pcb--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
