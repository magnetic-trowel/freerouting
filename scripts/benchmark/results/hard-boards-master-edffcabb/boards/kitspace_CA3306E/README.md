# kitspace_CA3306E

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 1 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 970.37036 |
| Wall clock | 0.3 min |
| Connections (total / still open) | 45 / 1 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 1 | 0 | 970.4 | stagnation | 0.1 min |
| continue | 1 | 0 | 970.4 | stagnation | 0.0 min |
| direct-first | 1 | 0 | 970.4 | stagnation | 0.0 min |
| fanout-retry | 38 | 0 | 103.7 | repeated | 0.0 min |
| short-escape | 1 | 0 | 970.4 | stagnation | 0.0 min |
| open-escape | 1 | 0 | 970.4 | stagnation | 0.0 min |
| no-fanout | 1 | 0 | 970.4 | stagnation | 0.0 min |

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
| clearance | 0 | 2 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 3 | 0 |

Open connections reported by KiCad:

- PTH pad 7 [GPIO25] of TXB0108 @ (110.96, 112.40) ↔ PTH pad 22 [GPIO25] of XS1 @ (133.77, 102.23)

## Board

- Source: https://github.com/kelu124/bomanz (author , license [{'name': 'License not found', 'link': ''}])
- 2 layers, 0 nets, 10 components, 65.0 × 30.0 mm, 2 zones
- Human reference layout: 206 segments, 7 vias, 756.81 mm of track, 151 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_CA3306E/`](../../../../fixtures/PCBench/kitspace_CA3306E/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 1 | None | 0 | 0.3 min | FAIL |
| 2.4.1 | COMPLETED | 9 | None | 0 | 0.9 min | FAIL |
| 2.5.0 | COMPLETED | 1 | 0 | 0 | 0.5 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 1 | 0 | 0 | 1.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 1 | 0 | 0 | 1.1 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 0 | 1.4 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 0 | 0.8 min | FAIL |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 0.3 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_CA3306E--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
