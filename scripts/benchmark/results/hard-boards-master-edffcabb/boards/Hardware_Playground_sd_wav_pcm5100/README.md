# Hardware_Playground_sd_wav_pcm5100

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 2 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 993.266 |
| Wall clock | 9.2 min |
| Connections (total / still open) | 396 / 2 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **continue**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 5 | 0 | 983.2 | stagnation | 1.7 min |
| direct-first | 9 | 0 | 969.7 | stagnation | 1.7 min |
| fanout-retry | 4 | 0 | 986.5 | stagnation | 1.0 min |
| short-escape | 9 | 0 | 969.7 | stagnation | 1.2 min |
| open-escape | 5 | 0 | 983.2 | stagnation | 2.3 min |
| no-fanout | 5 | 0 | 983.2 | stagnation | 0.9 min |
| continue | 2 | 0 | 993.3 | stagnation | 0.3 min |

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
| clearance | 41 | 18 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 19 | 16 |

Open connections reported by KiCad:

- PTH pad 3 [/BTN3] of J1 @ (62.77, 84.56) ↔ Pad 10 [/BTN3] of U1 on F.Cu @ (69.00, 84.15)
- Pad 39 [/BTN7] of U1 on F.Cu @ (80.40, 84.15) ↔ PTH pad 3 [/BTN7] of J2 @ (94.85, 78.06)

## Board

- Source: https://github.com/mtiutiu/Hardware_Playground (author mtiutiu, license None)
- 2 layers, 20 nets, 329 components, 39.7 × 49.98 mm, 2 zones
- Human reference layout: 460 segments, 24 vias, 957.39 mm of track, 641 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/Hardware_Playground_sd_wav_pcm5100/`](../../../../fixtures/PCBench/Hardware_Playground_sd_wav_pcm5100/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 43 | None | 18 | 3.8 min | FAIL |
| 2.4.1 | TIMED_OUT | 23 | None | 20 | 15.3 min | FAIL |
| 2.5.0 | COMPLETED | 17 | 0 | 0 | 9.6 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 4 | 0 | 0 | 20.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 4 | 0 | 0 | 30.2 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 2 | 0 | 0 | 25.8 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 2 | 0 | 0 | 9.2 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `Hardware_Playground_sd_wav_pcm5100--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
