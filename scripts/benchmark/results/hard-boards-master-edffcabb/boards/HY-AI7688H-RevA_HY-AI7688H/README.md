# HY-AI7688H-RevA_HY-AI7688H

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 5 |
| Clearance violations (total) | 2 |
| of which router-introduced | 0 |
| of which pre-existing in input | 2 (unfixable: 2) |
| Worst violation depth | 11.920928955078125 µm |
| Quality score | 982.48706 |
| Wall clock | 1.4 min |
| Connections (total / still open) | 382 / 5 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 5 | 0 | 982.5 | stagnation | 0.2 min |
| direct-first | 5 | 0 | 982.5 | stagnation | 0.3 min |
| fanout-retry | 243 | 0 | 242.5 | repeated | 0.0 min |
| short-escape | 6 | 0 | 979.0 | stagnation | 0.2 min |
| open-escape | 5 | 0 | 982.5 | stagnation | 0.3 min |
| no-fanout | 5 | 0 | 982.5 | stagnation | 0.3 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 5 | 0 |
| clearance | 0 | 1 |
| hole_clearance | 6 | 8 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 3 | 3 |

Open connections reported by KiCad:

- Track [GND] on B.Cu, length 0.4675 mm @ (77.62, 110.38) ↔ PTH pad 6 [GND] of P1 @ (78.83, 108.71)
- Pad 5 [GND] of P1 on F.Cu @ (78.83, 107.58) ↔ Track [GND] on F.Cu, length 2.8737 mm @ (83.06, 109.73)
- PTH pad 6 [GND] of P1 @ (78.83, 108.71) ↔ Pad 5 [GND] of P1 on F.Cu @ (78.83, 107.58)
- Track [GND] on F.Cu, length 1.0380 mm @ (79.28, 102.33) ↔ PTH pad 6 [GND] of P1 @ (78.83, 103.86)
- Track [GND] on B.Cu, length 1.4368 mm @ (103.00, 114.05) ↔ Track [GND] on F.Cu, length 1.8696 mm @ (105.50, 111.89)

## Board

- Source: https://github.com/HYknight/HY-AI7688H-RevA (author HYknight, license None)
- 2 layers, 26 nets, 217 components, 97.0 × 82.0 mm, 8 zones
- Human reference layout: 1169 segments, 144 vias, 2992.99 mm of track, 982 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/HY-AI7688H-RevA_HY-AI7688H/`](../../../../fixtures/PCBench/HY-AI7688H-RevA_HY-AI7688H/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 7 | None | 13 | 2.7 min | FAIL |
| 2.4.1 | COMPLETED | 4 | None | 15 | 6.0 min | FAIL |
| 2.5.0 | COMPLETED | 10 | 0 | 2 | 3.6 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 10 | 0 | 2 | 13.4 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 10 | 0 | 2 | 12.9 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 5 | 0 | 2 | 16.1 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 5 | 0 | 2 | 1.4 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `HY-AI7688H-RevA_HY-AI7688H--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
