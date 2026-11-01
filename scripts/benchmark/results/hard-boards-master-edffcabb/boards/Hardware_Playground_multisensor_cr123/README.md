# Hardware_Playground_multisensor_cr123

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
| Quality score | 943.01996 |
| Wall clock | 1.5 min |
| Connections (total / still open) | 117 / 5 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **no-fanout**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 8 | 0 | 908.8 | stagnation | 0.3 min |
| direct-first | 8 | 0 | 908.8 | stagnation | 0.2 min |
| fanout-retry | 8 | 0 | 908.8 | stagnation | 0.2 min |
| short-escape | 11 | 0 | 874.6 | stagnation | 0.3 min |
| open-escape | 8 | 0 | 908.8 | stagnation | 0.3 min |
| no-fanout | 5 | 0 | 943.0 | stagnation | 0.2 min |

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
| clearance | 34 | 40 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 18 | 12 |

Open connections reported by KiCad:

- Pad 7 [Net-(L2-Pad1)] of U1 on F.Cu @ (140.39, 91.91) ↔ Pad 1 [Net-(L2-Pad1)] of L2 on F.Cu @ (141.89, 83.69)
- Track [/SS] on B.Cu, length 1.3541 mm @ (121.60, 88.58) ↔ Pad 1 [/SS] of R3 on B.Cu @ (125.41, 90.45)
- PTH pad 3 [/D0] of P4 @ (112.87, 96.84) ↔ Pad 30 [/D0] of IC1 on B.Cu @ (113.10, 86.18)
- Pad 27 [/SDA] of IC1 on B.Cu @ (113.10, 88.58) ↔ PTH pad 3 [/SDA] of P2 @ (126.11, 96.90)
- Pad 28 [/SCL] of IC1 on B.Cu @ (113.10, 87.78) ↔ PTH pad 4 [/SCL] of P2 @ (128.65, 96.90)

## Board

- Source: https://github.com/mtiutiu/Hardware_Playground (author mtiutiu, license None)
- 2 layers, 24 nets, 64 components, 48.2 × 22.16 mm, 2 zones
- Human reference layout: 371 segments, 30 vias, 768.54 mm of track, 376 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/Hardware_Playground_multisensor_cr123/`](../../../../fixtures/PCBench/Hardware_Playground_multisensor_cr123/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 36 | None | 88 | 1.3 min | FAIL |
| 2.4.1 | COMPLETED | 26 | None | 58 | 4.2 min | FAIL |
| 2.5.0 | COMPLETED | 6 | 0 | 0 | 1.8 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 3 | 0 | 0 | 5.5 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 3 | 0 | 0 | 6.3 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 6 | 0 | 0 | 5.5 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 6 | 0 | 0 | 4.9 min | FAIL |
| master-edffcabb | COMPLETED | 5 | 0 | 0 | 1.5 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `Hardware_Playground_multisensor_cr123--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
