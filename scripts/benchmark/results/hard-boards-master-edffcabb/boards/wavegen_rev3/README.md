# wavegen_rev3

Tier **C** (complex). Category: **timeout**: hit the 6 h job budget before the router finished. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | TIMED_OUT |
| Unrouted connections | 2 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 995.098 |
| Wall clock | 360.1 min |
| Connections (total / still open) | 544 / 2 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | None |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **fanout-retry**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 992.6 | stagnation | 2.2 min |
| continue | 3 | 0 | 992.6 | stagnation | 0.8 min |
| direct-first | 3 | 0 | 992.6 | stagnation | 1.0 min |
| fanout-retry | 2 | 0 | 995.1 | stagnation | 1.4 min |
| short-escape | 293 | 0 | 307.6 | timed_out | 354.6 min |

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
| clearance | 0 | 4 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 15 | 15 |

Open connections reported by KiCad:

- Pad 23 [+3V3] of U6 on F.Cu @ (143.01, 83.86) ↔ Track [+3V3] on F.Cu, length 0.6442 mm @ (144.85, 86.56)
- Pad 6 [Net-(L3-Pad1)] of U5 on F.Cu @ (151.11, 91.93) ↔ Track [Net-(L3-Pad1)] on In2.Cu, length 3.1397 mm @ (151.76, 90.61)

## Board

- Source: https://github.com/Fifty-Nine/wavegen (author Fifty-Nine, license NOASSERTION)
- 4 layers, 22 nets, 289 components, 55.88 × 76.2 mm, 4 zones
- Human reference layout: 925 segments, 94 vias, 1616.39 mm of track, 873 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/wavegen_rev3/`](../../../../fixtures/PCBench/wavegen_rev3/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 17 | None | 214 | 9.3 min | FAIL |
| 2.4.1 | TIMED_OUT | 78 | None | 228 | 15.2 min | FAIL |
| 2.5.0 | TIMED_OUT | 32 | 0 | 0 | 20.2 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 7 | 0 | 0 | 20.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 1 | 0 | 0 | 54.0 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 0 | 195.8 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | TIMED_OUT | 2 | 0 | 0 | 360.1 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `wavegen_rev3--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
