# kitspace_hack

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 1 |
| Clearance violations (total) | 58 |
| of which router-introduced | 0 |
| of which pre-existing in input | 58 (unfixable: 58) |
| Worst violation depth | 0.0 µm |
| Quality score | 985.25134 |
| Wall clock | 1.4 min |
| Connections (total / still open) | 126 / 1 |
| Autorouter stop reason | stagnation after 29 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 1 | 0 | 985.3 | stagnation | 0.2 min |
| continue | 1 | 0 | 985.3 | stagnation | 0.0 min |
| direct-first | 3 | 0 | 964.1 | stagnation | 0.2 min |
| fanout-retry | 5 | 0 | 942.9 | stagnation | 0.2 min |
| short-escape | 5 | 0 | 942.9 | stagnation | 0.3 min |
| open-escape | 2 | 0 | 974.7 | stagnation | 0.2 min |
| no-fanout | 5 | 0 | 942.9 | stagnation | 0.3 min |

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
| clearance | 0 | 7 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 77 | 95 |

Open connections reported by KiCad:

- Pad 5 [USB-D-] of U2 on F.Cu @ (162.79, 85.60) ↔ Pad 23 [USB-D-] of U3 on F.Cu @ (168.31, 86.94)

## Board

- Source: https://github.com/kitspace-forks/HACK (author , license [{'name': 'CERN', 'link': 'https://raw.githubusercontent.com/kitspace-forks/HACK/master/LICENCE.txt'}])
- 2 layers, 13 nets, 32 components, 36.83 × 19.05 mm, 10 zones
- Human reference layout: 401 segments, 33 vias, 545.23 mm of track, 288 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_hack/`](../../../../fixtures/PCBench/kitspace_hack/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 32 | None | 124 | 5.3 min | FAIL |
| 2.4.1 | COMPLETED | 42 | None | 126 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 12 | 0 | 0 | 1.2 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 1 | 0 | 0 | 5.5 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 1 | 0 | 0 | 4.7 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 58 | 3.8 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 58 | 3.4 min | clean |
| master-edffcabb | COMPLETED | 1 | 0 | 58 | 1.4 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_hack--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
