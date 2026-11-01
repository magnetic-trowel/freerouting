# badge2015_badge2015

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
| Quality score | 938.8379 |
| Wall clock | 1.3 min |
| Connections (total / still open) | 109 / 5 |
| Autorouter stop reason | stagnation after 33 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **open-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 6 | 0 | 926.6 | stagnation | 0.2 min |
| direct-first | 6 | 0 | 926.6 | stagnation | 0.2 min |
| fanout-retry | 6 | 0 | 926.6 | stagnation | 0.2 min |
| short-escape | 6 | 0 | 926.6 | stagnation | 0.2 min |
| open-escape | 5 | 0 | 938.8 | stagnation | 0.2 min |
| no-fanout | 6 | 0 | 926.6 | stagnation | 0.1 min |

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
| clearance | 0 | 20 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 2 |

Open connections reported by KiCad:

- Pad 1 [Net-(C10-Pad1)] of C10 on F.Cu @ (96.75, 100.66) ↔ Pad 4 [Net-(C10-Pad1)] of MAG on F.Cu @ (100.50, 102.06)
- Pad 2 [/SDA] of R19 on F.Cu @ (90.51, 100.89) ↔ Pad 6 [/SDA] of MAG on F.Cu @ (102.50, 102.46)
- Pad 6 [/SDA] of MAG on F.Cu @ (102.50, 102.46) ↔ Pad 12 [/SDA] of U1 on F.Cu @ (122.05, 100.29)
- Pad 7 [/SCL] of MAG on F.Cu @ (102.50, 102.06) ↔ Track [/SCL] on B.Cu, length 25.5404 mm @ (87.91, 102.26)
- Pad 2 [Net-(C9-Pad2)] of C9 on F.Cu @ (99.70, 97.07) ↔ Pad 1 [Net-(C9-Pad2)] of MAG on F.Cu @ (100.50, 100.86)

## Board

- Source: https://github.com/cryptovillage/badge2015 (author cryptovillage, license NOASSERTION)
- 2 layers, 47 nets, 57 components, 38.1 × 0.0 mm, 3 zones
- Human reference layout: 529 segments, 67 vias, 1889.09 mm of track, 624 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/badge2015_badge2015/`](../../../../fixtures/PCBench/badge2015_badge2015/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 7 | None | 8 | 1.2 min | FAIL |
| 2.4.1 | COMPLETED | 4 | None | 0 | 2.5 min | FAIL |
| 2.5.0 | COMPLETED | 6 | 0 | 0 | 1.3 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 5 | 0 | 0 | 10.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 5 | 0 | 0 | 9.3 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 6 | 0 | 0 | 7.4 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 5 | 0 | 0 | 4.7 min | FAIL |
| master-edffcabb | COMPLETED | 5 | 0 | 0 | 1.3 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `badge2015_badge2015--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
