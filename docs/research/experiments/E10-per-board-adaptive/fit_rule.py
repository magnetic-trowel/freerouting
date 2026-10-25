#!/usr/bin/env python3
"""Fits and validates a deterministic per-board parameter rule for E10, without overfitting.

Usage: python3 -I fit_rule.py <features.jsonl> <campaign>/summary.jsonl <split.json>

- features.jsonl: output of dsn_features.py for every board of the campaign.
- summary.jsonl: E10 campaign (every board routed with every candidate setting; the harness format
  is matched loosely, see FIELDS).
- split.json: {"holdout": [...boards never used for fitting...], "groups": {board: group}};
  written once before the campaign is analysed (see README, "Validation").

Loss per (board, setting), lexicographic and expressed as one number so that it can be averaged:
  1e6 * reload-DRC unconnected + 1e5 * reload-DRC violations + 100 * vias / vias(control)
  + 10 * length / length(control) + cpu / cpu(control)
Rule class: a depth-1 or depth-2 threshold tree on the pre-registered features, each leaf picks one
candidate setting. At most 3 leaves. A rule is fitted by exhaustive search over the feature
quantiles (deciles) on the fitting boards only.
"""

import itertools
import json
import random
import sys

FEATURES = [
    "signal_layers",
    "pins",
    "nets",
    "smd_pin_fraction",
    "pure_smd_net_fraction",
    "area_per_pin_mm2",
    "pins_per_cm2",
    "routing_demand_ratio",
    "aspect_ratio",
    "plane_scopes",
]
FIELDS = {
    "board": ("board",),
    "variant": ("variant", "arm", "name"),
    "state": ("state",),
    "open": ("drc_unconnected", "drc_open", "unconnected"),
    "viol": ("drc_violations", "drc_viol"),
    "vias": ("vias", "via_count"),
    "length": ("wire_length", "length", "trace_length"),
    "cpu": ("cpu", "cpu_s", "cpu_seconds"),
}
SEED = 20261024
PERMUTATIONS = 200


def field(rec, key, default=0):
    for name in FIELDS[key]:
        if rec.get(name) is not None:
            return rec[name]
    return default


def load(features_path, summary_path):
    feats = {}
    for line in open(features_path):
        r = json.loads(line)
        if "error" not in r:
            feats[r["board"]] = r
    raw = {}
    for line in open(summary_path):
        if not line.strip():
            continue
        r = json.loads(line)
        if field(r, "state", "") != "COMPLETED":
            continue
        viol = field(r, "viol", 0)
        viol = sum(viol.values()) if isinstance(viol, dict) else int(viol)
        raw[(field(r, "board"), field(r, "variant"))] = (
            int(field(r, "open")),
            viol,
            float(field(r, "vias")),
            float(field(r, "length")),
            float(field(r, "cpu")),
        )
    settings = sorted({v for _, v in raw})
    loss = {}
    for b in feats:
        base = raw.get((b, "control"))
        if base is None or any((b, s) not in raw for s in settings):
            continue  # only boards where every setting finished
        for s in settings:
            o, v, vias, length, cpu = raw[(b, s)]
            loss[(b, s)] = (
                1e6 * o
                + 1e5 * v
                + 100 * (vias + 1) / (base[2] + 1)
                + 10 * (length + 1) / (base[3] + 1)
                + (cpu + 1) / (base[4] + 1)
            )
    boards = sorted({b for b, _ in loss})
    return feats, settings, loss, boards


def best_global(boards, settings, loss):
    return min(settings, key=lambda s: sum(loss[(b, s)] for b in boards))


def thresholds(boards, feats, f):
    values = sorted(feats[b][f] for b in boards)
    qs = {values[min(len(values) - 1, int(len(values) * q / 10))] for q in range(1, 10)}
    return sorted(qs)


def predict(rule, feat):
    kind = rule[0]
    if kind == "const":
        return rule[1]
    if kind == "split":
        _, f, t, lo, hi = rule
        return lo if feat[f] <= t else hi
    _, f, t, lo, (f2, t2, a, b) = rule  # depth 2 on the high side: 3 leaves
    if feat[f] <= t:
        return lo
    return a if feat[f2] <= t2 else b


