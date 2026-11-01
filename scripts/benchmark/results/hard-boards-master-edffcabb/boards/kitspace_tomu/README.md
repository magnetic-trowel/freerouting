# kitspace_tomu

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 5 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 844.96124 |
| Wall clock | 0.5 min |
| Connections (total / still open) | 43 / 5 |
| Autorouter stop reason | stagnation after 28 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 5 | 0 | 845.0 | stagnation | 0.1 min |
| direct-first | 6 | 0 | 814.0 | stagnation | 0.1 min |
| fanout-retry | 7 | 0 | 782.9 | stagnation | 0.1 min |
| short-escape | 6 | 0 | 814.0 | stagnation | 0.1 min |
| open-escape | 7 | 0 | 782.9 | stagnation | 0.1 min |
| no-fanout | 7 | 0 | 782.9 | stagnation | 0.0 min |

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
| clearance | 0 | 0 |
| hole_clearance | 4 | 2 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 22 | 39 |

Open connections reported by KiCad:

- Pad 1 [/USB_5V] of P3 on F.Cu @ (17.75, 26.12) ↔ Track [/USB_5V] on B.Cu, length 0.4803 mm @ (19.88, 24.75)
- Track [GND] on B.Cu, length 1.2000 mm @ (18.77, 23.55) ↔ Track [GND] on B.Cu, length 0.4525 mm @ (18.07, 21.52)
- Pad 21 [GND] of U1 on B.Cu @ (23.25, 21.12) ↔ Pad 4 [GND] of P3 on F.Cu @ (22.00, 19.81)
- Track [+3V3] on B.Cu, length 0.9899 mm @ (21.25, 25.07) ↔ Track [+3V3] on B.Cu, length 0.6053 mm @ (23.90, 25.77)
- Pad 23 [/CAP0B] of U1 on B.Cu @ (24.55, 21.12) ↔ Pad 1 [/CAP0B] of W1 on B.Cu @ (28.50, 20.55)

## Board

- Source: https://github.com/im-tomu/tomu-hardware (author , license [{'name': 'Creative Commons', 'link': 'https://raw.githubusercontent.com/im-tomu/tomu-hardware/master/LICENSE'}, {'name': 'TAPR', 'link': 'https://raw.githubusercontent.com/im-tomu/tomu-hardware/master/LICENSE.TAPR'}])
- 2 layers, 5 nets, 16 components, 13.0 × 11.0 mm, 10 zones
- Human reference layout: 176 segments, 13 vias, 155.63 mm of track, 210 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_tomu/`](../../../../fixtures/PCBench/kitspace_tomu/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 6 | None | 12 | 0.2 min | FAIL |
| 2.4.1 | COMPLETED | 6 | None | 20 | 0.8 min | FAIL |
| 2.5.0 | COMPLETED | 6 | 0 | 0 | 0.7 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 6 | 0 | 0 | 1.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 6 | 0 | 0 | 1.0 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 6 | 0 | 0 | 1.6 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 5 | 0 | 0 | 1.1 min | FAIL |
| master-edffcabb | COMPLETED | 5 | 0 | 0 | 0.5 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_tomu--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
