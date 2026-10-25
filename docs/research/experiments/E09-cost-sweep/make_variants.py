#!/usr/bin/env python3
"""Generates variants.txt for E09 (2^(7-3) resolution IV screening design plus control arms).

Usage: python3 -I make_variants.py > variants.txt

Design: base factors A, B, C, D in standard (Yates) order, A varying fastest; generators
E = ABC, F = BCD, G = ACD. Defining relation I = ABCE = BCDF = ACDG = ADEF = BDEG = ABFG = CEFG:
every word has length 4, so main effects are aliased only with three-factor interactions and
two-factor interactions are aliased in groups of three (resolution IV).
Low and high levels of the numeric factors are symmetric around the current default on a log
scale, so the control (all defaults) is the design's centre point for A..F.
"""

FACTORS = [
    # (letter, flag, low, high, default)
    ("A", "--router.scoring.via_costs", "25", "100", "50"),
    ("B", "--router.scoring.start_ripup_costs", "50", "200", "100"),
    ("C", "--router.autorouter.smd_net_via_cost_factor", "0.03", "0.3", "0.1"),
    ("D", "--router.scoring.default_undesired_direction_trace_cost", "0.5", "2.0", "1.0"),
    ("E", "--router.trace_pull_tight_accuracy", "160", "1600", "500"),
    ("F", "--router.scoring.plane_via_costs", "2", "12", "5"),
    ("G", "--router.fanout.enabled", "false", "true", "true"),
]

# Seeds for the noise arms: same settings as the control, only the autorouter item order from
# pass 2 on is permuted (AutoroutePassRunner.reorderSingleThreadItems). Their spread is the
# chaotic noise floor every effect is compared against.
NOISE_SEEDS = [1, 2, 3]


def design_rows():
    rows = []
    for k in range(16):
        a = 1 if k & 1 else -1
        b = 1 if k & 2 else -1
        c = 1 if k & 4 else -1
        d = 1 if k & 8 else -1
        e = a * b * c
        f = b * c * d
        g = a * c * d
        rows.append((a, b, c, d, e, f, g))
    return rows


def main():
    print("# E09 cost sweep: <name> <jar> <flags>; first line is the control (current defaults).")
    print("# Factor levels (low/high, default): " + "; ".join(
        f"{x[0]}={x[1].lstrip('-')} {x[2]}/{x[3]} ({x[4]})" for x in FACTORS))
    print("control head")
    for seed in NOISE_SEEDS:
        print(f"seed{seed} head --router.autorouter.item_order_seed={seed}")
    for k, row in enumerate(design_rows(), start=1):
        signs = "".join("+" if s > 0 else "-" for s in row)
        flags = " ".join(
            f"{flag}={high if s > 0 else low}"
            for (letter, flag, low, high, default), s in zip(FACTORS, row)
        )
        print(f"# d{k:02d} ABCDEFG {signs}")
        print(f"d{k:02d} head {flags}")


if __name__ == "__main__":
    main()
