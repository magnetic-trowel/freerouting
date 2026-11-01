# newer-motor-controllers_stm32f407riser

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 3 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 971.01447 |
| Wall clock | 195.8 min |
| Connections (total / still open) | 138 / 3 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **continue**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 6 | 0 | 942.0 | stagnation | 1.5 min |
| direct-first | 7 | 0 | 932.4 | stagnation | 1.2 min |
| fanout-retry | 9 | 0 | 913.0 | stagnation | 188.9 min |
| short-escape | 6 | 0 | 942.0 | stagnation | 1.1 min |
| open-escape | 7 | 0 | 932.4 | stagnation | 1.0 min |
| no-fanout | 4 | 0 | 961.4 | stagnation | 1.3 min |
| continue | 3 | 0 | 971.0 | stagnation | 0.6 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 3 | 0 |
| clearance | 0 | 6 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- PTH pad 3 [/Power MOSFETS/H2] of J4 @ (142.99, 139.28) ↔ Pad 42 [/Power MOSFETS/H2] of U1 on F.Cu @ (151.26, 130.27)
- Pad 2 [Net-(C1-Pad2)] of C1 on B.Cu @ (174.18, 139.70) ↔ Pad 7 [Net-(C1-Pad2)] of U1 on F.Cu @ (162.76, 130.27)
- Pad 23 [/Power MOSFETS/RX_SCL_MOSI] of U1 on F.Cu @ (157.76, 123.77) ↔ Track [/Power MOSFETS/RX_SCL_MOSI] on F.Cu, length 1.3519 mm @ (154.76, 125.12)

## Board

- Source: https://github.com/lizardmech/newer-motor-controllers (author lizardmech, license None)
- 4 layers, 86 nets, 57 components, 69.09 × 41.91 mm, 4 zones
- Human reference layout: 732 segments, 79 vias, 1497.53 mm of track, 423 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/newer-motor-controllers_stm32f407riser/`](../../../../fixtures/PCBench/newer-motor-controllers_stm32f407riser/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 59 | None | 10 | 2.1 min | FAIL |
| 2.4.1 | COMPLETED | 52 | None | 0 | 5.1 min | FAIL |
| 2.5.0 | TIMED_OUT | 3 | 0 | 0 | 10.2 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 12 | 0 | 0 | 10.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 5 | 0 | 0 | 35.3 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 3 | 0 | 0 | 317.3 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 3 | 0 | 0 | 195.8 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `newer-motor-controllers_stm32f407riser--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
