# wavegen_wavegen

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
| Quality score | 990.54376 |
| Wall clock | 1.7 min |
| Connections (total / still open) | 141 / 1 |
| Autorouter stop reason | stagnation after 20 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **fanout-retry**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 4 | 0 | 962.2 | stagnation | 0.3 min |
| continue | 4 | 0 | 962.2 | stagnation | 0.2 min |
| direct-first | 3 | 0 | 971.6 | stagnation | 0.2 min |
| fanout-retry | 1 | 0 | 990.5 | stagnation | 0.1 min |
| short-escape | 1 | 0 | 990.5 | stagnation | 0.3 min |
| open-escape | 3 | 0 | 971.6 | stagnation | 0.2 min |
| no-fanout | 4 | 0 | 962.2 | stagnation | 0.2 min |
| continue | 1 | 0 | 990.5 | stagnation | 0.1 min |

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
| copper_edge_clearance | 0 | 1 |

Open connections reported by KiCad:

- Track [+3V3] on B.Cu, length 4.4382 mm @ (73.41, 74.45) ↔ Pad 2 [+3V3] of U3 on F.Cu @ (74.05, 76.81)

## Board

- Source: https://github.com/Fifty-Nine/wavegen (author Fifty-Nine, license NOASSERTION)
- 2 layers, 33 nets, 53 components, 50.8 × 50.8 mm, 2 zones
- Human reference layout: 447 segments, 53 vias, 1008.12 mm of track, 281 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/wavegen_wavegen/`](../../../../fixtures/PCBench/wavegen_wavegen/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 4 | None | 2 | 0.8 min | FAIL |
| 2.4.1 | COMPLETED | 4 | None | 1 | 3.6 min | FAIL |
| 2.5.0 | COMPLETED | 9 | 0 | 0 | 1.8 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 6 | 0 | 0 | 8.7 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 6 | 0 | 0 | 7.5 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 0 | 4.4 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 0 | 4.5 min | clean |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 1.7 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `wavegen_wavegen--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
