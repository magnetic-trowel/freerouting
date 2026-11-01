# kitspace_sympetrum-v2%20FF1.1

Tier **C** (complex). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 18 |
| of which router-introduced | 0 |
| of which pre-existing in input | 18 (unfixable: 18) |
| Worst violation depth | 152.4 µm |
| Quality score | 998.2235 |
| Wall clock | 7.4 min |
| Connections (total / still open) | 146 / 0 |
| Autorouter stop reason | completed after 15 passes |
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
| unconnected | 0 | 0 |
| clearance | 3 | 3 |
| hole_clearance | 0 | 0 |
| shorting_items | 3 | 3 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 42 | 47 |

## Board

- Source: https://github.com/borgel/sympetrum-v2 (author , license [{'name': 'MIT', 'link': 'https://raw.githubusercontent.com/borgel/sympetrum-v2/master/LICENSE'}])
- 2 layers, 40 nets, 67 components, 98.76 × 76.76 mm, 4 zones
- Human reference layout: 555 segments, 104 vias, 1681.2 mm of track, 544 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_sympetrum-v2%20FF1.1/`](../../../../fixtures/PCBench/kitspace_sympetrum-v2%2520FF1.1/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 20 | None | 26 | 5.6 min | FAIL |
| 2.4.1 | COMPLETED | 44 | None | 18 | 5.2 min | FAIL |
| 2.5.0 | TIMED_OUT | 44 | 0 | 18 | 10.1 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 44 | 0 | 18 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 42 | 0 | 18 | 60.3 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 26 | 0 | 18 | 178.7 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 0 | 0 | 18 | 7.4 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_sympetrum-v2%20FF1.1--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
