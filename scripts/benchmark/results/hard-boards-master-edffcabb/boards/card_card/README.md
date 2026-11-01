# card_card

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
| Quality score | 833.3333 |
| Wall clock | 0.4 min |
| Connections (total / still open) | 32 / 4 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 4 | 0 | 833.3 | stagnation | 0.1 min |
| continue | 4 | 0 | 833.3 | stagnation | 0.1 min |
| direct-first | 4 | 0 | 833.3 | stagnation | 0.1 min |
| fanout-retry | 10 | 0 | 583.3 | repeated | 0.0 min |
| short-escape | 4 | 0 | 833.3 | stagnation | 0.1 min |
| open-escape | 4 | 0 | 833.3 | stagnation | 0.1 min |
| no-fanout | 4 | 0 | 833.3 | stagnation | 0.1 min |

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
| clearance | 4 | 4 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 13 | 19 |

Open connections reported by KiCad:

- Track [GND] on B.Cu, length 0.7827 mm @ (190.55, 89.85) ↔ PTH pad 6 [GND] of CON1 @ (194.05, 87.63)
- Track [GND] on B.Cu, length 1.1827 mm @ (193.50, 91.20) ↔ Pad 7 [GND] of CON1 on F.Cu @ (194.05, 97.37)
- PTH pad 6 [GND] of CON1 @ (194.05, 87.63) ↔ Pad 8 [GND] of CON1 on F.Cu @ (198.05, 87.63)
- Pad 9 [GND] of CON1 on F.Cu @ (198.05, 97.37) ↔ Pad 7 [GND] of CON1 on F.Cu @ (194.05, 97.37)

## Board

- Source: https://github.com/Hylian/card (author Hylian, license None)
- 2 layers, 7 nets, 10 components, 55.0 × 85.0 mm, 56 zones
- Human reference layout: 143 segments, 12 vias, 480.17 mm of track, 376 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/card_card/`](../../../../fixtures/PCBench/card_card/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 4 | None | 25 | 0.2 min | FAIL |
| 2.4.1 | COMPLETED | 4 | None | 21 | 0.6 min | FAIL |
| 2.5.0 | COMPLETED | 4 | 0 | 0 | 0.9 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 4 | 0 | 0 | 2.0 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 4 | 0 | 0 | 2.3 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 4 | 0 | 0 | 1.3 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 4 | 0 | 0 | 1.1 min | FAIL |
| master-edffcabb | COMPLETED | 4 | 0 | 0 | 0.4 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `card_card--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
