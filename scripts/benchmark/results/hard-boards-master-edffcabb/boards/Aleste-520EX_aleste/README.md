# Aleste-520EX_aleste

Tier **D** (diagnostic). Category: **timeout**: hit the 6 h job budget before the router finished. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | TIMED_OUT |
| Unrouted connections | 0 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 1000.0 |
| Wall clock | 360.1 min |
| Connections (total / still open) | 1648 / 0 |
| Autorouter stop reason | completed after 13 passes |
| Optimizer | no_improvement |

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
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 4 |
| copper_edge_clearance | 11 | 77 |

Open connections reported by KiCad:

- PTH pad 7 [/Gate Array/"A"] of D26 @ (132.08, 179.07) ↔ PTH pad 14 [/Gate Array/"A"] of D87 @ (142.24, 173.99)
- PTH pad 5 [/Gate Array/"A"] of D87 @ (137.16, 166.37) ↔ PTH pad 14 [/Gate Array/"A"] of D88 @ (142.24, 162.56)
- PTH pad 14 [/Gate Array/"A"] of D88 @ (142.24, 162.56) ↔ PTH pad 5 [/Gate Array/"A"] of D88 @ (137.16, 154.94)
- PTH pad 14 [/Gate Array/"A"] of D87 @ (142.24, 173.99) ↔ PTH pad 5 [/Gate Array/"A"] of D87 @ (137.16, 166.37)

## Board

- Source: https://github.com/LasDesu/Aleste-520EX (author LasDesu, license None)
- 2 layers, 243 nets, 313 components, 334.01 × 193.04 mm, 0 zones
- Human reference layout: 8264 segments, 1041 vias, 57005.55 mm of track, 868 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/Aleste-520EX_aleste/`](../../../../fixtures/PCBench/Aleste-520EX_aleste/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | FAILED | 37 | None | 302 | 33.0 min | FAIL |
| 2.4.1 | TIMED_OUT | 875 | None | 219 | 30.2 min | FAIL |
| 2.5.0 | TIMED_OUT | 446 | 0 | 48 | 45.2 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 444 | 0 | 48 | 45.2 min | FAIL |
| 2.6.0-RC2 | TIMED_OUT | 185 | 0 | 48 | 480.2 min | FAIL |
| 2.6.0-RC3 | FAILED | 0 | None | 0 | 396.0 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | TIMED_OUT | 0 | 0 | 0 | 360.1 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `Aleste-520EX_aleste--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
