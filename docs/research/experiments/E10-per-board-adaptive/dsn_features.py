#!/usr/bin/env python3
"""Deterministic board features from a Specctra DSN file (E10 per-board adaptive parameters).

Usage: python3 -I dsn_features.py <board.dsn> [<board.dsn> ...] > features.jsonl

One JSON object per board. Only the DSN is read (no routing), so the features are available
before routing starts and could later be computed inside Freerouting from the loaded board.
All lengths are in millimetres, areas in mm^2.
"""

import json
import math
import os
import sys


def tokenize(text):
    """Tokenizes a DSN s-expression, honouring (string_quote ") and quoted tokens."""
    tokens = []
    i = 0
    n = len(text)
    quote = '"'
    while i < n:
        c = text[i]
        if c in " \t\r\n":
            i += 1
        elif c == "(" or c == ")":
            tokens.append(c)
            i += 1
        elif c == quote:
            j = text.find(quote, i + 1)
            if j < 0:
                j = n
            tokens.append(text[i + 1 : j])
            i = j + 1
        else:
            j = i
            while j < n and text[j] not in " \t\r\n()":
                j += 1
            tok = text[i:j]
            tokens.append(tok)
            i = j
            # "(string_quote ")" : the quote character itself is the next token.
            if tok == "string_quote":
                while i < n and text[i] in " \t":
                    i += 1
                if i < n and text[i] != ")":
                    quote = text[i]
                    tokens.append(quote)
                    i += 1
    return tokens


def parse(tokens):
    stack = [[]]
    for t in tokens:
        if t == "(":
            stack.append([])
        elif t == ")":
            if len(stack) > 1:
                done = stack.pop()
                stack[-1].append(done)
        else:
            stack[-1].append(t)
    while len(stack) > 1:
        done = stack.pop()
        stack[-1].append(done)
    return stack[0][0] if stack[0] else []


def children(node, name):
    return [c for c in node if isinstance(c, list) and c and c[0] == name]


def child(node, name):
    found = children(node, name)
    return found[0] if found else None


def num(x, default=0.0):
    try:
        return float(x)
    except (TypeError, ValueError):
        return default


def polygon_area(points):
    area = 0.0
    for k in range(len(points)):
        x1, y1 = points[k]
        x2, y2 = points[(k + 1) % len(points)]
        area += x1 * y2 - x2 * y1
    return abs(area) / 2.0


def mst_length(points):
    """Prim's algorithm, O(n^2); fine for nets up to a few thousand pins."""
    n = len(points)
    if n < 2:
        return 0.0
    in_tree = [False] * n
    dist = [math.inf] * n
    dist[0] = 0.0
    total = 0.0
    for _ in range(n):
        u = -1
        best = math.inf
        for v in range(n):
            if not in_tree[v] and dist[v] < best:
                best = dist[v]
                u = v
        if u < 0:
            break
        in_tree[u] = True
        total += best
        ux, uy = points[u]
        for v in range(n):
            if not in_tree[v]:
                d = math.hypot(points[v][0] - ux, points[v][1] - uy)
                if d < dist[v]:
                    dist[v] = d
    return total


