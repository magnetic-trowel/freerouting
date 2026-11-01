# kitspace_OtterCastAudioV2

Tier **D** (diagnostic). Category: **unrouted**: router finished but left connections open. Freerouting `master-edffcabb`.

## Result

| Metric | Value |
|---|---|
| Exit state | COMPLETED |
| Unrouted connections | 111 |
| Clearance violations (total) | 2 |
| of which router-introduced | 0 |
| of which pre-existing in input | 2 (unfixable: 2) |
| Worst violation depth | 50.0 µm |
| Quality score | 412.6516 |
| Wall clock | 96.5 min |
| Connections (total / still open) | 252 / 111 |
| Autorouter stop reason | stagnation after 18 passes |
| Optimizer | skipped |

## Router fallback strategies tried

After the default route, the router continues it (`continue`), then retries the board from scratch with each alternative strategy and keeps the best. Chosen: **short-escape**.

| Strategy | Unrouted | New violations | Score | Stop | Time |
|---|---|---|---|---|---|
| default | 119 | 0 | 370.3 | stagnation | 15.4 min |
| direct-first | 117 | 0 | 380.9 | stagnation | 14.3 min |
| fanout-retry | 112 | 0 | 407.4 | stagnation | 17.4 min |
| short-escape | 111 | 0 | 412.7 | stagnation | 17.7 min |
| open-escape | 114 | 0 | 396.8 | stagnation | 16.0 min |
| no-fanout | 122 | 0 | 354.5 | stagnation | 15.5 min |

## Layouts

Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, B.Cu blue, inner layers green and orange.

Routed by `master-edffcabb`:

(routed render not published: third-party board; `scripts/pcbench/render_layout.sh <board_id>`)

Human reference layout:

(human reference render not published: `scripts/pcbench/render_layout.sh <board_id>`)

## KiCad DRC (the board's own rules, stricter than Freerouting's checks)

| Check | `master-edffcabb` | Human reference |
|---|---|---|
| unconnected | 111 | 0 |
| clearance | 4 | 499 |
| hole_clearance | 0 | 5 |
| shorting_items | 0 | 37 |
| tracks_crossing | 0 | 50 |
| copper_edge_clearance | 2 | 9 |

Open connections reported by KiCad:

