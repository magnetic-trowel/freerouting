# zx-sizif-512-ext_sizif512ext

Tier **D** (diagnostic). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 23 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 938.42035 |
| Wall clock | 152.3 min |
| Connections (total / still open) | 498 / 23 |
| Autorouter stop reason | stagnation after 33 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 23 | 0 | 938.4 | stagnation | 25.0 min |
| direct-first | 31 | 0 | 917.0 | stagnation | 35.2 min |
| fanout-retry | 38 | 0 | 898.3 | stagnation | 20.7 min |
| short-escape | 37 | 0 | 900.9 | stagnation | 21.3 min |
| open-escape | 27 | 0 | 927.7 | stagnation | 24.9 min |
| no-fanout | 35 | 0 | 906.3 | stagnation | 25.0 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 23 | 0 |
| clearance | 0 | 1 |
| hole_clearance | 0 | 3 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 50 | 60 |

Open connections reported by KiCad:

- Pad 9 [GND] of U11 on F.Cu @ (140.32, 118.28) ↔ Pad 49 [GND] of U11 on F.Cu @ (143.27, 117.28)
- Pad 10 [GND] of U11 on F.Cu @ (140.32, 118.68) ↔ Pad 9 [GND] of U11 on F.Cu @ (140.32, 118.28)
- Pad 10 [GND] of U11 on F.Cu @ (140.32, 118.68) ↔ Pad 13 [GND] of U11 on F.Cu @ (141.07, 120.23)
- Pad 10 [GND] of U11 on F.Cu @ (140.32, 118.68) ↔ Track [GND] on B.Cu, length 4.8437 mm @ (138.03, 120.22)
- Pad 2 [GND] of C70 on B.Cu @ (140.47, 112.42) ↔ Track [GND] on B.Cu, length 0.6250 mm @ (138.03, 114.40)
- Pad 14 [GND] of U11 on F.Cu @ (141.47, 120.23) ↔ Pad 13 [GND] of U11 on F.Cu @ (141.07, 120.23)
- Track [GND] on B.Cu, length 1.6942 mm @ (143.72, 123.32) ↔ Pad 14 [GND] of U11 on F.Cu @ (141.47, 120.23)
- Pad 2 [GND] of C69 on B.Cu @ (144.57, 112.42) ↔ Pad 2 [GND] of C70 on B.Cu @ (140.47, 112.42)
- Pad 77 [GND] of U7 on F.Cu @ (201.06, 96.15) ↔ Track [GND] on B.Cu, length 1.1767 mm @ (195.37, 96.52)
- Track [GND] on B.Cu, length 0.9215 mm @ (209.85, 76.52) ↔ Pad 1 [GND] of U13 on B.Cu @ (202.05, 77.17)
- Pad 8 [+3V3] of U11 on F.Cu @ (140.32, 117.88) ↔ Pad 11 [+3V3] of U11 on F.Cu @ (140.32, 119.08)
- Pad 11 [+3V3] of U11 on F.Cu @ (140.32, 119.08) ↔ Pad 12 [+3V3] of U11 on F.Cu @ (140.32, 119.48)
- Pad 12 [+3V3] of U11 on F.Cu @ (140.32, 119.48) ↔ Pad 1 [+3V3] of C71 on B.Cu @ (140.47, 122.12)
- Pad 47 [+3V3] of U11 on F.Cu @ (141.47, 114.33) ↔ Pad 8 [+3V3] of U11 on F.Cu @ (140.32, 117.88)
- Pad 46 [+3V3] of U11 on F.Cu @ (141.87, 114.33) ↔ Pad 47 [+3V3] of U11 on F.Cu @ (141.47, 114.33)
- Pad 46 [+3V3] of U11 on F.Cu @ (141.87, 114.33) ↔ Pad 41 [+3V3] of U11 on F.Cu @ (143.87, 114.33)
- Track [+3V3] on B.Cu, length 0.0379 mm @ (142.53, 112.39) ↔ Pad 46 [+3V3] of U11 on F.Cu @ (141.87, 114.33)
- Pad 41 [+3V3] of U11 on F.Cu @ (143.87, 114.33) ↔ Pad 35 [+3V3] of U11 on F.Cu @ (146.22, 115.48)
- Pad 20 [+3V3] of U11 on F.Cu @ (143.87, 120.23) ↔ Pad 1 [+3V3] of C68 on B.Cu @ (144.57, 122.12)
- Pad 20 [+3V3] of U11 on F.Cu @ (143.87, 120.23) ↔ Pad 12 [+3V3] of U11 on F.Cu @ (140.32, 119.48)
- Pad 35 [+3V3] of U11 on F.Cu @ (146.22, 115.48) ↔ Pad 31 [+3V3] of U11 on F.Cu @ (146.22, 117.08)
- Pad 5 [Net-(C26-Pad1)] of U11 on F.Cu @ (140.32, 116.68) ↔ Track [Net-(C26-Pad1)] on B.Cu, length 1.6452 mm @ (139.03, 113.80)
- Pad 2 [/MIDI_R] of U11 on F.Cu @ (140.32, 115.48) ↔ PTH pad 1 [/MIDI_R] of C22 @ (144.81, 108.47)

## Board

- Source: https://github.com/UzixLS/zx-sizif-512-ext (author UzixLS, license MIT)
- 2 layers, 45 nets, 156 components, 213.0 × 64.0 mm, 2 zones
- Human reference layout: 2519 segments, 294 vias, 10955.95 mm of track, 470 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/zx-sizif-512-ext_sizif512ext/`](../../../../fixtures/PCBench/zx-sizif-512-ext_sizif512ext/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 52 | None | 169 | 16.7 min | FAIL |
| 2.4.1 | COMPLETED | 48 | None | 94 | 15.2 min | FAIL |
| 2.5.0 | TIMED_OUT | 148 | 0 | 0 | 20.3 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 136 | 0 | 0 | 20.3 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 115 | 0 | 0 | 321.1 min | FAIL |
| 2.6.0-RC3 | TIMED_OUT | 27 | 0 | 0 | 360.2 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 23 | 0 | 0 | 152.3 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `zx-sizif-512-ext_sizif512ext--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
