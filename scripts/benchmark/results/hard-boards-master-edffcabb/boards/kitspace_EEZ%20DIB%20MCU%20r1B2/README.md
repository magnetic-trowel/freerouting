# kitspace_EEZ%20DIB%20MCU%20r1B2

Tier **D** (diagnostic). Category: **pre-existing clearance only**: fully routed and the router added nothing; every violation was already in the input (the runner calls these CLEAN). Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 0 |
| Clearance violations (total) | 5 |
| of which router-introduced | 0 |
| of which pre-existing in input | 5 (unfixable: 5) |
| Worst violation depth | 47.6 µm |
| Quality score | 999.94556 |
| Wall clock | 179.9 min |
| Connections (total / still open) | 638 / 0 |
| Autorouter stop reason | completed after 17 passes |
| Optimizer | no_improvement |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **open-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 1 | 0 | 997.9 | stagnation | 13.3 min |
| continue | 1 | 0 | 997.9 | stagnation | 5.1 min |
| direct-first | 1 | 0 | 997.9 | stagnation | 18.7 min |
| fanout-retry | 1 | 0 | 997.9 | stagnation | 15.1 min |
| short-escape | 1 | 0 | 997.9 | stagnation | 11.3 min |
| open-escape | 0 | 0 | 999.9 | completed | 9.6 min |

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
| clearance | 1 | 0 |
| hole_clearance | 0 | 0 |
| shorting_items | 5 | 5 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 1 | 2 |

## Board

- Source: https://github.com/eez-open/modular-psu (author , license [{'name': 'TAPR', 'link': 'https://raw.githubusercontent.com/eez-open/modular-psu/master/LICENSE.TXT'}])
- 4 layers, 219 nets, 194 components, 216.4 × 145.8 mm, 13 zones
- Human reference layout: 9426 segments, 591 vias, 9089.44 mm of track, 293 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_EEZ%20DIB%20MCU%20r1B2/`](../../../../fixtures/PCBench/kitspace_EEZ%2520DIB%2520MCU%2520r1B2/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | FAILED | 5 | None | 9 | 17.0 min | FAIL |
| 2.4.1 | COMPLETED | 145 | None | 9 | 15.2 min | FAIL |
| 2.5.0 | TIMED_OUT | 0 | 0 | 5 | 20.3 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 1 | 0 | 5 | 20.2 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 1 | 0 | 5 | 130.1 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 0 | 0 | 5 | 158.3 min | clean |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 0 | 0 | 5 | 179.9 min | clean |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_EEZ%20DIB%20MCU%20r1B2--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