- Pad 1 [AP_CK32KO] of R76 on F.Cu @ (76.11, 97.03) ↔ Pad 1 [AP_CK32KO] of R86 on F.Cu @ (99.20, 106.91)
- Pad 1 [AP_CK32KO] of R86 on F.Cu @ (99.20, 106.91) ↔ Pad P3 [AP_CK32KO] of U1 on F.Cu @ (100.42, 104.53)
- Track [+3.3V_RTC] on F.Cu, length 2.0249 mm @ (99.22, 107.82) ↔ Pad N6 [+3.3V_RTC] of U1 on F.Cu @ (99.78, 102.58)
- Pad 1 [+3.3V_RTC] of C98 on F.Cu @ (102.88, 99.55) ↔ Pad N6 [+3.3V_RTC] of U1 on F.Cu @ (99.78, 102.58)
- Track [+3.3V_RTC] on F.Cu, length 0.7767 mm @ (110.35, 115.95) ↔ Track [+3.3V_RTC] on F.Cu, length 0.4000 mm @ (111.60, 113.28)
- Pad K5 [EPHY_LINK_LED] of U1 on F.Cu @ (97.83, 103.22) ↔ Pad 1 [EPHY_LINK_LED] of R87 on F.Cu @ (104.58, 96.64)
- Pad 12 [Net-(J8-Pad12)] of J8 on B.Cu @ (104.48, 93.10) ↔ Pad 2 [Net-(J8-Pad12)] of R87 on F.Cu @ (104.58, 95.70)
- Pad 1 [EPHY_TX_N] of R92 on F.Cu @ (106.37, 97.41) ↔ Pad 8 [EPHY_TX_N] of J8 on B.Cu @ (107.03, 93.10)
- Pad 1 [EPHY_TX_P] of R93 on F.Cu @ (106.36, 98.26) ↔ Pad 7 [EPHY_TX_P] of J8 on B.Cu @ (108.30, 93.10)
- Pad A2 [GPIO_RST] of U1 on F.Cu @ (91.97, 105.17) ↔ Track [GPIO_RST] on F.Cu, length 0.4260 mm @ (119.40, 104.87)
- Track [USB_VBUSDET] on F.Cu, length 0.2168 mm @ (90.50, 115.90) ↔ Pad A5 [USB_VBUSDET] of U1 on F.Cu @ (91.97, 103.22)
- Pad B3 [BT_UART_RX] of U1 on F.Cu @ (92.62, 104.53) ↔ Track [BT_UART_RX] on F.Cu, length 0.6133 mm @ (87.45, 105.52)
- Pad 42 [BT_UART_TX] of U7 on F.Cu @ (86.55, 105.52) ↔ Pad B4 [BT_UART_TX] of U1 on F.Cu @ (92.62, 103.88)
- Pad B5 [MIC_DET] of U1 on F.Cu @ (92.62, 103.22) ↔ Pad 2 [MIC_DET] of R40 on F.Cu @ (95.00, 82.28)
- Pad 41 [BT_UART_RTS] of U7 on F.Cu @ (85.65, 105.52) ↔ Pad C4 [BT_UART_RTS] of U1 on F.Cu @ (93.28, 103.88)
- Pad 13 [WL_WAKE_AP] of U7 on F.Cu @ (87.45, 94.08) ↔ Pad C5 [WL_WAKE_AP] of U1 on F.Cu @ (93.28, 103.22)
- Pad 1 [GPIO_LED1] of R3 on F.Cu @ (87.02, 90.00) ↔ Pad C7 [GPIO_LED1] of U1 on F.Cu @ (93.28, 101.92)
- Pad 1 [GPIO_LED2] of R4 on F.Cu @ (87.02, 85.90) ↔ Pad C8 [GPIO_LED2] of U1 on F.Cu @ (93.28, 101.28)
- Pad 1 [Net-(R2-Pad1)] of R2 on F.Cu @ (90.83, 99.06) ↔ Pad C13 [Net-(R2-Pad1)] of U1 on F.Cu @ (93.28, 98.03)
- Pad 2 [WL_PMU_EN] of R73 on F.Cu @ (87.22, 92.77) ↔ Pad 12 [WL_PMU_EN] of U7 on F.Cu @ (88.35, 94.08)
- Pad 12 [WL_PMU_EN] of U7 on F.Cu @ (88.35, 94.08) ↔ Pad E16 [WL_PMU_EN] of U1 on F.Cu @ (94.58, 96.08)
- Pad F1 [EPHY_RX_N] of U1 on F.Cu @ (95.22, 105.83) ↔ Pad 1 [EPHY_RX_N] of R90 on F.Cu @ (108.80, 97.99)
- Pad 1 [EPHY_RX_N] of R90 on F.Cu @ (108.80, 97.99) ↔ Pad 2 [EPHY_RX_N] of J8 on B.Cu @ (112.11, 93.10)
- Pad F2 [EPHY_RX_P] of U1 on F.Cu @ (95.22, 105.17) ↔ Pad 1 [EPHY_RX_P] of R91 on F.Cu @ (108.81, 98.82)
- Pad 1 [EPHY_RX_P] of R91 on F.Cu @ (108.81, 98.82) ↔ Pad 1 [EPHY_RX_P] of J8 on B.Cu @ (113.38, 93.10)
- Pad 1 [Net-(R83-Pad1)] of R83 on F.Cu @ (90.83, 102.38) ↔ Pad F5 [Net-(R83-Pad1)] of U1 on F.Cu @ (95.22, 103.22)
- Pad 6 [AP_WAKE_BT] of U7 on F.Cu @ (89.57, 99.80) ↔ Pad F12 [AP_WAKE_BT] of U1 on F.Cu @ (95.22, 98.67)
- Pad 1 [MIPI_SCK] of R51 on F.Cu @ (105.44, 96.64) ↔ Track [MIPI_SCK] on B.Cu, length 0.4039 mm @ (111.18, 95.67)
- Track [MIPI_SCK] on B.Cu, length 0.4039 mm @ (111.18, 95.67) ↔ Pad 2 [MIPI_SCK] of U3 on F.Cu @ (114.00, 107.33)
- Track [Net-(C82-Pad1)] on F.Cu, length 0.8250 mm @ (94.67, 106.91) ↔ Pad 1 [Net-(C82-Pad1)] of C83 on F.Cu @ (96.50, 106.91)
- Pad 1 [Net-(C82-Pad1)] of R80 on F.Cu @ (95.80, 108.65) ↔ Pad 1 [Net-(C82-Pad1)] of C88 on F.Cu @ (96.58, 108.61)
- Pad 1 [Net-(C82-Pad1)] of C83 on F.Cu @ (96.50, 106.91) ↔ Pad H6 [Net-(C82-Pad1)] of U1 on F.Cu @ (96.53, 102.58)
- Pad 1 [Net-(C82-Pad1)] of C88 on F.Cu @ (96.58, 108.61) ↔ Pad 1 [Net-(C82-Pad1)] of C83 on F.Cu @ (96.50, 106.91)
- Pad 1 [Net-(R82-Pad1)] of R82 on F.Cu @ (90.83, 101.67) ↔ Pad J5 [Net-(R82-Pad1)] of U1 on F.Cu @ (97.17, 103.22)
- Pad J16 [MIPI_SDA] of U1 on F.Cu @ (97.17, 96.08) ↔ Pad 1 [MIPI_SDA] of R50 on F.Cu @ (106.28, 96.14)
- Pad 27 [MIPI_SDA] of U6 on F.Cu @ (112.90, 94.00) ↔ Pad 1 [MIPI_SDA] of R50 on F.Cu @ (106.28, 96.14)
- Pad 1 [MIPI_SDA] of U3 on F.Cu @ (114.40, 107.33) ↔ Pad 27 [MIPI_SDA] of U6 on F.Cu @ (112.90, 94.00)
- Pad L5 [EPHY_SPD_LED] of U1 on F.Cu @ (98.47, 103.22) ↔ Pad 1 [EPHY_SPD_LED] of R88 on F.Cu @ (107.88, 99.65)
- Pad L6 [+3.0VA] of U1 on F.Cu @ (98.47, 102.58) ↔ Pad M7 [+3.0VA] of U1 on F.Cu @ (99.12, 101.92)
- Track [+3.0VA] on B.Cu, length 4.4896 mm @ (103.62, 106.05) ↔ Pad L6 [+3.0VA] of U1 on F.Cu @ (98.47, 102.58)
- … 71 more in `../../drc-summary.json`

