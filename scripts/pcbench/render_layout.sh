#!/bin/bash
# Render one PCBench board for visual inspection: import a routed .ses into the placed KiCad board,
# run KiCad DRC on it and on the human reference, and write PNGs with the problems drawn on top.
#
# Usage: scripts/pcbench/render_layout.sh <board_id> [label]      (label defaults to master-edffcabb)
#
# Writes $LAYOUTS_DIR/<board_id>/ (default build/pcbench-layouts):
#   master.png / reference.png       routed result / human layout (processed.kicad_pcb)
#                                    magenta = open connections, yellow = KiCad electrical DRC violations
#   master-routed.kicad_pcb          the routed board
#   master-drc.json / reference-drc.json   KiCad DRC reports
#
# Needs KiCad 10 (kicad-cli + Python with pcbnew) and a Python with cairosvg. Override the commands with
# FREEROUTING_KICAD_CLI, FREEROUTING_KICAD_PYTHON and CAIROSVG_PYTHON (they may include a prefix such as a
# container exec). Skips outputs that already exist.
set -euo pipefail
B="$1"
LABEL="${2:-master-edffcabb}"
REPO="$(cd "$(dirname "$0")/../.." && pwd)"
FIX="${FIXTURES_DIR:-$REPO/scripts/benchmark/fixtures/PCBench}/$B"
SES="${OUTPUTS_DIR:-$REPO/scripts/benchmark/outputs}/$B--unrouted--$LABEL.ses"
DST="${LAYOUTS_DIR:-$REPO/build/pcbench-layouts}/$B"
read -r -a KCLI <<< "${FREEROUTING_KICAD_CLI:-kicad-cli}"
read -r -a KPY <<< "${FREEROUTING_KICAD_PYTHON:-python3}"
read -r -a PY <<< "${CAIROSVG_PYTHON:-python3}"
TOOL="$REPO/scripts/pcbench/kicad_layout_tool.py"
mkdir -p "$DST"
TMP=$(mktemp -d -p "$DST" .tmp.XXXX)
trap 'rm -rf "$TMP"' EXIT

svg2png() {  # in.svg out.png; recolours the overlay: User.1 (open connections) magenta, User.2 (DRC) yellow
  "${PY[@]}" -c "
import cairosvg, sys
s = open(sys.argv[1]).read().replace('#C2C2C2', '#FF30FF').replace('#5994DC', '#FFE000')
cairosvg.svg2png(bytestring=s.encode(), write_to=sys.argv[2], output_width=2400, background_color='#101010')" "$1" "$2"
}

render() {  # board.kicad_pcb out.png extra_layers
  local layers
  layers=$("${KPY[@]}" "$TOOL" layers "$1" 2>/dev/null | tail -1)
  "${KCLI[@]}" pcb export svg --mode-single --page-size-mode 2 --exclude-drawing-sheet --drill-shape-opt 1 \
    --layers "$layers,Edge.Cuts$3" -o "$TMP/r.svg" "$1" >/dev/null 2>&1
  svg2png "$TMP/r.svg" "$2"
}

# KiCad writes .kicad_prl next to boards it opens, so work on a copy and keep the fixture folder clean.
cp "$FIX/processed.kicad_pcb" "$TMP/reference.kicad_pcb"
[ -s "$DST/reference.png" ] || render "$TMP/reference.kicad_pcb" "$DST/reference.png" ""
[ -s "$DST/reference-drc.json" ] || "${KCLI[@]}" pcb drc --format json --units mm -o "$DST/reference-drc.json" "$TMP/reference.kicad_pcb" >/dev/null 2>&1 || true

if [ -s "$SES" ]; then
  if [ ! -s "$DST/master.png" ] || [ "$SES" -nt "$DST/master.png" ]; then
    "${KPY[@]}" "$TOOL" import "$FIX/unrouted.kicad_pcb" "$SES" "$DST/master-routed.kicad_pcb" >/dev/null 2>&1
    "${KCLI[@]}" pcb drc --format json --units mm -o "$DST/master-drc.json" "$DST/master-routed.kicad_pcb" >/dev/null 2>&1 || true
    "${KPY[@]}" "$TOOL" overlay "$DST/master-routed.kicad_pcb" "$DST/master-drc.json" "$TMP/ov.kicad_pcb" >/dev/null 2>&1
    render "$TMP/ov.kicad_pcb" "$DST/master.png" ",User.1,User.2"
    rm -f "$DST"/*.kicad_prl "$DST"/*.kicad_pro
  fi
fi
echo "ok $B"
