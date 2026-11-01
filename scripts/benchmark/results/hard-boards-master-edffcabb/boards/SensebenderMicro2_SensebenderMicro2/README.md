# SensebenderMicro2_SensebenderMicro2

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 2 |
| Clearance violations (total) | 7 |
| of which router-introduced | 0 |
| of which pre-existing in input | 7 (unfixable: 7) |
| Worst violation depth | 177.8 µm |
| Quality score | 982.5855 |
| Wall clock | 1.8 min |
| Connections (total / still open) | 159 / 2 |
| Autorouter stop reason | stagnation after 20 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **short-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 974.2 | stagnation | 0.3 min |
| continue | 3 | 0 | 974.2 | stagnation | 0.1 min |
| direct-first | 3 | 1 | 974.0 | stagnation | 0.4 min |
| fanout-retry | 3 | 0 | 974.2 | stagnation | 0.2 min |
| short-escape | 2 | 0 | 982.6 | stagnation | 0.2 min |
| open-escape | 6 | 0 | 949.0 | stagnation | 0.2 min |
| no-fanout | 2 | 1 | 982.4 | stagnation | 0.2 min |
| continue | 2 | 0 | 982.6 | stagnation | 0.1 min |

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
| clearance | 1 | 14 |
| hole_clearance | 0 | 0 |
| shorting_items | 1 | 1 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 24 | 32 |

Open connections reported by KiCad:

- Track [+3V3] on F.Cu, length 1.2700 mm @ (98.17, 86.19) ↔ Track [+3V3] on F.Cu, length 0.0001 mm @ (93.76, 92.05)
- Track [/MISO] on B.Cu, length 2.5964 mm @ (98.16, 95.58) ↔ Pad 2 [/MISO] of U4 on B.Cu @ (105.67, 100.10)

## Board

- Source: https://github.com/tbowmo/SensebenderMicro2 (author tbowmo, license NOASSERTION)
- 2 layers, 17 nets, 86 components, 21.5 × 32.0 mm, 2 zones
- Human reference layout: 576 segments, 43 vias, 598.15 mm of track, 451 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/SensebenderMicro2_SensebenderMicro2/`](../../../../fixtures/PCBench/SensebenderMicro2_SensebenderMicro2/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 7 | None | 22 | 1.3 min | FAIL |
| 2.4.1 | COMPLETED | 6 | None | 44 | 4.9 min | FAIL |
| 2.5.0 | COMPLETED | 3 | 0 | 7 | 4.1 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 0 | 0 | 7 | 9.8 min | clean |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 7 | 11.7 min | clean |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 7 | 7.8 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 7 | 6.8 min | clean |
| master-edffcabb | COMPLETED | 2 | 0 | 7 | 1.8 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `SensebenderMicro2_SensebenderMicro2--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
