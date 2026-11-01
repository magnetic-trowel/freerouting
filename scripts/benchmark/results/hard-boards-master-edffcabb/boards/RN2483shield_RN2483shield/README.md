# RN2483shield_RN2483shield

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 4 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 888.8889 |
| Wall clock | 0.5 min |
| Connections (total / still open) | 48 / 4 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 4 | 0 | 888.9 | stagnation | 0.1 min |
| continue | 4 | 0 | 888.9 | stagnation | 0.1 min |
| direct-first | 4 | 0 | 888.9 | stagnation | 0.1 min |
| fanout-retry | 33 | 0 | 208.3 | repeated | 0.0 min |
| short-escape | 4 | 0 | 888.9 | stagnation | 0.1 min |
| open-escape | 4 | 0 | 888.9 | stagnation | 0.1 min |
| no-fanout | 4 | 0 | 888.9 | stagnation | 0.1 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 4 | 0 |
| clearance | 0 | 0 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- PTH pad 2 [/HW_TX] of P1 @ (193.04, 62.23) ↔ PTH pad 1 [/HW_TX] of SHIELD1 @ (209.46, 54.20)
- PTH pad 8 [/AD_P8] of SHIELD1 @ (190.16, 54.20) ↔ PTH pad 3 [/AD_P8] of P1 @ (190.50, 62.23)
- PTH pad 9 [/AD_P9] of SHIELD1 @ (187.62, 54.20) ↔ PTH pad 4 [/AD_P9] of P1 @ (187.96, 62.23)
- PTH pad 10 [/AD_P10] of SHIELD1 @ (185.08, 54.20) ↔ PTH pad 5 [/AD_P10] of P1 @ (185.42, 62.23)

## Board

- Source: https://github.com/jpmeijers/RN2483shield (author jpmeijers, license CC0-1.0)
- 2 layers, 2 nets, 8 components, 68.9 × 53.6 mm, 3 zones
- Human reference layout: 115 segments, 14 vias, 407.64 mm of track, 250 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/RN2483shield_RN2483shield/`](../../../../fixtures/PCBench/RN2483shield_RN2483shield/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 4 | None | 0 | 0.2 min | FAIL |
| 2.4.1 | COMPLETED | 4 | None | 0 | 1.0 min | FAIL |
| 2.5.0 | COMPLETED | 4 | 0 | 0 | 0.5 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 4 | 0 | 0 | 1.8 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 4 | 0 | 0 | 1.8 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 4 | 0 | 0 | 1.2 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 4 | 0 | 0 | 1.5 min | FAIL |
| master-edffcabb | COMPLETED | 4 | 0 | 0 | 0.5 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `RN2483shield_RN2483shield--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
