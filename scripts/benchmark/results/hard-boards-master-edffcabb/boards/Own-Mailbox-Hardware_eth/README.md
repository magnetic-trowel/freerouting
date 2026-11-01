# Own-Mailbox-Hardware_eth

Tier **B** (routine). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 1 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 995.46484 |
| Wall clock | 9.0 min |
| Connections (total / still open) | 294 / 1 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **continue**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 2 | 0 | 990.9 | stagnation | 1.5 min |
| continue | 1 | 0 | 995.5 | stagnation | 0.6 min |
| direct-first | 1 | 0 | 995.5 | stagnation | 1.9 min |
| fanout-retry | 1 | 0 | 995.5 | stagnation | 1.0 min |
| short-escape | 3 | 0 | 986.4 | stagnation | 1.5 min |
| open-escape | 1 | 0 | 995.5 | stagnation | 1.1 min |
| no-fanout | 1 | 0 | 995.5 | stagnation | 1.4 min |

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
| clearance | 0 | 0 |
| hole_clearance | 0 | 2 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 24 | 11 |

Open connections reported by KiCad:

- Pad 1 [PSWITCH] of C25 on F.Cu @ (61.70, 52.60) ↔ Pad 119 [PSWITCH] of IC1 on F.Cu @ (46.36, 45.24)

## Board

- Source: https://github.com/pparent76/Own-Mailbox-Hardware (author pparent76, license None)
- 4 layers, 82 nets, 99 components, 50.0 × 50.0 mm, 6 zones
- Human reference layout: 2642 segments, 212 vias, 2399.92 mm of track, 1309 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/Own-Mailbox-Hardware_eth/`](../../../../fixtures/PCBench/Own-Mailbox-Hardware_eth/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 4 | None | 0 | 4.8 min | FAIL |
| 2.4.1 | TIMED_OUT | 11 | None | 10 | 5.2 min | FAIL |
| 2.5.0 | COMPLETED | 1 | 0 | 0 | 9.6 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 1 | 0 | 0 | 10.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 0 | 0 | 0 | 26.2 min | clean |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 0 | 12.2 min | clean |
| 2.6.0-RC4 | COMPLETED | 0 | 0 | 0 | 10.5 min | clean |
| master-edffcabb | COMPLETED | 1 | 0 | 0 | 9.0 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `Own-Mailbox-Hardware_eth--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
