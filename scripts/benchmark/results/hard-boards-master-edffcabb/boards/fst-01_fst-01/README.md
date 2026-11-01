# fst-01_fst-01

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 8 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 833.3333 |
| Wall clock | 0.6 min |
| Connections (total / still open) | 64 / 8 |
| Autorouter stop reason | stagnation after 22 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **no-fanout**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 12 | 0 | 750.0 | stagnation | 0.2 min |
| direct-first | 11 | 0 | 770.8 | stagnation | 0.1 min |
| fanout-retry | 11 | 0 | 770.8 | stagnation | 0.1 min |
| short-escape | 12 | 0 | 750.0 | stagnation | 0.1 min |
| open-escape | 11 | 0 | 770.8 | stagnation | 0.1 min |
| no-fanout | 8 | 0 | 833.3 | stagnation | 0.1 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 7 | 0 |
| clearance | 0 | 0 |
| hole_clearance | 0 | 5 |
| shorting_items | 1 | 1 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 36 | 14 |

Open connections reported by KiCad:

- Pad 36 [GND] of U1 on Front @ (116.21, 95.84) ↔ Pad 35 [GND] of U1 on Front @ (116.71, 95.84)
- Pad 35 [GND] of U1 on Front @ (116.71, 95.84) ↔ Pad 1 [GND] of C5 on Front @ (116.47, 94.63)
- Track [GND] on Front, length 2.0828 mm @ (117.17, 97.57) ↔ Pad 35 [GND] of U1 on Front @ (116.71, 95.84)
- Track [GND] on Front, length 2.0828 mm @ (117.17, 97.57) ↔ Track [GND] on Front, length 2.0828 mm @ (119.25, 99.66)
- Pad 36 [GND] of U1 on Front @ (119.25, 97.57) ↔ Track [GND] on Back, length 13.1398 mm @ (121.84, 97.33)
- Pad 1 [/OSC_IN] of C1 on Front @ (115.38, 93.41) ↔ Pad 1 [/OSC_IN] of X1 on Front @ (112.99, 93.57)
- Pad 2 [/OSC_IN] of U1 on Front @ (115.44, 97.11) ↔ Pad 1 [/OSC_IN] of C1 on Front @ (115.38, 93.41)

## Board

- Source: https://github.com/vinaebizs/fst-01 (author vinaebizs, license None)
- 2 layers, 2 nets, 25 components, 26.48 × 13.15 mm, 4 zones
- Human reference layout: 270 segments, 12 vias, 298.74 mm of track, 261 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/fst-01_fst-01/`](../../../../fixtures/PCBench/fst-01_fst-01/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 11 | None | 32 | 0.4 min | FAIL |
| 2.4.1 | COMPLETED | 10 | None | 21 | 1.1 min | FAIL |
| 2.5.0 | COMPLETED | 13 | 0 | 0 | 0.4 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 11 | 0 | 0 | 1.6 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 11 | 0 | 0 | 2.5 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 11 | 0 | 0 | 2.1 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 8 | 0 | 0 | 1.6 min | FAIL |
| master-edffcabb | COMPLETED | 8 | 0 | 0 | 0.6 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `fst-01_fst-01--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
