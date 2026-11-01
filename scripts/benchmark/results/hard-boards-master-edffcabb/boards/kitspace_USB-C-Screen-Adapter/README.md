# kitspace_USB-C-Screen-Adapter

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 2 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 986.5997 |
| Wall clock | 5.0 min |
| Connections (total / still open) | 199 / 2 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **continue**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 4 | 0 | 973.2 | stagnation | 0.7 min |
| continue | 2 | 0 | 986.6 | stagnation | 0.3 min |
| direct-first | 3 | 0 | 979.9 | stagnation | 1.0 min |
| fanout-retry | 3 | 0 | 979.9 | stagnation | 0.6 min |
| short-escape | 4 | 0 | 973.2 | stagnation | 0.9 min |
| open-escape | 2 | 0 | 986.6 | stagnation | 0.7 min |
| no-fanout | 2 | 0 | 986.6 | stagnation | 0.7 min |

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
| clearance | 3 | 0 |
| hole_clearance | 101 | 54 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 23 | 55 |

Open connections reported by KiCad:

- Pad 2 [_DP_AUX_N] of R18 on F.Cu @ (104.40, 107.52) ↔ Pad 15 [_DP_AUX_N] of U3 on F.Cu @ (130.05, 112.46)
- Pad 17 [Net-(D3-Pad1)] of U3 on F.Cu @ (129.05, 112.46) ↔ Pad 1 [Net-(D3-Pad1)] of D3 on F.Cu @ (139.05, 106.51)

## Board

- Source: https://github.com/Jana-Marie/OtterScreenAdapter (author , license [{'name': 'GNU', 'link': 'https://raw.githubusercontent.com/Jana-Marie/OtterScreenAdapter/master/LICENSE'}])
- 4 layers, 33 nets, 65 components, 40.0 × 20.8 mm, 7 zones
- Human reference layout: 746 segments, 175 vias, 952.62 mm of track, 394 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_USB-C-Screen-Adapter/`](../../../../fixtures/PCBench/kitspace_USB-C-Screen-Adapter/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 5 | None | 11 | 2.9 min | FAIL |
| 2.4.1 | COMPLETED | 5 | None | 14 | 5.2 min | FAIL |
| 2.5.0 | COMPLETED | 1 | 0 | 0 | 7.0 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 1 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 1 | 0 | 0 | 16.8 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 0 | 34.4 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 0 | 20.4 min | FAIL |
| master-edffcabb | COMPLETED | 2 | 0 | 0 | 5.0 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_USB-C-Screen-Adapter--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
