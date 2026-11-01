# kitspace_teensy-fx

Tier **C** (complex). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 6 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 959.18365 |
| Wall clock | 20.4 min |
| Connections (total / still open) | 196 / 6 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **open-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 15 | 0 | 898.0 | stagnation | 4.5 min |
| direct-first | 11 | 0 | 925.2 | stagnation | 3.4 min |
| fanout-retry | 11 | 0 | 925.2 | stagnation | 3.4 min |
| short-escape | 10 | 0 | 932.0 | stagnation | 3.7 min |
| open-escape | 6 | 0 | 959.2 | stagnation | 1.6 min |
| no-fanout | 9 | 0 | 938.8 | stagnation | 3.7 min |

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
| clearance | 0 | 3 |
| hole_clearance | 0 | 1 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Track [GND] on F.Cu, length 0.5240 mm @ (258.57, 50.75) ↔ Pad 7 [GND] of U3 on F.Cu @ (258.05, 48.75)
- Pad 2 [Net-(J2-PadR)] of U3 on F.Cu @ (258.05, 51.25) ↔ Pad R [Net-(J2-PadR)] of J2 on B.Cu @ (278.00, 48.30)
- Pad 5 [/SGTL audio/filter_audio_3v3] of U3 on F.Cu @ (258.05, 49.75) ↔ Pad 1 [/SGTL audio/filter_audio_3v3] of C6 on F.Cu @ (260.86, 50.50)
- Pad 1 [/SGTL audio/filter_audio_3v3] of C6 on F.Cu @ (260.86, 50.50) ↔ Pad 2 [/SGTL audio/filter_audio_3v3] of L1 on F.Cu @ (260.86, 48.50)
- Pad 1 [/SGTL audio/filter_audio_3v3] of C7 on F.Cu @ (260.86, 52.50) ↔ Pad 1 [/SGTL audio/filter_audio_3v3] of C6 on F.Cu @ (260.86, 50.50)
- Track [+3V3] on In1.Cu, length 4.7511 mm @ (251.49, 49.50) ↔ Pad 20 [+3V3] of U3 on F.Cu @ (253.15, 49.75)

## Board

- Source: https://github.com/mattvenn/teensy-audio-fx (author , license [{'name': 'License not found', 'link': ''}])
- 4 layers, 63 nets, 82 components, 60.0 × 100.0 mm, 2 zones
- Human reference layout: 1218 segments, 115 vias, 3517.55 mm of track, 292 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_teensy-fx/`](../../../../fixtures/PCBench/kitspace_teensy-fx/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 5 | None | 0 | 4.4 min | FAIL |
| 2.4.1 | COMPLETED | 12 | None | 0 | 5.1 min | FAIL |
| 2.5.0 | TIMED_OUT | 18 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 19 | 0 | 0 | 10.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 12 | 0 | 0 | 57.7 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 4 | 0 | 0 | 113.6 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 6 | 0 | 0 | 20.4 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_teensy-fx--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
