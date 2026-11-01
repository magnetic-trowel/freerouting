# Hardware_Playground_rfm69w_adapter

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 2 |
| Clearance violations (total) | 32 |
| of which router-introduced | 0 |
| of which pre-existing in input | 32 (unfixable: 32) |
| Worst violation depth | 0.0 µm |
| Quality score | 973.92365 |
| Wall clock | 1.0 min |
| Connections (total / still open) | 114 / 2 |
| Autorouter stop reason | stagnation after 28 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **direct-first**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 962.2 | stagnation | 0.2 min |
| continue | 3 | 0 | 962.2 | stagnation | 0.1 min |
| direct-first | 2 | 0 | 973.9 | stagnation | 0.2 min |
| fanout-retry | 75 | 0 | 225.4 | repeated | 0.0 min |
| short-escape | 4 | 0 | 950.5 | stagnation | 0.1 min |
| open-escape | 2 | 0 | 973.9 | stagnation | 0.1 min |
| no-fanout | 2 | 0 | 973.9 | stagnation | 0.1 min |
| continue | 2 | 0 | 973.9 | stagnation | 0.1 min |

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
| clearance | 20 | 0 |
| hole_clearance | 4 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 51 | 40 |

Open connections reported by KiCad:

- Pad 22 [/SD_SO] of U1 on F.Cu @ (123.40, 71.50) ↔ Pad 2 [/SD_SO] of U2 on F.Cu @ (130.38, 70.05)
- Track [Net-(C4-Pad1)] on F.Cu, length 0.7843 mm @ (124.64, 67.04) ↔ Pad 28 [Net-(C4-Pad1)] of U1 on F.Cu @ (121.25, 69.85)

## Board

- Source: https://github.com/mtiutiu/Hardware_Playground (author mtiutiu, license None)
- 2 layers, 8 nets, 87 components, 20.0 × 16.0 mm, 4 zones
- Human reference layout: 304 segments, 30 vias, 328.3 mm of track, 305 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/Hardware_Playground_rfm69w_adapter/`](../../../../fixtures/PCBench/Hardware_Playground_rfm69w_adapter/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 4 | None | 38 | 0.7 min | FAIL |
| 2.4.1 | COMPLETED | 16 | None | 39 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 1 | 0 | 0 | 0.6 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 0 | 0 | 0 | 1.5 min | clean |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 0 | 1.2 min | clean |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 32 | 3.1 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 32 | 0.4 min | clean |
| master-edffcabb | COMPLETED | 2 | 0 | 32 | 1.0 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `Hardware_Playground_rfm69w_adapter--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
