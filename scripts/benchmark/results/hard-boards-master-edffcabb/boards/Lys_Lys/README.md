# Lys_Lys

Tier **B** (routine). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 2 |
| of which router-introduced | 0 |
| of which pre-existing in input | 2 (unfixable: 2) |
| Worst violation depth | 56.671142578125 µm |
| Quality score | 999.93146 |
| Wall clock | 1.7 min |
| Connections (total / still open) | 493 / 0 |
| Autorouter stop reason | completed after 4 passes |
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
| unconnected | 44 | 0 |
| clearance | 7 | 4 |
| hole_clearance | 0 | 1 |
| shorting_items | 32 | 32 |
| tracks_crossing | 0 | 1 |
| copper_edge_clearance | 0 | 3 |

Open connections reported by KiCad:

- PTH pad 0 [GND] of D3 @ (85.16, 46.73) ↔ PTH pad 0 [GND] of D3 @ (85.85, 46.59)
- PTH pad 0 [GND] of D3 @ (85.30, 47.42) ↔ PTH pad 0 [GND] of D3 @ (85.16, 46.73)
- PTH pad 0 [GND] of D3 @ (85.45, 48.12) ↔ PTH pad 0 [GND] of D3 @ (86.14, 47.97)
- PTH pad 0 [GND] of D3 @ (85.45, 48.12) ↔ PTH pad 0 [GND] of D3 @ (85.30, 47.42)
- PTH pad 0 [GND] of D3 @ (85.60, 48.81) ↔ PTH pad 0 [GND] of D3 @ (86.29, 48.66)
- PTH pad 0 [GND] of D3 @ (85.85, 46.59) ↔ PTH pad 0 [GND] of D3 @ (86.54, 46.44)
- PTH pad 0 [GND] of D3 @ (85.85, 46.59) ↔ PTH pad 0 [GND] of D3 @ (85.70, 45.89)
- PTH pad 0 [GND] of D3 @ (86.14, 47.97) ↔ PTH pad 0 [GND] of D3 @ (85.99, 47.28)
- PTH pad 0 [GND] of D3 @ (86.29, 48.66) ↔ PTH pad 0 [GND] of D3 @ (86.14, 47.97)
- PTH pad 0 [GND] of D3 @ (86.44, 49.35) ↔ Track [GND] on B.Cu, length 0.2366 mm @ (86.75, 50.21)
- PTH pad 0 [GND] of D3 @ (86.44, 49.35) ↔ PTH pad 0 [GND] of D3 @ (85.74, 49.50)
- PTH pad 0 [GND] of D3 @ (86.54, 46.44) ↔ PTH pad 0 [GND] of D3 @ (87.23, 46.29)
- PTH pad 0 [GND] of D3 @ (86.54, 46.44) ↔ PTH pad 0 [GND] of D3 @ (86.39, 45.75)
- PTH pad 0 [GND] of D3 @ (86.69, 47.13) ↔ PTH pad 0 [GND] of D3 @ (85.99, 47.28)
- PTH pad 0 [GND] of D3 @ (86.69, 47.13) ↔ PTH pad 0 [GND] of D3 @ (86.83, 47.82)
- Track [GND] on B.Cu, length 9.9387 mm @ (86.75, 60.15) ↔ PTH pad 0 [GND] of D3 @ (87.27, 49.90)
- PTH pad 0 [GND] of D3 @ (86.98, 48.51) ↔ PTH pad 0 [GND] of D3 @ (87.13, 49.21)
- PTH pad 0 [GND] of D3 @ (86.98, 48.51) ↔ PTH pad 0 [GND] of D3 @ (86.83, 47.82)
- PTH pad 0 [GND] of D3 @ (87.08, 45.60) ↔ PTH pad 0 [GND] of D3 @ (87.23, 46.29)
- Track [GND] on F.Cu, length 6.3440 mm @ (87.08, 45.60) ↔ PTH pad 0 [GND] of D3 @ (87.78, 45.45)
- PTH pad 0 [GND] of D3 @ (87.13, 49.21) ↔ PTH pad 0 [GND] of D3 @ (87.27, 49.90)
- PTH pad 0 [GND] of D3 @ (87.27, 49.90) ↔ PTH pad 0 [GND] of D3 @ (87.97, 49.75)
- PTH pad 0 [GND] of D3 @ (87.38, 46.98) ↔ PTH pad 0 [GND] of D3 @ (87.53, 47.67)
- PTH pad 0 [GND] of D3 @ (87.78, 45.45) ↔ PTH pad 0 [GND] of D3 @ (87.92, 46.14)
- PTH pad 0 [GND] of D3 @ (87.82, 49.06) ↔ PTH pad 0 [GND] of D3 @ (88.51, 48.91)
- PTH pad 0 [GND] of D3 @ (87.82, 49.06) ↔ PTH pad 0 [GND] of D3 @ (87.67, 48.37)
- PTH pad 0 [GND] of D3 @ (87.97, 49.75) ↔ PTH pad 0 [GND] of D3 @ (87.82, 49.06)
- PTH pad 0 [GND] of D3 @ (88.07, 46.84) ↔ PTH pad 0 [GND] of D3 @ (87.92, 46.14)
- PTH pad 0 [GND] of D3 @ (88.07, 46.84) ↔ PTH pad 0 [GND] of D3 @ (87.38, 46.98)
- PTH pad 0 [GND] of D3 @ (88.22, 47.53) ↔ PTH pad 0 [GND] of D3 @ (88.36, 48.22)
- PTH pad 0 [GND] of D3 @ (88.36, 48.22) ↔ PTH pad 0 [GND] of D3 @ (89.06, 48.07)
- PTH pad 0 [GND] of D3 @ (88.47, 45.31) ↔ PTH pad 0 [GND] of D3 @ (87.78, 45.45)
- PTH pad 0 [GND] of D3 @ (88.51, 48.91) ↔ PTH pad 0 [GND] of D3 @ (89.20, 48.76)
- PTH pad 0 [GND] of D3 @ (88.61, 46.00) ↔ PTH pad 0 [GND] of D3 @ (87.92, 46.14)
- PTH pad 0 [GND] of D3 @ (88.66, 49.60) ↔ PTH pad 0 [GND] of D3 @ (87.97, 49.75)
- PTH pad 0 [GND] of D3 @ (88.91, 47.38) ↔ PTH pad 0 [GND] of D3 @ (88.76, 46.69)
- PTH pad 0 [GND] of D3 @ (89.06, 48.07) ↔ PTH pad 0 [GND] of D3 @ (89.20, 48.76)
- PTH pad 0 [GND] of D3 @ (89.20, 48.76) ↔ PTH pad 0 [GND] of D3 @ (89.35, 49.46)
- PTH pad 0 [GND] of D3 @ (89.31, 45.85) ↔ PTH pad 0 [GND] of D3 @ (89.45, 46.54)
- PTH pad 0 [GND] of D3 @ (89.60, 47.23) ↔ PTH pad 0 [GND] of D3 @ (88.91, 47.38)
- … 4 more in `../../drc-summary.json`

## Board

- Source: https://github.com/Utstumo/Lys (author Utstumo, license NOASSERTION)
- 2 layers, 36 nets, 78 components, 53.0 × 79.0 mm, 10 zones
- Human reference layout: 818 segments, 82 vias, 1651.33 mm of track, 551 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/Lys_Lys/`](../../../../fixtures/PCBench/Lys_Lys/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 3 | None | 1249 | 4.7 min | FAIL |
| 2.4.1 | TIMED_OUT | 87 | None | 1252 | 5.2 min | FAIL |
| 2.5.0 | COMPLETED | 3 | 0 | 47 | 7.2 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 0 | 0 | 2 | 10.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 2 | 11.3 min | clean |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 2 | 16.4 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 2 | 9.8 min | clean |
| master-edffcabb | COMPLETED | 0 | 0 | 2 | 1.7 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `Lys_Lys--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
