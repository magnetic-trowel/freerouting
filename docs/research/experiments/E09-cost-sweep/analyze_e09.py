#!/usr/bin/env python3
"""Analyses an E09 paired campaign: noise floor, main effects per family, decision rule.

Usage: python3 -I analyze_e09.py <campaign>/summary.jsonl <variants.txt> <boards.txt>

Reads one record per (board, variant). Field names are matched loosely so that the script works
with the shared harness output (see the aliases in FIELDS). Runs that did not finish (state other
than COMPLETED) are listed and excluded from every comparison.
"""

import collections
import json
import math
import re
import sys

FIELDS = {
    "board": ("board",),
    "variant": ("variant", "arm", "name"),
    "state": ("state",),
    "unrouted": ("unrouted", "router_unrouted"),
    "drc_open": ("drc_unconnected", "drc_open", "unconnected", "unconnected_items"),
    "drc_viol": ("drc_violations", "drc_viol", "violations_by_type"),
    "vias": ("vias", "via_count"),
    "length": ("wire_length", "length", "trace_length", "wire_length_mm"),
    "cpu": ("cpu", "cpu_s", "cpu_seconds"),
}

FACTOR_FLAGS = {
    "A": "router.scoring.via_costs",
    "B": "router.scoring.start_ripup_costs",
    "C": "router.autorouter.smd_net_via_cost_factor",
    "D": "router.scoring.default_undesired_direction_trace_cost",
    "E": "router.trace_pull_tight_accuracy",
    "F": "router.scoring.plane_via_costs",
    "G": "router.fanout.enabled",
}


def field(rec, key, default=None):
    for name in FIELDS[key]:
        if name in rec and rec[name] is not None:
            return rec[name]
    return default


def board_name(path):
    return re.sub(r"\.dsn$", "", path.strip().split("/")[-1])


def read_families(boards_txt):
    """Family tag from the comment line that follows each board line (see boards.txt)."""
    families = {}
    last = None
    for line in open(boards_txt):
        line = line.strip()
        if line.startswith("fixtures/"):
            last = board_name(line.split()[0])
        elif line.startswith("#") and last and "|" in line:
            families[last] = line.lstrip("# ").split("|")[0].strip()
            last = None
    return families


def read_signs(variants_txt):
    signs = {}
    for line in open(variants_txt):
        m = re.match(r"#\s*(d\d+)\s+ABCDEFG\s+([+-]{7})", line)
        if m:
            signs[m.group(1)] = m.group(2)
    return signs


def violations(rec, clearance=True):
    """Clearance-type violations (clearance, holeClearance, ...) or, with clearance=False, the
    hygiene types (via_dangling, track_dangling, isolated_island_dead_copper, ...)."""
    v = field(rec, "drc_viol", {})
    if not isinstance(v, dict):
        return int(v or 0) if clearance else 0
    return sum(int(n) for t, n in v.items() if ("clearance" in str(t).lower()) == clearance)


