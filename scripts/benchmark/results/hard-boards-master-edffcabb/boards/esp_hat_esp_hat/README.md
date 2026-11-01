# esp_hat_esp_hat

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 2 |
| Clearance violations (total) | 2 |
| of which router-introduced | 0 |
| of which pre-existing in input | 2 (unfixable: 2) |
| Worst violation depth | 200.0 µm |
| Quality score | 987.5231 |
| Wall clock | 17.4 min |
| Connections (total / still open) | 219 / 2 |
| Autorouter stop reason | stagnation after 22 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 2 | 0 | 987.5 | stagnation | 0.2 min |
| continue | 2 | 0 | 987.5 | stagnation | 0.1 min |
| direct-first | 2 | 0 | 987.5 | stagnation | 16.1 min |
| fanout-retry | 5 | 0 | 969.3 | stagnation | 0.3 min |
| short-escape | 2 | 0 | 987.5 | stagnation | 0.2 min |
| open-escape | 3 | 0 | 981.4 | stagnation | 0.2 min |
| no-fanout | 5 | 0 | 969.3 | stagnation | 0.2 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 6 | 0 |
| clearance | 0 | 0 |
| hole_clearance | 0 | 0 |
| shorting_items | 2 | 2 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 38 | 46 |

Open connections reported by KiCad:

- PTH pad 1 [GND] of W3 @ (68.50, 88.50) ↔ Track [GND] on F.Cu, length 1.4000 mm @ (68.50, 90.92)
- PTH pad 1 [GND] of W4 @ (68.50, 111.50) ↔ Track [GND] on F.Cu, length 1.4000 mm @ (68.50, 113.92)
- Pad 33 [GND] of U1 on F.Cu @ (81.60, 104.31) ↔ Track [GND] on B.Cu, length 2.7500 mm @ (78.05, 100.75)
- Track [GND] on F.Cu, length 1.4000 mm @ (127.90, 90.92) ↔ PTH pad 1 [GND] of W6 @ (126.50, 88.50)
- PTH pad 1 [GND] of W5 @ (126.50, 111.50) ↔ Track [GND] on F.Cu, length 1.4000 mm @ (126.50, 109.08)
- Pad 2 [/50ohm_LNA] of U1 on F.Cu @ (80.06, 102.20) ↔ Pad 1 [/50ohm_LNA] of L2 on F.Cu @ (74.20, 102.10)

## Board

- Source: https://github.com/al177/esp_hat (author al177, license CC-BY-4.0)
- 2 layers, 14 nets, 113 components, 73.0 × 30.0 mm, 6 zones
- Human reference layout: 459 segments, 68 vias, 678.41 mm of track, 699 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/esp_hat_esp_hat/`](../../../../fixtures/PCBench/esp_hat_esp_hat/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 23 | None | 48 | 1.8 min | FAIL |
| 2.4.1 | COMPLETED | 29 | None | 112 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 9 | 0 | 2 | 3.4 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 3 | 0 | 2 | 9.7 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 3 | 0 | 2 | 11.6 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 2 | 0 | 2 | 12.6 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 2 | 9.6 min | FAIL |
| master-edffcabb | COMPLETED | 2 | 0 | 2 | 17.4 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `esp_hat_esp_hat--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
