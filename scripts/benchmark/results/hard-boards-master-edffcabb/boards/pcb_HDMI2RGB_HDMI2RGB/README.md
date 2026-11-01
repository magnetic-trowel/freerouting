# pcb_HDMI2RGB_HDMI2RGB

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
| Quality score | 996.4061 |
| Wall clock | 7.0 min |
| Connections (total / still open) | 371 / 1 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **default**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 1 | 0 | 996.4 | stagnation | 0.9 min |
| continue | 1 | 0 | 996.4 | stagnation | 0.1 min |
| direct-first | 3 | 0 | 989.2 | stagnation | 0.8 min |
| fanout-retry | 1 | 0 | 996.4 | stagnation | 1.1 min |
| short-escape | 3 | 0 | 989.2 | stagnation | 1.0 min |
| open-escape | 1 | 0 | 996.4 | stagnation | 0.9 min |
| no-fanout | 3 | 0 | 989.2 | stagnation | 2.1 min |

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
| clearance | 0 | 13 |
| hole_clearance | 1 | 9 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 10 | 14 |

Open connections reported by KiCad:

- Track [GND] on F.Cu, length 0.7700 mm @ (59.08, 117.21) ↔ Track [GND] on B.Cu, length 1.5000 mm @ (59.08, 117.55)

## Board

- Source: https://github.com/seniorDesignMAY1607/pcb_HDMI2RGB (author seniorDesignMAY1607, license GPL-3.0)
- 4 layers, 51 nets, 115 components, 58.42 × 63.5 mm, 5 zones
- Human reference layout: 1466 segments, 110 vias, 3032.68 mm of track, 639 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/pcb_HDMI2RGB_HDMI2RGB/`](../../../../fixtures/PCBench/pcb_HDMI2RGB_HDMI2RGB/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 7 | None | 140 | 6.1 min | FAIL |
| 2.4.1 | TIMED_OUT | 59 | None | 150 | 5.1 min | FAIL |
| 2.5.0 | TIMED_OUT | 30 | 0 | 0 | 10.3 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 3 | 0 | 0 | 10.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 2 | 0 | 0 | 29.9 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 0 | 54.7 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 0 | 14.6 min | clean |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 7.0 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `pcb_HDMI2RGB_HDMI2RGB--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
