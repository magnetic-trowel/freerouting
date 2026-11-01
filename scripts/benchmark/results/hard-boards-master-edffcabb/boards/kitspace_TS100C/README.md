# kitspace_TS100C

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
| Quality score | 853.8813 |
| Wall clock | 2.8 min |
| Connections (total / still open) | 73 / 8 |
| Autorouter stop reason | stagnation after 26 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **short-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 9 | 0 | 835.6 | stagnation | 0.5 min |
| direct-first | 9 | 0 | 835.6 | stagnation | 0.5 min |
| fanout-retry | 11 | 0 | 799.1 | stagnation | 0.5 min |
| short-escape | 8 | 0 | 853.9 | stagnation | 0.5 min |
| open-escape | 12 | 0 | 780.8 | stagnation | 0.5 min |
| no-fanout | 9 | 0 | 835.6 | stagnation | 0.3 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 8 | 0 |
| clearance | 0 | 27 |
| hole_clearance | 0 | 2 |
| shorting_items | 0 | 2 |
| tracks_crossing | 0 | 9 |
| copper_edge_clearance | 20 | 52 |

Open connections reported by KiCad:

- Pad 25 [INT_N] of U4 on F.Cu @ (105.30, 88.44) ↔ Track [INT_N] on F.Cu, length 1.2625 mm @ (101.27, 83.21)
- Track [SDA] on F.Cu, length 1.4312 mm @ (102.23, 85.69) ↔ Pad 8 [SDA] of U3 on F.Cu @ (97.83, 87.31)
- Pad 7 [SCL] of U3 on F.Cu @ (97.33, 87.31) ↔ Track [SCL] on F.Cu, length 1.1092 mm @ (103.31, 85.47)
- Pad 32 [USB_N] of U4 on F.Cu @ (108.80, 88.44) ↔ Track [USB_N] on F.Cu, length 0.8567 mm @ (91.92, 86.10)
- Pad 7 [nRST] of U4 on F.Cu @ (107.80, 81.56) ↔ Pad 3 [nRST] of P1 on F.Cu @ (119.03, 88.61)
- Pad 10 [IIN] of U4 on F.Cu @ (106.30, 81.56) ↔ Track [IIN] on F.Cu, length 0.1816 mm @ (127.62, 84.41)
- Pad 2 [Net-(SW2-Pad2)] of SW2 on B.Cu @ (153.70, 82.92) ↔ Pad 14 [Net-(SW2-Pad2)] of U4 on F.Cu @ (104.61, 82.75)
- Pad 43 [OLED_SDA] of U4 on F.Cu @ (111.49, 84.75) ↔ Track [OLED_SDA] on B.Cu, length 1.6935 mm @ (126.69, 87.12)

## Board

- Source: https://github.com/Jana-Marie/Otter-Iron (author , license [{'name': 'GNU', 'link': 'https://raw.githubusercontent.com/Jana-Marie/Otter-Iron/main/LICENSE'}])
- 4 layers, 79 nets, 64 components, 84.0 × 10.0 mm, 21 zones
- Human reference layout: 527 segments, 108 vias, 724.66 mm of track, 161 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_TS100C/`](../../../../fixtures/PCBench/kitspace_TS100C/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 15 | None | 40 | 1.4 min | FAIL |
| 2.4.1 | COMPLETED | 13 | None | 14 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 10 | 0 | 0 | 2.2 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 10 | 0 | 0 | 9.3 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 10 | 0 | 0 | 7.1 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 10 | 0 | 0 | 15.2 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 9 | 0 | 0 | 18.7 min | FAIL |
| master-edffcabb | COMPLETED | 8 | 0 | 0 | 2.8 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_TS100C--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
