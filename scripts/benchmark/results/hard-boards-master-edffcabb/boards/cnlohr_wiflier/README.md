# cnlohr_wiflier

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 3 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 972.7891 |
| Wall clock | 3.5 min |
| Connections (total / still open) | 147 / 3 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **continue**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 14 | 0 | 873.0 | stagnation | 0.7 min |
| direct-first | 9 | 0 | 918.4 | stagnation | 0.6 min |
| fanout-retry | 15 | 0 | 863.9 | stagnation | 0.6 min |
| short-escape | 7 | 0 | 936.5 | stagnation | 0.5 min |
| open-escape | 4 | 0 | 963.7 | stagnation | 0.6 min |
| no-fanout | 8 | 0 | 927.4 | stagnation | 0.4 min |
| continue | 3 | 0 | 972.8 | stagnation | 0.2 min |

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
| copper_edge_clearance | 20 | 32 |

Open connections reported by KiCad:

- Track [+3.3V] on B.Cu, length 4.0329 mm @ (56.16, 64.00) ↔ Track [+3.3V] on F.Cu, length 0.3520 mm @ (52.32, 63.35)
- Pad 21 [N-0000054] of U5 on F.Cu @ (54.57, 60.10) ↔ Pad 6 [N-0000054] of U6 on F.Cu @ (56.24, 56.71)
- Pad 10 [SDA] of U5 on F.Cu @ (57.32, 63.85) ↔ Track [SDA] on B.Cu, length 1.8513 mm @ (64.65, 59.82)

## Board

- Source: https://github.com/cnlohr/wiflier (author , license )
- 2 layers, 0 nets, 57 components, 37.75 × 21.5 mm, 3 zones
- Human reference layout: 800 segments, 85 vias, 805.47 mm of track, 305 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/cnlohr_wiflier/`](../../../../fixtures/PCBench/cnlohr_wiflier/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 17 | None | 7 | 1.8 min | FAIL |
| 2.4.1 | COMPLETED | 36 | None | 16 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 33 | 0 | 0 | 4.0 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 10 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 8 | 0 | 0 | 10.8 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 8 | 0 | 0 | 22.3 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 7 | 0 | 0 | 13.3 min | FAIL |
| master-edffcabb | COMPLETED | 3 | 0 | 0 | 3.5 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `cnlohr_wiflier--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
