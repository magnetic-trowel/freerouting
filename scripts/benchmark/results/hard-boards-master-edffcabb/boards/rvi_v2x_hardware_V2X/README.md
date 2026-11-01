# rvi_v2x_hardware_V2X

Tier **D** (diagnostic). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 2 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 994.46747 |
| Wall clock | 55.7 min |
| Connections (total / still open) | 482 / 2 |
| Autorouter stop reason | stagnation after 42 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **open-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 7 | 0 | 980.6 | stagnation | 7.4 min |
| direct-first | 8 | 0 | 977.9 | stagnation | 9.9 min |
| fanout-retry | 5 | 0 | 986.2 | stagnation | 9.3 min |
| short-escape | 8 | 0 | 977.9 | stagnation | 7.5 min |
| open-escape | 2 | 0 | 994.5 | stagnation | 9.4 min |
| no-fanout | 9 | 0 | 975.1 | stagnation | 9.8 min |
| continue | 2 | 0 | 994.5 | stagnation | 2.2 min |

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
| clearance | 0 | 1 |
| hole_clearance | 1 | 3 |
| shorting_items | 1 | 1 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 48 | 29 |

Open connections reported by KiCad:

- Pad 25 [GND] of U16 on B.Cu @ (93.25, 21.75) ↔ Track [GND] on F.Cu, length 1.8740 mm @ (96.57, 21.90)
- Pad 27 [SEQ_DP] of U20 on B.Cu @ (71.70, 29.20) ↔ Pad 3 [SEQ_DP] of U16 on B.Cu @ (90.55, 22.65)

## Board

- Source: https://github.com/PDXostc/rvi_v2x_hardware (author PDXostc, license MPL-2.0)
- 2 layers, 116 nets, 198 components, 85.0 × 56.0 mm, 38 zones
- Human reference layout: 2759 segments, 416 vias, 4514.07 mm of track, 1399 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/rvi_v2x_hardware_V2X/`](../../../../fixtures/PCBench/rvi_v2x_hardware_V2X/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 10 | None | 15 | 14.6 min | FAIL |
| 2.4.1 | COMPLETED | 38 | None | 21 | 15.3 min | FAIL |
| 2.5.0 | TIMED_OUT | 87 | 0 | 0 | 20.2 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 81 | 0 | 0 | 20.3 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 69 | 0 | 0 | 147.1 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 3 | 0 | 0 | 265.8 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 2 | 0 | 0 | 55.7 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `rvi_v2x_hardware_V2X--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
