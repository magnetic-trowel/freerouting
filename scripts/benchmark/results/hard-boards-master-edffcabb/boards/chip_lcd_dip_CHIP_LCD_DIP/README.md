# chip_lcd_dip_CHIP_LCD_DIP

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 1 |
| Clearance violations (total) | 36 |
| of which router-introduced | 0 |
| of which pre-existing in input | 36 (unfixable: 36) |
| Worst violation depth | 53.9036865234375 µm |
| Quality score | 987.95654 |
| Wall clock | 2.5 min |
| Connections (total / still open) | 135 / 1 |
| Autorouter stop reason | stagnation after 38 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 1 | 0 | 988.0 | stagnation | 0.8 min |
| continue | 1 | 0 | 988.0 | stagnation | 0.1 min |
| direct-first | 2 | 0 | 978.1 | stagnation | 0.4 min |
| fanout-retry | 95 | 0 | 195.4 | repeated | 0.0 min |
| short-escape | 1 | 0 | 988.0 | stagnation | 0.4 min |
| open-escape | 1 | 0 | 988.0 | stagnation | 0.4 min |
| no-fanout | 3 | 0 | 968.2 | stagnation | 0.4 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 1 | 0 |
| clearance | 34 | 36 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 34 | 32 |

Open connections reported by KiCad:

- Track [+3V3] on B.Cu, length 0.8589 mm @ (104.90, 95.56) ↔ Pad 4 [+3V3] of P5 on B.Cu @ (103.50, 97.56)

## Board

- Source: https://github.com/cmnybo/chip_lcd_dip (author cmnybo, license None)
- 2 layers, 5 nets, 26 components, 41.4 × 61.09 mm, 3 zones
- Human reference layout: 723 segments, 50 vias, 1745.46 mm of track, 696 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/chip_lcd_dip_CHIP_LCD_DIP/`](../../../../fixtures/PCBench/chip_lcd_dip_CHIP_LCD_DIP/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 10 | None | 42 | 1.4 min | FAIL |
| 2.4.1 | COMPLETED | 33 | None | 41 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 30 | 0 | 34 | 5.3 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 4 | 0 | 34 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 4 | 0 | 34 | 12.2 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 36 | 9.2 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 36 | 9.3 min | FAIL |
| master-edffcabb | COMPLETED | 1 | 0 | 36 | 2.5 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `chip_lcd_dip_CHIP_LCD_DIP--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