def features(path):
    with open(path, "r", encoding="utf-8", errors="replace") as f:
        root = parse(tokenize(f.read()))

    # Units: coordinates are in (unit ...) (default: resolution unit). Convert to mm.
    unit_scale = {"um": 0.001, "mm": 1.0, "mil": 0.0254, "inch": 25.4, "cm": 10.0}
    res = child(root, "resolution")
    unit_node = child(root, "unit")
    unit_name = (unit_node[1] if unit_node else (res[1] if res else "mil")).lower()
    to_mm = unit_scale.get(unit_name, 0.001)
    resolution_per_unit = num(res[2], 1.0) if res and len(res) > 2 else 1.0
    resolution_unit_mm = unit_scale.get(res[1].lower(), 0.001) if res else 0.0254

    parser_scope = child(root, "parser") or []
    host = child(parser_scope, "host_cad")
    host_cad = " ".join(v for v in host[1:] if not isinstance(v, list)) if host else ""

    structure = child(root, "structure") or []
    layers = children(structure, "layer")
    plane_layers = set()
    for plane in children(structure, "plane"):
        for shape in plane[2:]:
            if isinstance(shape, list) and len(shape) > 1:
                plane_layers.add(shape[1])
    signal_layers = []
    power_layers = []
    for layer in layers:
        layer_type = child(layer, "type")
        kind = layer_type[1] if layer_type and len(layer_type) > 1 else "signal"
        # Freerouting routes a "power" layer that carries no plane as a signal layer.
        is_power = kind == "power" and layer[1] in plane_layers
        (power_layers if is_power else signal_layers).append(layer[1])
    layer_names = [layer[1] for layer in layers]

    # Board outline: largest closed path/rect of (boundary ...) scopes.
    outline_area = 0.0
    bbox = None
    for boundary in children(structure, "boundary"):
        for shape in boundary[1:]:
            if not isinstance(shape, list):
                continue
            if shape[0] in ("path", "polygon"):
                coords = [num(v) for v in shape[3:] if not isinstance(v, list)]
                pts = [
                    (coords[k] * to_mm, coords[k + 1] * to_mm)
                    for k in range(0, len(coords) - 1, 2)
                ]
            elif shape[0] == "rect":
                x1, y1, x2, y2 = (num(v) * to_mm for v in shape[2:6])
                pts = [(x1, y1), (x2, y1), (x2, y2), (x1, y2)]
            else:
                continue
            if len(pts) < 3:
                continue
            a = polygon_area(pts)
            if a > outline_area:
                outline_area = a
                xs = [p[0] for p in pts]
                ys = [p[1] for p in pts]
                bbox = (min(xs), min(ys), max(xs), max(ys))
    width = (bbox[2] - bbox[0]) if bbox else 0.0
    height = (bbox[3] - bbox[1]) if bbox else 0.0

    # Rules: default trace width and clearance.
    # (rule ...) may appear once with several entries (KiCad) or once per entry (EasyEDA,
    # which also spells clearance as "clear").
    trace_width = 0.0
    clearance = 0.0
    for rule in children(structure, "rule"):
        width_rule = child(rule, "width")
        if width_rule and trace_width == 0.0:
            trace_width = num(width_rule[1]) * to_mm
        for cl in children(rule, "clearance") + children(rule, "clear"):
            if len(cl) == 2 and clearance == 0.0:
                clearance = num(cl[1]) * to_mm
    plane_scopes = children(structure, "plane")

    # Library: padstack layer spans and image pin offsets.
    library = child(root, "library") or []
    padstack_layers = {}
    padstack_size = {}
    for ps in children(library, "padstack"):
        name = ps[1]
        lays = set()
        size = 0.0
        for shape in children(ps, "shape"):
            for s in shape[1:]:
                if isinstance(s, list) and len(s) > 1:
                    lays.add(s[1])
                    if s[0] == "circle" and len(s) > 2:
                        size = max(size, num(s[2]) * to_mm)
                    elif s[0] == "rect" and len(s) > 5:
                        size = max(
                            size,
                            abs(num(s[4]) - num(s[2])) * to_mm,
                            abs(num(s[5]) - num(s[3])) * to_mm,
                        )
        padstack_layers[name] = lays
        padstack_size[name] = size
    images = {}
    for image in children(library, "image"):
        pins = {}
        for pin in children(image, "pin"):
            # (pin padstack [(rotate a)] pin_id x y)
            rest = [v for v in pin[2:] if not isinstance(v, list)]
            if len(rest) >= 3:
                pins[rest[0]] = (pin[1], num(rest[1]) * to_mm, num(rest[2]) * to_mm)
        images[image[1]] = pins

    def is_smd(padstack):
        lays = padstack_layers.get(padstack, set())
        if "signal" in lays:
            return False
        return len(lays) == 1

    # Placement: component reference -> (image, x, y, side, rotation).
    placement = child(root, "placement") or []
    comp_pos = {}
    for comp in children(placement, "component"):
        image = comp[1]
        for place in children(comp, "place"):
            vals = [v for v in place[1:] if not isinstance(v, list)]
            if len(vals) >= 5:
                comp_pos[vals[0]] = (
                    image,
                    num(vals[1]) * to_mm,
                    num(vals[2]) * to_mm,
                    vals[3],
                    num(vals[4]),
                )

    def pin_location(pin_ref):
        comp_name, _, pin_id = pin_ref.rpartition("-")
        if comp_name not in comp_pos:
            return None
        image, cx, cy, side, rot = comp_pos[comp_name]
        pin = images.get(image, {}).get(pin_id)
        if pin is None:
            return None
        padstack, px, py = pin
        if side == "back":
            px = -px
        a = math.radians(rot)
        x = cx + px * math.cos(a) - py * math.sin(a)
        y = cy + px * math.sin(a) + py * math.cos(a)
        return padstack, x, y

    network = child(root, "network") or []
    nets = children(network, "net")
    net_count = 0
    pin_count = 0
    smd_pins = 0
    connections = 0
    pure_smd_nets = 0
    max_net_pins = 0
    airline_total = 0.0
    airline_max = 0.0
    plane_net_names = {p[1] for p in plane_scopes if len(p) > 1}
    for net in nets:
        pins_node = child(net, "pins")
        refs = [r for r in (pins_node[1:] if pins_node else []) if not isinstance(r, list)]
        if len(refs) < 1:
            continue
        net_count += 1
        pin_count += len(refs)
        max_net_pins = max(max_net_pins, len(refs))
        located = [pin_location(r) for r in refs]
        located = [p for p in located if p is not None]
        smd_here = sum(1 for p in located if is_smd(p[0]))
        smd_pins += smd_here
        if located and smd_here == len(located):
            pure_smd_nets += 1
        if len(refs) >= 2:
            connections += len(refs) - 1
            if net[1] not in plane_net_names:
                length = mst_length([(p[1], p[2]) for p in located])
                airline_total += length
                airline_max = max(airline_max, length)

    signal_layer_count = max(1, len(signal_layers))
    pitch = max(trace_width + clearance, 1e-3)
    routing_capacity = outline_area * signal_layer_count / pitch if outline_area else 0.0
    return {
        "board": os.path.splitext(os.path.basename(path))[0],
        "host_cad": host_cad,
        "unit": unit_name,
        "resolution_per_unit": resolution_per_unit,
        "layers": len(layer_names),
        "signal_layers": len(signal_layers),
        "power_layers": len(power_layers),
        "plane_scopes": len(plane_scopes),
        "components": len(comp_pos),
        "nets": net_count,
        "pins": pin_count,
        "connections": connections,
        "max_net_pins": max_net_pins,
        "smd_pin_fraction": round(smd_pins / pin_count, 4) if pin_count else 0.0,
        "pure_smd_net_fraction": round(pure_smd_nets / net_count, 4) if net_count else 0.0,
        "board_area_mm2": round(outline_area, 2),
        "aspect_ratio": round(max(width, height) / min(width, height), 3)
        if min(width, height) > 0
        else 0.0,
        "area_per_pin_mm2": round(outline_area / pin_count, 3) if pin_count else 0.0,
        "pins_per_cm2": round(pin_count / (outline_area / 100.0), 3) if outline_area else 0.0,
        "trace_width_mm": round(trace_width, 4),
        "clearance_mm": round(clearance, 4),
        "airline_total_mm": round(airline_total, 1),
        "airline_max_mm": round(airline_max, 1),
        # Demand / supply: total airline length over the track length that fits on the
        # signal layers at one track pitch. Above ~0.3 boards tend to need rip-up.
        "routing_demand_ratio": round(airline_total / routing_capacity, 4)
        if routing_capacity
        else 0.0,
        # Pull-tight accuracy is in board units: 500 units in mm on this board.
        "pull_tight_500_units_mm": round(500 / resolution_per_unit * resolution_unit_mm, 5),
    }


def main(argv):
    for path in argv[1:]:
        try:
            print(json.dumps(features(path), sort_keys=False))
        except Exception as e:  # noqa: BLE001 - report and continue with the next board
            print(json.dumps({"board": os.path.basename(path), "error": str(e)}))


if __name__ == "__main__":
    main(sys.argv)
