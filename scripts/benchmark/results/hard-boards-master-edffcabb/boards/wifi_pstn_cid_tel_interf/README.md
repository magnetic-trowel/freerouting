# wifi_pstn_cid_tel_interf

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 2 |
| Clearance violations (total) | 2 |
| of which router-introduced | 0 |
| of which pre-existing in input | 2 (unfixable: 2) |
| Worst violation depth | 3.902862548828125 µm |
| Quality score | 976.46826 |
| Wall clock | 0.4 min |
| Connections (total / still open) | 114 / 2 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **continue**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 3 | 0 | 964.8 | stagnation | 0.1 min |
| continue | 2 | 0 | 976.5 | stagnation | 0.0 min |
| direct-first | 2 | 0 | 976.5 | stagnation | 0.0 min |
| fanout-retry | 3 | 0 | 964.8 | stagnation | 0.1 min |
| short-escape | 3 | 0 | 964.8 | stagnation | 0.1 min |
| open-escape | 3 | 0 | 964.8 | stagnation | 0.0 min |
| no-fanout | 3 | 0 | 964.8 | stagnation | 0.0 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 2 | 0 |
| clearance | 2 | 2 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Pad 1 [+5V] of P1 on F.Cu @ (244.82, 121.70) ↔ Track [+5V] on B.Cu, length 4.7429 mm @ (235.59, 121.92)
- Pad 5 [GND] of P1 on F.Cu @ (244.82, 119.10) ↔ PTH pad 6 [GND] of P1 @ (244.82, 117.90)

## Board

- Source: https://github.com/aniline/wifi_pstn_cid (author aniline, license None)
- 2 layers, 33 nets, 55 components, 86.36 × 66.04 mm, 0 zones
- Human reference layout: 353 segments, 6 vias, 1284.82 mm of track, 117 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/wifi_pstn_cid_tel_interf/`](../../../../fixtures/PCBench/wifi_pstn_cid_tel_interf/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 2 | None | 2 | 0.3 min | FAIL |
| 2.4.1 | COMPLETED | 2 | None | 2 | 0.7 min | FAIL |
| 2.5.0 | COMPLETED | 3 | 0 | 2 | 0.6 min | FAIL |
| 2.6.0-RC1 | COMPLETED | 3 | 0 | 2 | 1.7 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 3 | 0 | 2 | 1.2 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 2 | 0 | 2 | 1.1 min | FAIL |
| 2.6.0-RC4 | COMPLETED | 2 | 0 | 2 | 1.3 min | FAIL |
| master-edffcabb | COMPLETED | 2 | 0 | 2 | 0.4 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `wifi_pstn_cid_tel_interf--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
