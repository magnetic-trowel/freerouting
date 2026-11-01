# Hardware_Playground_serial_gw_ATSAMD21E

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
| Quality score | 969.9248 |
| Wall clock | 1.5 min |
| Connections (total / still open) | 133 / 3 |
| Autorouter stop reason | stagnation after 25 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 969.9 | stagnation | 0.3 min |
| continue | 3 | 0 | 969.9 | stagnation | 0.1 min |
| direct-first | 4 | 0 | 959.9 | stagnation | 0.2 min |
| fanout-retry | 6 | 0 | 939.8 | stagnation | 0.2 min |
| short-escape | 4 | 0 | 959.9 | stagnation | 0.2 min |
| open-escape | 3 | 0 | 969.9 | stagnation | 0.2 min |
| no-fanout | 3 | 0 | 969.9 | stagnation | 0.2 min |

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
| clearance | 28 | 28 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 9 | 3 |

Open connections reported by KiCad:

- Pad 5 [/GW_TX_LED] of U1 on F.Cu @ (130.40, 87.90) ↔ Pad 2 [/GW_TX_LED] of R3 on F.Cu @ (141.40, 90.80)
- Pad 7 [/GW_ERR_LED] of U1 on F.Cu @ (130.40, 86.30) ↔ Pad 2 [/GW_ERR_LED] of R4 on F.Cu @ (141.25, 86.10)
- Track [/ATSHA204A] on F.Cu, length 2.2500 mm @ (136.50, 78.60) ↔ Pad 8 [/ATSHA204A] of U1 on F.Cu @ (130.40, 85.50)

## Board

- Source: https://github.com/mtiutiu/Hardware_Playground (author mtiutiu, license None)
- 2 layers, 13 nets, 78 components, 38.9 × 21.2 mm, 4 zones
- Human reference layout: 359 segments, 33 vias, 612.18 mm of track, 273 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/Hardware_Playground_serial_gw_ATSAMD21E/`](../../../../fixtures/PCBench/Hardware_Playground_serial_gw_ATSAMD21E/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 8 | None | 27 | 1.0 min | FAIL |
| 2.4.1 | COMPLETED | 5 | None | 2 | 4.2 min | FAIL |
| 2.5.0 | COMPLETED | 2 | 0 | 0 | 0.5 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 2 | 0 | 0 | 3.3 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 2 | 0 | 0 | 3.2 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 0 | 1.6 min | clean |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 0 | 4.4 min | FAIL |
| master-edffcabb | COMPLETED | 3 | 0 | 0 | 1.5 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `Hardware_Playground_serial_gw_ATSAMD21E--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