def main(argv):
    records = [json.loads(line) for line in open(argv[1]) if line.strip()]
    signs = read_signs(argv[2])
    families = read_families(argv[3])
    runs = {}
    unfinished = []
    for rec in records:
        key = (field(rec, "board"), field(rec, "variant"))
        if field(rec, "state") != "COMPLETED":
            unfinished.append(key)
            continue
        runs[key] = rec
    boards = sorted({b for b, _ in runs})
    if unfinished:
        print("Excluded (not COMPLETED):", ", ".join(f"{b}/{v}" for b, v in sorted(unfinished)))

    def metrics(b, v):
        r = runs.get((b, v))
        if r is None:
            return None
        return {
            "open": int(field(r, "drc_open", 0)) + 0,
            "unrouted": int(field(r, "unrouted", 0) or 0),
            "viol": violations(r),
            "dangling": violations(r, clearance=False),
            "vias": float(field(r, "vias", 0) or 0),
            "length": float(field(r, "length", 0) or 0),
            "cpu": float(field(r, "cpu", 0) or 0),
        }

    # 1. Noise floor from the control and the item-order seed arms.
    noise = {}
    print("\n## Noise floor (control + seed arms)")
    print("board | open (min..max) | vias sd | length sd %")
    for b in boards:
        arms = [metrics(b, v) for v in ("control", "seed1", "seed2", "seed3")]
        arms = [a for a in arms if a]
        if len(arms) < 2:
            continue
        vias = [a["vias"] for a in arms]
        opens = [a["open"] for a in arms]
        lens = [a["length"] for a in arms]
        mean_v = sum(vias) / len(vias)
        sd_v = math.sqrt(sum((x - mean_v) ** 2 for x in vias) / (len(vias) - 1))
        mean_l = sum(lens) / len(lens) or 1.0
        sd_l = math.sqrt(sum((x - mean_l) ** 2 for x in lens) / (len(lens) - 1)) / mean_l * 100
        noise[b] = {"vias_sd": sd_v, "open_range": max(opens) - min(opens)}
        print(f"{b} | {min(opens)}..{max(opens)} | {sd_v:.1f} | {sd_l:.2f}")

    # 2. Main effects. Response per board: open difference to control, log via ratio,
    #    length ratio, CPU ratio. Effect = mean(high) - mean(low) over the 16 design rows.
    print("\n## Main effects (high minus low level), per family and overall")
    fam_of = {b: families.get(b, "?") for b in boards}
    fams = sorted(set(fam_of.values()))
    header = "factor | " + " | ".join(f"{f} d_open / via% " for f in fams) + " | all d_open / via% / len% / cpu%"
    print(header)
    for i, letter in enumerate("ABCDEFG"):
        row = [letter + " " + FACTOR_FLAGS[letter]]
        for group in fams + ["all"]:
            members = [b for b in boards if group == "all" or fam_of[b] == group]
            hi = collections.defaultdict(list)
            lo = collections.defaultdict(list)
            for b in members:
                base = metrics(b, "control")
                if not base:
                    continue
                for v, s in signs.items():
                    m = metrics(b, v)
                    if not m:
                        continue
                    target = hi if s[i] == "+" else lo
                    target["open"].append(m["open"] - base["open"])
                    target["via"].append(math.log((m["vias"] + 1) / (base["vias"] + 1)))
                    target["len"].append(math.log((m["length"] + 1) / (base["length"] + 1)))
                    target["cpu"].append(math.log((m["cpu"] + 1) / (base["cpu"] + 1)))

            def eff(k):
                if not hi[k] or not lo[k]:
                    return float("nan")
                return sum(hi[k]) / len(hi[k]) - sum(lo[k]) / len(lo[k])

            if group == "all":
                row.append(
                    f"{eff('open'):+.2f} / {100 * (math.exp(eff('via')) - 1):+.1f} / "
                    f"{100 * (math.exp(eff('len')) - 1):+.1f} / {100 * (math.exp(eff('cpu')) - 1):+.0f}"
                )
            else:
                row.append(f"{eff('open'):+.2f} / {100 * (math.exp(eff('via')) - 1):+.1f}")
        print(" | ".join(row))

    # 3. Per-variant gate against the control: completion and DRC never worse on any board.
    print("\n## Gate per variant (no board may lose completion or gain violations)")
    print("Gate: per board, reload-DRC unconnected, router unrouted and clearance-type violations")
    print("never above the control; dangling/island counts only as a total (they move with noise).")
    print("variant | boards | open worse | clearance worse | total open | total dangling | "
          "total vias | total cpu s")
    variants = ["control", "seed1", "seed2", "seed3"] + sorted(signs)
    for v in variants:
        worse_open, worse_viol, tot_open, tot_vias, tot_cpu, n = [], [], 0, 0.0, 0.0, 0
        tot_dangling = 0
        for b in boards:
            base, m = metrics(b, "control"), metrics(b, v)
            if not base or not m:
                continue
            n += 1
            tot_open += m["open"]
            tot_dangling += m["dangling"]
            tot_vias += m["vias"]
            tot_cpu += m["cpu"]
            if m["open"] > base["open"] or m["unrouted"] > base["unrouted"]:
                worse_open.append(b)
            if m["viol"] > base["viol"]:
                worse_viol.append(b)
        print(
            f"{v} | {n} | {len(worse_open)} {','.join(worse_open)} | "
            f"{len(worse_viol)} {','.join(worse_viol)} | {tot_open} | {tot_dangling} | "
            f"{tot_vias:.0f} | {tot_cpu:.0f}"
        )


if __name__ == "__main__":
    main(sys.argv)
