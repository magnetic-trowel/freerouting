# Helium_helium_1-6

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 1 |
| Clearance violations (total) | 5 |
| of which router-introduced | 0 |
| of which pre-existing in input | 5 (unfixable: 5) |
| Worst violation depth | 200.0 µm |
| Quality score | 994.45483 |
| Wall clock | 0.7 min |
| Connections (total / still open) | 259 / 1 |
| Autorouter stop reason | completed after 8 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **direct-first**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 4 | 0 | 979.0 | stagnation | 0.3 min |
| continue | 2 | 0 | 989.3 | stagnation | 0.2 min |
| direct-first | 1 | 0 | 994.5 | completed | 0.1 min |

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
| clearance | 2 | 0 |
| hole_clearance | 0 | 0 |
| shorting_items | 5 | 5 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Pad 1 [GNDA] of XXX1 on B.Cu @ (260.59, 97.28) ↔ Track [GNDA] on F.Cu, length 0.2295 mm @ (260.61, 94.29)

## Board

- Source: https://github.com/blueshiftPDX/Helium (author blueshiftPDX, license GPL-3.0)
- 2 layers, 0 nets, 112 components, 86.99 × 65.02 mm, 11 zones
- Human reference layout: 1170 segments, 153 vias, 2086.1 mm of track, 412 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/Helium_helium_1-6/`](../../../../fixtures/PCBench/Helium_helium_1-6/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 2 | None | 17 | 0.9 min | FAIL |
| 2.4.1 | COMPLETED | 4 | None | 15 | 5.4 min | FAIL |
| 2.5.0 | COMPLETED | 12 | 0 | 7 | 2.8 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 12 | 0 | 7 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 12 | 0 | 7 | 11.6 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 5 | 9.2 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 5 | 4.1 min | FAIL |
| master-edffcabb | COMPLETED | 1 | 0 | 5 | 0.7 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `Helium_helium_1-6--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
