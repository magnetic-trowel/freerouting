# HACK_hack

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
| Wall clock | 1.6 min |
| Connections (total / still open) | 126 / 1 |
| Autorouter stop reason | stagnation after 23 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **continue**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 964.1 | stagnation | 0.2 min |
| continue | 3 | 0 | 964.1 | stagnation | 0.1 min |
| direct-first | 3 | 0 | 964.1 | stagnation | 0.2 min |
| fanout-retry | 2 | 0 | 974.7 | stagnation | 0.1 min |
| short-escape | 4 | 0 | 953.5 | stagnation | 0.2 min |
| open-escape | 4 | 0 | 953.5 | stagnation | 0.2 min |
| no-fanout | 2 | 0 | 974.7 | stagnation | 0.3 min |
| continue | 1 | 0 | 985.3 | stagnation | 0.1 min |

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
| clearance | 0 | 8 |
| hole_clearance | 1 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 78 | 95 |

Open connections reported by KiCad:

- Pad 5 [USB-D-] of U2 on F.Cu @ (162.79, 85.60) ↔ Pad 23 [USB-D-] of U3 on F.Cu @ (168.31, 86.94)

## Board

- Source: https://github.com/MickMad/HACK (author MickMad, license NOASSERTION)
- 2 layers, 13 nets, 32 components, 36.83 × 19.05 mm, 10 zones
- Human reference layout: 401 segments, 33 vias, 545.23 mm of track, 288 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/HACK_hack/`](../../../../fixtures/PCBench/HACK_hack/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 6 | None | 124 | 1.0 min | FAIL |
| 2.4.1 | COMPLETED | 50 | None | 124 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 6 | 0 | 0 | 0.8 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 2 | 0 | 0 | 3.0 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 2 | 0 | 0 | 4.2 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 58 | 4.7 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 58 | 3.4 min | FAIL |
| master-edffcabb | COMPLETED | 1 | 0 | 58 | 1.6 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `HACK_hack--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
