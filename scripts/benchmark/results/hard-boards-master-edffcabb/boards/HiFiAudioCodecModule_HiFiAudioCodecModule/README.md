# HiFiAudioCodecModule_HiFiAudioCodecModule

Tier **B** (routine). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 1 |
| of which router-introduced | 0 |
| of which pre-existing in input | 1 (unfixable: 1) |
| Worst violation depth | 95.901123046875 µm |
| Quality score | 999.72565 |
| Wall clock | 0.2 min |
| Connections (total / still open) | 72 / 0 |
| Autorouter stop reason | completed after 16 passes |
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
| unconnected | 1 | 0 |
| clearance | 1 | 1 |
| hole_clearance | 4 | 0 |
| shorting_items | 0 | 1 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Track [GND] on F.Cu, length 0.2298 mm @ (26.24, 29.92) ↔ Track [GND] on B.Cu, length 2.2905 mm @ (26.24, 29.92)

## Board

- Source: https://github.com/MickMad/HiFiAudioCodecModule (author MickMad, license NOASSERTION)
- 2 layers, 2 nets, 26 components, 25.4 × 25.4 mm, 8 zones
- Human reference layout: 178 segments, 15 vias, 293.69 mm of track, 281 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/HiFiAudioCodecModule_HiFiAudioCodecModule/`](../../../../fixtures/PCBench/HiFiAudioCodecModule_HiFiAudioCodecModule/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 1 | None | 19 | 0.8 min | FAIL |
| 2.4.1 | COMPLETED | 1 | None | 19 | 0.8 min | FAIL |
| 2.5.0 | COMPLETED | 1 | 0 | 1 | 0.3 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 0 | 0 | 1 | 0.2 min | clean |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 1 | 0.2 min | clean |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 1 | 0.5 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 1 | 0.3 min | clean |
| master-edffcabb | COMPLETED | 0 | 0 | 1 | 0.2 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `HiFiAudioCodecModule_HiFiAudioCodecModule--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
