# 96boards-robomezzi_96boards-robomezzi

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 1 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 995.27185 |
| Wall clock | 8.5 min |
| Connections (total / still open) | 282 / 1 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 1 | 0 | 995.3 | stagnation | 0.9 min |
| continue | 1 | 0 | 995.3 | stagnation | 0.9 min |
| direct-first | 1 | 0 | 995.3 | stagnation | 0.8 min |
| fanout-retry | 2 | 0 | 990.5 | stagnation | 1.2 min |
| short-escape | 1 | 0 | 995.3 | stagnation | 1.3 min |
| open-escape | 1 | 0 | 995.3 | stagnation | 1.6 min |
| no-fanout | 3 | 0 | 985.8 | stagnation | 1.8 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 1 | 0 |
| clearance | 4 | 4 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Pad 10 [Net-(C13-Pad1)] of U6 on F.Cu @ (141.08, 102.47) ↔ Pad 1 [Net-(C13-Pad1)] of C13 on B.Cu @ (142.57, 101.78)

## Board

- Source: https://github.com/mwelling/96boards-robomezzi (author mwelling, license NOASSERTION)
- 4 layers, 165 nets, 119 components, 85.0 × 54.0 mm, 2 zones
- Human reference layout: 968 segments, 113 vias, 2366.8 mm of track, 478 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/96boards-robomezzi_96boards-robomezzi/`](../../../../fixtures/PCBench/96boards-robomezzi_96boards-robomezzi/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 2 | None | 10 | 1.7 min | FAIL |
| 2.4.1 | COMPLETED | 4 | None | 6 | 11.1 min | FAIL |
| 2.5.0 | COMPLETED | 2 | 0 | 0 | 8.9 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 1 | 0 | 0 | 20.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 1 | 0 | 0 | 28.8 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 0 | 43.9 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 8.5 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `96boards-robomezzi_96boards-robomezzi--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
