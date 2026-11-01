# Radar-System_fmcw_radar

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
| Quality score | 928.8889 |
| Wall clock | 1.3 min |
| Connections (total / still open) | 75 / 4 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **open-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 6 | 0 | 893.3 | stagnation | 0.2 min |
| direct-first | 13 | 0 | 768.9 | stagnation | 0.2 min |
| fanout-retry | 17 | 0 | 697.8 | repeated | 0.0 min |
| short-escape | 6 | 0 | 893.3 | stagnation | 0.2 min |
| open-escape | 4 | 0 | 928.9 | stagnation | 0.2 min |
| no-fanout | 13 | 0 | 768.9 | stagnation | 0.2 min |
| continue | 4 | 0 | 928.9 | stagnation | 0.2 min |

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
| copper_edge_clearance | 10 | 10 |

Open connections reported by KiCad:

- PTH pad PAD [GND] of U1 @ (136.45, 41.65) ↔ Track [GND] on F.Cu, length 0.8083 mm @ (138.16, 41.90)
- PTH pad PAD [GND] of U3 @ (149.25, 41.20) ↔ Track [GND] on F.Cu, length 0.5417 mm @ (149.15, 38.80)
- Track [GND] on F.Cu, length 0.4417 mm @ (159.25, 41.40) ↔ PTH pad PAD [GND] of U4 @ (158.30, 41.40)

## Board

- Source: https://github.com/wvat/Radar-System (author wvat, license None)
- 4 layers, 18 nets, 30 components, 51.0 × 25.0 mm, 7 zones
- Human reference layout: 439 segments, 214 vias, 553.56 mm of track, 298 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/Radar-System_fmcw_radar/`](../../../../fixtures/PCBench/Radar-System_fmcw_radar/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 5 | None | 1 | 0.8 min | FAIL |
| 2.4.1 | COMPLETED | 5 | None | 11 | 3.4 min | FAIL |
| 2.5.0 | COMPLETED | 6 | 0 | 0 | 2.0 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 4 | 0 | 0 | 5.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 4 | 0 | 0 | 5.4 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 4 | 0 | 0 | 6.5 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 4 | 0 | 0 | 4.3 min | FAIL |
| master-edffcabb | COMPLETED | 4 | 0 | 0 | 1.3 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `Radar-System_fmcw_radar--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
