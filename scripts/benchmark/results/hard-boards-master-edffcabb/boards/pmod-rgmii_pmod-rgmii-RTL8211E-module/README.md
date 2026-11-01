# pmod-rgmii_pmod-rgmii-RTL8211E-module

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 2 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 987.4214 |
| Wall clock | 7.8 min |
| Connections (total / still open) | 212 / 2 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **continue**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 981.1 | stagnation | 1.7 min |
| continue | 2 | 0 | 987.4 | stagnation | 0.7 min |
| direct-first | 3 | 0 | 981.1 | stagnation | 0.4 min |
| fanout-retry | 8 | 0 | 949.7 | stagnation | 2.0 min |
| short-escape | 4 | 0 | 974.8 | stagnation | 1.0 min |
| open-escape | 4 | 0 | 974.8 | stagnation | 0.9 min |
| no-fanout | 8 | 0 | 949.7 | stagnation | 1.1 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 2 | 0 |
| clearance | 44 | 52 |
| hole_clearance | 1 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Pad 28 [N-0000053] of U3 on F.Cu @ (102.15, 126.61) ↔ Via [N-0000053] on F.Cu - B.Cu @ (100.51, 124.95)
- Pad 8 [N-000004] of U3 on F.Cu @ (107.95, 127.01) ↔ PTH pad 5 [N-000004] of J1 @ (120.27, 127.39)

## Board

- Source: https://github.com/mithro/pmod-rgmii (author mithro, license None)
- 4 layers, 0 nets, 70 components, 57.08 × 83.8 mm, 4 zones
- Human reference layout: 989 segments, 192 vias, 1790.58 mm of track, 658 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/pmod-rgmii_pmod-rgmii-RTL8211E-module/`](../../../../fixtures/PCBench/pmod-rgmii_pmod-rgmii-RTL8211E-module/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 5 | None | 43 | 2.8 min | FAIL |
| 2.4.1 | COMPLETED | 27 | None | 0 | 5.2 min | FAIL |
| 2.5.0 | COMPLETED | 15 | 0 | 0 | 9.3 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 9 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 8 | 0 | 0 | 46.7 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 2 | 0 | 0 | 64.1 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 2 | 0 | 0 | 7.8 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `pmod-rgmii_pmod-rgmii-RTL8211E-module--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
