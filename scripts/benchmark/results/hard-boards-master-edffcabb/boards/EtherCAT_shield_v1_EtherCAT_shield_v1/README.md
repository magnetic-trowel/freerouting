# EtherCAT_shield_v1_EtherCAT_shield_v1

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 6 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 972.1254 |
| Wall clock | 19.6 min |
| Connections (total / still open) | 287 / 6 |
| Autorouter stop reason | stagnation after 20 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **fanout-retry**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 8 | 0 | 962.8 | stagnation | 3.4 min |
| direct-first | 11 | 0 | 948.9 | stagnation | 3.2 min |
| fanout-retry | 6 | 0 | 972.1 | stagnation | 3.8 min |
| short-escape | 8 | 0 | 962.8 | stagnation | 4.7 min |
| open-escape | 6 | 0 | 972.1 | stagnation | 1.9 min |
| no-fanout | 9 | 0 | 958.2 | stagnation | 2.6 min |

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
| clearance | 4 | 4 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 3 | 0 |

Open connections reported by KiCad:

- Track [+3V3] on F.Cu, length 2.4000 mm @ (126.50, 83.70) ↔ Pad 5 [+3V3] of U1 on F.Cu @ (128.90, 82.70)
- Pad 1 [Net-(R12-Pad1)] of R12 on F.Cu @ (125.00, 84.55) ↔ Pad 9 [Net-(R12-Pad1)] of U1 on F.Cu @ (128.90, 84.70)
- Track [Net-(R21-Pad1)] on F.Cu, length 0.2883 mm @ (123.40, 84.55) ↔ Pad 10 [Net-(R21-Pad1)] of U1 on F.Cu @ (128.90, 85.20)
- Pad 2 [SIO1] of J5 on F.Cu @ (109.50, 82.50) ↔ Pad 13 [SIO1] of U1 on F.Cu @ (128.90, 86.70)
- Pad 50 [~{SCS}] of U1 on F.Cu @ (136.60, 80.00) ↔ Pad 2 [~{SCS}] of J3 on B.Cu @ (159.00, 101.70)
- Pad 57 [Net-(R13-Pad1)] of U1 on F.Cu @ (133.10, 80.00) ↔ Pad 1 [Net-(R13-Pad1)] of R13 on B.Cu @ (133.60, 78.00)

## Board

- Source: https://github.com/yuqlid/EtherCAT_shield_v1 (author yuqlid, license None)
- 2 layers, 90 nets, 115 components, 71.12 × 81.28 mm, 4 zones
- Human reference layout: 1268 segments, 154 vias, 2871.01 mm of track, 812 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/EtherCAT_shield_v1_EtherCAT_shield_v1/`](../../../../fixtures/PCBench/EtherCAT_shield_v1_EtherCAT_shield_v1/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 6 | None | 46 | 4.9 min | FAIL |
| 2.4.1 | TIMED_OUT | 33 | None | 42 | 5.2 min | FAIL |
| 2.5.0 | TIMED_OUT | 10 | 0 | 0 | 10.2 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 30 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 4 | 0 | 0 | 60.7 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 5 | 0 | 0 | 103.0 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 6 | 0 | 0 | 19.6 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `EtherCAT_shield_v1_EtherCAT_shield_v1--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
