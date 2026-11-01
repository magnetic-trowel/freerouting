# memsarray_mems_modules

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 1 |
| Clearance violations (total) | 8 |
| of which router-introduced | 0 |
| of which pre-existing in input | 8 (unfixable: 8) |
| Worst violation depth | 0.0 µm |
| Quality score | 859.5238 |
| Wall clock | 0.2 min |
| Connections (total / still open) | 10 / 1 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **direct-first**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 592.9 | stagnation | 0.0 min |
| continue | 3 | 0 | 592.9 | stagnation | 0.0 min |
| direct-first | 1 | 0 | 859.5 | stagnation | 0.0 min |
| fanout-retry | 5 | 0 | 326.2 | repeated | 0.0 min |
| short-escape | 2 | 0 | 726.2 | stagnation | 0.0 min |
| open-escape | 3 | 0 | 592.9 | stagnation | 0.0 min |
| no-fanout | 3 | 0 | 592.9 | stagnation | 0.0 min |
| continue | 1 | 0 | 859.5 | stagnation | 0.0 min |

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
| clearance | 0 | 0 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 14 | 16 |

Open connections reported by KiCad:

- Pad 4 [/OUT] of MIC1 on F.Cu @ (35.73, 34.21) ↔ PTH pad 1 [/OUT] of P4 @ (37.08, 39.12)

## Board

- Source: https://github.com/lfochamon/memsarray (author lfochamon, license None)
- 2 layers, 0 nets, 6 components, 10.7 × 11.49 mm, 2 zones
- Human reference layout: 32 segments, 2 vias, 44.81 mm of track, 37 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/memsarray_mems_modules/`](../../../../fixtures/PCBench/memsarray_mems_modules/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 3 | None | 8 | 0.1 min | FAIL |
| 2.4.1 | COMPLETED | 5 | None | 8 | 0.2 min | FAIL |
| 2.5.0 | COMPLETED | 3 | 0 | 0 | 0.1 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 3 | 0 | 0 | 0.4 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 3 | 0 | 0 | 0.2 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 2 | 0 | 8 | 0.3 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 8 | 0.3 min | FAIL |
| master-edffcabb | COMPLETED | 1 | 0 | 8 | 0.2 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `memsarray_mems_modules--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
