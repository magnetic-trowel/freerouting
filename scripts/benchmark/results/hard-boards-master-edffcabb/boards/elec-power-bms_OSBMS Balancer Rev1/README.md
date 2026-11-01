# elec-power-bms_OSBMS Balancer Rev1

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
| Quality score | 983.9357 |
| Wall clock | 3.4 min |
| Connections (total / still open) | 166 / 2 |
| Autorouter stop reason | stagnation after 24 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **direct-first**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 975.9 | stagnation | 0.4 min |
| continue | 3 | 0 | 975.9 | stagnation | 0.2 min |
| direct-first | 2 | 0 | 983.9 | stagnation | 0.4 min |
| fanout-retry | 2 | 0 | 983.9 | stagnation | 0.4 min |
| short-escape | 5 | 0 | 959.8 | stagnation | 0.4 min |
| open-escape | 2 | 0 | 983.9 | stagnation | 0.5 min |
| no-fanout | 5 | 0 | 959.8 | stagnation | 0.8 min |
| continue | 2 | 0 | 983.9 | stagnation | 0.2 min |

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
| clearance | 0 | 0 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 3 | 0 |

Open connections reported by KiCad:

- Pad 10 [Net-(C1-Pad2)] of U1 on F.Cu @ (202.34, 131.80) ↔ Track [Net-(C1-Pad2)] on F.Cu, length 1.9471 mm @ (198.88, 137.83)
- Track [Net-(R2-Pad1)] on F.Cu, length 1.5240 mm @ (212.80, 131.83) ↔ Pad 31 [Net-(R2-Pad1)] of U1 on F.Cu @ (209.14, 129.80)

## Board

- Source: https://github.com/rLoopTeam/elec-power-bms (author rLoopTeam, license None)
- 2 layers, 53 nets, 74 components, 49.0 × 49.0 mm, 16 zones
- Human reference layout: 788 segments, 166 vias, 1623.1 mm of track, 433 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/elec-power-bms_OSBMS Balancer Rev1/`](../../../../fixtures/PCBench/elec-power-bms_OSBMS%20Balancer%20Rev1/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 5 | None | 1 | 0.8 min | FAIL |
| 2.4.1 | COMPLETED | 6 | None | 1 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 2 | 0 | 0 | 2.8 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 2 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 2 | 0 | 0 | 14.6 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 0 | 18.0 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 3 | 0 | 0 | 12.2 min | FAIL |
| master-edffcabb | COMPLETED | 2 | 0 | 0 | 3.4 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `elec-power-bms_OSBMS Balancer Rev1--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
