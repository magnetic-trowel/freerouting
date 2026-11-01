# Teensy-3.5-Breakout-Boaard_TeensyMegaIoShield

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 1 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 993.5588 |
| Wall clock | 1.2 min |
| Connections (total / still open) | 207 / 1 |
| Autorouter stop reason | stagnation after 45 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 1 | 0 | 993.6 | stagnation | 0.9 min |
| continue | 1 | 0 | 993.6 | stagnation | 0.2 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 1 | 0 |
| clearance | 0 | 0 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 9 | 6 |

Open connections reported by KiCad:

- PTH pad 1 [/A21] of J4 @ (179.86, 101.70) ↔ PTH pad 24 [/A21] of P1 @ (199.90, 86.11)

## Board

- Source: https://github.com/mjs513/Teensy-3.5-Breakout-Boaard (author mjs513, license None)
- 2 layers, 29 nets, 47 components, 101.6 × 59.3 mm, 0 zones
- Human reference layout: 1140 segments, 26 vias, 3392.44 mm of track, 219 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/Teensy-3.5-Breakout-Boaard_TeensyMegaIoShield/`](../../../../fixtures/PCBench/Teensy-3.5-Breakout-Boaard_TeensyMegaIoShield/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 9 | None | 0 | 1.6 min | FAIL |
| 2.4.1 | TIMED_OUT | 4 | None | 0 | 5.2 min | FAIL |
| 2.5.0 | TIMED_OUT | 0 | 1 | 1 | 10.1 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 1 | 0 | 0 | 3.7 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 1 | 0 | 0 | 3.3 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 0 | 8.9 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 0 | 5.9 min | clean |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 1.2 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `Teensy-3.5-Breakout-Boaard_TeensyMegaIoShield--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
