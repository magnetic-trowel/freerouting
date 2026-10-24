#!/usr/bin/env python3
"""Writes the pre-registered E10 hold-out split from features.jsonl (deterministic).

Usage: python3 -I make_split.py <features.jsonl> > split.json

Groups are design sources (KiCad further split by layer class and SMD share) (they share CAD exporter, design style and often the same author), so
leave-one-group-out tests transfer to an unseen source. The hold-out is every fourth board by a
stable hash of its name within each group, about 25 %, never looked at while fitting.
"""

import hashlib
import json
import sys


def group(r):
    name = r["board"]
    host = r.get("host_cad", "").lower()
    if name.startswith("Issue508-DAC2020"):
        return "dac2020"
    if name.startswith("PCBench"):
        return "pcbench"
    if "kicad" in host or "pcbnew" in host:
        # KiCad is most of the corpus: split it by layer class and SMD share.
        cls = layer_class(r)
        if cls == "2L":
            cls += "-smd" if r["smd_pin_fraction"] >= 0.6 else "-mix"
        return "kicad-" + cls
    if r.get("unit") == "mil" or "easyeda" in host:
        return "easyeda-mil"
    return "other"


def layer_class(r):
    n = r["signal_layers"]
    return "1L" if n <= 1 else ("2L" if n == 2 else "ML")


def main(path):
    rows = [json.loads(line) for line in open(path)]
    rows = [r for r in rows if "error" not in r and r["pins"] >= 10]
    holdout = []
    for g in sorted({group(r) for r in rows}):
        members = sorted(
            (r for r in rows if group(r) == g),
            key=lambda r: hashlib.sha1(r["board"].encode()).hexdigest(),
        )
        holdout += [r["board"] for k, r in enumerate(members) if k % 4 == 0]
    print(json.dumps({"holdout": sorted(holdout), "groups": {r["board"]: group(r) for r in rows}},
                     indent=1))


if __name__ == "__main__":
    main(sys.argv[1])
