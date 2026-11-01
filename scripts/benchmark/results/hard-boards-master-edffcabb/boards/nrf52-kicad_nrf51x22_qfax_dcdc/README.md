# nrf52-kicad_nrf51x22_qfax_dcdc

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
| Quality score | 970.37036 |
| Wall clock | 0.5 min |
| Connections (total / still open) | 90 / 2 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 2 | 0 | 970.4 | stagnation | 0.1 min |
| continue | 2 | 0 | 970.4 | stagnation | 0.0 min |
| direct-first | 4 | 4 | 940.3 | stagnation | 0.1 min |
| fanout-retry | 2 | 0 | 970.4 | stagnation | 0.1 min |
| short-escape | 2 | 0 | 970.4 | stagnation | 0.1 min |
| open-escape | 4 | 0 | 940.7 | stagnation | 0.1 min |
| no-fanout | 4 | 0 | 940.7 | stagnation | 0.1 min |

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
| clearance | 0 | 2 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 27 | 26 |

Open connections reported by KiCad:

- Pad 32 [Net-(C6-Pad2)] of U1 on F.Cu @ (87.13, -86.09) ↔ Pad 2 [Net-(C6-Pad2)] of C6 on F.Cu @ (88.89, -87.22)
- Pad 33 [Net-(C7-Pad2)] of U1 on F.Cu @ (87.13, -86.49) ↔ Pad 2 [Net-(C7-Pad2)] of C7 on F.Cu @ (88.89, -88.11)

## Board

- Source: https://github.com/jacobrosenthal/nrf52-kicad (author jacobrosenthal, license None)
- 2 layers, 11 nets, 54 components, 17.25 × 15.04 mm, 3 zones
- Human reference layout: 212 segments, 3 vias, 302.07 mm of track, 450 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/nrf52-kicad_nrf51x22_qfax_dcdc/`](../../../../fixtures/PCBench/nrf52-kicad_nrf51x22_qfax_dcdc/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 5 | None | 18 | 0.3 min | FAIL |
| 2.4.1 | COMPLETED | 19 | None | 51 | 1.5 min | FAIL |
| 2.5.0 | COMPLETED | 2 | 0 | 0 | 0.7 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 2 | 0 | 0 | 1.5 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 2 | 0 | 0 | 2.4 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 2 | 0 | 0 | 1.3 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 2 | 0 | 0 | 1.4 min | FAIL |
| master-edffcabb | COMPLETED | 2 | 0 | 0 | 0.5 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `nrf52-kicad_nrf51x22_qfax_dcdc--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
