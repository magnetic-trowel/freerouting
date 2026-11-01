# PCBench integration

Real-world KiCad boards from [PCBench/PCBench](https://github.com/PCBench/PCBench) for
ground-truth routing evaluation.

This workspace expects the clone at **`C:\Work\PCBench`** (override with
`FREEROUTING_PCBENCH`). Each board folder under `PCBs\` contains:

- `raw.kicad_pcb` — professionally routed original
- `processed.kicad_pcb` — cleaned source used as conversion input
- `metadata.json` — layers, CAD version, origin
- `final.json` — PCBench PCB-RDL (ML format; not consumed by Freerouting)

KiCad 10 CLI is expected at **`C:\Program Files\KiCad\10.0\bin\kicad-cli.exe`**
(override with `FREEROUTING_KICAD_CLI`). Specctra DSN export is **not** available as a
`kicad-cli pcb export` subcommand in KiCad 10; conversion uses KiCad's bundled Python
(`C:\Program Files\KiCad\10.0\bin\python.exe`) and `pcbnew.ExportSpecctraDSN`.

## Pipeline

1. `Fetch-PCBench.ps1` — verify the local PCBench clone (does not re-clone).
2. `strip_kicad_routing.py` — remove segments, vias, and zone fills from `.kicad_pcb`.
3. `Convert-PCBenchBoards.ps1` — export stripped boards to Specctra `.dsn` via `pcbnew.ExportSpecctraDSN`.
4. `Build-GroundTruth.ps1` — record reference via/segment counts and KiCad DRC on `raw.kicad_pcb`.
5. `Run-PCBenchGate.ps1` — route converted boards and collect benchmark metrics (G3 gate).

Converted DSNs land in `scripts/benchmark/fixtures/PCBench/` (gitignored). Cache files
(stripped PCBs, ground-truth JSON) land in `scripts/pcbench/cache/` (gitignored).

## Requirements

- Python 3.8+
- KiCad 10 `kicad-cli` (DRC) and `python.exe` + pcbnew (DSN export / SES import)
- Freerouting executable JAR (`gradlew.bat executableJar`) for optional DsnReader smoke-load

## Stratified PoC boards (Phase 0)

`Convert-PCBenchBoards.ps1 -MaxBoards 3` prefers:

- `1Bitsy_1bitsy` (4-layer)
- one 2-layer board
- one 6+ layer board

## Known caveats

- KiCad DSN export omits copper-to-edge clearance (Issue 558); external KiCad DRC on the
  original `raw.kicad_pcb` is the arbiter for PCBench evaluation.
- PCBench boards are KiCad 4–7 era; KiCad 10 may upgrade-on-load during export.
- Headless `pcbnew.ImportSpecctraSES` on KiCad 10.0.2 returned false for Freerouting SES
  (tracks unchanged). G3 should treat KiCad DRC on `raw.kicad_pcb` as the external arbiter
  until SES reimport is reliable. On KiCad 10.0.7 the import works headless as long as no
  `wx.App` is created (`import_specctra_ses.py` creates one and fails without an X display;
  `kicad_layout_tool.py import` does not).
- `strip_kicad_routing.py` must remove `segment`/`via` children of the `(kicad_pcb ...)`
  wrapper; dropping only file-level s-exprs leaves the original copper in place.

## Hard-board reruns

Rerun only the boards earlier releases failed on, keep every failing layout, and compare the routed
result with the human layout under KiCad DRC. Results for `master-edffcabb` are in
`scripts/benchmark/results/hard-boards-master-edffcabb/README.md`.

- `make_hard_board_subset.py` — fixtures dir (symlinks + filtered `catalog.json`) holding the boards
  that failed for the given labels in `benchmarks.json`.
- `collect_hard_board_failures.py` — snapshot failing runs (input, `.ses`, log, result JSON, run
  record) while a run is in progress.
- `render_layout.sh` + `kicad_layout_tool.py` — import a `.ses` into the placed board, run KiCad DRC
  on it and on the human reference, and render PNGs with open connections and electrical violations
  drawn on top. KiCad commands come from `FREEROUTING_KICAD_CLI` / `FREEROUTING_KICAD_PYTHON`.
- `hard_boards_report.py` — summary, per-board pages and pictures under
  `scripts/benchmark/results/hard-boards-<label>/`.
- `compact_log.py` — collapse repeated log messages in place (`[x N, last <time>]`). The runner
  does this to each board's log when the board finishes; pass `--keep-raw-logs` to skip it. Some
  boards repeat `Polyline: must contain at least 2 different points` hundreds of millions of times,
  which made single logs 2-12 GB; compacted they are about 1 MB.
