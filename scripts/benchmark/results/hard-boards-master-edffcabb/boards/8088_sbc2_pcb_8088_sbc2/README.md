# 8088_sbc2_pcb_8088_sbc2

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 1 |
| Clearance violations (total) | 2 |
| of which router-introduced | 0 |
| of which pre-existing in input | 2 (unfixable: 2) |
| Worst violation depth | 99.90234375 µm |
| Quality score | 995.8235 |
| Wall clock | 2.6 min |
| Connections (total / still open) | 328 / 1 |
| Autorouter stop reason | stagnation after 19 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 1 | 0 | 995.8 | stagnation | 0.6 min |
| continue | 1 | 0 | 995.8 | stagnation | 0.1 min |
| direct-first | 1 | 0 | 995.8 | stagnation | 0.4 min |
| fanout-retry | 1 | 0 | 995.8 | stagnation | 0.3 min |
| short-escape | 1 | 0 | 995.8 | stagnation | 0.3 min |
| open-escape | 1 | 0 | 995.8 | stagnation | 0.4 min |
| no-fanout | 1 | 0 | 995.8 | stagnation | 0.4 min |

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
| clearance | 2 | 3 |
| hole_clearance | 34 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Track [VCC] on B.Cu, length 26.8403 mm @ (222.72, 61.73) ↔ Pad 6 [VCC] of U6 on F.Cu @ (220.80, 71.51)

## Board

- Source: https://github.com/TobiasKaiser/8088_sbc2_pcb (author TobiasKaiser, license None)
- 2 layers, 82 nets, 65 components, 200.0 × 100.0 mm, 10 zones
- Human reference layout: 1545 segments, 221 vias, 9829.87 mm of track, 915 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/8088_sbc2_pcb_8088_sbc2/`](../../../../fixtures/PCBench/8088_sbc2_pcb_8088_sbc2/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 1 | None | 2 | 2.5 min | FAIL |
| 2.4.1 | COMPLETED | 1 | None | 2 | 4.1 min | FAIL |
| 2.5.0 | COMPLETED | 47 | 0 | 2 | 2.9 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 14 | 0 | 2 | 17.6 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 14 | 0 | 2 | 19.7 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 2 | 15.1 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 1 | 0 | 2 | 2.6 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `8088_sbc2_pcb_8088_sbc2--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
