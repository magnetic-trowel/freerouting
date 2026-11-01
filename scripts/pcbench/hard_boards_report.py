#!/usr/bin/env python3
"""Write the hard-board report for one version label into scripts/benchmark/results/hard-boards-<label>/.

Inputs: the label's runs in benchmarks.json, Freerouting's result JSONs and .ses files in
scripts/benchmark/outputs/, and (optional) render_layout.sh output in build/pcbench-layouts/.
Output:
  README.md            summary, method comparison, findings, failing-board table, how to reproduce
  LAYOUTS.md           every board with KiCad DRC counts (routed result vs human reference)
  drc-summary.json     per-board KiCad DRC counts plus every open connection KiCad reports
  boards/<board>/      for each failing board: README.md, master.ses, result.json, master.png, reference.png

Example:
  python3 scripts/pcbench/hard_boards_report.py --label master-edffcabb
"""
from __future__ import annotations

import argparse
import collections
import json
import shutil
import statistics
from pathlib import Path
from urllib.parse import quote

REPO = Path(__file__).resolve().parents[2]
TIERS = ["A", "D", "C", "B"]
TIER_NAMES = {"A": "golden canary", "B": "routine", "C": "complex", "D": "diagnostic"}
OLD_LABELS = ["1.9.0", "2.4.1", "2.5.0", "2.6.0-RC1", "2.6.0-RC2", "2.6.0-RC3", "2.6.0-RC4"]
KICAD_ELECTRICAL = ["clearance", "hole_clearance", "shorting_items", "tracks_crossing", "copper_edge_clearance"]
CATEGORY_NOTE = {
    "timeout": "hit the 6 h job budget before the router finished",
    "unrouted": "router finished but left connections open",
    "router violations": "fully routed but the router added clearance violations",
    "pre-existing clearance only": "fully routed and the router added nothing; every violation was already in "
    "the input (the runner calls these CLEAN)",
}
LEGEND = (
    "Magenta lines join the two ends of each connection left open; yellow rings mark KiCad DRC electrical "
    "violations (clearance, hole clearance, shorts, track crossings, copper-to-edge). Copper: F.Cu red, "
    "B.Cu blue, inner layers green and orange."
)


def board_id(r):
    return Path(r["fixture"]["relative_path"]).parent.name


def bench_clean(r):
    """The benchmark's own pass criterion."""
    q, e = r.get("quality") or {}, r.get("exit") or {}
    return (
        e.get("state") == "COMPLETED"
        and (q.get("unrouted_connections") or 0) == 0
        and (q.get("router_introduced_violations") or 0) == 0
    )


def failing(r):
    """Stricter than bench_clean: any clearance violation counts, even one already in the input."""
    return not bench_clean(r) or ((r.get("quality") or {}).get("clearance_violations") or 0) > 0


def category(r):
    """One primary bucket per run, most severe first."""
    q, e = r.get("quality") or {}, r.get("exit") or {}
    if e.get("state") != "COMPLETED":
        return "timeout" if e.get("timed_out") or e.get("state") == "TIMED_OUT" else "crash/other"
    if (q.get("unrouted_connections") or 0) > 0:
        return "unrouted"
    if (q.get("router_introduced_violations") or 0) > 0:
        return "router violations"
    if (q.get("clearance_violations") or 0) > 0:
        return "pre-existing clearance only"
    return "clean"


def fmt_min(sec):
    return "-" if sec is None else f"{sec / 60:.1f} min"


