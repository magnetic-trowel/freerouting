# kitspace_120-channel-test-board

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 1 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 994.64526 |
| Wall clock | 7.3 min |
| Connections (total / still open) | 249 / 1 |
| Autorouter stop reason | stagnation after 37 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **short-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 11 | 0 | 941.1 | stagnation | 2.5 min |
| direct-first | 11 | 0 | 941.1 | stagnation | 0.9 min |
| fanout-retry | 122 | 0 | 346.7 | repeated | 0.1 min |
| short-escape | 1 | 0 | 994.6 | stagnation | 0.6 min |
| open-escape | 2 | 0 | 989.3 | stagnation | 0.9 min |
| no-fanout | 3 | 0 | 983.9 | stagnation | 2.0 min |
| continue | 1 | 0 | 994.6 | stagnation | 0.1 min |

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
| copper_edge_clearance | 4 | 0 |

Open connections reported by KiCad:

- Pad 1 [Net-(C67-Pad1)] of P67 on Front @ (45.72, 76.20) ↔ Pad 1 [Net-(C67-Pad1)] of C67 on Front @ (65.17, 76.20)

## Board

- Source: https://github.com/sci-bots/dropbot-120-channel-test-board.kicad (author , license [{'name': 'Creative Commons', 'link': 'https://raw.githubusercontent.com/sci-bots/dropbot-120-channel-test-board.kicad/main/LICENSE'}])
- 2 layers, 128 nets, 258 components, 76.01 × 50.79 mm, 1 zones
- Human reference layout: 1125 segments, 285 vias, 2806.62 mm of track, 570 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_120-channel-test-board/`](../../../../fixtures/PCBench/kitspace_120-channel-test-board/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 7 | None | 0 | 4.8 min | FAIL |
| 2.4.1 | TIMED_OUT | 20 | None | 0 | 15.2 min | FAIL |
| 2.5.0 | COMPLETED | 3 | 0 | 0 | 6.9 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 1 | 0 | 0 | 20.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 1 | 0 | 0 | 22.1 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 0 | 54.2 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 7.3 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_120-channel-test-board--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
