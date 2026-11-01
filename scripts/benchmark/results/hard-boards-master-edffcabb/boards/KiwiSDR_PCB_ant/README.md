# KiwiSDR_PCB_ant

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
| Quality score | 991.3978 |
| Wall clock | 0.8 min |
| Connections (total / still open) | 155 / 1 |
| Autorouter stop reason | stagnation after 30 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 1 | 0 | 991.4 | stagnation | 0.2 min |
| continue | 1 | 0 | 991.4 | stagnation | 0.1 min |
| direct-first | 1 | 0 | 991.4 | stagnation | 0.1 min |
| fanout-retry | 3 | 0 | 974.2 | stagnation | 0.2 min |
| short-escape | 4 | 0 | 965.6 | stagnation | 0.1 min |
| open-escape | 4 | 0 | 965.6 | stagnation | 0.1 min |
| no-fanout | 1 | 0 | 991.4 | stagnation | 0.1 min |

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
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Track [/RX-] on Gnd, length 0.9583 mm @ (218.79, 76.20) ↔ Pad 2 [/RX-] of J10 on all copper layers @ (218.79, 75.25)
- Track [/RX-] on Gnd, length 7.5517 mm @ (218.79, 83.75) ↔ Pad 3 [/RX-] of J10 on all copper layers @ (218.79, 83.75)
- Pad D [/FB3] of Q507 on Gnd @ (152.10, 72.05) ↔ Pad 4 [/FB3] of T501 on Gnd @ (156.86, 69.94)

## Board

- Source: https://github.com/mfkiwl/KiwiSDR_PCB (author mfkiwl, license None)
- 2 layers, 0 nets, 85 components, 138.0 × 38.0 mm, 5 zones
- Human reference layout: 420 segments, 54 vias, 841.52 mm of track, 159 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/KiwiSDR_PCB_ant/`](../../../../fixtures/PCBench/KiwiSDR_PCB_ant/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 2 | None | 0 | 0.6 min | FAIL |
| 2.4.1 | COMPLETED | 2 | None | 5 | 1.8 min | FAIL |
| 2.5.0 | COMPLETED | 3 | 0 | 4 | 1.0 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 1 | 0 | 3 | 2.0 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 1 | 0 | 3 | 1.5 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 0 | 2.5 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 0 | 0.7 min | clean |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 0.8 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `KiwiSDR_PCB_ant--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
