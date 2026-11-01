# kitspace_USBI2C01

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 2 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 963.47034 |
| Wall clock | 0.7 min |
| Connections (total / still open) | 73 / 2 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **short-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 945.2 | stagnation | 0.2 min |
| continue | 3 | 0 | 945.2 | stagnation | 0.1 min |
| direct-first | 3 | 0 | 945.2 | stagnation | 0.1 min |
| fanout-retry | 4 | 0 | 926.9 | stagnation | 0.1 min |
| short-escape | 2 | 0 | 963.5 | stagnation | 0.1 min |
| open-escape | 3 | 0 | 945.2 | stagnation | 0.1 min |
| no-fanout | 4 | 0 | 926.9 | stagnation | 0.1 min |
| continue | 2 | 0 | 963.5 | stagnation | 0.0 min |

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
| clearance | 2 | 27 |
| hole_clearance | 6 | 7 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Pad P$2 [GND] of X1 on B.Cu @ (152.43, 107.61) ↔ Pad P$25 [GND] of X1 on B.Cu @ (151.68, 105.64)
- Pad P$2 [GND] of X1 on B.Cu @ (152.43, 107.61) ↔ Track [GND] on B.Cu, length 2.0683 mm @ (151.84, 110.12)

## Board

- Source: https://github.com/mlab-modules/USBI2C01 (author , license [{'name': 'GNU', 'link': 'https://raw.githubusercontent.com/mlab-modules/USBI2C01/USBI2C01A/LICENSE'}])
- 2 layers, 26 nets, 26 components, 29.97 × 40.13 mm, 2 zones
- Human reference layout: 193 segments, 11 vias, 369.5 mm of track, 142 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_USBI2C01/`](../../../../fixtures/PCBench/kitspace_USBI2C01/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 4 | None | 4 | 0.4 min | FAIL |
| 2.4.1 | COMPLETED | 1 | None | 2 | 0.9 min | FAIL |
| 2.5.0 | COMPLETED | 6 | 0 | 0 | 0.5 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 2 | 0 | 0 | 1.4 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 2 | 0 | 0 | 1.4 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 2 | 0 | 0 | 1.7 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 2 | 0 | 0 | 1.5 min | FAIL |
| master-edffcabb | COMPLETED | 2 | 0 | 0 | 0.7 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_USBI2C01--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
