# Ttl_wlan_radar

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
| Quality score | 990.57715 |
| Wall clock | 7.4 min |
| Connections (total / still open) | 283 / 2 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **continue**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 985.9 | stagnation | 1.1 min |
| continue | 2 | 0 | 990.6 | stagnation | 0.6 min |
| direct-first | 9 | 0 | 957.6 | stagnation | 1.1 min |
| fanout-retry | 3 | 0 | 985.9 | stagnation | 1.0 min |
| short-escape | 2 | 0 | 990.6 | stagnation | 1.0 min |
| open-escape | 2 | 0 | 990.6 | stagnation | 1.6 min |
| no-fanout | 6 | 0 | 971.7 | stagnation | 0.9 min |

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
| clearance | 39 | 41 |
| hole_clearance | 0 | 0 |
| shorting_items | 21 | 25 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 17 | 10 |

Open connections reported by KiCad:

- Track [GND] on F.Cu, length 0.5917 mm @ (138.60, 89.88) ↔ PTH pad PAD [GND] of U2 @ (137.70, 89.63)
- Pad 2 [GND] of C9 on F.Cu @ (147.77, 87.43) ↔ PTH pad PAD [GND] of U1 @ (146.94, 89.18)

## Board

- Source: https://github.com/Ttl/fmcw (author , license )
- 4 layers, 71 nets, 128 components, 54.99 × 24.96 mm, 10 zones
- Human reference layout: 1857 segments, 509 vias, 1527.39 mm of track, 1321 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/Ttl_wlan_radar/`](../../../../fixtures/PCBench/Ttl_wlan_radar/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 3 | None | 9 | 4.3 min | FAIL |
| 2.4.1 | TIMED_OUT | 3 | None | 15 | 5.3 min | FAIL |
| 2.5.0 | COMPLETED | 14 | 0 | 0 | 8.3 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 16 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 10 | 0 | 0 | 29.7 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 3 | 0 | 0 | 47.0 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 5 | 0 | 0 | 20.4 min | FAIL |
| master-edffcabb | COMPLETED | 2 | 0 | 0 | 7.4 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `Ttl_wlan_radar--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
