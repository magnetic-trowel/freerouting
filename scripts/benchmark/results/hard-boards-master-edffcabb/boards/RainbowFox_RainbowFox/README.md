# RainbowFox_RainbowFox

Tier **D** (diagnostic). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 1 |
| Clearance violations (total) | 30 |
| of which router-introduced | 0 |
| of which pre-existing in input | 30 (unfixable: 30) |
| Worst violation depth | 152.4 µm |
| Quality score | 997.2297 |
| Wall clock | 99.1 min |
| Connections (total / still open) | 721 / 1 |
| Autorouter stop reason | stagnation after 45 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **no-fanout**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 993.5 | stagnation | 27.7 min |
| continue | 2 | 0 | 995.4 | stagnation | 5.3 min |
| direct-first | 5 | 0 | 989.8 | stagnation | 11.7 min |
| fanout-retry | 538 | 0 | 168.3 | repeated | 0.1 min |
| short-escape | 6 | 0 | 988.0 | stagnation | 22.6 min |
| open-escape | 3 | 0 | 993.5 | stagnation | 16.9 min |
| no-fanout | 1 | 0 | 997.2 | stagnation | 11.8 min |
| continue | 1 | 0 | 997.2 | stagnation | 2.6 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 6 | 0 |
| clearance | 145 | 138 |
| hole_clearance | 199 | 31 |
| shorting_items | 9 | 2 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 3 | 0 |

Open connections reported by KiCad:

- Track [/ledmatrix/LED-A/SW12] on Back, length 1.9649 mm @ (62.83, 136.41) ↔ Track [/ledmatrix/LED-A/SW12] on Back, length 5.0688 mm @ (60.33, 135.89)
- Track [/ledmatrix/LED-B/CS5] on Front, length 18.4125 mm @ (345.44, 116.84) ↔ Track [/ledmatrix/LED-B/CS5] on Back, length 1.9011 mm @ (325.91, 118.75)
- Track [/mcu/PTC2] on Back, length 2.8171 mm @ (318.22, 122.18) ↔ Track [/mcu/PTC2] on Front, length 1.6972 mm @ (315.19, 127.41)
- Track [Net-(D47-Pad2)] on Back, length 1.0202 mm @ (60.17, 147.96) ↔ Track [Net-(D47-Pad2)] on Front, length 14.1709 mm @ (74.50, 151.13)
- Pad 18 [/ledmatrix/LED-A/CS4] of U2 on Back @ (126.98, 152.95) ↔ Track [/ledmatrix/LED-A/CS4] on Front, length 17.8270 mm @ (127.22, 100.75)
- Track [/ledmatrix/LED-B/CS6] on Front, length 16.0315 mm @ (345.44, 78.74) ↔ Track [/ledmatrix/LED-B/CS6] on Back, length 1.9768 mm @ (328.30, 80.64)

## Board

- Source: https://github.com/Project-Aurora/RainbowFox (author Project-Aurora, license None)
- 2 layers, 79 nets, 316 components, 304.8 × 95.25 mm, 8 zones
- Human reference layout: 3738 segments, 113 vias, 18333.63 mm of track, 1140 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/RainbowFox_RainbowFox/`](../../../../fixtures/PCBench/RainbowFox_RainbowFox/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | FAILED | 8 | None | 118 | 17.0 min | FAIL |
| 2.4.1 | COMPLETED | 167 | None | 118 | 15.2 min | FAIL |
| 2.5.0 | TIMED_OUT | 131 | 0 | 52 | 20.2 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 41 | 0 | 30 | 20.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 3 | 0 | 30 | 287.6 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 1 | 0 | 30 | 326.2 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 1 | 0 | 30 | 99.1 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `RainbowFox_RainbowFox--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
