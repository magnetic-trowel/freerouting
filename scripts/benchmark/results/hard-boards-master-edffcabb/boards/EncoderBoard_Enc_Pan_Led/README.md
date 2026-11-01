# EncoderBoard_Enc_Pan_Led

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
| Quality score | 986.2543 |
| Wall clock | 3.0 min |
| Connections (total / still open) | 97 / 1 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **open-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 958.8 | stagnation | 0.5 min |
| continue | 3 | 0 | 958.8 | stagnation | 0.3 min |
| direct-first | 3 | 0 | 958.8 | stagnation | 0.4 min |
| fanout-retry | 2 | 0 | 972.5 | stagnation | 0.3 min |
| short-escape | 2 | 0 | 972.5 | stagnation | 0.5 min |
| open-escape | 1 | 0 | 986.3 | stagnation | 0.2 min |
| no-fanout | 3 | 0 | 958.8 | stagnation | 0.6 min |
| continue | 1 | 0 | 986.3 | stagnation | 0.1 min |

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
| hole_clearance | 0 | 1 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 6 |

Open connections reported by KiCad:

- Pad 3 [/LED14] of IC1 on B.Cu @ (129.35, 75.15) ↔ Pad 2 [/LED14] of R18 on B.Cu @ (133.25, 89.10)

## Board

- Source: https://github.com/hexeguitar/EncoderBoard (author hexeguitar, license NOASSERTION)
- 2 layers, 19 nets, 58 components, 42.0 × 34.0 mm, 2 zones
- Human reference layout: 629 segments, 31 vias, 1006.6 mm of track, 229 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/EncoderBoard_Enc_Pan_Led/`](../../../../fixtures/PCBench/EncoderBoard_Enc_Pan_Led/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 8 | None | 8 | 1.4 min | FAIL |
| 2.4.1 | COMPLETED | 4 | None | 8 | 4.1 min | FAIL |
| 2.5.0 | COMPLETED | 5 | 0 | 0 | 3.3 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 1 | 0 | 0 | 9.6 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 1 | 0 | 0 | 10.5 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 0 | 14.5 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 0 | 6.8 min | clean |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 3.0 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `EncoderBoard_Enc_Pan_Led--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
