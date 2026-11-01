# FMCW_RADAR_Radar RF

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
| Quality score | 996.675 |
| Wall clock | 5.1 min |
| Connections (total / still open) | 401 / 1 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **continue**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 4 | 0 | 986.7 | stagnation | 0.7 min |
| continue | 1 | 0 | 996.7 | stagnation | 0.4 min |
| direct-first | 5 | 0 | 983.4 | stagnation | 1.0 min |
| fanout-retry | 2 | 0 | 993.4 | stagnation | 0.7 min |
| short-escape | 1 | 0 | 996.7 | stagnation | 0.5 min |
| open-escape | 4 | 0 | 986.7 | stagnation | 0.6 min |
| no-fanout | 3 | 0 | 990.0 | stagnation | 1.0 min |

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
| clearance | 23 | 26 |
| hole_clearance | 0 | 0 |
| shorting_items | 16 | 19 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 51 | 37 |

Open connections reported by KiCad:

- Track [+5V] on B.Cu, length 0.4119 mm @ (156.61, 79.75) ↔ Track [+5V] on F.Cu, length 0.4890 mm @ (156.51, 79.50)

## Board

- Source: https://github.com/ckflight/FMCW_RADAR (author ckflight, license GPL-3.0)
- 4 layers, 72 nets, 143 components, 42.93 × 24.46 mm, 30 zones
- Human reference layout: 1042 segments, 619 vias, 867.7 mm of track, 827 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/FMCW_RADAR_Radar RF/`](../../../../fixtures/PCBench/FMCW_RADAR_Radar%20RF/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 4 | None | 170 | 3.7 min | FAIL |
| 2.4.1 | COMPLETED | 1 | None | 190 | 15.3 min | FAIL |
| 2.5.0 | COMPLETED | 7 | 0 | 0 | 4.8 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 5 | 0 | 0 | 20.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 5 | 0 | 0 | 20.4 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 0 | 55.1 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 5.1 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `FMCW_RADAR_Radar RF--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
