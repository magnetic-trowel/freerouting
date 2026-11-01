# hardware-designs_spirit1-board

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 1 |
| Clearance violations (total) | 1 |
| of which router-introduced | 0 |
| of which pre-existing in input | 1 (unfixable: 1) |
| Worst violation depth | 23.102465820312503 µm |
| Quality score | 974.6634 |
| Wall clock | 0.5 min |
| Connections (total / still open) | 53 / 1 |
| Autorouter stop reason | stagnation after 26 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **open-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 924.3 | stagnation | 0.1 min |
| continue | 2 | 0 | 949.5 | stagnation | 0.1 min |
| direct-first | 2 | 0 | 949.5 | stagnation | 0.0 min |
| fanout-retry | 2 | 0 | 949.5 | stagnation | 0.1 min |
| short-escape | 3 | 0 | 924.3 | stagnation | 0.1 min |
| open-escape | 1 | 0 | 974.7 | stagnation | 0.0 min |
| no-fanout | 2 | 0 | 949.5 | stagnation | 0.0 min |
| continue | 1 | 0 | 974.7 | stagnation | 0.0 min |

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
| clearance | 0 | 1 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 5 | 16 |

Open connections reported by KiCad:

- Pad B1 [/50ohm] of U2 on F.Cu @ (100.94, 109.88) ↔ Track [/50ohm] on F.Cu, length 2.5760 mm @ (110.00, 108.26)

## Board

- Source: https://github.com/snowcap-electronics/hardware-designs (author snowcap-electronics, license None)
- 2 layers, 0 nets, 23 components, 27.0 × 24.0 mm, 2 zones
- Human reference layout: 229 segments, 39 vias, 358.63 mm of track, 133 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/hardware-designs_spirit1-board/`](../../../../fixtures/PCBench/hardware-designs_spirit1-board/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 4 | None | 27 | 0.3 min | FAIL |
| 2.4.1 | COMPLETED | 2 | None | 6 | 0.8 min | FAIL |
| 2.5.0 | COMPLETED | 2 | 0 | 1 | 0.4 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 1 | 0 | 1 | 1.0 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 1 | 0 | 1 | 1.1 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 1 | 2.4 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 1 | 1.2 min | FAIL |
| master-edffcabb | COMPLETED | 1 | 0 | 1 | 0.5 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `hardware-designs_spirit1-board--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
