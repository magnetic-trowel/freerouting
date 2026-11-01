# flip32plus_flip32

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 1 |
| Clearance violations (total) | 2 |
| of which router-introduced | 0 |
| of which pre-existing in input | 2 (unfixable: 2) |
| Worst violation depth | 131.9000244140625 µm |
| Quality score | 990.7426 |
| Wall clock | 4.6 min |
| Connections (total / still open) | 148 / 1 |
| Autorouter stop reason | stagnation after 27 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **no-fanout**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 6 | 0 | 945.7 | stagnation | 1.1 min |
| direct-first | 2 | 0 | 981.7 | stagnation | 1.3 min |
| fanout-retry | 75 | 0 | 328.6 | repeated | 0.0 min |
| short-escape | 2 | 0 | 981.7 | stagnation | 0.5 min |
| open-escape | 6 | 0 | 945.7 | stagnation | 1.0 min |
| no-fanout | 1 | 0 | 990.7 | stagnation | 0.5 min |
| continue | 1 | 0 | 990.7 | stagnation | 0.1 min |

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
| clearance | 14 | 15 |
| hole_clearance | 0 | 0 |
| shorting_items | 7 | 6 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 36 | 14 |

Open connections reported by KiCad:

- Pad 27 [/UU7] of U3 on F.Cu @ (150.35, 147.65) ↔ Pad 1 [/UU7] of PB14 on B.Cu @ (158.50, 151.50)

## Board

- Source: https://github.com/simonantonio/flip32plus (author simonantonio, license None)
- 2 layers, 0 nets, 60 components, 36.0 × 36.0 mm, 3 zones
- Human reference layout: 929 segments, 71 vias, 1445.94 mm of track, 426 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/flip32plus_flip32/`](../../../../fixtures/PCBench/flip32plus_flip32/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 3 | None | 2 | 2.4 min | FAIL |
| 2.4.1 | COMPLETED | 12 | None | 7 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 2 | 0 | 2 | 3.5 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 5 | 0 | 2 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 3 | 0 | 2 | 22.9 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 2 | 47.4 min | clean |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 2 | 17.3 min | FAIL |
| master-edffcabb | COMPLETED | 1 | 0 | 2 | 4.6 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `flip32plus_flip32--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
