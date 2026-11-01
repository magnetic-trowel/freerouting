# retroreflectors_RUMBAFLOCK

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 3 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 692.3077 |
| Wall clock | 0.1 min |
| Connections (total / still open) | 13 / 3 |
| Autorouter stop reason | stagnation after 22 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 692.3 | stagnation | 0.0 min |
| continue | 3 | 0 | 692.3 | stagnation | 0.0 min |
| direct-first | 3 | 0 | 692.3 | stagnation | 0.0 min |
| fanout-retry | 3 | 0 | 692.3 | stagnation | 0.0 min |
| short-escape | 4 | 0 | 589.7 | repeated | 0.0 min |
| open-escape | 3 | 0 | 692.3 | stagnation | 0.0 min |
| no-fanout | 3 | 0 | 692.3 | stagnation | 0.0 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 3 | 0 |
| clearance | 4 | 7 |
| hole_clearance | 0 | 0 |
| shorting_items | 2 | 3 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 10 | 10 |

Open connections reported by KiCad:

- Track [GND] on B.Cu, length 1.0414 mm @ (81.81, 50.90) ↔ Pad 1 [GND] of R4 on B.Cu @ (80.75, 53.85)
- PTH pad 2 [N-000003] of P1 @ (80.64, 55.12) ↔ Pad 1 [N-000003] of R2 on F.Cu @ (81.43, 53.98)
- Pad 2 [N-000004] of R3 on F.Cu @ (81.13, 51.94) ↔ Pad 2 [N-000004] of D1 on F.Cu @ (82.04, 52.96)

## Board

- Source: https://github.com/mossmann/retroreflectors (author , license [{'name': 'GNU General Public License v2.0', 'link': 'https://api.github.com/licenses/gpl-2.0'}])
- 2 layers, 0 nets, 7 components, 6.35 × 6.35 mm, 0 zones
- Human reference layout: 21 segments, 0 vias, 13.63 mm of track, 72 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/retroreflectors_RUMBAFLOCK/`](../../../../fixtures/PCBench/retroreflectors_RUMBAFLOCK/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 5 | None | 4 | 0.1 min | FAIL |
| 2.4.1 | COMPLETED | 7 | None | 12 | 0.1 min | FAIL |
| 2.5.0 | COMPLETED | 5 | 0 | 4 | 0.1 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 3 | 0 | 0 | 0.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 3 | 0 | 0 | 0.1 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 3 | 0 | 0 | 0.4 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 3 | 0 | 0 | 0.2 min | FAIL |
| master-edffcabb | COMPLETED | 3 | 0 | 0 | 0.1 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `retroreflectors_RUMBAFLOCK--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
