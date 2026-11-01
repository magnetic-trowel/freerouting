# EUC-VESC_electronics_sept

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 2 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 996.52325 |
| Wall clock | 25.4 min |
| Connections (total / still open) | 767 / 2 |
| Autorouter stop reason | stagnation after 21 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 2 | 0 | 996.5 | stagnation | 6.0 min |
| continue | 2 | 0 | 996.5 | stagnation | 3.1 min |
| direct-first | 0 | 1 | 1000.0 | completed | 0.5 min |
| fanout-retry | 3 | 0 | 994.8 | stagnation | 3.0 min |
| short-escape | 2 | 1 | 996.5 | stagnation | 4.4 min |
| open-escape | 3 | 0 | 994.8 | stagnation | 4.3 min |
| no-fanout | 2 | 0 | 996.5 | stagnation | 3.9 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 2 | 0 |
| clearance | 3 | 3 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 3 | 3 |

Open connections reported by KiCad:

- Pad 2 [Net-(C34-Pad2)] of U6 on F.Cu @ (241.33, 134.47) ↔ Pad 2 [Net-(C34-Pad2)] of C34 on F.Cu @ (241.94, 131.70)
- Pad 6 [Net-(R52-Pad2)] of U11 on F.Cu @ (248.63, 139.85) ↔ Pad 2 [Net-(R52-Pad2)] of R52 on F.Cu @ (251.73, 133.60)

## Board

- Source: https://github.com/EGG-electric-unicycle/EUC-VESC_electronics (author EGG-electric-unicycle, license None)
- 4 layers, 102 nets, 577 components, 100.0 × 150.0 mm, 66 zones
- Human reference layout: 1408 segments, 78 vias, 4625.63 mm of track, 958 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/EUC-VESC_electronics_sept/`](../../../../fixtures/PCBench/EUC-VESC_electronics_sept/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 16 | None | 32 | 13.2 min | FAIL |
| 2.4.1 | TIMED_OUT | 8 | None | 28 | 30.3 min | FAIL |
| 2.5.0 | COMPLETED | 1 | 0 | 0 | 14.6 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 1 | 0 | 0 | 45.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 1 | 0 | 0 | 74.9 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 0 | 36.8 min | clean |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 2 | 0 | 0 | 25.4 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `EUC-VESC_electronics_sept--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
