# DronPi_dronPi

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 8 |
| Clearance violations (total) | 2 |
| of which router-introduced | 0 |
| of which pre-existing in input | 2 (unfixable: 2) |
| Worst violation depth | 23.1536865234375 µm |
| Quality score | 971.49585 |
| Wall clock | 12.8 min |
| Connections (total / still open) | 375 / 8 |
| Autorouter stop reason | stagnation after 19 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 8 | 0 | 971.5 | stagnation | 1.4 min |
| direct-first | 12 | 0 | 957.3 | stagnation | 2.2 min |
| fanout-retry | 8 | 0 | 971.5 | stagnation | 2.3 min |
| short-escape | 13 | 0 | 953.7 | stagnation | 2.1 min |
| open-escape | 8 | 0 | 971.5 | stagnation | 3.0 min |
| no-fanout | 12 | 0 | 957.3 | stagnation | 1.8 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 8 | 0 |
| clearance | 4 | 5 |
| hole_clearance | 1 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 7 | 0 |

Open connections reported by KiCad:

- Track [+3v3] on F.Cu, length 1.4323 mm @ (146.16, 104.88) ↔ Pad 1 [+3v3] of IC2 on F.Cu @ (147.65, 102.15)
- Track [+3v3] on F.Cu, length 1.4323 mm @ (146.16, 104.88) ↔ Pad 8 [+3v3] of IC2 on F.Cu @ (148.55, 104.65)
- Pad 8 [+3v3] of IC2 on F.Cu @ (148.55, 104.65) ↔ Pad 13 [+3v3] of IC2 on F.Cu @ (150.65, 104.15)
- Track [+3v3] on F.Cu, length 0.5551 mm @ (149.24, 97.67) ↔ Pad 1 [+3v3] of IC2 on F.Cu @ (147.65, 102.15)
- Pad 1 [+3v3] of C12 on F.Cu @ (152.65, 104.14) ↔ Pad 13 [+3v3] of IC2 on F.Cu @ (150.65, 104.15)
- Track [SPI_MISO] on F.Cu, length 0.4452 mm @ (146.19, 100.35) ↔ Pad 9 [SPI_MISO] of IC2 on F.Cu @ (148.95, 104.65)
- Pad 5 [+5v_SERV] of IC11 on F.Cu @ (173.61, 125.11) ↔ Pad 5 [+5v_SERV] of IC12 on F.Cu @ (173.61, 122.57)
- PTH pad 26 [SPI_CS_MPU] of J1 @ (145.84, 76.45) ↔ Pad 22 [SPI_CS_MPU] of IC2 on F.Cu @ (148.95, 101.65)

## Board

- Source: https://github.com/radioelf/DronPi (author radioelf, license None)
- 2 layers, 103 nets, 141 components, 0.0 × 0.0 mm, 2 zones
- Human reference layout: 1606 segments, 138 vias, 2804.72 mm of track, 875 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/DronPi_dronPi/`](../../../../fixtures/PCBench/DronPi_dronPi/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 15 | None | 35 | 6.3 min | FAIL |
| 2.4.1 | TIMED_OUT | 11 | None | 33 | 15.2 min | FAIL |
| 2.5.0 | COMPLETED | 22 | 0 | 2 | 6.6 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 16 | 0 | 2 | 20.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 14 | 0 | 2 | 32.9 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 5 | 0 | 2 | 40.4 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 8 | 0 | 2 | 12.8 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `DronPi_dronPi--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
