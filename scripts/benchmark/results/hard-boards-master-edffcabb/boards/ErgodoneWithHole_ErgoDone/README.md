# ErgodoneWithHole_ErgoDone

Tier **C** (complex). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 8 |
| of which router-introduced | 0 |
| of which pre-existing in input | 8 (unfixable: 8) |
| Worst violation depth | 5.48338623046875 µm |
| Quality score | 999.82117 |
| Wall clock | 2.6 min |
| Connections (total / still open) | 360 / 0 |
| Autorouter stop reason | completed after 5 passes |
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
| clearance | 240 | 296 |
| hole_clearance | 12 | 20 |
| shorting_items | 199 | 199 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 22 | 18 |

Open connections reported by KiCad:

- Track [Net-(SW5:13-Pad2)] on Back, length 4.8341 mm @ (189.07, 67.01) ↔ Track [Net-(SW5:13-Pad2)] on Back, length 1.7961 mm @ (187.64, 64.47)
- Track [Net-(SW3:13-Pad2)] on Back, length 1.7961 mm @ (187.64, 102.56) ↔ Track [Net-(SW3:13-Pad2)] on Back, length 4.8341 mm @ (189.07, 105.10)
- Track [Net-(SW4:13-Pad2)] on Back, length 4.8341 mm @ (189.07, 86.05) ↔ Track [Net-(SW4:13-Pad2)] on Back, length 1.7961 mm @ (187.64, 83.51)
- Track [Net-(SW2:13-Pad2)] on Back, length 4.8341 mm @ (189.07, 124.15) ↔ Track [Net-(SW2:13-Pad2)] on Back, length 1.7961 mm @ (187.64, 121.61)

## Board

- Source: https://github.com/randomdot/ErgodoneWithHole (author randomdot, license GPL-3.0)
- 2 layers, 56 nets, 71 components, 182.84 × 158.87 mm, 2 zones
- Human reference layout: 1402 segments, 29 vias, 5601.68 mm of track, 1388 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/ErgodoneWithHole_ErgoDone/`](../../../../fixtures/PCBench/ErgodoneWithHole_ErgoDone/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 19 | None | 28 | 12.7 min | FAIL |
| 2.4.1 | TIMED_OUT | 19 | None | 32 | 15.2 min | FAIL |
| 2.5.0 | COMPLETED | 19 | 0 | 16 | 9.6 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 20 | 0 | 8 | 20.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 20 | 0 | 8 | 59.9 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 17 | 0 | 8 | 166.3 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 0 | 0 | 8 | 2.6 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `ErgodoneWithHole_ErgoDone--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
