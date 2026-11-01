# cnlohr_wiflier_B

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 3 |
| Clearance violations (total) | 1 |
| of which router-introduced | 0 |
| of which pre-existing in input | 1 (unfixable: 1) |
| Worst violation depth | 0.0 µm |
| Quality score | 974.6257 |
| Wall clock | 3.2 min |
| Connections (total / still open) | 158 / 3 |
| Autorouter stop reason | stagnation after 25 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 974.6 | stagnation | 0.5 min |
| continue | 3 | 0 | 974.6 | stagnation | 0.2 min |
| direct-first | 5 | 0 | 957.7 | stagnation | 0.3 min |
| fanout-retry | 6 | 0 | 949.3 | stagnation | 0.6 min |
| short-escape | 6 | 0 | 949.3 | stagnation | 0.7 min |
| open-escape | 6 | 0 | 949.3 | stagnation | 0.4 min |
| no-fanout | 7 | 0 | 940.9 | stagnation | 0.4 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 3 | 0 |
| clearance | 0 | 0 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 36 | 21 |

Open connections reported by KiCad:

- Pad 23 [N-0000044] of U5 on F.Cu @ (53.57, 60.10) ↔ Pad 5 [N-0000044] of U6 on F.Cu @ (54.97, 56.71)
- Track [SDA] on B.Cu, length 1.0044 mm @ (63.94, 59.11) ↔ Pad 10 [SDA] of U5 on F.Cu @ (57.32, 63.85)
- Pad 2 [N-0000021] of R1 on F.Cu @ (50.76, 64.91) ↔ Pad 31 [N-0000021] of U5 on F.Cu @ (52.32, 63.85)

## Board

- Source: https://github.com/cnlohr/wiflier (author , license )
- 2 layers, 0 nets, 61 components, 37.75 × 21.5 mm, 3 zones
- Human reference layout: 908 segments, 90 vias, 804.98 mm of track, 352 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/cnlohr_wiflier_B/`](../../../../fixtures/PCBench/cnlohr_wiflier_B/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 14 | None | 10 | 1.8 min | FAIL |
| 2.4.1 | COMPLETED | 35 | None | 19 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 34 | 0 | 0 | 4.0 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 5 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 5 | 0 | 0 | 13.8 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 6 | 0 | 1 | 17.2 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 6 | 0 | 1 | 14.0 min | FAIL |
| master-edffcabb | COMPLETED | 3 | 0 | 1 | 3.2 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `cnlohr_wiflier_B--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
