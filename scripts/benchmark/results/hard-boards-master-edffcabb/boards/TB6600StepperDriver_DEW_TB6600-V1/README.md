# TB6600StepperDriver_DEW_TB6600-V1

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 5 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 959.3496 |
| Wall clock | 2.2 min |
| Connections (total / still open) | 164 / 5 |
| Autorouter stop reason | stagnation after 26 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **no-fanout**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 9 | 0 | 926.8 | stagnation | 0.5 min |
| direct-first | 7 | 0 | 943.1 | stagnation | 0.5 min |
| fanout-retry | 6 | 0 | 951.2 | stagnation | 0.5 min |
| short-escape | 159 | 0 | 20.3 | repeated | 0.0 min |
| open-escape | 6 | 0 | 951.2 | stagnation | 0.2 min |
| no-fanout | 5 | 0 | 959.3 | stagnation | 0.4 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 5 | 0 |
| clearance | 0 | 0 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 7 | 1 |

Open connections reported by KiCad:

- PTH pad 1 [+36V] of R4 @ (179.71, 150.50) ↔ Track [+36V] on B.Cu, length 0.1134 mm @ (184.90, 153.03)
- Pad 6 [+36V] of IC1 on B.Cu @ (203.75, 129.45) ↔ Track [+36V] on B.Cu, length 14.7340 mm @ (204.67, 121.08)
- Pad 20 [+36V] of IC1 on B.Cu @ (217.75, 129.45) ↔ Track [+36V] on F.Cu, length 5.7150 mm @ (207.65, 118.11)
- PTH pad 2 [/OUT1B] of X2 @ (205.74, 168.91) ↔ Pad 12 [/OUT1B] of IC1 on B.Cu @ (209.75, 129.45)
- Pad 19 [N-0000058] of IC1 on F.Cu @ (216.75, 129.45) ↔ PTH pad 1 [N-0000058] of C11 @ (218.44, 143.51)

## Board

- Source: https://github.com/dewhisna/TB6600StepperDriver (author dewhisna, license None)
- 2 layers, 0 nets, 75 components, 86.36 × 66.67 mm, 0 zones
- Human reference layout: 567 segments, 20 vias, 2736.4 mm of track, 518 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/TB6600StepperDriver_DEW_TB6600-V1/`](../../../../fixtures/PCBench/TB6600StepperDriver_DEW_TB6600-V1/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 11 | None | 0 | 1.6 min | FAIL |
| 2.4.1 | COMPLETED | 24 | None | 0 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 21 | 0 | 0 | 3.9 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 12 | 0 | 0 | 10.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 12 | 0 | 0 | 9.1 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 3 | 0 | 0 | 11.8 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 5 | 0 | 0 | 5.9 min | FAIL |
| master-edffcabb | COMPLETED | 5 | 0 | 0 | 2.2 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `TB6600StepperDriver_DEW_TB6600-V1--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
