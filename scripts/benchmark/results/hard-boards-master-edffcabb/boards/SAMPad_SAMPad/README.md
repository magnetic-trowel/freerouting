# SAMPad_SAMPad

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 4 |
| Clearance violations (total) | 24 |
| of which router-introduced | 0 |
| of which pre-existing in input | 24 (unfixable: 24) |
| Worst violation depth | 152.4 µm |
| Quality score | 983.65466 |
| Wall clock | 17.3 min |
| Connections (total / still open) | 361 / 4 |
| Autorouter stop reason | stagnation after 31 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **direct-first**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 5 | 0 | 980.0 | stagnation | 2.6 min |
| direct-first | 4 | 0 | 983.7 | stagnation | 3.8 min |
| fanout-retry | 5 | 0 | 980.0 | stagnation | 2.1 min |
| short-escape | 4 | 0 | 983.7 | stagnation | 2.7 min |
| open-escape | 5 | 0 | 980.0 | stagnation | 1.7 min |
| no-fanout | 5 | 0 | 980.0 | stagnation | 2.9 min |
| continue | 4 | 0 | 983.7 | stagnation | 1.6 min |

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
| clearance | 24 | 44 |
| hole_clearance | 153 | 159 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 36 | 19 |

Open connections reported by KiCad:

- Track [/ledmatrix/LEDI1] on F.Cu, length 9.0299 mm @ (137.91, 128.07) ↔ Pad 3 [/ledmatrix/LEDI1] of P6 on B.Cu @ (145.12, 128.65)
- Track [/ledmatrix/LEDI2] on B.Cu, length 2.5884 mm @ (152.29, 120.37) ↔ Pad 4 [/ledmatrix/LEDI2] of P6 on B.Cu @ (145.12, 127.38)
- Via [+5V] on F.Cu - B.Cu @ (129.79, 122.31) ↔ Pad 2 [+5V] of P6 on B.Cu @ (146.39, 127.38)
- Pad 1 [/mcu/MISO] of P6 on B.Cu @ (146.39, 128.65) ↔ Track [/mcu/MISO] on B.Cu, length 0.5817 mm @ (136.59, 144.90)

## Board

- Source: https://github.com/mohitg11/SAMPad (author mohitg11, license None)
- 2 layers, 41 nets, 154 components, 73.15 × 116.01 mm, 8 zones
- Human reference layout: 1942 segments, 127 vias, 5540.64 mm of track, 1232 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/SAMPad_SAMPad/`](../../../../fixtures/PCBench/SAMPad_SAMPad/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 8 | None | 164 | 9.3 min | FAIL |
| 2.4.1 | COMPLETED | 48 | None | 167 | 15.4 min | FAIL |
| 2.5.0 | COMPLETED | 79 | 0 | 24 | 11.5 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 44 | 0 | 24 | 20.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 44 | 0 | 24 | 45.5 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 5 | 0 | 24 | 100.7 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 4 | 0 | 24 | 17.3 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `SAMPad_SAMPad--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
