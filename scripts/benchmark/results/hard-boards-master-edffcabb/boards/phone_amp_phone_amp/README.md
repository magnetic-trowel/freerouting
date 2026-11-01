# phone_amp_phone_amp

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
| Quality score | 984.8485 |
| Wall clock | 1.1 min |
| Connections (total / still open) | 88 / 1 |
| Autorouter stop reason | stagnation after 38 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **open-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 954.5 | stagnation | 0.2 min |
| continue | 2 | 0 | 969.7 | stagnation | 0.1 min |
| direct-first | 3 | 0 | 954.5 | stagnation | 0.1 min |
| fanout-retry | 30 | 0 | 545.5 | repeated | 0.0 min |
| short-escape | 4 | 0 | 939.4 | stagnation | 0.2 min |
| open-escape | 1 | 0 | 984.8 | stagnation | 0.2 min |
| no-fanout | 2 | 0 | 969.7 | stagnation | 0.2 min |
| continue | 1 | 0 | 984.8 | stagnation | 0.1 min |

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
| clearance | 1 | 10 |
| hole_clearance | 2 | 8 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 114 | 165 |

Open connections reported by KiCad:

- Track [Net-(R11-Pad2)] on F.Cu, length 1.5400 mm @ (144.76, 121.03) ↔ Pad 9 [Net-(R11-Pad2)] of U2 on F.Cu @ (141.00, 100.08)

## Board

- Source: https://github.com/iocapa/phone_amp (author iocapa, license None)
- 2 layers, 27 nets, 42 components, 11.62 × 56.15 mm, 2 zones
- Human reference layout: 561 segments, 32 vias, 632.97 mm of track, 537 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/phone_amp_phone_amp/`](../../../../fixtures/PCBench/phone_amp_phone_amp/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 7 | None | 0 | 0.5 min | FAIL |
| 2.4.1 | COMPLETED | 4 | None | 31 | 2.2 min | FAIL |
| 2.5.0 | COMPLETED | 4 | 0 | 0 | 1.3 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 2 | 0 | 0 | 4.4 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 2 | 0 | 0 | 3.7 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 2 | 0 | 0 | 4.1 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 0 | 3.3 min | FAIL |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 1.1 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `phone_amp_phone_amp--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
