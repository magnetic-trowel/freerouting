# solar-lora_solar-lora

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 14 |
| Clearance violations (total) | 5 |
| of which router-introduced | 0 |
| of which pre-existing in input | 5 (unfixable: 5) |
| Worst violation depth | 0.0 µm |
| Quality score | 865.38855 |
| Wall clock | 1.6 min |
| Connections (total / still open) | 139 / 14 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 14 | 0 | 865.4 | stagnation | 0.2 min |
| direct-first | 15 | 0 | 855.8 | stagnation | 0.3 min |
| fanout-retry | 18 | 0 | 827.0 | stagnation | 0.3 min |
| short-escape | 17 | 0 | 836.6 | stagnation | 0.3 min |
| open-escape | 16 | 0 | 846.2 | stagnation | 0.2 min |
| no-fanout | 15 | 0 | 855.8 | stagnation | 0.3 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 14 | 0 |
| clearance | 0 | 0 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 8 | 5 |

Open connections reported by KiCad:

- Pad 10 [Earth] of U1 on F.Cu @ (152.03, 127.00) ↔ Pad 21 [Earth] of U1 on F.Cu @ (153.38, 126.62)
- Pad 11 [Earth] of U1 on F.Cu @ (153.00, 127.97) ↔ Pad 10 [Earth] of U1 on F.Cu @ (152.03, 127.00)
- Pad 11 [Earth] of U1 on F.Cu @ (153.00, 127.97) ↔ Track [Earth] on F.Cu, length 0.9517 mm @ (149.55, 131.00)
- Track [reg_in] on F.Cu, length 1.0017 mm @ (153.50, 106.00) ↔ PTH pad 1 [reg_in] of reg in @ (157.50, 106.50)
- Track [reg_in] on F.Cu, length 5.6800 mm @ (153.50, 107.00) ↔ Pad 1 [reg_in] of U1 on F.Cu @ (155.00, 124.03)
- Pad 1 [reg_in] of U1 on F.Cu @ (155.00, 124.03) ↔ Pad 20 [reg_in] of U1 on F.Cu @ (155.97, 125.00)
- Pad 4 [VCC] of U1 on F.Cu @ (153.50, 124.03) ↔ Pad 3 [VCC] of U1 on F.Cu @ (154.00, 124.03)
- Pad 4 [VCC] of U1 on F.Cu @ (153.50, 124.03) ↔ Pad 9 [VCC] of U1 on F.Cu @ (152.03, 126.50)
- Pad 19 [VCC] of U1 on F.Cu @ (155.97, 125.50) ↔ Pad 3 [VCC] of U1 on F.Cu @ (154.00, 124.03)
- Track [VCC] on F.Cu, length 0.8667 mm @ (155.97, 126.00) ↔ Pad 19 [VCC] of U1 on F.Cu @ (155.97, 125.50)
- Pad 17 [VCC] of U1 on F.Cu @ (155.97, 126.50) ↔ Track [VCC] on F.Cu, length 0.8667 mm @ (155.97, 126.00)
- Track [batt] on B.Cu, length 6.7695 mm @ (151.64, 121.81) ↔ Pad 14 [batt] of U1 on F.Cu @ (154.50, 127.97)
- Pad 15 [batt] of U1 on F.Cu @ (155.00, 127.97) ↔ Pad 14 [batt] of U1 on F.Cu @ (154.50, 127.97)
- Pad 16 [batt] of U1 on F.Cu @ (155.97, 127.00) ↔ Pad 15 [batt] of U1 on F.Cu @ (155.00, 127.97)

## Board

- Source: https://github.com/mattvenn/solar-lora (author mattvenn, license None)
- 2 layers, 18 nets, 64 components, 60.0 × 60.0 mm, 2 zones
- Human reference layout: 591 segments, 46 vias, 1635.78 mm of track, 457 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/solar-lora_solar-lora/`](../../../../fixtures/PCBench/solar-lora_solar-lora/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 21 | None | 11 | 1.1 min | FAIL |
| 2.4.1 | COMPLETED | 16 | None | 11 | 3.1 min | FAIL |
| 2.5.0 | COMPLETED | 25 | 0 | 0 | 2.8 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 23 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 23 | 0 | 0 | 15.6 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 16 | 0 | 5 | 7.0 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 14 | 0 | 5 | 5.5 min | FAIL |
| master-edffcabb | COMPLETED | 14 | 0 | 5 | 1.6 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `solar-lora_solar-lora--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
