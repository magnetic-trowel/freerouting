# xwhatits-capsense-controller_model-f-usb

Tier **B** (routine). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 10 |
| of which router-introduced | 0 |
| of which pre-existing in input | 10 (unfixable: 10) |
| Worst violation depth | 52.799877929687504 µm |
| Quality score | 999.47437 |
| Wall clock | 2.7 min |
| Connections (total / still open) | 213 / 0 |
| Autorouter stop reason | completed after 17 passes |
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
| clearance | 2 | 2 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 15 | 15 |

## Board

- Source: https://github.com/BASLQC/xwhatits-capsense-controller (author BASLQC, license GPL-3.0)
- 2 layers, 0 nets, 92 components, 140.0 × 28.5 mm, 2 zones
- Human reference layout: 890 segments, 59 vias, 2296.61 mm of track, 480 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/xwhatits-capsense-controller_model-f-usb/`](../../../../fixtures/PCBench/xwhatits-capsense-controller_model-f-usb/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 0 | None | 12 | 5.4 min | clean |
| 2.4.1 | COMPLETED | 9 | None | 26 | 5.1 min | FAIL |
| 2.5.0 | COMPLETED | 13 | 0 | 4 | 3.3 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 0 | 0 | 4 | 6.2 min | clean |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 4 | 6.7 min | clean |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 10 | 19.5 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 10 | 1.4 min | clean |
| master-edffcabb | COMPLETED | 0 | 0 | 10 | 2.7 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `xwhatits-capsense-controller_model-f-usb--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