def load_json(p: Path):
    try:
        return json.loads(p.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None


def drc_counts(d):
    if not d:
        return None
    c = collections.Counter(v.get("type") for v in d.get("violations", []))
    c["unconnected"] = len(d.get("unconnected_items", []))
    c["electrical"] = sum(c[t] for t in KICAD_ELECTRICAL)
    return c


def compact_png(src: Path, dst: Path) -> bool:
    """Downscale and palette-reduce a render (~70 KB instead of ~500 KB) so it can live in git."""
    try:
        from PIL import Image
    except ImportError:
        return False
    if not src.exists():
        return False
    im = Image.open(src).convert("RGB")
    w = min(1400, im.width)
    im = im.resize((w, round(im.height * w / im.width)), Image.LANCZOS)
    im.quantize(colors=24).save(dst, optimize=True)
    return True


class Report:
    def __init__(self, args):
        self.label = args.label
        self.out = args.out or REPO / f"scripts/benchmark/results/hard-boards-{self.label}"
        self.layouts = args.layouts
        self.outputs = args.outputs
        bench = load_json(REPO / "scripts/benchmark/results/benchmarks.json")["runs"]
        self.runs = {board_id(r): r for r in bench if (r.get("binary") or {}).get("version_label") == self.label}
        cat = load_json(args.fixtures / "catalog.json")["boards"]
        self.catalog = {b["board_id"]: b for b in cat if b["board_id"] in self.runs}
        self.old = collections.defaultdict(dict)
        for r in bench:
            lbl = (r.get("binary") or {}).get("version_label")
            if lbl in OLD_LABELS and board_id(r) in self.runs:
                self.old[board_id(r)][lbl] = r
        stem = lambda b: self.outputs / f"{b}--unrouted--{self.label}"  # noqa: E731
        self.results = {b: load_json(Path(f"{stem(b)}-result.json")) for b in self.runs}
        self.ses = {b: Path(f"{stem(b)}.ses") for b in self.runs}
        self.drc = {
            b: (load_json(self.layouts / b / "master-drc.json"), load_json(self.layouts / b / "reference-drc.json"))
            for b in self.runs
        }
        self.kd = {b: (drc_counts(m), drc_counts(r)) for b, (m, r) in self.drc.items()}
        order = lambda b: (TIERS.index(self.catalog[b]["tier"]), category(self.runs[b]), b)  # noqa: E731
        self.failing = sorted((b for b, r in self.runs.items() if failing(r)), key=order)

    def board_link(self, b, name="README.md"):
        return f"boards/{quote(b)}/{name}"

    def write(self):
        self.out.mkdir(parents=True, exist_ok=True)
        for b in self.failing:
            self.write_board(b)
        self.write_drc_summary()
        self.write_layouts_index()
        self.write_readme()
        print(f"wrote {self.out.relative_to(REPO)}: {len(self.runs)} boards, {len(self.failing)} failing")

    def write_board(self, b):
        cat, r, res, hist = self.catalog[b], self.runs[b], self.results[b], self.old[b]
        q, e = r["quality"], r["exit"]
        bd, ref = cat.get("board") or {}, cat.get("reference") or {}
        dims = bd.get("dimensions_mm") or {}
        dst = self.out / "boards" / b
        dst.mkdir(parents=True, exist_ok=True)
        if self.ses[b].exists():
            shutil.copy2(self.ses[b], dst / "master.ses")
        if res:
            (dst / "result.json").write_text(json.dumps(res, indent=1) + "\n", encoding="utf-8")
        pics = {n: compact_png(self.layouts / b / f"{n}.png", dst / f"{n}.png") for n in ("master", "reference")}

        L = [
            f"# {b}",
            "",
            f"Tier **{cat['tier']}** ({TIER_NAMES.get(cat['tier'], '?')}). "
            f"Category: **{category(r)}**: {CATEGORY_NOTE.get(category(r), '')}. Freerouting `{self.label}`.",
            "",
            "## Result",
            "",
            "| Metric | Value |",
            "|---|---|",
            f"| Exit state | {e.get('state')}{' (timed out)' if e.get('timed_out') else ''} |",
            f"| Unrouted connections | {q.get('unrouted_connections')} |",
            f"| Clearance violations (total) | {q.get('clearance_violations')} |",
            f"| of which router-introduced | {q.get('router_introduced_violations')} |",
            f"| of which pre-existing in input | {q.get('pre_existing_violations')} "
            f"(unfixable: {q.get('unfixable_clearance_violations')}) |",
            f"| Worst violation depth | {q.get('max_violation_um')} µm |",
            f"| Quality score | {q.get('quality_score')} |",
            f"| Wall clock | {fmt_min(q.get('wall_clock_seconds'))} |",
        ]
        if res:
            conn = (res.get("board_statistics") or {}).get("connections") or {}
            phases = res.get("phases") or {}
            ar = phases.get("autorouter") or {}
            L += [
                f"| Connections (total / still open) | {conn.get('maximum_count')} / {conn.get('incomplete_count')} |",
                f"| Autorouter stop reason | {ar.get('stop_reason')} after {ar.get('passes_completed')} passes |",
                f"| Optimizer | {(phases.get('optimizer') or {}).get('stop_reason')} |",
            ]
            fb = phases.get("fallback") or {}
            if fb.get("attempts"):
                L += [
                    "",
                    "## Router fallback strategies tried",
                    "",
                    "After the default route, the router continues it (`continue`), then retries the board from "
                    f"scratch with each alternative strategy and keeps the best. Chosen: **{fb.get('chosen')}**.",
                    "",
                    "| Strategy | Unrouted | New violations | Score | Stop | Time |",
                    "|---|---|---|---|---|---|",
                ] + [
                    f"| {a.get('strategy')} | {a.get('unrouted')} | {a.get('new_or_changed_violations')} | "
                    f"{a.get('router_score', 0):.1f} | {a.get('stop_reason')} | {fmt_min(a.get('duration_seconds'))} |"
                    for a in fb["attempts"]
                ]
        if any(pics.values()):
            L += ["", "## Layouts", "", LEGEND, ""]
            if pics["master"]:
                L += [f"Routed by `{self.label}`:", "", "![routed](master.png)", ""]
            if pics["reference"]:
                L += ["Human reference layout:", "", "![human reference](reference.png)"]
        km, kr = self.kd[b]
        if km and kr:
            L += [
                "",
                "## KiCad DRC (the board's own rules, stricter than Freerouting's checks)",
                "",
                f"| Check | `{self.label}` | Human reference |",
                "|---|---|---|",
            ] + [f"| {t} | {km[t]} | {kr[t]} |" for t in ["unconnected"] + KICAD_ELECTRICAL]
            items = (self.drc[b][0] or {}).get("unconnected_items", [])
            if items:
                L += ["", "Open connections reported by KiCad:", ""] + [
                    "- " + " ↔ ".join(f"{i['description']} @ ({i['pos']['x']:.2f}, {i['pos']['y']:.2f})"
                                      for i in it.get("items", []) if "pos" in i)
                    for it in items[:40]
                ] + ([f"- … {len(items) - 40} more in `../../drc-summary.json`"] if len(items) > 40 else [])
        L += [
            "",
            "## Board",
            "",
            f"- Source: {cat.get('source')} (author {cat.get('author')}, "
            f"license {cat.get('license_spdx') or cat.get('license')})",
            f"- {bd.get('layers')} layers, {bd.get('nets')} nets, {bd.get('components')} components, "
            f"{dims.get('width')} × {dims.get('height')} mm, {bd.get('zones')} zones",
            f"- Human reference layout: {ref.get('segments')} segments, {ref.get('vias')} vias, "
            f"{ref.get('track_length_mm')} mm of track, {ref.get('kicad_drc_violations')} KiCad DRC violations",
            f"- Fixture (DSN input, placed and reference KiCad boards): "
            f"[`scripts/benchmark/fixtures/PCBench/{b}/`](../../../../fixtures/PCBench/{quote(b)}/)",
            "",
            "## Freerouting versions on this board",
            "",
            "| Version | State | Unrouted | Router violations | Clearance total | Time | Benchmark verdict |",
            "|---|---|---|---|---|---|---|",
        ]
        for lbl in OLD_LABELS + [self.label]:
            h = r if lbl == self.label else hist.get(lbl)
            if not h:
                L.append(f"| {lbl} | not run | | | | | |")
                continue
            hq, he = h.get("quality") or {}, h.get("exit") or {}
            L.append(
                f"| {lbl} | {he.get('state')} | {hq.get('unrouted_connections')} | "
                f"{hq.get('router_introduced_violations')} | {hq.get('clearance_violations')} | "
                f"{fmt_min(hq.get('wall_clock_seconds'))} | {'clean' if bench_clean(h) else 'FAIL'} |"
            )
        L += [
            "",
            "## Files",
            "",
            "- `master.ses`: the router's output session (best result found; partial when connections are open). "
            "Import it into the fixture's `unrouted.kicad_pcb` with `scripts/pcbench/kicad_layout_tool.py import`.",
            "- `result.json`: Freerouting's own result: settings snapshot, per-phase statistics, fallback attempts.",
            "- The runner's record for this board is the `" + r["cache_key"] + "` entry in "
            "`scripts/benchmark/results/benchmarks.json`. Full logs were not kept in git (gitignored, tens of MB each).",
        ]
        (dst / "README.md").write_text("\n".join(L) + "\n", encoding="utf-8")

    def write_drc_summary(self):
        out = {}
        for b in sorted(self.runs):
            m, r = self.drc[b]
            if not (m and r):
                continue
            out[b] = {
                "master": {k: v for k, v in drc_counts(m).items() if k in KICAD_ELECTRICAL + ["unconnected"]},
                "reference": {k: v for k, v in drc_counts(r).items() if k in KICAD_ELECTRICAL + ["unconnected"]},
                "master_unconnected_items": [
                    [{"description": i.get("description"), "pos": i.get("pos")} for i in it.get("items", [])]
                    for it in m.get("unconnected_items", [])
                ],
            }
        (self.out / "drc-summary.json").write_text(json.dumps(out, indent=1) + "\n", encoding="utf-8")

    def write_layouts_index(self):
        L = [
            f"# All {len(self.runs)} boards: `{self.label}` vs human reference",
            "",
            "KiCad columns are DRC counts, routed result / human reference. Pictures are committed for failing "
            "boards (follow the board link); render any board with `scripts/pcbench/render_layout.sh <board_id>`.",
            "",
            "| Board | Tier | Benchmark verdict | Unrouted | KiCad unconnected | KiCad electrical |",
            "|---|---|---|---|---|---|",
        ]
        for b in sorted(self.runs, key=lambda b: (TIERS.index(self.catalog[b]["tier"]), b)):
            r = self.runs[b]
            m, rf = self.kd[b]
            name = f"[{b}]({self.board_link(b)})" if b in self.failing else b
            L.append(
                f"| {name} | {self.catalog[b]['tier']} | {'clean' if bench_clean(r) else 'FAIL'} | "
                f"{r['quality'].get('unrouted_connections')} | {m['unconnected'] if m else '-'} | "
                f"{'%s / %s' % (m['electrical'], rf['electrical']) if m and rf else '-'} |"
            )
        (self.out / "LAYOUTS.md").write_text("\n".join(L) + "\n", encoding="utf-8")

    def write_readme(self):
        runs, catalog, old = self.runs, self.catalog, self.old
        tiers = collections.defaultdict(collections.Counter)
        for b, r in runs.items():
            c = tiers[catalog[b]["tier"]]
            c["boards"] += 1
            c[category(r)] += 1
            c["bench_clean"] += bench_clean(r)
            c["failing"] += failing(r)
        L = [
            f"# PCBench hard boards: Freerouting `{self.label}`",
            "",
            f"The {len(runs)} PCBench boards that Freerouting **2.5.0 or 2.6.0-RC4** failed on, rerun with "
            f"`{self.label}` (magnetic-trowel/freerouting). This folder keeps every board that still fails, with "
            "its routed output, Freerouting's result file and pictures, so the next round of router work can start "
            "from it without rerunning anything. Generated by `scripts/pcbench/hard_boards_report.py`.",
            "",
            "| File | What |",
            "|---|---|",
            "| `README.md` | This summary |",
            "| `LAYOUTS.md` | All boards with KiCad DRC counts, routed vs human reference |",
            "| `boards/<board>/` | Each failing board: `README.md`, `master.ses`, `result.json`, `master.png`, `reference.png` |",
            "| `drc-summary.json` | KiCad DRC counts per board and every open connection KiCad reports |",
            "",
            "## How results are judged",
            "",
            "- **Benchmark clean** (the runner's criterion, also used to choose the boards): exit state COMPLETED, "
            "0 unrouted connections, 0 router-introduced clearance violations.",
            "- **Failing** (stricter): not benchmark clean, or any clearance violation at all, even ones already in "
            "the input. Boards failing only for that reason are *pre-existing clearance only*: not routing failures.",
            "- Categories, most severe first: "
            + "; ".join(f"**{k}**: {v}" for k, v in CATEGORY_NOTE.items())
            + ".",
            "",
            "## Results by tier",
            "",
            "| Tier | Boards | Clean | Failing | Benchmark clean | Timeout | Unrouted | Router violations | "
            "Pre-existing clearance only |",
            "|---|---|---|---|---|---|---|---|---|",
        ]
        tot = collections.Counter()
        for T in TIERS:
            c = tiers[T]
            tot.update(c)
            L.append(
                f"| {T} ({TIER_NAMES[T]}) | {c['boards']} | {c['boards'] - c['failing']} | {c['failing']} | "
                f"{c['bench_clean']} | {c['timeout']} | {c['unrouted']} | {c['router violations']} | "
                f"{c['pre-existing clearance only']} |"
            )
        L.append(
            f"| **All** | {tot['boards']} | {tot['boards'] - tot['failing']} | {tot['failing']} | "
            f"{tot['bench_clean']} | {tot['timeout']} | {tot['unrouted']} | {tot['router violations']} | "
            f"{tot['pre-existing clearance only']} |"
        )

        L += [
            "",
            "## Methods compared",
            "",
            "### Freerouting versions on these boards",
            "",
            "Benchmark-clean boards / boards run. Earlier versions come from `benchmarks.json` (a different "
            "machine, so compare outcomes, not times). The boards were chosen *because* 2.5.0 or RC4 failed, so "
            "older versions look worse here than on the full corpus.",
            "",
            "| Version | " + " | ".join(f"Tier {T}" for T in TIERS) + " | All |",
            "|---|" + "---|" * (len(TIERS) + 1),
        ]
        for lbl in OLD_LABELS + [self.label]:
            cells, ac, an = [], 0, 0
            for T in TIERS:
                rs = [(runs[b] if lbl == self.label else old[b].get(lbl)) for b in runs if catalog[b]["tier"] == T]
                rs = [x for x in rs if x]
                cl = sum(bench_clean(x) for x in rs)
                ac, an = ac + cl, an + len(rs)
                cells.append(f"{cl}/{len(rs)}" if rs else "not run")
            L.append(f"| {lbl} | " + " | ".join(cells) + (f" | {ac}/{an} ({100 * ac / an:.0f}%) |" if an else " | - |"))

        fixed, regressed, still = [], [], []
        for b, r in runs.items():
            rc4 = old[b].get("2.6.0-RC4")
            if not rc4:
                continue
            if bench_clean(r) and not bench_clean(rc4):
                fixed.append(b)
            elif not bench_clean(r) and bench_clean(rc4):
                regressed.append(b)
            elif not bench_clean(r):
                still.append(b)
        L += [
            "",
            f"Against RC4 on the boards both ran: **{len(fixed)} fixed**, **{len(still)} still failing**, "
            f"**{len(regressed)} regressed** (clean in RC4, failing now). Tiers C and D were never run with RC4.",
        ]
        if regressed:
            L.append("Regressions: " + ", ".join(f"[{b}]({self.board_link(b)})" for b in sorted(regressed)) + ".")

        strat = collections.defaultdict(lambda: {"tried": 0, "zero": 0, "best": 0, "chosen": 0, "unrouted": [], "time": []})
        with_fb = 0
        for res in self.results.values():
            fb = ((res or {}).get("phases") or {}).get("fallback") or {}
            att = fb.get("attempts") or []
            if not att:
                continue
            with_fb += 1
            best = min(a.get("unrouted") or 0 for a in att)
            for a in att:
                s = strat[a.get("strategy")]
                s["tried"] += 1
                s["zero"] += (a.get("unrouted") or 0) == 0 and (a.get("new_or_changed_violations") or 0) == 0
                s["best"] += (a.get("unrouted") or 0) == best
                s["unrouted"].append(a.get("unrouted") or 0)
                s["time"].append(a.get("duration_seconds") or 0)
            if fb.get("chosen") in strat:
                strat[fb["chosen"]]["chosen"] += 1
        L += [
            "",
            "### Strategies inside the routing fallback ladder",
            "",
            "When the default route leaves connections open, the router first keeps routing that result "
            "(`continue`), then reroutes from scratch with each alternative (`direct-first, fanout-retry, "
            "short-escape, open-escape, no-fanout`) and keeps the best (`RoutingFallback.java`, "
            f"`RoutingFallbackRunner.java`). Recorded on {with_fb} boards; boards that route cleanly by default have "
            "no ladder. *Fully routed* = 0 unrouted and 0 new violations; *tied best* = lowest unrouted count on "
            "that board.",
            "",
            "| Strategy | Boards tried | Fully routed | Tied best | Chosen | Median unrouted | Mean unrouted | Median time |",
            "|---|---|---|---|---|---|---|---|",
        ] + [
            f"| {n} | {s['tried']} | {s['zero']} | {s['best']} | {s['chosen']} | "
            f"{statistics.median(s['unrouted']):.0f} | {statistics.mean(s['unrouted']):.1f} | "
            f"{fmt_min(statistics.median(s['time']))} |"
            for n, s in sorted(strat.items(), key=lambda kv: -kv[1]["best"])
        ] + [
            "",
            "No single strategy wins everywhere, so choosing per board, or combining strategies, is the obvious lever.",
        ]

        both = {b: v for b, v in self.kd.items() if v[0] and v[1]}
        if both:
            worse = {t: sum(1 for m, r in both.values() if m[t] > r[t]) for t in KICAD_ELECTRICAL}
            clean_worse = sorted(
                b for b, (m, r) in both.items() if bench_clean(runs[b]) and m["electrical"] > r["electrical"]
            )
            L += [
                "",
                "## KiCad DRC finds problems the benchmark metrics miss",
                "",
                "Each routed `.ses` was imported into the placed KiCad board and checked with KiCad 10 DRC against "
                f"the board's own design rules, alongside the human layout. Of {len(both)} boards, the routed result "
                "has **more** violations than the human layout on: "
                + ", ".join(f"{t} {n}" for t, n in worse.items())
                + f". **{len(clean_worse)} benchmark-clean boards** are worse than the human layout under KiCad DRC, "
                "so 'clean' means clean by Freerouting's own checks only.",
                "",
                "Spot-checked causes (real defects in the routed boards, not artifacts of the import):",
                "",
                "- **copper_edge_clearance**: boards often require 0.5 mm, Freerouting used "
                "`copper_to_edge_clearance_um = 250` (CAL430FR_CAL430F: a via 0.33 mm from the edge). The DSN "
                "export omits copper-to-edge clearance (Issue 558, see `scripts/pcbench/README.md`).",
                "- **shorting_items**: tracks through copper text on F.Cu (BMS-bq76940_EvaluationsBoard) and through "
                "no-net pads (ATMEGA328-Motor-Board: GND tracks over an unconnected pad of U5). Neither seems to be an "
                "obstacle in the DSN the router gets.",
                "- **hole_clearance**: Freerouting ran with `hole_clearance_um = 0`, so KiCad's hole-clearance rule is "
                "never checked (sbc_sbc: 199 violations vs 4 in the human layout).",
                "",
                "These look fixable in the DSN export or router settings, without better routing. Judge a better router "
                "by KiCad DRC too: 0 unconnected, electrical violations no worse than the reference.",
                "",
                "Benchmark-clean boards worse than the reference: " + ", ".join(clean_worse) + ".",
            ]

        L += [
            "",
            "## Failing boards",
            "",
            "By tier, then category. Boards with 1-2 open connections are probably the cheapest wins; "
            "*pre-existing clearance only* boards need no routing work. *KiCad elec.* = KiCad DRC electrical "
            "violations, routed / human reference.",
            "",
            "| Board | Tier | Category | Unrouted | Router viol. | Clearance | KiCad elec. | State | Time | "
            "Best fallback | RC4 | 2.5.0 |",
            "|---|---|---|---|---|---|---|---|---|---|---|---|",
        ]
        for b in self.failing:
            r = runs[b]
            q, e = r["quality"], r["exit"]
            fb = ((self.results.get(b) or {}).get("phases") or {}).get("fallback") or {}
            m, rf = self.kd[b]

            def verdict(lbl):
                h = old[b].get(lbl)
                if not h:
                    return "-"
                return "clean" if bench_clean(h) else f"unr {(h.get('quality') or {}).get('unrouted_connections')}"

            L.append(
                f"| [{b}]({self.board_link(b)}) | {catalog[b]['tier']} | {category(r)} | "
                f"{q.get('unrouted_connections')} | {q.get('router_introduced_violations')} | "
                f"{q.get('clearance_violations')} | {'%s / %s' % (m['electrical'], rf['electrical']) if m and rf else '-'} | "
                f"{e.get('state')} | {fmt_min(q.get('wall_clock_seconds'))} | {fb.get('chosen') or '-'} | "
                f"{verdict('2.6.0-RC4')} | {verdict('2.5.0')} |"
            )

        L += [
            "",
            "## How this was produced",
            "",
            "Requirements: JDK 25 with `java` on PATH, **Python 3.12+** for the runner, KiCad 10 (kicad-cli and "
            "Python with pcbnew) and `cairosvg` for the pictures.",
            "",
            "```bash",
            "./gradlew --no-daemon executableJar",
            "python3 scripts/pcbench/make_hard_board_subset.py --out build/pcbench-hard-boards/PCBench",
            f"python3.12 scripts/pcbench/run_corpus_benchmark.py --jar build/libs/freerouting-current-executable.jar \\",
            f"    --fixtures-dir build/pcbench-hard-boards/PCBench --tier All --workers 8 --version-label {self.label}",
            f"python3 scripts/pcbench/collect_hard_board_failures.py --label {self.label} \\",
            "    --fixtures build/pcbench-hard-boards/PCBench --loop 600   # optional, while the run is going",
            f"scripts/pcbench/render_layout.sh <board_id> {self.label}      # once per board",
            f"python3 scripts/pcbench/hard_boards_report.py --label {self.label}",
            "```",
            "",
            "- Selection: boards whose 2.5.0 or 2.6.0-RC4 run had unrouted > 0, router-introduced violations > 0 or "
            "a non-COMPLETED state: 323 board ids, of which 9 are no longer in the catalog "
            "(TMC261-stepstick_TMC261-stepstick-v1, eeg_brainboard_wearable, karabas-nano revA/revB/revC, "
            "kitspace_d20_tri_r0.2/r0.3, mechkeys_lfk_revc, wavegen_waveform-generator), leaving "
            f"{len(runs)}. Tier E is not part of `--tier All`.",
            "- Run: one runner, 8 workers, `--router.max_threads=1`, `-Xmx4g`, the default 6 h per-board budget, "
            "on a 128-thread Linux machine. It took about 17 h; the last hours were a few 1-6 h boards with most "
            "workers idle.",
            "- Pictures and KiCad DRC: `render_layout.sh` on every board (about 35 s each, one core).",
            "",
            "## What didn't work",
            "",
            "- `run_corpus_benchmark.py` needs Python 3.12+: line 1084 nests the same quote type inside an f-string, "
            "a `SyntaxError` on 3.10. It also calls plain `java`, so a JDK must be on PATH.",
            "- The runner's last step runs `powershell ... run-benchmarks.ps1 -ReportOnly` (line 1590), which "
            "raises `FileNotFoundError` on Linux. The run exits 1 after `benchmarks.json` is complete; "
            "`benchmarks.md` and the charts were not regenerated.",
            "- The runner rewrites `benchmarks.json` with `indent=2` (line 1102), doubling the 75 MB single-line file "
            "the repo keeps. The committed file is the original bytes with the new runs appended in compact form.",
            "- `import_specctra_ses.py` creates a `wx.App` and fails without an X display (\"Unable to access the X "
            "Display\"). `kicad_layout_tool.py import` does the same import without wx and works headless.",
            "- Running KiCad DRC or plotting directly on the fixture boards writes a `.kicad_prl` next to each one; "
            "`render_layout.sh` now works on a temporary copy.",
            "- Five failing boards wrote 2.6-12 GB logs (33 GB in total), over 99.8% of it the same "
            "`Polyline: must contain at least 2 different points` warning "
            "(`geometry/planar/Polyline.java:36`), with no net or location. These are among the slowest "
            "boards, so the degenerate polylines are worth investigating as a routing lead. The runner now "
            "collapses repeated messages when each board finishes (`compact_log.py`, `--keep-raw-logs` to "
            "opt out).",
            "- A single N-worker runner leaves workers idle on the long tail. The boards are independent: next "
            "time, run one job per board (1 core, ~5 GB) or several runners on disjoint subsets in separate "
            "clones (the runner takes a lock per clone).",
        ]
        (self.out / "README.md").write_text("\n".join(L) + "\n", encoding="utf-8")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--label", default="master-edffcabb")
    ap.add_argument("--fixtures", type=Path, default=REPO / "scripts/benchmark/fixtures/PCBench")
    ap.add_argument("--outputs", type=Path, default=REPO / "scripts/benchmark/outputs")
    ap.add_argument("--layouts", type=Path, default=REPO / "build/pcbench-layouts")
    ap.add_argument("--out", type=Path, default=None)
    Report(ap.parse_args()).write()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