## Board

- Source: https://github.com/Ottercast/OtterCastAudioV2 (author , license [{'name': 'MIT', 'link': 'https://raw.githubusercontent.com/Ottercast/OtterCastAudioV2/main/LICENSE'}])
- 4 layers, 266 nets, 226 components, 25.0 × 49.27 mm, 28 zones
- Human reference layout: 2813 segments, 451 vias, 2535.73 mm of track, 690 KiCad DRC violations
- Fixture (DSN input, placed and reference KiCad boards): [`scripts/benchmark/fixtures/PCBench/kitspace_OtterCastAudioV2/`](../../../../fixtures/PCBench/kitspace_OtterCastAudioV2/)

## Freerouting versions on this board

| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |
|---|---|---|---|---|---|---|
| 1.9.0 | COMPLETED | 121 | None | 6 | 15.6 min | FAIL |
| 2.4.1 | COMPLETED | 106 | None | 6 | 15.1 min | FAIL |
| 2.5.0 | TIMED_OUT | 111 | 0 | 2 | 20.1 min | FAIL |
| 2.6.0-RC1 | TIMED_OUT | 114 | 0 | 2 | 20.1 min | FAIL |
| 2.6.0-RC2 | COMPLETED | 111 | 0 | 2 | 195.7 min | FAIL |
| 2.6.0-RC3 | COMPLETED | 111 | 0 | 2 | 229.1 min | FAIL |
| 2.6.0-RC4 | not run | | | | | |
| master-edffcabb | COMPLETED | 111 | 0 | 2 | 96.5 min | FAIL |

## Files

- `master.ses` (kept on disk, not published: third-party board): the router's output session (best result found; partial when connections are open). Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.
- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.
- The runner's record for this board is the `kitspace_OtterCastAudioV2--unrouted--master-edffcabb--edffcabb2` entry in `scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).
