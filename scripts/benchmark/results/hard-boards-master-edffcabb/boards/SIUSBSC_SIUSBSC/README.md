# SIUSBSC_SIUSBSC

Tier **B** (routine). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 2 |
| of which router-introduced | 0 |
| of which pre-existing in input | 2 (unfixable: 2) |
| Worst violation depth | 3.902862548828125 µm |
| Quality score | 999.733 |
| Wall clock | 0.2 min |
| Connections (total / still open) | 65 / 0 |
| Autorouter stop reason | completed after 2 passes |
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
| clearance | 2 | 3 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 13 | 6 |

## Board

- Source: https://github.com/jcreedon/SIUSBSC (author jcreedon, license NOASSERTION)
- 2 layers, 4 nets, 31 components, 25.4 × 25.4 mm, 1 zones
- Human reference layout: 201 segments, 15 vias, 225.39 mm of track, 152 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/SIUSBSC_SIUSBSC/`](../../../../fixtures/PCBench/SIUSBSC_SIUSBSC/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 0 | None | 2 | 1.0 min | clean |
| 2.4.1 | COMPLETED | 0 | None | 5 | 2.6 min | clean |
| 2.5.0 | COMPLETED | 1 | 0 | 2 | 0.6 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 0 | 0 | 2 | 0.6 min | clean |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 2 | 0.7 min | clean |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 2 | 0.5 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 2 | 0.3 min | clean |
| master-edffcabb | COMPLETED | 0 | 0 | 2 | 0.2 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `SIUSBSC_SIUSBSC--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
