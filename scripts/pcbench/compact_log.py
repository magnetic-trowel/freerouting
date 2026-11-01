#!/usr/bin/env python3
"""Collapse runs of repeated Freerouting log messages in place.

A run of N consecutive lines with the same message (timestamp ignored) becomes its first line plus
"  [x N, last <timestamp>]". Every distinct message keeps its order, count, and first and last time;
only the timestamps of the repeats in between are dropped. The file stays plain text under the same
name, so tail-readers and log-name conventions keep working.

Some boards log the same warning hundreds of millions of times (for example
"Polyline: must contain at least 2 different points"), which turns a ~1 MB log into 2-12 GB.

Usage:
  python3 scripts/pcbench/compact_log.py scripts/benchmark/logs/*.log
"""
from __future__ import annotations

import os
import sys
from pathlib import Path

# "2026-10-31 17:37:09.969 WARN   message": the timestamp is the first 23 characters.
TIMESTAMP_LEN = 23


def _split(line: str) -> tuple[str, str]:
    if len(line) > TIMESTAMP_LEN and line[4] == "-" and line[10] == " " and line[TIMESTAMP_LEN] == " ":
        return line[:TIMESTAMP_LEN], line[TIMESTAMP_LEN + 1 :]
    return "", line


def compact_log(path: Path) -> tuple[int, int]:
    """Rewrite `path` with repeats collapsed; return (lines before, lines after).

    Writes to a temporary file next to it and replaces the original only after the whole file was
    written, so an interrupted run never leaves a truncated log.
    """
    tmp = path.with_name(path.name + ".compacting")
    lines_in = lines_out = 0
    first_ts, message, count, last_ts = "", None, 0, ""

    with open(path, encoding="utf-8", errors="replace", newline="") as src, open(
        tmp, "w", encoding="utf-8", newline="\n"
    ) as dst:

        def flush() -> None:
            nonlocal lines_out
            if message is None:
                return
            prefix = f"{first_ts} " if first_ts else ""
            suffix = f"  [x {count}, last {last_ts}]" if last_ts else f"  [x {count}]"
            suffix = suffix if count > 1 else ""
            dst.write(f"{prefix}{message}{suffix}\n")
            lines_out += 1

        for raw in src:
            lines_in += 1
            ts, msg = _split(raw.rstrip("\r\n"))
            if msg == message:
                count += 1
                last_ts = ts
                continue
            flush()
            first_ts, message, count, last_ts = ts, msg, 1, ts
        flush()

    os.replace(tmp, path)
    return lines_in, lines_out


def main(argv: list[str]) -> int:
    if not argv:
        print(__doc__, file=sys.stderr)
        return 2
    for name in argv:
        path = Path(name)
        before = path.stat().st_size
        lines_in, lines_out = compact_log(path)
        print(f"{path}: {lines_in} -> {lines_out} lines, {before / 1e6:.1f} -> {path.stat().st_size / 1e6:.1f} MB")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
