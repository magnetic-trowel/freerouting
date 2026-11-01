# cleanhawk250_cleanhawk250-pdb

Tier **B** (routine). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 12 |
| of which router-introduced | 0 |
| of which pre-existing in input | 12 (unfixable: 12) |
| Worst violation depth | 63.6 µm |
| Quality score | 998.81396 |
| Wall clock | 0.8 min |
| Connections (total / still open) | 170 / 0 |
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
| clearance | 4 | 2 |
| hole_clearance | 10 | 7 |
| shorting_items | 0 | 2 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

## Board

- Source: https://github.com/kylemanna/cleanhawk250 (author kylemanna, license CC-BY-SA-4.0)
- 2 layers, 35 nets, 80 components, 134.44 × 79.37 mm, 11 zones
- Human reference layout: 505 segments, 74 vias, 1374.55 mm of track, 841 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/cleanhawk250_cleanhawk250-pdb/`](../../../../fixtures/PCBench/cleanhawk250_cleanhawk250-pdb/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 9 | None | 28 | 3.4 min | FAIL |
| 2.4.1 | COMPLETED | 11 | None | 28 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 12 | 0 | 12 | 5.8 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 12 | 0 | 12 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 12 | 0 | 12 | 21.6 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 8 | 0 | 12 | 35.0 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 8 | 0 | 12 | 16.2 min | FAIL |
| master-edffcabb | COMPLETED | 0 | 0 | 12 | 0.8 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `cleanhawk250_cleanhawk250-pdb--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
