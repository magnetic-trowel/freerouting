# kitspace_d20_r0.1

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 3 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 986.3014 |
| Wall clock | 12.6 min |
| Connections (total / still open) | 292 / 3 |
| Autorouter stop reason | stagnation after 36 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **short-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 6 | 0 | 972.6 | stagnation | 2.6 min |
| direct-first | 4 | 0 | 981.7 | stagnation | 2.3 min |
| fanout-retry | 8 | 0 | 963.5 | stagnation | 1.9 min |
| short-escape | 3 | 0 | 986.3 | stagnation | 1.9 min |
| open-escape | 6 | 1 | 972.6 | stagnation | 1.1 min |
| no-fanout | 11 | 0 | 949.8 | stagnation | 2.2 min |
| continue | 3 | 0 | 986.3 | stagnation | 0.6 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 3 | 0 |
| clearance | 0 | 0 |
| hole_clearance | 139 | 87 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 4 | 4 |

Open connections reported by KiCad:

- Pad 49 [gnd] of U8 on F.Cu @ (161.50, 107.00) ↔ Track [gnd] on F.Cu, length 0.7505 mm @ (158.97, 111.50)
- Pad 2 [gnd] of R2 on F.Cu @ (174.30, 111.36) ↔ Track [gnd] on In1.Cu, length 10.6772 mm @ (174.75, 110.25)
- Pad 7 [ice_config_done] of U7 on F.Cu @ (153.94, 106.75) ↔ Pad 7 [ice_config_done] of U8 on F.Cu @ (161.25, 103.56)

## Board

- Source: https://github.com/gregdavill/d20-hardware (author , license [{'name': 'BSD', 'link': 'https://raw.githubusercontent.com/gregdavill/d20-hardware/main/LICENSE'}])
- 4 layers, 0 nets, 91 components, 39.25 × 31.0 mm, 254 zones
- Human reference layout: 1069 segments, 209 vias, 920.87 mm of track, 242 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_d20_r0.1/`](../../../../fixtures/PCBench/kitspace_d20_r0.1/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 8 | None | 10 | 5.1 min | FAIL |
| 2.4.1 | COMPLETED | 66 | None | 14 | 5.1 min | FAIL |
| 2.5.0 | TIMED_OUT | 30 | 0 | 0 | 10.2 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 18 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 18 | 0 | 0 | 52.5 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 2 | 0 | 0 | 182.0 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 3 | 0 | 0 | 12.6 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_d20_r0.1--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
