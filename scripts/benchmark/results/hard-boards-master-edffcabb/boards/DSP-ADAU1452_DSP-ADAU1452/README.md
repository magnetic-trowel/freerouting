# DSP-ADAU1452_DSP-ADAU1452

Tier **D** (diagnostic). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 27 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 951.4825 |
| Wall clock | 243.4 min |
| Connections (total / still open) | 742 / 27 |
| Autorouter stop reason | stagnation after 20 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **short-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 28 | 0 | 949.7 | stagnation | 24.9 min |
| direct-first | 29 | 0 | 947.9 | stagnation | 23.5 min |
| fanout-retry | 21 | 2 | 962.2 | stagnation | 42.3 min |
| short-escape | 27 | 0 | 951.5 | stagnation | 112.3 min |
| open-escape | 30 | 0 | 946.1 | stagnation | 20.5 min |
| no-fanout | 27 | 0 | 951.5 | stagnation | 19.8 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 27 | 0 |
| clearance | 0 | 0 |
| hole_clearance | 0 | 0 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 0 |
| copper_edge_clearance | 0 | 0 |

Open connections reported by KiCad:

- Pad 5 [/ADAU1452/ADAU_SPDIF_TX] of U201 on F.Cu @ (162.52, 102.68) ↔ Pad 2 [/ADAU1452/ADAU_SPDIF_TX] of R403 on F.Cu @ (171.36, 95.50)
- Track [GNDD] on F.Cu, length 0.0769 mm @ (163.98, 104.15) ↔ Track [GNDD] on F.Cu, length 1.2019 mm @ (161.31, 103.17)
- Track [/ADAU1452/DVDD] on In2.Cu, length 7.2213 mm @ (158.66, 107.14) ↔ Pad 53 [/ADAU1452/DVDD] of U201 on F.Cu @ (155.73, 111.59)
- Pad 2 [/ADAU1452/SELFBOOT] of R201 on F.Cu @ (129.94, 97.65) ↔ PTH pad 1 [/ADAU1452/SELFBOOT] of JP201 @ (130.88, 99.93)
- PTH pad 1 [/ADAU1452/SELFBOOT] of JP201 @ (130.88, 99.93) ↔ Pad 34 [/ADAU1452/SELFBOOT] of U201 on F.Cu @ (150.78, 103.38)
- Track [/ADAU1452/XTALIN] on F.Cu, length 3.0140 mm @ (152.24, 81.93) ↔ Pad 21 [/ADAU1452/XTALIN] of U201 on F.Cu @ (155.37, 98.79)
- Track [/ADAU1452/IOVDD] on F.Cu, length 2.2980 mm @ (157.87, 95.36) ↔ Track [/ADAU1452/IOVDD] on In1.Cu, length 15.6381 mm @ (158.51, 79.64)
- Pad 56 [/ADAU1452/IOVDD] of U201 on F.Cu @ (158.27, 111.59) ↔ Pad 1 [/ADAU1452/IOVDD] of C241 on F.Cu @ (159.77, 114.41)
- Pad 56 [/ADAU1452/IOVDD] of U201 on F.Cu @ (158.27, 111.59) ↔ Track [/ADAU1452/IOVDD] on In2.Cu, length 1.6600 mm @ (149.34, 115.77)
- Pad 1 [/ADAU1452/IOVDD] of C241 on F.Cu @ (159.77, 114.41) ↔ Pad 1 [/ADAU1452/IOVDD] of C240 on F.Cu @ (159.77, 117.20)
- Pad 2 [/ADAU1452/IOVDD] of U201 on F.Cu @ (163.58, 103.74) ↔ Track [/ADAU1452/IOVDD] on F.Cu, length 0.0818 mm @ (157.84, 98.08)
- Track [/ADAU1452/IOVDD] on F.Cu, length 3.0480 mm @ (172.28, 101.27) ↔ Pad 2 [/ADAU1452/IOVDD] of U201 on F.Cu @ (163.58, 103.74)
- Pad 4 [/ADAU1452/ADAU_SPDIF_RX] of U201 on F.Cu @ (162.87, 103.03) ↔ PTH pad 2 [/ADAU1452/ADAU_SPDIF_RX] of JP401 @ (191.20, 77.71)
- Pad 11 [/Connectors/AUXADCIN_4] of U201 on F.Cu @ (160.39, 100.56) ↔ Pad 1 [/Connectors/AUXADCIN_4] of D507 on B.Cu @ (175.96, 72.96)
- Pad 2 [/ADAU1452/MCLK_ADAU] of R233 on B.Cu @ (153.06, 111.36) ↔ Pad 23 [/ADAU1452/MCLK_ADAU] of U201 on F.Cu @ (154.67, 99.50)
- Pad 24 [/ADAU1452/DSP_RESET] of U201 on F.Cu @ (154.31, 99.85) ↔ Track [/ADAU1452/DSP_RESET] on B.Cu, length 1.2745 mm @ (150.43, 94.67)
- Pad 1 [/Connectors/M_SPI_MISO] of R227 on F.Cu @ (136.72, 77.92) ↔ Pad 29 [/Connectors/M_SPI_MISO] of U201 on F.Cu @ (152.55, 101.62)
- Pad 30 [/ADAU1452/MISO/SDA] of U201 on F.Cu @ (152.19, 101.97) ↔ Track [/ADAU1452/MISO/SDA] on F.Cu, length 2.0291 mm @ (135.45, 94.36)
- Pad 1 [/ADAU1452/DACOUT_BCLK1] of R558 on F.Cu @ (138.58, 113.14) ↔ Pad 43 [/ADAU1452/DACOUT_BCLK1] of U201 on F.Cu @ (152.19, 108.05)
- Pad 1 [/ADAU1452/DACOUT_SDATA1] of R559 on F.Cu @ (138.58, 115.68) ↔ Pad 44 [/ADAU1452/DACOUT_SDATA1] of U201 on F.Cu @ (152.55, 108.41)
- Pad 1 [/ADAU1452/DACOUT_LRCLK3] of R565 on F.Cu @ (143.96, 119.66) ↔ Pad 50 [/ADAU1452/DACOUT_LRCLK3] of U201 on F.Cu @ (154.67, 110.53)
- Pad 57 [/ADAU1452/ADCIN_BCLK0] of U201 on F.Cu @ (158.63, 111.23) ↔ Pad 2 [/ADAU1452/ADCIN_BCLK0] of R531 on F.Cu @ (167.45, 119.66)
- Pad 59 [/ADAU1452/ADCIN_SDATA0] of U201 on F.Cu @ (159.33, 110.53) ↔ Pad 2 [/ADAU1452/ADCIN_SDATA0] of R532 on F.Cu @ (169.99, 119.66)
- Pad 65 [/ADAU1452/ADCIN_BCLK2] of U201 on F.Cu @ (161.45, 108.41) ↔ Pad 2 [/ADAU1452/ADCIN_BCLK2] of R523 on F.Cu @ (191.50, 114.16)
- Pad 66 [/ADAU1452/ADCIN_LRCLK2] of U201 on F.Cu @ (161.81, 108.05) ↔ Pad 2 [/ADAU1452/ADCIN_LRCLK2] of R522 on B.Cu @ (191.50, 116.70)
- Pad 68 [/ADAU1452/ADCIN_BCLK3] of U201 on F.Cu @ (162.52, 107.34) ↔ Pad 2 [/ADAU1452/ADCIN_BCLK3] of R519 on F.Cu @ (191.50, 93.07)
- Pad 70 [/ADAU1452/ADCIN_SDATA3] of U201 on F.Cu @ (163.22, 106.64) ↔ Pad 2 [/ADAU1452/ADCIN_SDATA3] of R520 on F.Cu @ (191.50, 90.53)

## Board

- Source: https://github.com/ohdsp/DSP-ADAU1452 (author ohdsp, license NOASSERTION)
- 4 layers, 116 nets, 377 components, 100.0 × 100.0 mm, 25 zones
- Human reference layout: 1992 segments, 355 vias, 4113.52 mm of track, 802 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/DSP-ADAU1452_DSP-ADAU1452/`](../../../../fixtures/PCBench/DSP-ADAU1452_DSP-ADAU1452/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | FAILED | 55 | None | 0 | 33.0 min | FAIL |
| 2.4.1 | COMPLETED | 36 | None | 0 | 30.5 min | FAIL |
| 2.5.0 | TIMED_OUT | 38 | 0 | 0 | 45.3 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 32 | 0 | 0 | 45.3 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 31 | 0 | 0 | 280.7 min | FAIL |
| 2.6.0-RC3 | TIMED_OUT | 18 | 0 | 0 | 360.4 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 27 | 0 | 0 | 243.4 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `DSP-ADAU1452_DSP-ADAU1452--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
