# eeg_brainboard_wearable_v2

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
| Quality score | 996.1905 |
| Wall clock | 5.9 min |
| Connections (total / still open) | 350 / 1 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **continue**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 988.6 | stagnation | 0.9 min |
| continue | 1 | 0 | 996.2 | stagnation | 0.6 min |
| direct-first | 2 | 0 | 992.4 | stagnation | 1.6 min |
| fanout-retry | 96 | 0 | 634.3 | repeated | 0.1 min |
| short-escape | 4 | 0 | 984.8 | stagnation | 0.7 min |
| open-escape | 2 | 0 | 992.4 | stagnation | 0.6 min |
| no-fanout | 1 | 0 | 996.2 | stagnation | 1.4 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 0 | 0 |
| clearance | 5 | 7 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 3 |

## Board

- Source: https://github.com/gskelly/eeg (author gskelly, license None)
- 4 layers, 39 nets, 134 components, 61.06 × 54.61 mm, 13 zones
- Human reference layout: 1270 segments, 126 vias, 1792.0 mm of track, 872 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/eeg_brainboard_wearable_v2/`](../../../../fixtures/PCBench/eeg_brainboard_wearable_v2/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 5 | None | 6 | 5.1 min | FAIL |
| 2.4.1 | COMPLETED | 42 | None | 6 | 5.1 min | FAIL |
| 2.5.0 | TIMED_OUT | 21 | 0 | 0 | 10.2 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 6 | 0 | 0 | 10.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 3 | 0 | 0 | 30.1 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 0 | 34.4 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 5.9 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `eeg_brainboard_wearable_v2--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
