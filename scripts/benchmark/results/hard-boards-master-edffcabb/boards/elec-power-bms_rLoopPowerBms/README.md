# elec-power-bms_rLoopPowerBms

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 2 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 986.0384 |
| Wall clock | 6.7 min |
| Connections (total / still open) | 191 / 2 |
| Autorouter stop reason | stagnation after 33 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **fanout-retry**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 7 | 0 | 951.1 | stagnation | 1.2 min |
| direct-first | 3 | 0 | 979.1 | stagnation | 1.4 min |
| fanout-retry | 2 | 0 | 986.0 | stagnation | 1.2 min |
| short-escape | 3 | 0 | 979.1 | stagnation | 0.9 min |
| open-escape | 5 | 0 | 965.1 | stagnation | 0.9 min |
| no-fanout | 6 | 0 | 958.1 | stagnation | 0.8 min |
| continue | 2 | 0 | 986.0 | stagnation | 0.3 min |

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
| clearance | 1 | 1 |
| hole_clearance | 0 | 1 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Pad 8 [Net-(C1-Pad1)] of U1 on F.Cu @ (202.34, 130.80) ↔ Track [Net-(C1-Pad1)] on F.Cu, length 0.7069 mm @ (198.88, 132.70)
- Pad 1 [Net-(R2-Pad1)] of R2 on F.Cu @ (212.80, 130.30) ↔ Pad 31 [Net-(R2-Pad1)] of U1 on F.Cu @ (209.14, 129.80)

## Board

- Source: https://github.com/rLoopTeam/elec-power-bms (author rLoopTeam, license None)
- 2 layers, 41 nets, 87 components, 70.0 × 60.0 mm, 22 zones
- Human reference layout: 1157 segments, 205 vias, 2118.65 mm of track, 634 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/elec-power-bms_rLoopPowerBms/`](../../../../fixtures/PCBench/elec-power-bms_rLoopPowerBms/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 6 | None | 1 | 2.1 min | FAIL |
| 2.4.1 | COMPLETED | 6 | None | 1 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 5 | 0 | 1 | 7.4 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 4 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 4 | 0 | 0 | 31.2 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 0 | 35.8 min | clean |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 2 | 0 | 0 | 6.7 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `elec-power-bms_rLoopPowerBms--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