def total(rule, boards, feats, loss):
    return sum(loss[(b, predict(rule, feats[b]))] for b in boards)


def fit(boards, feats, settings, loss):
    best = ("const", best_global(boards, settings, loss))
    best_loss = total(best, boards, feats, loss)
    for f in FEATURES:
        for t in thresholds(boards, feats, f):
            for lo, hi in itertools.product(settings, repeat=2):
                if lo == hi:
                    continue
                rule = ("split", f, t, lo, hi)
                value = total(rule, boards, feats, loss)
                if value < best_loss:
                    best, best_loss = rule, value
    if best[0] == "split":
        _, f, t, lo, _ = best
        high = [b for b in boards if feats[b][f] > t]
        for f2 in FEATURES:
            for t2 in thresholds(high, feats, f2) if high else []:
                for a, b2 in itertools.product(settings, repeat=2):
                    if a == b2:
                        continue
                    rule = ("split2", f, t, lo, (f2, t2, a, b2))
                    value = total(rule, boards, feats, loss)
                    # A third leaf must pay for itself: 1 % of the loss at least.
                    if value < best_loss * 0.99:
                        best, best_loss = rule, value
    return best


def main(argv):
    feats, settings, loss, boards = load(argv[1], argv[2])
    split = json.load(open(argv[3]))
    holdout = [b for b in split["holdout"] if b in boards]
    fitting = [b for b in boards if b not in holdout]
    groups = split.get("groups", {})
    print(f"{len(boards)} boards with every setting finished; fitting {len(fitting)}, "
          f"hold-out {len(holdout)}; settings: {', '.join(settings)}")

    # Leave-one-group-out cross-validation on the fitting boards.
    print("\n## Leave-one-group-out on the fitting boards")
    gains = []
    for g in sorted({groups.get(b, "?") for b in fitting}):
        test = [b for b in fitting if groups.get(b, "?") == g]
        train = [b for b in fitting if groups.get(b, "?") != g]
        if not test or not train:
            continue
        rule = fit(train, feats, settings, loss)
        glob = ("const", best_global(train, settings, loss))
        gain = total(glob, test, feats, loss) - total(rule, test, feats, loss)
        gains.append(gain)
        print(f"group {g}: {len(test)} boards, rule {rule}, gain over global {gain:+.1f}")

    # Final rule on all fitting boards, evaluated once on the hold-out boards.
    rule = fit(fitting, feats, settings, loss)
    glob = ("const", best_global(fitting, settings, loss))
    oracle = sum(min(loss[(b, s)] for s in settings) for b in holdout)
    print("\n## Final rule:", rule)
    print("global best setting:", glob[1])
    if holdout:
        r_loss = total(rule, holdout, feats, loss)
        g_loss = total(glob, holdout, feats, loss)
        c_loss = total(("const", "control"), holdout, feats, loss)
        print(f"hold-out loss: rule {r_loss:.1f}, global {g_loss:.1f}, control {c_loss:.1f}, "
              f"oracle {oracle:.1f}")
        worse = [b for b in holdout
                 if loss[(b, predict(rule, feats[b]))] >= 1e5 + loss[(b, "control")]]
        print("hold-out boards where the rule loses completion or adds violations:", worse)

    # Permutation test: features shuffled across fitting boards; the real rule's in-sample gain
    # must beat the 95th percentile of the shuffled gains.
    rng = random.Random(SEED)
    real_gain = total(glob, fitting, feats, loss) - total(rule, fitting, feats, loss)
    shuffled = []
    for _ in range(PERMUTATIONS):
        perm = fitting[:]
        rng.shuffle(perm)
        fake = {b: feats[p] for b, p in zip(fitting, perm)}
        r = fit(fitting, fake, settings, loss)
        shuffled.append(total(glob, fitting, fake, loss) - total(r, fitting, fake, loss))
    shuffled.sort()
    p95 = shuffled[int(0.95 * len(shuffled)) - 1]
    print(f"\nin-sample gain {real_gain:.1f}; permutation 95th percentile {p95:.1f}; "
          f"{'PASS' if real_gain > p95 else 'FAIL'}")


if __name__ == "__main__":
    main(sys.argv)
