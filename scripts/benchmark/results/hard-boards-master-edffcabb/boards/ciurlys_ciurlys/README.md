# ciurlys_ciurlys

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 5 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 803.9216 |
| Wall clock | 0.5 min |
| Connections (total / still open) | 34 / 5 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **short-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 7 | 0 | 725.5 | stagnation | 0.1 min |
| direct-first | 9 | 0 | 647.1 | stagnation | 0.1 min |
| fanout-retry | 6 | 0 | 764.7 | stagnation | 0.1 min |
| short-escape | 5 | 0 | 803.9 | stagnation | 0.0 min |
| open-escape | 7 | 0 | 725.5 | stagnation | 0.1 min |
| no-fanout | 7 | 0 | 725.5 | stagnation | 0.1 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 5 | 0 |
| clearance | 0 | 1 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 9 | 12 |

Open connections reported by KiCad:

- Track [Net-(C1-Pad1)] on F.Cu, length 0.8450 mm @ (122.29, 101.11) ↔ Track [Net-(C1-Pad1)] on F.Cu, length 1.2500 mm @ (117.10, 102.70)
- Track [Net-(C2-Pad1)] on F.Cu, length 1.4238 mm @ (124.35, 103.59) ↔ Pad 4 [Net-(C2-Pad1)] of IC1 on F.Cu @ (115.37, 104.90)
- Track [VCC] on F.Cu, length 1.7912 mm @ (118.48, 103.23) ↔ Track [VCC] on F.Cu, length 1.4000 mm @ (127.80, 102.90)
- Track [Net-(IC1-Pad1)] on F.Cu, length 4.1470 mm @ (122.29, 104.50) ↔ Pad 1 [Net-(IC1-Pad1)] of IC1 on F.Cu @ (112.43, 103.00)
- Track [Net-(IC1-Pad3)] on F.Cu, length 1.4455 mm @ (112.43, 104.90) ↔ Pad 3 [Net-(IC1-Pad3)] of P2 on F.Cu @ (119.75, 104.50)

## Board

- Source: https://github.com/Miceuz/ciurlys (author Miceuz, license None)
- 2 layers, 9 nets, 17 components, 41.0 × 12.0 mm, 1 zones
- Human reference layout: 116 segments, 9 vias, 260.24 mm of track, 124 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/ciurlys_ciurlys/`](../../../../fixtures/PCBench/ciurlys_ciurlys/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 11 | None | 2 | 0.4 min | FAIL |
| 2.4.1 | COMPLETED | 7 | None | 4 | 1.1 min | FAIL |
| 2.5.0 | COMPLETED | 6 | 0 | 0 | 0.6 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 5 | 0 | 0 | 1.7 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 5 | 0 | 0 | 1.6 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 6 | 0 | 0 | 1.4 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 5 | 0 | 0 | 1.2 min | FAIL |
| master-edffcabb | COMPLETED | 5 | 0 | 0 | 0.5 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `ciurlys_ciurlys--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
