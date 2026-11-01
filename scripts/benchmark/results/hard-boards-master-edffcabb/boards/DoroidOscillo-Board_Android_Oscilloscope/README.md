# DoroidOscillo-Board_Android_Oscilloscope

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 4 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 982.74005 |
| Wall clock | 5.1 min |
| Connections (total / still open) | 309 / 4 |
| Autorouter stop reason | stagnation after 32 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **direct-first**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 5 | 0 | 978.4 | stagnation | 1.6 min |
| direct-first | 4 | 0 | 982.7 | stagnation | 1.3 min |
| fanout-retry | 82 | 0 | 646.2 | repeated | 0.2 min |
| short-escape | 4 | 0 | 982.7 | stagnation | 0.6 min |
| open-escape | 4 | 0 | 982.7 | stagnation | 0.5 min |
| no-fanout | 7 | 0 | 969.8 | stagnation | 0.6 min |
| continue | 4 | 0 | 982.7 | stagnation | 0.2 min |

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
| copper_edge_clearance | 10 | 8 |

Open connections reported by KiCad:

- Pad D8 [PROBE_COMP] of U1 on F.Cu @ (122.90, 120.30) ↔ Pad 2 [PROBE_COMP] of R19 on F.Cu @ (131.00, 108.85)
- Pad 1 [/USB_VBUS] of R29 on F.Cu @ (113.45, 126.80) ↔ Pad E3 [/USB_VBUS] of U1 on F.Cu @ (118.90, 121.10)
- Pad H3 [JTAG_TDO] of U1 on F.Cu @ (118.90, 123.50) ↔ PTH pad 4 [JTAG_TDO] of P3 @ (120.60, 141.40)
- Pad 1 [SPIFI_CS] of U8 on F.Cu @ (118.75, 103.39) ↔ Pad E7 [SPIFI_CS] of U1 on F.Cu @ (122.10, 121.10)

## Board

- Source: https://github.com/aroerina/DoroidOscillo-Board (author aroerina, license None)
- 4 layers, 39 nets, 134 components, 70.0 × 43.0 mm, 4 zones
- Human reference layout: 1070 segments, 155 vias, 1603.15 mm of track, 207 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/DoroidOscillo-Board_Android_Oscilloscope/`](../../../../fixtures/PCBench/DoroidOscillo-Board_Android_Oscilloscope/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 4 | None | 34 | 3.4 min | FAIL |
| 2.4.1 | COMPLETED | 5 | None | 42 | 5.2 min | FAIL |
| 2.5.0 | COMPLETED | 5 | 0 | 0 | 4.1 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 5 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 3 | 0 | 0 | 16.5 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 3 | 0 | 0 | 28.8 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 2 | 0 | 0 | 17.9 min | FAIL |
| master-edffcabb | COMPLETED | 4 | 0 | 0 | 5.1 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `DoroidOscillo-Board_Android_Oscilloscope--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
