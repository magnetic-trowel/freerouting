# nan-15_pcb

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
| Quality score | 973.06396 |
| Wall clock | 2.9 min |
| Connections (total / still open) | 99 / 2 |
| Autorouter stop reason | stagnation after 30 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **no-fanout**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 5 | 0 | 932.7 | stagnation | 0.5 min |
| direct-first | 5 | 0 | 932.7 | stagnation | 0.5 min |
| fanout-retry | 4 | 0 | 946.1 | stagnation | 0.3 min |
| short-escape | 3 | 0 | 959.6 | stagnation | 0.6 min |
| open-escape | 4 | 0 | 946.1 | stagnation | 0.4 min |
| no-fanout | 2 | 0 | 973.1 | stagnation | 0.6 min |
| continue | 2 | 0 | 973.1 | stagnation | 0.1 min |

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
| clearance | 0 | 6 |
| hole_clearance | 0 | 9 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 2 |
| copper_edge_clearance | 15 | 1 |

Open connections reported by KiCad:

- Track [GND] on F.Cu, length 0.9020 mm @ (147.44, 104.62) ↔ Pad 33 [GND] of IC1 on F.Cu @ (148.80, 102.39)
- Pad 2 [Net-(C5-Pad1)] of IC1 on F.Cu @ (150.90, 105.05) ↔ Pad 1 [Net-(C5-Pad1)] of C5 on F.Cu @ (169.07, 113.42)

## Board

- Source: https://github.com/trebb/nan-15 (author trebb, license NOASSERTION)
- 2 layers, 24 nets, 51 components, 68.58 × 71.12 mm, 2 zones
- Human reference layout: 690 segments, 48 vias, 2270.64 mm of track, 338 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/nan-15_pcb/`](../../../../fixtures/PCBench/nan-15_pcb/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 5 | None | 6 | 2.1 min | FAIL |
| 2.4.1 | COMPLETED | 7 | None | 6 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 11 | 0 | 0 | 3.9 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 12 | 0 | 0 | 10.3 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 10 | 0 | 0 | 20.5 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 3 | 0 | 0 | 24.5 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 2 | 0 | 0 | 9.6 min | FAIL |
| master-edffcabb | COMPLETED | 2 | 0 | 0 | 2.9 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `nan-15_pcb--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
