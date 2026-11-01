# rfcx-sentinel-pcb_MPPT

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 13 |
| Clearance violations (total) | 10 |
| of which router-introduced | 0 |
| of which pre-existing in input | 10 (unfixable: 10) |
| Worst violation depth | 199.90234375 µm |
| Quality score | 886.74316 |
| Wall clock | 273.0 min |
| Connections (total / still open) | 156 / 13 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 13 | 0 | 886.7 | stagnation | 0.2 min |
| direct-first | 20 | 0 | 826.9 | stagnation | 0.3 min |
| fanout-retry | 13 | 0 | 886.7 | stagnation | 0.1 min |
| short-escape | 14 | 0 | 878.2 | stagnation | 271.4 min |
| open-escape | 14 | 0 | 878.2 | stagnation | 0.1 min |
| no-fanout | 19 | 0 | 835.5 | stagnation | 0.3 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 13 | 0 |
| clearance | 10 | 24 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Pad 4 [Net-(C13-Pad1)] of U1 on F.Cu @ (153.83, 65.79) ↔ Pad 1 [Net-(C13-Pad1)] of D1 on F.Cu @ (155.00, 68.24)
- Track [Net-(C14-Pad1)] on F.Cu, length 0.8316 mm @ (171.85, 52.16) ↔ Pad 4 [Net-(C14-Pad1)] of U2 on F.Cu @ (174.27, 65.79)
- Pad 4 [Net-(C14-Pad1)] of U2 on F.Cu @ (174.27, 65.79) ↔ Pad 1 [Net-(C14-Pad1)] of D2 on F.Cu @ (175.35, 68.24)
- Track [Net-(C15-Pad1)] on F.Cu, length 0.8316 mm @ (171.68, 75.46) ↔ Pad 4 [Net-(C15-Pad1)] of U3 on F.Cu @ (174.14, 88.90)
- Pad 1 [Net-(C15-Pad1)] of D3 on F.Cu @ (175.44, 91.29) ↔ Pad 4 [Net-(C15-Pad1)] of U3 on F.Cu @ (174.14, 88.90)
- Pad 4 [Net-(C16-Pad1)] of U4 on F.Cu @ (153.25, 88.90) ↔ Track [Net-(C16-Pad1)] on F.Cu, length 1.2067 mm @ (154.42, 91.41)
- Pad 6 [Net-(C26-Pad1)] of U5 on F.Cu @ (194.40, 73.96) ↔ Track [Net-(C26-Pad1)] on F.Cu, length 2.1585 mm @ (193.73, 75.38)
- Pad 7 [/Panel_Out_V] of U5 on F.Cu @ (194.40, 73.31) ↔ Pad 1 [/Panel_Out_V] of R26 on F.Cu @ (197.34, 73.16)
- Pad 2 [Net-(D1-Pad2)] of D1 on F.Cu @ (151.73, 68.24) ↔ Pad 3 [Net-(D1-Pad2)] of U1 on F.Cu @ (153.18, 65.79)
- Track [Net-(D2-Pad2)] on F.Cu, length 0.5477 mm @ (172.74, 68.24) ↔ Pad 3 [Net-(D2-Pad2)] of U2 on F.Cu @ (173.62, 65.79)
- Track [Net-(D3-Pad2)] on F.Cu, length 1.2067 mm @ (172.16, 91.29) ↔ Pad 3 [Net-(D3-Pad2)] of U3 on F.Cu @ (173.49, 88.90)
- Pad 2 [Net-(D4-Pad2)] of D4 on F.Cu @ (151.15, 91.41) ↔ Pad 3 [Net-(D4-Pad2)] of U4 on F.Cu @ (152.60, 88.90)
- Track [Net-(D9-Pad2)] on F.Cu, length 3.3267 mm @ (194.29, 68.03) ↔ Pad 8 [Net-(D9-Pad2)] of U5 on F.Cu @ (194.40, 72.66)

## Board

- Source: https://github.com/rfcx/rfcx-sentinel-pcb (author rfcx, license Apache-2.0)
- 2 layers, 33 nets, 88 components, 83.82 × 63.5 mm, 2 zones
- Human reference layout: 342 segments, 17 vias, 783.77 mm of track, 307 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/rfcx-sentinel-pcb_MPPT/`](../../../../fixtures/PCBench/rfcx-sentinel-pcb_MPPT/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 13 | None | 10 | 1.3 min | FAIL |
| 2.4.1 | COMPLETED | 12 | None | 10 | 4.0 min | FAIL |
| 2.5.0 | COMPLETED | 16 | 0 | 10 | 0.8 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 14 | 0 | 10 | 4.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 14 | 0 | 10 | 3.3 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 14 | 0 | 10 | 5.6 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 14 | 0 | 10 | 3.9 min | FAIL |
| master-edffcabb | COMPLETED | 13 | 0 | 10 | 273.0 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `rfcx-sentinel-pcb_MPPT--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
