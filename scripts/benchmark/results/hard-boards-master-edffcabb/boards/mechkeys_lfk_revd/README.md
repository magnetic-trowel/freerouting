# mechkeys_lfk_revd

Tier **D** (diagnostic). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 32 |
| of which router-introduced | 0 |
| of which pre-existing in input | 32 (unfixable: 32) |
| Worst violation depth | 197.0 µm |
| Quality score | 998.9479 |
| Wall clock | 40.0 min |
| Connections (total / still open) | 782 / 0 |
| Autorouter stop reason | completed after 19 passes |
| Optimizer | no_improvement |

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
| clearance | 56 | 65 |
| hole_clearance | 181 | 172 |
| shorting_items | 4 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 29 | 12 |

Open connections reported by KiCad:

- Track [GND] on B.Cu, length 6.5400 mm @ (347.66, 28.57) ↔ Track [GND] on B.Cu, length 1.6799 mm @ (347.79, 24.36)
- PTH pad SW1 [C3] of SW_74 @ (89.13, 108.05) ↔ Track [C3] on B.Cu, length 12.3408 mm @ (88.42, 104.78)
- PTH pad SW1 [C4] of SW_75 @ (110.56, 108.05) ↔ Track [C4] on F.Cu, length 3.3129 mm @ (111.92, 104.78)
- Track [Net-(D85-Pad2)] on B.Cu, length 1.8088 mm @ (319.09, 104.78) ↔ PTH pad SW1 [Net-(D85-Pad2)] of SW_87 @ (322.49, 108.05)

## Board

- Source: https://github.com/BalzGuenat/mechkeys (author BalzGuenat, license None)
- 2 layers, 108 nets, 339 components, 367.75 × 93.25 mm, 2 zones
- Human reference layout: 3545 segments, 286 vias, 19199.71 mm of track, 1399 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/mechkeys_lfk_revd/`](../../../../fixtures/PCBench/mechkeys_lfk_revd/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | FAILED | 10 | None | 75 | 17.0 min | FAIL |
| 2.4.1 | TIMED_OUT | 160 | None | 77 | 15.2 min | FAIL |
| 2.5.0 | TIMED_OUT | 133 | 0 | 53 | 20.3 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 0 | 0 | 32 | 20.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 32 | 279.3 min | clean |
| 2.6.0-RC3 | FAILED | 0 | None | 32 | 396.0 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 0 | 0 | 32 | 40.0 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `mechkeys_lfk_revd--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
