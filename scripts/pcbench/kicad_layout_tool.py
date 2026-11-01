"""KiCad-side helpers for render_layout.sh. Run with KiCad's Python (needs pcbnew).

  import  <base.kicad_pcb> <in.ses> <out.kicad_pcb>   import a Specctra session into the placed board
  overlay <in.kicad_pcb> <drc.json> <out.kicad_pcb>   draw DRC problems on User.1 (open connections) and User.2 (clearance)
  layers  <in.kicad_pcb>                              print the enabled copper layer names, comma separated

Unlike import_specctra_ses.py this never creates a wx.App, so it works without an X display
(pcbnew.ImportSpecctraSES succeeds headless on KiCad 10.0.7).
"""
import json
import sys

import pcbnew

# DRC types that mean real electrical trouble; the cosmetic ones (silk, courtyard, mask) are left off the picture.
ELECTRICAL = {"clearance", "shorting_items", "hole_clearance", "tracks_crossing", "copper_edge_clearance",
              "track_dangling", "via_dangling", "hole_to_hole", "drill_out_of_range"}


def mm(v):
    return pcbnew.FromMM(v)


def add_line(board, layer, a, b, width):
    s = pcbnew.PCB_SHAPE(board)
    s.SetShape(pcbnew.SHAPE_T_SEGMENT)
    s.SetStart(pcbnew.VECTOR2I(mm(a["x"]), mm(a["y"])))
    s.SetEnd(pcbnew.VECTOR2I(mm(b["x"]), mm(b["y"])))
    s.SetLayer(layer)
    s.SetWidth(mm(width))
    board.Add(s)


def add_ring(board, layer, p, radius, width):
    s = pcbnew.PCB_SHAPE(board)
    s.SetShape(pcbnew.SHAPE_T_CIRCLE)
    s.SetCenter(pcbnew.VECTOR2I(mm(p["x"]), mm(p["y"])))
    s.SetEnd(pcbnew.VECTOR2I(mm(p["x"] + radius), mm(p["y"])))
    s.SetLayer(layer)
    s.SetWidth(mm(width))
    board.Add(s)


def main():
    cmd, args = sys.argv[1], sys.argv[2:]
    if cmd == "import":
        board = pcbnew.LoadBoard(args[0])
        if not pcbnew.ImportSpecctraSES(board, args[1]):
            sys.exit("ImportSpecctraSES failed")
        board.Save(args[2])
    elif cmd == "overlay":
        board = pcbnew.LoadBoard(args[0])
        drc = json.load(open(args[1]))
        u1, u2 = board.GetLayerID("User.1"), board.GetLayerID("User.2")
        enabled = board.GetEnabledLayers()
        enabled.AddLayer(u1)
        enabled.AddLayer(u2)
        board.SetEnabledLayers(enabled)
        for item in drc.get("unconnected_items", []):
            pts = [i["pos"] for i in item.get("items", []) if "pos" in i]
            if len(pts) >= 2:
                add_line(board, u1, pts[0], pts[1], 0.25)
            for p in pts:
                add_ring(board, u1, p, 0.6, 0.2)
        for v in drc.get("violations", []):
            if v.get("type") in ELECTRICAL:
                for i in v.get("items", [])[:1]:
                    if "pos" in i:
                        add_ring(board, u2, i["pos"], 0.5, 0.15)
        board.Save(args[2])
    elif cmd == "layers":
        board = pcbnew.LoadBoard(args[0])
        print(",".join(board.GetLayerName(l) for l in board.GetEnabledLayers().CuStack()))
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main()
