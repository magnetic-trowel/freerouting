# opendous_Upconverter

Tier **C** (complex). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 8 |
| of which router-introduced | 0 |
| of which pre-existing in input | 8 (unfixable: 8) |
| Worst violation depth | 11.3636474609375 µm |
| Quality score | 999.87317 |
| Wall clock | 5.8 min |
| Connections (total / still open) | 835 / 0 |
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
| unconnected | 4 | 0 |
| clearance | 0 | 0 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 6 |

Open connections reported by KiCad:

- Track [GND] on Back, length 1.0034 mm @ (46.28, 42.22) ↔ Pad 2 [GND] of IC1 on Front @ (46.99, 41.91)
- Pad 2 [GND] of IC6 on Front @ (49.53, 67.31) ↔ Track [GND] on Front, length 2.1590 mm @ (50.55, 67.31)
- Pad 2 [GND] of IC7 on Front @ (104.14, 49.53) ↔ Track [GND] on Back, length 3.5560 mm @ (103.12, 49.53)
- Pad 1 [N-0000041] of C67 on Front @ (108.58, 31.50) ↔ Pad 1 [N-0000041] of U4 on Back @ (108.58, 31.72)

## Board

- Source: https://github.com/opendous/Upconverter1v3 (author , license [{'name': 'The Unlicense', 'link': 'https://api.github.com/licenses/unlicense'}])
- 2 layers, 0 nets, 670 components, 96.52 × 50.8 mm, 81 zones
- Human reference layout: 1184 segments, 0 vias, 2206.06 mm of track, 576 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/opendous_Upconverter/`](../../../../fixtures/PCBench/opendous_Upconverter/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 111 | None | 182 | 10.4 min | FAIL |
| 2.4.1 | COMPLETED | 117 | None | 170 | 30.2 min | FAIL |
| 2.5.0 | COMPLETED | 7 | 0 | 8 | 6.4 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 0 | 0 | 8 | 6.5 min | clean |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 8 | 7.2 min | clean |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 8 | 46.3 min | clean |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 0 | 0 | 8 | 5.8 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `opendous_Upconverter--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
