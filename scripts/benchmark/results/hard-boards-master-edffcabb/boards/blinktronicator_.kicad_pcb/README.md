# blinktronicator_.kicad_pcb

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 1 |
| Clearance violations (total) | 2 |
| of which router-introduced | 0 |
| of which pre-existing in input | 2 (unfixable: 2) |
| Worst violation depth | 49.10042724609375 µm |
| Quality score | 981.6667 |
| Wall clock | 1.5 min |
| Connections (total / still open) | 74 / 1 |
| Autorouter stop reason | stagnation after 32 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 1 | 0 | 981.7 | stagnation | 0.4 min |
| continue | 1 | 0 | 981.7 | stagnation | 0.1 min |
| direct-first | 1 | 0 | 981.7 | stagnation | 0.1 min |
| fanout-retry | 2 | 0 | 963.6 | stagnation | 0.2 min |
| short-escape | 3 | 0 | 945.6 | stagnation | 0.3 min |
| open-escape | 3 | 0 | 945.6 | stagnation | 0.2 min |
| no-fanout | 2 | 0 | 963.6 | stagnation | 0.1 min |

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
| clearance | 5 | 2 |
| hole_clearance | 0 | 0 |
| shorting_items | 3 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 94 | 20 |

Open connections reported by KiCad:

- Pad 1 [N-0000031] of D15 on F.Cu @ (168.19, 40.57) ↔ Pad 6 [N-0000031] of U4 on F.Cu @ (171.28, 38.74)

## Board

- Source: https://github.com/zakqwy/blinktronicator (author zakqwy, license MIT)
- 2 layers, 0 nets, 37 components, 23.57 × 23.55 mm, 0 zones
- Human reference layout: 414 segments, 43 vias, 384.62 mm of track, 198 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/blinktronicator_.kicad_pcb/`](../../../../fixtures/PCBench/blinktronicator_.kicad_pcb/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 4 | None | 14 | 0.9 min | FAIL |
| 2.4.1 | COMPLETED | 2 | None | 2 | 2.2 min | FAIL |
| 2.5.0 | COMPLETED | 1 | 0 | 2 | 2.3 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 0 | 0 | 2 | 10.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 2 | 15.7 min | clean |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 2 | 6.3 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 2 | 4.4 min | clean |
| master-edffcabb | COMPLETED | 1 | 0 | 2 | 1.5 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `blinktronicator_.kicad_pcb--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
