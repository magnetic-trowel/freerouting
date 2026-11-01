"""Tests for compact_log.py."""

from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from compact_log import compact_log

WARN = "WARN   Polyline: must contain at least 2 different points"


class CompactLogTest(unittest.TestCase):
    def compact(self, text: str) -> tuple[str, tuple[int, int]]:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "board.log"
            path.write_text(text, encoding="utf-8")
            counts = compact_log(path)
            self.assertEqual([p.name for p in Path(tmp).iterdir()], ["board.log"])
            return path.read_text(encoding="utf-8"), counts

    def test_collapses_repeated_message_and_keeps_first_and_last_time(self):
        text = (
            "2026-10-31 17:37:09.000 INFO   Loading board\n"
            + "".join(f"2026-10-31 17:37:09.{i:03d} {WARN}\n" for i in range(1, 6))
            + "2026-10-31 17:37:10.000 INFO   Pass 1 done\n"
        )
        out, counts = self.compact(text)
        self.assertEqual(
            out,
            "2026-10-31 17:37:09.000 INFO   Loading board\n"
            f"2026-10-31 17:37:09.001 {WARN}  [x 5, last 2026-10-31 17:37:09.005]\n"
            "2026-10-31 17:37:10.000 INFO   Pass 1 done\n",
        )
        self.assertEqual(counts, (7, 3))

    def test_only_consecutive_repeats_collapse(self):
        text = (
            f"2026-10-31 17:37:09.001 {WARN}\n"
            "2026-10-31 17:37:09.002 DEBUG  Pass 2\n"
            f"2026-10-31 17:37:09.003 {WARN}\n"
        )
        out, counts = self.compact(text)
        self.assertEqual(out, text)
        self.assertEqual(counts, (3, 3))

    def test_lines_without_timestamp_are_kept(self):
        text = "Exception in thread main\n\tat a.b(C.java:1)\n\tat a.b(C.java:1)\n"
        out, _ = self.compact(text)
        self.assertEqual(out, "Exception in thread main\n\tat a.b(C.java:1)  [x 2]\n")


if __name__ == "__main__":
    unittest.main()
