# kitspace_f-91w

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 4 |
| Clearance violations (total) | 1 |
| of which router-introduced | 0 |
| of which pre-existing in input | 1 (unfixable: 1) |
| Worst violation depth | 27.3006591796875 µm |
| Quality score | 945.4745 |
| Wall clock | 4.4 min |
| Connections (total / still open) | 98 / 4 |
| Autorouter stop reason | stagnation after 34 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **no-fanout**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 5 | 0 | 931.9 | stagnation | 0.7 min |
| direct-first | 5 | 0 | 931.9 | stagnation | 0.7 min |
| fanout-retry | 5 | 0 | 931.9 | stagnation | 0.5 min |
| short-escape | 6 | 0 | 918.3 | stagnation | 0.7 min |
| open-escape | 8 | 0 | 891.1 | stagnation | 0.9 min |
| no-fanout | 4 | 0 | 945.5 | stagnation | 0.5 min |
| continue | 4 | 0 | 945.5 | stagnation | 0.3 min |

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
| clearance | 0 | 0 |
| hole_clearance | 58 | 50 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 11 | 1 |

Open connections reported by KiCad:

- Pad 7 [/LCD_COM1] of U2 on F.Cu @ (53.04, 90.91) ↔ Pad 12 [/LCD_COM1] of U1 on F.Cu @ (56.25, 94.05)
- Pad 25 [/LCD_SEG9] of U1 on F.Cu @ (53.55, 98.75) ↔ Pad 12 [/LCD_SEG9] of U2 on F.Cu @ (56.55, 90.91)
- Pad 13 [/LCD_SEG13] of U1 on F.Cu @ (55.75, 94.05) ↔ Pad 16 [/LCD_SEG13] of U2 on F.Cu @ (59.36, 90.91)
- Pad 48 [/SCL] of U1 on F.Cu @ (61.75, 102.97) ↔ Pad 7 [/SCL] of U4 on F.Cu @ (68.16, 94.29)

## Board

- Source: https://github.com/carrotIndustries/pluto (author , license [{'name': 'GNU', 'link': 'https://raw.githubusercontent.com/carrotIndustries/pluto/master/LICENSE'}])
- 2 layers, 18 nets, 37 components, 24.4 × 24.38 mm, 11 zones
- Human reference layout: 617 segments, 68 vias, 635.19 mm of track, 334 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_f-91w/`](../../../../fixtures/PCBench/kitspace_f-91w/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 12 | None | 44 | 1.7 min | FAIL |
| 2.4.1 | TIMED_OUT | 6 | None | 52 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 4 | 0 | 8 | 4.3 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 4 | 0 | 7 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 4 | 0 | 7 | 18.1 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 3 | 0 | 1 | 26.5 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 2 | 0 | 1 | 9.2 min | FAIL |
| master-edffcabb | COMPLETED | 4 | 0 | 1 | 4.4 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_f-91w--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
