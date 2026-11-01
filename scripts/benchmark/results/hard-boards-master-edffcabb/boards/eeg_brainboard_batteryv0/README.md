# eeg_brainboard_batteryv0

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
| Quality score | 991.50745 |
| Wall clock | 1.7 min |
| Connections (total / still open) | 157 / 1 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **continue**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 2 | 0 | 983.0 | stagnation | 0.3 min |
| continue | 1 | 0 | 991.5 | stagnation | 0.1 min |
| direct-first | 3 | 0 | 974.5 | stagnation | 0.3 min |
| fanout-retry | 43 | 0 | 634.8 | repeated | 0.0 min |
| short-escape | 2 | 0 | 983.0 | stagnation | 0.2 min |
| open-escape | 2 | 0 | 983.0 | stagnation | 0.2 min |
| no-fanout | 2 | 0 | 983.0 | stagnation | 0.4 min |

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
| hole_clearance | 5 | 5 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 2 | 3 |

Open connections reported by KiCad:

- Track [GND] on B.Cu, length 2.9982 mm @ (231.48, 125.22) ↔ Pad 17 [GND] of U2 on B.Cu @ (235.44, 125.37)

## Board

- Source: https://github.com/gskelly/eeg (author gskelly, license None)
- 2 layers, 0 nets, 65 components, 86.36 × 54.61 mm, 10 zones
- Human reference layout: 486 segments, 39 vias, 1069.37 mm of track, 365 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/eeg_brainboard_batteryv0/`](../../../../fixtures/PCBench/eeg_brainboard_batteryv0/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 8 | None | 10 | 2.4 min | FAIL |
| 2.4.1 | TIMED_OUT | 10 | None | 10 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 18 | 0 | 0 | 4.5 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 8 | 0 | 0 | 8.5 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 8 | 0 | 0 | 6.4 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 2 | 0 | 0 | 12.4 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 0 | 5.5 min | FAIL |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 1.7 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `eeg_brainboard_batteryv0--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
