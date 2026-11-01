# minima-hardware_FrontPanel

Tier **B** (routine). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 24 |
| of which router-introduced | 0 |
| of which pre-existing in input | 24 (unfixable: 24) |
| Worst violation depth | 458.9080810546875 µm |
| Quality score | 993.9405 |
| Wall clock | 0.6 min |
| Connections (total / still open) | 127 / 0 |
| Autorouter stop reason | completed after 7 passes |
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
| clearance | 28 | 28 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 5 | 2 |

## Board

- Source: https://github.com/sarfata/minima-hardware (author sarfata, license None)
- 2 layers, 26 nets, 49 components, 140.0 × 62.0 mm, 0 zones
- Human reference layout: 125 segments, 6 vias, 964.4 mm of track, 341 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/minima-hardware_FrontPanel/`](../../../../fixtures/PCBench/minima-hardware_FrontPanel/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 2 | None | 33 | 0.6 min | FAIL |
| 2.4.1 | COMPLETED | 1 | None | 29 | 1.3 min | FAIL |
| 2.5.0 | COMPLETED | 5 | 0 | 24 | 1.3 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 0 | 0 | 24 | 5.4 min | clean |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 24 | 3.5 min | clean |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 24 | 2.9 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 24 | 0.6 min | clean |
| master-edffcabb | COMPLETED | 0 | 0 | 24 | 0.6 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `minima-hardware_FrontPanel--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
