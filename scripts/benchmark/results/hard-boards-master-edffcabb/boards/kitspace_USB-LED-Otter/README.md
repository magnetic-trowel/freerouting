# kitspace_USB-LED-Otter

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
| Quality score | 965.81195 |
| Wall clock | 0.5 min |
| Connections (total / still open) | 39 / 1 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **no-fanout**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 2 | 0 | 931.6 | stagnation | 0.1 min |
| continue | 2 | 0 | 931.6 | stagnation | 0.0 min |
| direct-first | 2 | 0 | 931.6 | stagnation | 0.0 min |
| fanout-retry | 3 | 0 | 897.4 | stagnation | 0.1 min |
| short-escape | 3 | 0 | 897.4 | stagnation | 0.1 min |
| open-escape | 3 | 0 | 897.4 | stagnation | 0.1 min |
| no-fanout | 1 | 0 | 965.8 | stagnation | 0.0 min |
| continue | 1 | 0 | 965.8 | stagnation | 0.0 min |

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
| copper_edge_clearance | 12 | 21 |

Open connections reported by KiCad:

- Track [+5V] on B.Cu, length 0.7360 mm @ (55.92, 62.23) ↔ Pad 1 [+5V] of C1 on B.Cu @ (54.88, 64.33)

## Board

- Source: https://github.com/Jana-Marie/USB-LED-Otter (author , license [{'name': 'CERN', 'link': 'https://raw.githubusercontent.com/Jana-Marie/USB-LED-Otter/master/LICENSE.md'}])
- 2 layers, 28 nets, 14 components, 12.0 × 15.0 mm, 2 zones
- Human reference layout: 168 segments, 17 vias, 152.37 mm of track, 105 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_USB-LED-Otter/`](../../../../fixtures/PCBench/kitspace_USB-LED-Otter/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 1 | None | 2 | 0.2 min | FAIL |
| 2.4.1 | COMPLETED | 5 | None | 11 | 1.1 min | FAIL |
| 2.5.0 | COMPLETED | 2 | 0 | 0 | 0.4 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 1 | 0 | 0 | 1.0 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 1 | 0 | 0 | 1.0 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 0 | 1.8 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 0 | 0.8 min | FAIL |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 0.5 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_USB-LED-Otter--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
