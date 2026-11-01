# LoRaWAN_GPS_lorawan_gps

Tier **B** (routine). Category: **timeout**: hit the 6 h job budget before the router finished. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | TIMED_OUT |
| Unrouted connections | 1 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 994.1263 |
| Wall clock | 360.1 min |
| Connections (total / still open) | 227 / 1 |
| Autorouter stop reason | stagnation after 31 passes |
| Optimizer | None |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 1 | 0 | 994.1 | stagnation | 1.0 min |
| continue | 1 | 0 | 994.1 | stagnation | 0.3 min |
| direct-first | 8 | 0 | 953.0 | stagnation | 1.4 min |
| fanout-retry | 6 | 0 | 964.8 | stagnation | 1.0 min |
| short-escape | 14 | 0 | 917.8 | timed_out | 356.3 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 5 | 4 |
| clearance | 1 | 5 |
| hole_clearance | 0 | 10 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 18 | 15 |

Open connections reported by KiCad:

- Track [GND] on F.Cu, length 5.7938 mm @ (107.52, 76.84) ↔ Track [GND] on B.Cu, length 2.5277 mm @ (107.52, 76.84)
- Pad 0 [GND] of SMA_E1 on all copper layers @ (107.52, 83.16) ↔ Pad 3 [GND] of SMA_E1 on B.Cu @ (107.52, 83.16)
- Pad 0 [GND] of SMA_E2 on all copper layers @ (177.77, 76.84) ↔ Pad 3 [GND] of SMA_E2 on B.Cu @ (177.77, 76.84)
- Pad 4 [GND] of SMA_E2 on B.Cu @ (177.77, 83.16) ↔ Pad 2 [GND] of SMA_E2 on all copper layers @ (177.77, 83.16)
- Pad 23 [/MCU/MPU_SCL] of U2 on F.Cu @ (127.62, 71.48) ↔ Track [/MCU/MPU_SCL] on B.Cu, length 1.2308 mm @ (127.34, 74.91)

## Board

- Source: https://github.com/robot-army/LoRaWAN_GPS (author robot-army, license GPL-3.0)
- 2 layers, 25 nets, 120 components, 95.0 × 20.0 mm, 5 zones
- Human reference layout: 935 segments, 93 vias, 1253.02 mm of track, 514 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/LoRaWAN_GPS_lorawan_gps/`](../../../../fixtures/PCBench/LoRaWAN_GPS_lorawan_gps/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 3 | None | 32 | 2.6 min | FAIL |
| 2.4.1 | COMPLETED | 4 | None | 39 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 5 | 0 | 0 | 7.8 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 5 | 0 | 0 | 10.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 5 | 0 | 0 | 28.3 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 0 | 53.7 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 0 | 2.1 min | clean |
| master-edffcabb | TIMED_OUT | 1 | 0 | 0 | 360.1 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `LoRaWAN_GPS_lorawan_gps--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
