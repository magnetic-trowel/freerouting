# icehat_icehat

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
| Quality score | 980.0995 |
| Wall clock | 2.6 min |
| Connections (total / still open) | 134 / 2 |
| Autorouter stop reason | stagnation after 20 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 2 | 0 | 980.1 | stagnation | 0.5 min |
| continue | 2 | 0 | 980.1 | stagnation | 0.2 min |
| direct-first | 2 | 0 | 980.1 | stagnation | 0.3 min |
| fanout-retry | 2 | 0 | 980.1 | stagnation | 0.5 min |
| short-escape | 2 | 0 | 980.1 | stagnation | 0.4 min |
| open-escape | 3 | 0 | 970.1 | stagnation | 0.3 min |
| no-fanout | 3 | 0 | 970.1 | stagnation | 0.4 min |

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
| clearance | 0 | 0 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 3 | 0 |

Open connections reported by KiCad:

- Pad 49 [GND] of U401 on F.Cu @ (133.47, 96.13) ↔ Track [GND] on In1.Cu, length 1.4678 mm @ (131.28, 94.40)
- PTH pad 6 [Net-(P403-Pad6)] of P403 @ (127.00, 105.98) ↔ Pad 23 [Net-(P403-Pad6)] of U401 on F.Cu @ (131.85, 96.45)

## Board

- Source: https://github.com/emeb/icehat (author emeb, license MIT)
- 4 layers, 59 nets, 40 components, 65.0 × 30.0 mm, 2 zones
- Human reference layout: 519 segments, 41 vias, 1251.21 mm of track, 165 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/icehat_icehat/`](../../../../fixtures/PCBench/icehat_icehat/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 2 | None | 42 | 1.6 min | FAIL |
| 2.4.1 | TIMED_OUT | 11 | None | 42 | 5.2 min | FAIL |
| 2.5.0 | COMPLETED | 21 | 0 | 0 | 3.8 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 4 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 7 | 0 | 0 | 11.7 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 0 | 10.7 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 0 | 12.8 min | FAIL |
| master-edffcabb | COMPLETED | 2 | 0 | 0 | 2.6 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `icehat_icehat--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
