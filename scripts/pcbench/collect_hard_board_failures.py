#!/usr/bin/env python3
"""Snapshot failing runs of one version label while a corpus benchmark is still running.

For every run of --label with unrouted connections, router-introduced violations, any clearance violation,
or a non-COMPLETED state, hard-link the input unrouted.dsn, the output .ses, the log and the result JSON into
<out>/<board>/ (copied when linking is not possible), write the run record there, and rewrite <out>/INDEX.md
(board, tier, reason). Hard links keep one copy on disk. Reads benchmarks.json plus run-journal.jsonl, so
results show up before the runner rewrites benchmarks.json.

Example (every 10 minutes while the runner works):
  python3 scripts/pcbench/collect_hard_board_failures.py --label master-edffcabb \\
      --fixtures build/pcbench-hard-boards/PCBench --out build/pcbench-failures --loop 600
"""
from __future__ import annotations

import argparse
import json
import os
import shutil
import time
from datetime import datetime
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
RESULTS = REPO / "scripts/benchmark/results"


def load_runs(label: str) -> list[dict]:
    """Latest record per cache_key for one version label; the journal wins over benchmarks.json."""
    runs: dict[str, dict] = {}
    try:
        for r in json.loads((RESULTS / "benchmarks.json").read_text(encoding="utf-8")).get("runs", []):
            if r.get("cache_key"):
                runs[r["cache_key"]] = r
    except (OSError, ValueError):
        pass  # mid-swap; the journal has the same records
    journal = RESULTS / "run-journal.jsonl"
    if journal.exists():
        for line in journal.read_text(encoding="utf-8", errors="replace").splitlines():
            try:
                r = json.loads(line)
            except ValueError:
                continue
            if r.get("cache_key"):
                runs[r["cache_key"]] = r
    return [r for r in runs.values() if (r.get("binary") or {}).get("version_label") == label]


def reasons(r: dict) -> list[str]:
    q, e = r.get("quality") or {}, r.get("exit") or {}
    out = []
    if (q.get("unrouted_connections") or 0) > 0:
        out.append(f"unrouted={q['unrouted_connections']}")
    if (q.get("router_introduced_violations") or 0) > 0:
        out.append(f"router_violations={q['router_introduced_violations']}")
    if (q.get("clearance_violations") or 0) > 0:
        out.append(f"clearance={q['clearance_violations']}")
    if e.get("state") != "COMPLETED":
        out.append(f"state={e.get('state')}" + (" (timeout)" if e.get("timed_out") else ""))
    return out


def resolve(rel: str | None) -> Path | None:
    """Run records store paths relative to scripts/ (older ones relative to the repo root)."""
    if not rel:
        return None
    for base in (REPO / "scripts", REPO):
        if (base / rel).exists():
            return base / rel
    return None


def link_or_copy(src: Path, dst: Path) -> None:
    """Hard-link src to dst so the snapshot costs no extra space; copy when linking is not possible."""
    if dst.exists():
        if os.path.samefile(src, dst):
            return
        dst.unlink()
    try:
        os.link(src, dst)
    except OSError:
        shutil.copy2(src, dst)


def collect(label: str, fixtures: Path, out: Path) -> tuple[int, int]:
    runs = load_runs(label)
    rows = []
    for r in sorted(runs, key=lambda r: ((r.get("fixture") or {}).get("tier") or "?", r["cache_key"])):
        why = reasons(r)
        if not why:
            continue
        board = Path(r["fixture"]["relative_path"]).parent.name
        dest = out / board
        dest.mkdir(parents=True, exist_ok=True)
        if (fixtures / board / "unrouted.dsn").exists():
            link_or_copy((fixtures / board / "unrouted.dsn").resolve(), dest / "unrouted.dsn")
        for key in ("output_file", "log_file", "result_json"):
            src = resolve(r.get(key))
            if src:
                link_or_copy(src, dest / src.name)
        (dest / "run_record.json").write_text(json.dumps(r, indent=2), encoding="utf-8")
        rows.append((board, r["fixture"].get("tier") or "?", ", ".join(why)))
    lines = [
        f"# Failing boards: Freerouting {label}",
        "",
        f"Updated {datetime.now().isoformat(timespec='seconds')}. {len(rows)} failing of {len(runs)} finished runs.",
        "",
        "| Board | Tier | Reason |",
        "|---|---|---|",
    ] + [f"| {b} | {t} | {w} |" for b, t, w in rows]
    out.mkdir(parents=True, exist_ok=True)
    (out / "INDEX.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    return len(runs), len(rows)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--label", required=True)
    ap.add_argument("--fixtures", type=Path, default=REPO / "scripts/benchmark/fixtures/PCBench")
    ap.add_argument("--out", type=Path, default=REPO / "build/pcbench-failures")
    ap.add_argument("--loop", type=int, default=0, help="repeat every N seconds (0 = once)")
    args = ap.parse_args()
    while True:
        done, failing = collect(args.label, args.fixtures, args.out)
        print(f"{datetime.now().isoformat(timespec='seconds')} finished={done} failing={failing}", flush=True)
        if not args.loop:
            return 0
        time.sleep(args.loop)


if __name__ == "__main__":
    raise SystemExit(main())
