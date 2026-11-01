# LeeChee_1800

Tier **D** (diagnostic). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 7 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 992.00916 |
| Wall clock | 21.1 min |
| Connections (total / still open) | 1168 / 7 |
| Autorouter stop reason | stagnation after 19 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **direct-first**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 8 | 0 | 990.9 | stagnation | 4.0 min |
| direct-first | 7 | 0 | 992.0 | stagnation | 4.6 min |
| fanout-retry | 8 | 0 | 990.9 | stagnation | 3.1 min |
| short-escape | 7 | 0 | 992.0 | stagnation | 2.9 min |
| open-escape | 7 | 0 | 992.0 | stagnation | 3.1 min |
| no-fanout | 9 | 0 | 989.7 | stagnation | 3.2 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 7 | 0 |
| clearance | 1 | 4 |
| hole_clearance | 112 | 153 |
| shorting_items | 0 | 1 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 199 | 190 |

Open connections reported by KiCad:

- Pad 15 [/ROW1] of U3 on B.Cu @ (318.06, 165.10) ↔ Track [/ROW1] on F.Cu, length 2.7794 mm @ (276.85, 146.55)
- PTH pad 2 [/ROW1] of MX_INS_1 @ (328.64, 146.55) ↔ Pad 15 [/ROW1] of U3 on B.Cu @ (318.06, 165.10)
- PTH pad 2 [/ROW1] of MX_INS_2 @ (338.17, 146.55) ↔ PTH pad 2 [/ROW1] of MX_INS_1 @ (328.64, 146.55)
- PTH pad 2 [/ROW1] of MX_HOME_1 @ (347.69, 146.55) ↔ PTH pad 2 [/ROW1] of MX_INS_2 @ (338.17, 146.55)
- PTH pad 2 [/ROW1] of MX_HOME_2 @ (357.22, 146.55) ↔ PTH pad 2 [/ROW1] of MX_PGUP_1 @ (366.74, 146.55)
- PTH pad 2 [/ROW1] of MX_HOME_2 @ (357.22, 146.55) ↔ PTH pad 2 [/ROW1] of MX_HOME_1 @ (347.69, 146.55)
- PTH pad 2 [/ROW1] of MX_PGUP_1 @ (366.74, 146.55) ↔ Track [/ROW1] on F.Cu, length 9.5250 mm @ (385.79, 146.55)

## Board

- Source: https://github.com/Gacathon/LeeChee (author Gacathon, license None)
- 2 layers, 298 nets, 587 components, 378.41 × 156.86 mm, 3 zones
- Human reference layout: 5942 segments, 381 vias, 22648.87 mm of track, 1530 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/LeeChee_1800/`](../../../../fixtures/PCBench/LeeChee_1800/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | FAILED | 22 | None | 64 | 33.0 min | FAIL |
| 2.4.1 | COMPLETED | 151 | None | 64 | 30.5 min | FAIL |
| 2.5.0 | TIMED_OUT | 84 | 0 | 0 | 45.7 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 81 | 0 | 0 | 45.3 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 80 | 0 | 0 | 220.2 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 7 | 0 | 0 | 89.2 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 7 | 0 | 0 | 21.1 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `LeeChee_1800--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
