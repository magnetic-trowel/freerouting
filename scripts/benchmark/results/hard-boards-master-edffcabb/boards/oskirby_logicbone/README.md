# oskirby_logicbone

Tier **D** (diagnostic). Category: **timeout**: hit the 6 h job budget before the router finished. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | TIMED_OUT |
| Unrouted connections | 34 |
| Clearance violations (total) | 0 |
| of which router-introduced | 0 |
| of which pre-existing in input | 0 (unfixable: 0) |
| Worst violation depth | 0.0 µm |
| Quality score | 961.84064 |
| Wall clock | 360.1 min |
| Connections (total / still open) | 1188 / 34 |
| Autorouter stop reason | timed_out after 30 passes |
| Optimizer | None |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **direct-first**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 60 | 0 | 932.7 | stagnation | 134.4 min |
| direct-first | 34 | 0 | 961.8 | timed_out | 225.6 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 34 | 0 |
| clearance | 1 | 0 |
| hole_clearance | 199 | 199 |
| shorting_items | 0 | 0 |
| tracks_crossing | 0 | 1 |
| copper_edge_clearance | 5 | 2 |

Open connections reported by KiCad:

- Track [VCCAUXTRX] on Pwr1.Cu, length 5.0737 mm @ (149.67, 60.03) ↔ Pad 1 [VCCAUXTRX] of C522 on B.Cu @ (149.22, 64.90)
- Pad D12 [/Ethernet/MDIO_CLK] of IC1 on F.Cu @ (137.70, 63.70) ↔ Track [/Ethernet/MDIO_CLK] on F.Cu, length 0.4150 mm @ (131.59, 58.05)
- Pad D17 [/Exp Headers/P8_LVDS7n] of IC1 on F.Cu @ (137.70, 59.70) ↔ PTH pad 28 [/Exp Headers/P8_LVDS7n] of P8 @ (152.71, 47.30)
- Pad E18 [/Exp Headers/P8_LVDS9p] of IC1 on F.Cu @ (138.50, 58.90) ↔ PTH pad 31 [/Exp Headers/P8_LVDS9p] of P8 @ (157.78, 49.84)
- Via [/Exp Headers/P8_LVDS9n] on F.Cu - B.Cu @ (144.48, 52.67) ↔ Pad F18 [/Exp Headers/P8_LVDS9n] of IC1 on F.Cu @ (139.30, 58.90)
- Pad F17 [/Exp Headers/P8_LVDS10p] of IC1 on F.Cu @ (139.30, 59.70) ↔ PTH pad 33 [/Exp Headers/P8_LVDS10p] of P8 @ (160.32, 49.84)
- Pad H16 [/Exp Headers/P8_LVDS12n] of IC1 on F.Cu @ (140.90, 60.50) ↔ PTH pad 38 [/Exp Headers/P8_LVDS12n] of P8 @ (165.41, 47.30)
- Pad D9 [/Exp Headers/P9_GPIO21] of IC1 on F.Cu @ (137.70, 66.10) ↔ PTH pad 21 [/Exp Headers/P9_GPIO21] of P9 @ (145.09, 98.09)
- Pad D8 [/Exp Headers/P9_GPIO27] of IC1 on F.Cu @ (137.70, 66.90) ↔ PTH pad 27 [/Exp Headers/P9_GPIO27] of P9 @ (152.71, 98.09)
- Track [/Exp Headers/P9_SPI_D1] on Gnd1.Cu, length 0.3576 mm @ (148.54, 76.00) ↔ Pad C6 [/Exp Headers/P9_SPI_D1] of IC1 on F.Cu @ (136.90, 68.50)
- Pad A7 [/FPGA Memory/DDR3_DM0] of IC2 on F.Cu @ (149.50, 80.70) ↔ Pad L4 [/FPGA Memory/DDR3_DM0] of IC1 on F.Cu @ (143.30, 70.10)
- Pad K3 [/FPGA Memory/DDR3_DQ3] of IC1 on F.Cu @ (142.50, 70.90) ↔ Pad C8 [/FPGA Memory/DDR3_DQ3] of IC2 on F.Cu @ (150.30, 82.30)
- Pad H1 [/FPGA Memory/DDR3_DQ7] of IC1 on F.Cu @ (140.90, 72.50) ↔ Pad E7 [/FPGA Memory/DDR3_DQ7] of IC2 on F.Cu @ (149.50, 83.90)
- Track [/FPGA Memory/DDR3_DQ5] on F.Cu, length 0.4435 mm @ (148.61, 80.11) ↔ Pad J3 [/FPGA Memory/DDR3_DQ5] of IC1 on F.Cu @ (141.70, 70.90)
- Pad N5 [/FPGA Memory/DDR3_CK-] of IC1 on F.Cu @ (144.90, 69.30) ↔ Track [/FPGA Memory/DDR3_CK-] on Gnd2.Cu, length 0.6341 mm @ (142.82, 83.95)
- Track [/FPGA Memory/DDR3_A10] on Sig2.Cu, length 3.2130 mm @ (141.96, 88.28) ↔ Pad G3 [/FPGA Memory/DDR3_A10] of IC1 on F.Cu @ (140.10, 70.90)
- Pad H7 [/FPGA Memory/DDR3_A10] of IC2 on F.Cu @ (149.50, 86.30) ↔ Pad 4 [/FPGA Memory/DDR3_A10] of RN1 on B.Cu @ (149.90, 86.38)
- Track [/FPGA Memory/DDR3_DQS1+] on F.Cu, length 0.5775 mm @ (136.47, 82.03) ↔ Pad H4 [/FPGA Memory/DDR3_DQS1+] of IC1 on F.Cu @ (140.90, 70.10)
- Track [/FPGA Memory/DDR3_DQ14] on F.Cu, length 0.8833 mm @ (136.67, 74.50) ↔ Pad D2 [/FPGA Memory/DDR3_DQ14] of IC3 on F.Cu @ (135.40, 83.10)
- Pad E2 [/FPGA Memory/DDR3_DQ13] of IC1 on F.Cu @ (138.50, 71.70) ↔ Pad E8 [/FPGA Memory/DDR3_DQ13] of IC3 on F.Cu @ (140.20, 83.90)
- Via [/FPGA Power/PMIC_SEQ] on F.Cu - B.Cu @ (116.41, 91.15) ↔ Pad 4 [/FPGA Power/PMIC_SEQ] of IC4 on F.Cu @ (122.47, 85.75)
- Track [/Ethernet/MMC_DAT0] on B.Cu, length 2.8979 mm @ (167.70, 63.78) ↔ Pad D13 [/Ethernet/MMC_DAT0] of IC1 on F.Cu @ (137.70, 62.90)
- Pad E6 [/Exp Headers/~{ADC_CSEL}] of IC1 on F.Cu @ (138.50, 68.50) ↔ Pad 11 [/Exp Headers/~{ADC_CSEL}] of IC9 on F.Cu @ (156.28, 89.25)
- Pad E9 [/Exp Headers/ADC_SCLK] of IC1 on F.Cu @ (138.50, 66.10) ↔ Track [/Exp Headers/ADC_SCLK] on F.Cu, length 0.3362 mm @ (156.88, 90.60)
- Track [/FPGA SERDES/~{M2_WDIS1}] on F.Cu, length 0.7550 mm @ (169.88, 62.10) ↔ Pad L17 [/FPGA SERDES/~{M2_WDIS1}] of IC1 on F.Cu @ (143.30, 59.70)
- Pad M17 [/FPGA SERDES/~{PCIE_RESET0}] of IC1 on F.Cu @ (144.10, 59.70) ↔ Pad 52 [/FPGA SERDES/~{PCIE_RESET0}] of J1 on F.Cu @ (169.50, 63.02)
- Pad N17 [/FPGA SERDES/M2_COEX3] of IC1 on F.Cu @ (144.90, 59.70) ↔ Pad 48 [/FPGA SERDES/M2_COEX3] of J1 on F.Cu @ (168.50, 63.02)
- Track [/FPGA SERDES/~{PCIE_RESET1}] on F.Cu, length 1.1412 mm @ (173.00, 63.02) ↔ Pad N19 [/FPGA SERDES/~{PCIE_RESET1}] of IC1 on F.Cu @ (144.90, 58.10)
- Pad 12 [/FPGA SERDES/M2_PCM_IN] of J1 on F.Cu @ (159.50, 63.02) ↔ Pad P16 [/FPGA SERDES/M2_PCM_IN] of IC1 on F.Cu @ (145.70, 60.50)
- Pad 34 [/FPGA SERDES/M2_CTS] of J1 on F.Cu @ (165.00, 63.02) ↔ Pad R18 [/FPGA SERDES/M2_CTS] of IC1 on F.Cu @ (146.50, 58.90)
- Pad T17 [/FPGA SERDES/M2_SDIO_CLK] of IC1 on F.Cu @ (147.30, 59.70) ↔ Track [/FPGA SERDES/M2_SDIO_CLK] on F.Cu, length 1.1108 mm @ (158.75, 55.48)
- Track [/FPGA SERDES/M2_VEND1] on F.Cu, length 1.5638 mm @ (166.50, 63.02) ↔ Pad T19 [/FPGA SERDES/M2_VEND1] of IC1 on F.Cu @ (147.30, 58.10)
- Pad 8 [/FPGA SERDES/M2_PCM_CLK] of J1 on F.Cu @ (158.50, 63.02) ↔ Pad U16 [/FPGA SERDES/M2_PCM_CLK] of IC1 on F.Cu @ (148.10, 60.50)
- Track [/FPGA SERDES/M2_TXD] on F.Cu, length 0.4561 mm @ (164.16, 61.78) ↔ Pad U17 [/FPGA SERDES/M2_TXD] of IC1 on F.Cu @ (148.10, 59.70)

## Board

- Source: https://github.com/oskirby/logicbone (author , license )
- 8 layers, 328 nets, 275 components, 86.36 × 54.61 mm, 0 zones
- Human reference layout: 9875 segments, 658 vias, 10496.78 mm of track, 652 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/oskirby_logicbone/`](../../../../fixtures/PCBench/oskirby_logicbone/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | FAILED | 156 | None | 0 | 17.0 min | FAIL |
| 2.4.1 | COMPLETED | 465 | None | 16 | 15.5 min | FAIL |
| 2.5.0 | TIMED_OUT | 471 | 0 | 0 | 20.2 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 477 | 0 | 0 | 20.3 min | FAIL |
| 2.6.0-RC2 | TIMED_OUT | 42 | 0 | 0 | 480.5 min | FAIL |
| 2.6.0-RC3 | TIMED_OUT | 70 | 0 | 0 | 361.0 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | TIMED_OUT | 34 | 0 | 0 | 360.1 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `oskirby_logicbone--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
