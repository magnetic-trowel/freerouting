# Hardware_Playground_samd51_micropython

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 4 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 934.95935 |
| Wall clock | 2.6 min |
| Connections (total / still open) | 82 / 4 |
| Autorouter stop reason | stagnation after 25 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **fanout-retry**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 7 | 0 | 886.2 | stagnation | 0.5 min |
| direct-first | 5 | 0 | 918.7 | stagnation | 0.3 min |
| fanout-retry | 4 | 0 | 935.0 | stagnation | 0.4 min |
| short-escape | 5 | 0 | 918.7 | stagnation | 0.5 min |
| open-escape | 6 | 0 | 902.4 | stagnation | 0.3 min |
| no-fanout | 6 | 0 | 902.4 | stagnation | 0.3 min |
| continue | 4 | 0 | 935.0 | stagnation | 0.2 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 4 | 0 |
| clearance | 0 | 13 |
| hole_clearance | 2 | 2 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 28 | 28 |

Open connections reported by KiCad:

- Pad 2 [+3V3] of R3 on F.Cu @ (37.17, 29.46) ↔ Track [+3V3] on F.Cu, length 1.0514 mm @ (35.26, 26.65)
- PTH pad 7 [A2] of JP2 @ (31.75, 33.78) ↔ Pad 7 [A2] of U1 on F.Cu @ (32.01, 29.41)
- Pad 16 [QSPI_DAT3] of U1 on F.Cu @ (35.26, 27.15) ↔ Pad 7 [QSPI_DAT3] of U2 on F.Cu @ (40.46, 23.27)
- Pad 20 [QSPI_CS] of U1 on F.Cu @ (35.26, 25.15) ↔ Track [QSPI_CS] on F.Cu, length 1.0240 mm @ (38.02, 29.46)

## Board

- Source: https://github.com/mtiutiu/Hardware_Playground (author mtiutiu, license None)
- 2 layers, 0 nets, 22 components, 35.56 × 18.8 mm, 2 zones
- Human reference layout: 279 segments, 59 vias, 490.45 mm of track, 340 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/Hardware_Playground_samd51_micropython/`](../../../../fixtures/PCBench/Hardware_Playground_samd51_micropython/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 14 | None | 0 | 1.2 min | FAIL |
| 2.4.1 | COMPLETED | 10 | None | 64 | 1.1 min | FAIL |
| 2.5.0 | COMPLETED | 9 | 0 | 0 | 1.9 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 5 | 0 | 0 | 8.7 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 5 | 0 | 0 | 7.4 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 3 | 0 | 0 | 17.3 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 2 | 0 | 0 | 8.1 min | FAIL |
| master-edffcabb | COMPLETED | 4 | 0 | 0 | 2.6 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `Hardware_Playground_samd51_micropython--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
