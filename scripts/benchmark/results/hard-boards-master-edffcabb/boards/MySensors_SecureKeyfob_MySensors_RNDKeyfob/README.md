# MySensors_SecureKeyfob_MySensors_RNDKeyfob

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
| Quality score | 981.22064 |
| Wall clock | 0.9 min |
| Connections (total / still open) | 71 / 1 |
| Autorouter stop reason | stagnation after 23 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 1 | 0 | 981.2 | stagnation | 0.2 min |
| continue | 1 | 0 | 981.2 | stagnation | 0.0 min |
| direct-first | 2 | 0 | 962.4 | stagnation | 0.1 min |
| fanout-retry | 1 | 0 | 981.2 | stagnation | 0.1 min |
| short-escape | 1 | 0 | 981.2 | stagnation | 0.1 min |
| open-escape | 1 | 0 | 981.2 | stagnation | 0.2 min |
| no-fanout | 2 | 0 | 962.4 | stagnation | 0.2 min |

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
| clearance | 62 | 41 |
| hole_clearance | 0 | 0 |
| shorting_items | 36 | 31 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 25 | 20 |

Open connections reported by KiCad:

- Track [GND] on B.Cu, length 0.3805 mm @ (189.85, 74.02) ↔ Pad 5 [GND] of U3 on B.Cu @ (190.85, 74.40)

## Board

- Source: https://github.com/kvoit/MySensors_SecureKeyfob (author kvoit, license GPL-3.0)
- 2 layers, 4 nets, 26 components, 27.0 × 42.9 mm, 3 zones
- Human reference layout: 436 segments, 44 vias, 672.7 mm of track, 498 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/MySensors_SecureKeyfob_MySensors_RNDKeyfob/`](../../../../fixtures/PCBench/MySensors_SecureKeyfob_MySensors_RNDKeyfob/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 4 | None | 10 | 0.6 min | FAIL |
| 2.4.1 | COMPLETED | 2 | None | 18 | 1.4 min | FAIL |
| 2.5.0 | COMPLETED | 1 | 0 | 0 | 1.4 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 1 | 0 | 0 | 5.0 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 1 | 0 | 0 | 5.7 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 0 | 3.6 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 1 | 0 | 0 | 1.8 min | FAIL |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 0.9 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `MySensors_SecureKeyfob_MySensors_RNDKeyfob--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
