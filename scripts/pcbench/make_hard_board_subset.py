#!/usr/bin/env python3
"""Build a PCBench fixtures directory holding only the boards earlier releases failed on.

A board is selected when its run for any --from-label had unrouted connections, router-introduced
violations, or a non-COMPLETED exit state. The output directory gets one symlink per board folder and
a catalog.json filtered to those boards, so it can be passed to run_corpus_benchmark.py --fixtures-dir.

Example:
  python3 scripts/pcbench/make_hard_board_subset.py --out build/pcbench-hard-boards/PCBench
"""
from __future__ import annotations

import argparse
import collections
import json
import os
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]


def failed(run: dict) -> bool:
    q, e = run.get("quality") or {}, run.get("exit") or {}
    return (
        (q.get("unrouted_connections") or 0) > 0
        or (q.get("router_introduced_violations") or 0) > 0
        or e.get("state") != "COMPLETED"
    )


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--benchmarks", type=Path, default=REPO / "scripts/benchmark/results/benchmarks.json")
    ap.add_argument("--fixtures", type=Path, default=REPO / "scripts/benchmark/fixtures/PCBench")
    ap.add_argument("--from-label", action="append", default=None,
                    help="version_label to take failures from (repeatable; default 2.5.0 and 2.6.0-RC4)")
    ap.add_argument("--out", type=Path, required=True)
    args = ap.parse_args()
    labels = args.from_label or ["2.5.0", "2.6.0-RC4"]

    runs = json.loads(args.benchmarks.read_text(encoding="utf-8"))["runs"]
    wanted = {
        Path(r["fixture"]["relative_path"]).parent.name
        for r in runs
        if (r.get("binary") or {}).get("version_label") in labels and failed(r)
    }
    fixtures = args.fixtures.resolve()
    catalog = json.loads((fixtures / "catalog.json").read_text(encoding="utf-8"))
    boards = [b for b in catalog["boards"] if b["board_id"] in wanted]
    missing = sorted(wanted - {b["board_id"] for b in boards})

    args.out.mkdir(parents=True, exist_ok=True)
    for b in boards:
        link = args.out / b["board_id"]
        if not link.exists():
            os.symlink(fixtures / b["board_id"], link)
    catalog["boards"] = boards
    catalog["total_boards"] = len(boards)
    (args.out / "catalog.json").write_text(json.dumps(catalog, indent=2), encoding="utf-8")

    tiers = collections.Counter(b["tier"] for b in boards)
    print(f"{len(boards)} boards ({dict(sorted(tiers.items()))}) from {len(wanted)} failing board ids in {labels}")
    if missing:
        print(f"{len(missing)} not in the current catalog, skipped: {', '.join(missing)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
