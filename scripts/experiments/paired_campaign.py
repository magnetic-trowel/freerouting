#!/usr/bin/env python3
"""Paired routing campaign: route every board with every variant side by side, then judge each
result with one evaluator jar (DSN + SES reload DRC).

Usage:
  python3 scripts/experiments/paired_campaign.py --out <dir> --boards <boards.txt> \
      --variants <variants.txt> [--flags "<common flags>"] [--evaluator <jar>] [--parallel N] \
      [--timeout HH:MM:SS] [--filter <substring>] [--force] [--dry-run]

boards.txt:   <fixture path relative to the repository root> [extra flags for this board]
variants.txt: <name> <jar path | head> [JVM -D/-X options and Freerouting flags]
              The first variant is the control. '#' starts a comment in both files.

Outputs (see scripts/experiments/README.md): <out>/summary.jsonl (one record per board and
variant, appended after each board, resumable) and <out>/summary.md (regenerated after each
board). Per-run files live in <out>/runs/<board>/<variant>/.

Standard library only; runs on Linux and macOS with Python 3.9 or later.
"""

from __future__ import annotations

import argparse
import collections
import concurrent.futures
import dataclasses
import datetime
import hashlib
import json
import math
import os
import re
import shlex
import shutil
import signal
import socket
import subprocess
import sys
import threading
import time
from pathlib import Path
from typing import Optional

REPO_ROOT = Path(__file__).resolve().parents[2]
SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_FLAGS = "-mt 1 -mp 25 --router.optimizer.enabled=false"
DEFAULT_TIMEOUT = "00:20:00"
DEFAULT_JVM_OPTS = "-Xmx3g"
HEAD_TOKEN = "head"

# Router log lines (identical wording in v2.5.0 and current master).
FINAL_RE = re.compile(
    r"Auto-routing stage completed(?P<timeout> with timeout)?.*?"
    r"\((?P<unrouted>\d+) unrouted and (?P<violations>\d+) violations?\)"
)
STAGE_SECONDS_RE = re.compile(r"Auto-routing stage completed.*?completed in ([\d.]+) seconds")
SCORE_RE = re.compile(r"Auto-routing stage completed.*?final score: ([\d.]+)")
PASS_RE = re.compile(r"Auto-routing pass #(\d+)")
STATE_RE = re.compile(r"finished with state: (\w+)")

# SES structure.
RESOLUTION_RE = re.compile(r"\(resolution\s+(\w+)\s+([\d.]+)\s*\)")
VIA_RE = re.compile(r"^\s*\(via\s", re.M)
PATH_RE = re.compile(
    r"\(path\s+(?:\"[^\"]*\"|[^\s()\"]+)\s+[-\d.eE+]+((?:\s+[-\d.eE+]+)*)\s*\)"
)
UNIT_TO_MM = {"um": 0.001, "mm": 1.0, "cm": 10.0, "mil": 0.0254, "inch": 25.4}

JVM_PREFIXES = ("-D", "-X", "-ea", "-da", "-esa", "-dsa", "-javaagent:", "-agentlib:",
                "-agentpath:", "-verbose:")
JVM_ESCAPE = "jvm:"
COMPLETED = "COMPLETED"

_running_lock = threading.Lock()
_running_pids: set[int] = set()
_stop = threading.Event()
_output_lock = threading.Lock()
_print_lock = threading.Lock()


def log(message: str) -> None:
    stamp = datetime.datetime.now().strftime("%H:%M:%S")
    with _print_lock:
        print(f"[{stamp}] {message}", flush=True)


# ---------------------------------------------------------------------------------------------
# Input files
# ---------------------------------------------------------------------------------------------


class Interrupted(Exception):
    """Raised in worker threads once the campaign is being stopped."""


@dataclasses.dataclass
class Board:
    name: str
    spec: str  # the path as written in boards.txt
    dsn: Path
    flags: list[str]


@dataclasses.dataclass
class Variant:
    name: str
    jar_spec: str
    jar: Optional[Path]
    flags: list[str]


def split_line(line: str) -> list[str]:
    return shlex.split(line, comments=True)


def split_jvm(flags: list[str]) -> tuple[list[str], list[str]]:
    """Separates JVM options (go before -jar) from Freerouting flags (go after the jar)."""
    jvm, app = [], []
    for flag in flags:
        if flag.startswith(JVM_ESCAPE):
            jvm.append(flag[len(JVM_ESCAPE):])
        elif flag.startswith(JVM_PREFIXES):
            jvm.append(flag)
        else:
            app.append(flag)
    return jvm, app


def read_boards(path: Path, repo: Path) -> list[Board]:
    boards, seen = [], {}
    for number, line in enumerate(path.read_text().splitlines(), 1):
        tokens = split_line(line)
        if not tokens:
            continue
        dsn = Path(os.path.expandvars(os.path.expanduser(tokens[0])))
        if not dsn.is_absolute():
            dsn = repo / dsn
        name = dsn.name[:-4] if dsn.name.lower().endswith(".dsn") else dsn.name
        if name in seen:
            sys.exit(f"{path}:{number}: board '{name}' is listed twice (also line {seen[name]})")
        seen[name] = number
        boards.append(Board(name, tokens[0], dsn, tokens[1:]))
    if not boards:
        sys.exit(f"{path}: no boards")
    return boards


def resolve_jar(spec: str, bases: list[Path]) -> Optional[Path]:
    candidate = Path(os.path.expandvars(os.path.expanduser(spec)))
    if candidate.is_absolute():
        return candidate
    for base in bases:
        if (base / candidate).exists():
            return (base / candidate).resolve()
    return None


def read_variants(path: Path, repo: Path) -> list[Variant]:
    variants, names = [], set()
    bases = [Path.cwd(), path.resolve().parent, repo]
    for number, line in enumerate(path.read_text().splitlines(), 1):
        tokens = split_line(line)
        if not tokens:
            continue
        if len(tokens) < 2:
            sys.exit(f"{path}:{number}: expected '<name> <jar|head> [flags...]'")
        name, jar_spec = tokens[0], tokens[1]
        if not re.fullmatch(r"[A-Za-z0-9._-]+", name):
            sys.exit(f"{path}:{number}: variant name '{name}' must match [A-Za-z0-9._-]+")
        if name in names:
            sys.exit(f"{path}:{number}: variant '{name}' is listed twice")
        names.add(name)
        jar = None if jar_spec == HEAD_TOKEN else resolve_jar(jar_spec, bases)
        if jar_spec != HEAD_TOKEN and jar is None:
            jar = Path(jar_spec)  # reported as missing later (or printed by --dry-run)
        variants.append(Variant(name, jar_spec, jar, tokens[2:]))
    if not variants:
        sys.exit(f"{path}: no variants")
    return variants


def timeout_seconds(value: str) -> int:
    parts = [int(p) for p in value.split(":")]
    while len(parts) < 3:
        parts.insert(0, 0)
    hours, minutes, seconds = parts[-3:]
    return hours * 3600 + minutes * 60 + seconds


def effective_timeout(default: str, flags: list[str]) -> str:
    value = default
    for flag in flags:
        if flag.startswith("--router.job_timeout="):
            value = flag.split("=", 1)[1]
    return value


# ---------------------------------------------------------------------------------------------
# Result parsing
# ---------------------------------------------------------------------------------------------


def sha1_file(path: Path) -> str:
    digest = hashlib.sha1()
    with open(path, "rb") as handle:
        for block in iter(lambda: handle.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def parse_router_log(text: str) -> dict:
    result: dict = {"state": None, "unrouted": None, "violations": None, "passes": None,
                    "score": None, "stage_s": None, "stage_timed_out": None}
    states = STATE_RE.findall(text)
    if states:
        result["state"] = states[-1]
    finals = list(FINAL_RE.finditer(text))
    if finals:
        last = finals[-1]
        result["unrouted"] = int(last.group("unrouted"))
        result["violations"] = int(last.group("violations"))
        result["stage_timed_out"] = last.group("timeout") is not None
    passes = PASS_RE.findall(text)
    if passes:
        result["passes"] = int(passes[-1])
    scores = SCORE_RE.findall(text)
    if scores:
        result["score"] = float(scores[-1])
    seconds = STAGE_SECONDS_RE.findall(text)
    if seconds:
        result["stage_s"] = float(seconds[-1])
    return result


def parse_ses(path: Path) -> dict:
    """Via count and total wire length in mm (sum of path segment lengths in network_out)."""
    text = path.read_text(errors="replace")
    routes = text.find("(routes")
    resolution = RESOLUTION_RE.search(text, routes if routes >= 0 else 0)
    network = text.find("(network_out")
    network_text = text[network:] if network >= 0 else ""
    units = 0.0
    for match in PATH_RE.finditer(network_text):
        numbers = [float(n) for n in match.group(1).split()]
        points = list(zip(numbers[0::2], numbers[1::2]))
        units += sum(math.dist(points[i], points[i + 1]) for i in range(len(points) - 1))
    length_mm = None
    resolution_text = None
    if resolution:
        unit, per_unit = resolution.group(1).lower(), float(resolution.group(2))
        resolution_text = f"{unit} {resolution.group(2)}"
        if unit in UNIT_TO_MM and per_unit > 0:
            length_mm = round(units / per_unit * UNIT_TO_MM[unit], 3)
    return {"vias": len(VIA_RE.findall(network_text)), "wire_length_mm": length_mm,
            "ses_resolution": resolution_text}


def is_clearance_type(violation_type: str) -> bool:
    return "clearance" in violation_type.lower()


def parse_drc(path: Path) -> dict:
    data = json.loads(path.read_text())
    by_type = collections.Counter(v.get("type", "?") for v in data.get("violations", []))
    return {
        "drc_unconnected": len(data.get("unconnectedItems", [])),
        "drc_violations": dict(sorted(by_type.items())),
        "drc_clearance": sum(n for t, n in by_type.items() if is_clearance_type(t)),
        "drc_other": sum(n for t, n in by_type.items() if not is_clearance_type(t)),
    }


# ---------------------------------------------------------------------------------------------
# Process execution
# ---------------------------------------------------------------------------------------------


def run_measured(cmd: list[str], log_path: Path, cwd: Path,
                 kill_after_s: Optional[float]) -> dict:
    """Runs cmd with output to log_path; returns wall/CPU seconds (os.wait4) and exit code.

    The process group is killed after kill_after_s seconds (None: never)."""
    killed = threading.Event()
    done = threading.Event()
    lock = threading.Lock()
    start = time.monotonic()
    with open(log_path, "w") as out:
        proc = subprocess.Popen(cmd, stdout=out, stderr=subprocess.STDOUT, cwd=str(cwd),
                                start_new_session=True)
    with _running_lock:
        _running_pids.add(proc.pid)
    if _stop.is_set():
        _kill_group(proc.pid)

    def kill() -> None:
        with lock:
            if not done.is_set():
                killed.set()
                _kill_group(proc.pid)

    timer = threading.Timer(kill_after_s or 0, kill)
    timer.daemon = True
    if kill_after_s:
        timer.start()
    try:
        _, status, usage = os.wait4(proc.pid, 0)
    finally:
        with lock:
            done.set()
        timer.cancel()
        with _running_lock:
            _running_pids.discard(proc.pid)
    proc.returncode = os.waitstatus_to_exitcode(status)
    if _stop.is_set():
        raise Interrupted()
    return {"wall_s": round(time.monotonic() - start, 2),
            "cpu_s": round(usage.ru_utime + usage.ru_stime, 2),
            "rc": proc.returncode, "killed": killed.is_set()}


def _kill_group(pid: int) -> None:
    try:
        os.killpg(pid, signal.SIGKILL)
    except (ProcessLookupError, PermissionError):
        pass


def kill_all_running() -> None:
    with _running_lock:
        pids = list(_running_pids)
    for pid in pids:
        _kill_group(pid)


# ---------------------------------------------------------------------------------------------
# Campaign
# ---------------------------------------------------------------------------------------------


class Campaign:
    def __init__(self, args: argparse.Namespace):
        self.args = args
        self.repo = Path(args.repo).resolve()
        self.out = Path(args.out).resolve()
        self.boards = read_boards(Path(args.boards), self.repo)
        self.variants = read_variants(Path(args.variants), self.repo)
        self.control = self.variants[0].name
        self.java = args.java or os.environ.get("JAVA") or shutil.which("java") or "java"
        self.wrap = shlex.split(args.wrap) if args.wrap else []
        self.common_jvm, self.common_app = split_jvm(shlex.split(args.flags))
        self.jvm_opts = shlex.split(args.jvm_opts)
        self.drc_flags = shlex.split(args.drc_flags)
        self.summary_jsonl = self.out / "summary.jsonl"
        self.summary_md = self.out / "summary.md"
        self.jar_sha1: dict[Path, str] = {}
        self.evaluator: Optional[Path] = None

    # -- preparation -----------------------------------------------------------------------

    def prepare(self) -> None:
        if any(v.jar_spec == HEAD_TOKEN for v in self.variants):
            head = self.head_jar()
            for variant in self.variants:
                if variant.jar_spec == HEAD_TOKEN:
                    variant.jar = head
        if self.args.evaluator:
            self.evaluator = resolve_jar(self.args.evaluator, [Path.cwd(), self.repo]) or Path(
                self.args.evaluator)
        else:
            self.evaluator = self.variants[0].jar
        if self.args.dry_run:
            return
        missing = [f"variant '{v.name}': {v.jar}" for v in self.variants if not v.jar.is_file()]
        if not self.evaluator.is_file():
            missing.append(f"evaluator: {self.evaluator}")
        missing += [f"board '{b.name}': {b.dsn}" for b in self.boards if not b.dsn.is_file()]
        if missing:
            sys.exit("missing input files:\n  " + "\n  ".join(missing))
        for jar in {v.jar for v in self.variants} | {self.evaluator}:
            self.jar_sha1[jar] = sha1_file(jar)

    def head_jar(self) -> Path:
        if self.args.head_jar:
            return resolve_jar(self.args.head_jar, [Path.cwd(), self.repo]) or Path(
                self.args.head_jar)
        commit = subprocess.run(["git", "-C", str(self.repo), "rev-parse", "--verify",
                                 self.args.head_commit + "^{commit}"], check=True,
                                capture_output=True, text=True).stdout.strip()
        patch = self.args.head_patch
        suffix = ""
        if patch != "-":
            patch = str(Path(patch).resolve())
            suffix = "-" + sha1_file(Path(patch))[:8]
        jar = self.out / "jars" / f"head-{commit[:12]}{suffix}.jar"
        cmd = self.wrap + [str(SCRIPT_DIR / "build_variant.sh"), commit, patch, str(jar)]
        if self.args.dry_run:
            print(shlex.join(cmd))
        elif not jar.is_file():
            log(f"building the 'head' jar: {shlex.join(cmd)}")
            subprocess.run(cmd, check=True, env={**os.environ, "FR_REPO": str(self.repo)})
        return jar

    # -- results file ----------------------------------------------------------------------

    def load_records(self) -> dict[tuple[str, str], dict]:
        records: dict[tuple[str, str], dict] = {}
        if self.summary_jsonl.exists():
            for number, line in enumerate(self.summary_jsonl.read_text().splitlines(), 1):
                if not line.strip():
                    continue
                try:
                    record = json.loads(line)
                except ValueError:
                    log(f"WARNING: {self.summary_jsonl}:{number}: ignoring a damaged record")
                    continue
                records[(record["board"], record["variant"])] = record
        return records

    def append_records(self, records: list[dict]) -> None:
        with _output_lock:
            with open(self.summary_jsonl, "a") as out:
                for record in records:
                    out.write(json.dumps(record, sort_keys=True) + "\n")
            write_summary_md(self)

    # -- commands --------------------------------------------------------------------------

    def route_command(self, board: Board, variant: Variant, run_dir: Path) -> tuple[list, str]:
        board_jvm, board_app = split_jvm(board.flags)
        variant_jvm, variant_app = split_jvm(variant.flags)
        app_flags = (["--router.job_timeout=" + self.args.timeout] + self.common_app + board_app
                     + variant_app)
        cmd = (self.wrap + [self.java] + self.jvm_opts + self.common_jvm + board_jvm + variant_jvm
               + ["-jar", str(variant.jar), "-de", str(board.dsn), "-do",
                  str(run_dir / "out.ses"), "--gui.enabled=false",
                  "--logging.file.enabled=false", "--logging.console.level=INFO",
                  "--user_data_path=" + str(run_dir / "ud")] + app_flags)
        return cmd, effective_timeout(self.args.timeout, app_flags)

    def drc_command(self, board: Board, run_dir: Path) -> list:
        return (self.wrap + [self.java] + self.jvm_opts + ["-jar", str(self.evaluator), "-de",
                f"{board.dsn}+{run_dir / 'out.ses'}", "-drc", str(run_dir / "drc.json"),
                "--gui.enabled=false", "--logging.file.enabled=false",
                "--user_data_path=" + str(run_dir / "ud2")] + self.drc_flags)

    def kill_after(self, timeout: str) -> Optional[float]:
        if self.args.kill_after:
            return float(self.args.kill_after)
        if self.wrap:
            return None  # the wrapper may queue the JVM for an unknown time
        return 2 * timeout_seconds(timeout) + 600

    # -- execution -------------------------------------------------------------------------

    def run_one(self, board: Board, variant: Variant) -> dict:
        if _stop.is_set():
            raise Interrupted()
        run_dir = self.out / "runs" / board.name / variant.name
        if run_dir.exists():
            shutil.rmtree(run_dir)
        run_dir.mkdir(parents=True)
        cmd, timeout = self.route_command(board, variant, run_dir)
        (run_dir / "command.txt").write_text(shlex.join(cmd) + "\n")
        log(f"start  {board.name} / {variant.name}")
        record = {
            "board": board.name, "variant": variant.name, "control": self.control,
            "dsn": str(board.dsn), "jar": str(variant.jar), "jar_sha1": self.jar_sha1[variant.jar],
            "flags": shlex.join(self.common_jvm + self.common_app + board.flags + variant.flags),
            "timeout": timeout, "host": socket.gethostname(), "cpus": os.cpu_count(),
            "started_at": datetime.datetime.now(datetime.timezone.utc).isoformat(
                timespec="seconds"),
        }
        record.update(run_measured(cmd, run_dir / "console.log", run_dir,
                                   self.kill_after(timeout)))
        record.update(parse_router_log((run_dir / "console.log").read_text(errors="replace")))
        if record["killed"]:
            record["state"] = "KILLED"
        elif record["state"] is None:
            record["state"] = "FAILED"
        record["timed_out"] = record["state"] in ("TIMED_OUT", "KILLED") or bool(
            record["stage_timed_out"])
        ses = run_dir / "out.ses"
        record.update({"ses_sha1": None, "vias": None, "wire_length_mm": None, "drc": None})
        if ses.is_file() and ses.stat().st_size > 0:
            record["ses_sha1"] = sha1_file(ses)
            record.update(parse_ses(ses))
            drc = self.run_drc(board, run_dir)
            record.update(drc)
        log(f"finish {board.name} / {variant.name}: {record['state']}, "
            f"unrouted {record['unrouted']}, DRC clearance {record.get('drc_clearance')}, "
            f"cpu {record['cpu_s']} s, wall {record['wall_s']} s")
        if not self.args.keep_user_data:
            for user_data in ("ud", "ud2"):
                shutil.rmtree(run_dir / user_data, ignore_errors=True)
        return record

    def run_drc(self, board: Board, run_dir: Path) -> dict:
        cmd = self.drc_command(board, run_dir)
        (run_dir / "drc_command.txt").write_text(shlex.join(cmd) + "\n")
        measured = run_measured(cmd, run_dir / "drc.log", run_dir, self.kill_after("00:15:00"))
        result = {"drc": "ok", "drc_cpu_s": measured["cpu_s"], "drc_rc": measured["rc"],
                  "evaluator": str(self.evaluator),
                  "evaluator_sha1": self.jar_sha1[self.evaluator]}
        try:
            result.update(parse_drc(run_dir / "drc.json"))
        except (OSError, ValueError) as error:
            result["drc"] = f"error: {error}"
        return result

    def run_board(self, board: Board, variants: list[Variant]) -> None:
        workers = min(len(variants), self.args.parallel)
        with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as pool:
            futures = [pool.submit(self.run_one, board, variant) for variant in variants]
            records = [future.result() for future in futures]
        self.append_records(records)

    def pending(self) -> list[tuple[Board, list[Variant]]]:
        existing = {} if self.args.force else self.load_records()
        work = []
        for board in self.boards:
            if self.args.filter and self.args.filter not in board.spec:
                continue
            todo = []
            for variant in self.variants:
                record = existing.get((board.name, variant.name))
                if record is None:
                    todo.append(variant)
                elif not self.args.dry_run and record.get("jar_sha1") != self.jar_sha1.get(
                        variant.jar):
                    log(f"WARNING: {board.name} / {variant.name} was run with a different jar "
                        f"({record.get('jar')}); keeping the old record (use --force to rerun)")
            if todo:
                work.append((board, todo))
        return work

    def run(self) -> None:
        self.prepare()
        work = self.pending()
        if self.args.dry_run:
            for board, variants in work:
                for variant in variants:
                    run_dir = self.out / "runs" / board.name / variant.name
                    print(shlex.join(self.route_command(board, variant, run_dir)[0]))
                    print(shlex.join(self.drc_command(board, run_dir)))
            return
        self.out.mkdir(parents=True, exist_ok=True)
        total_runs = sum(len(v) for _, v in work)
        log(f"{len(work)} boards / {total_runs} runs to do; parallel {self.args.parallel}; "
            f"control '{self.control}'; output {self.out}")
        if not work:
            with _output_lock:
                write_summary_md(self)
            return
        boards_at_once = max(1, self.args.parallel // len(self.variants))
        pool = concurrent.futures.ThreadPoolExecutor(max_workers=boards_at_once)
        try:
            futures = [pool.submit(self.run_board, board, variants) for board, variants in work]
            for future in futures:
                future.result()
        except BaseException:
            # Stop everything before the pool waits for its threads; interrupted runs are not
            # recorded, so a rerun with the same --out resumes them.
            _stop.set()
            kill_all_running()
            pool.shutdown(wait=True, cancel_futures=True)
            raise
        pool.shutdown()
        log(f"campaign done; see {self.summary_md}")


# ---------------------------------------------------------------------------------------------
# summary.md
# ---------------------------------------------------------------------------------------------


def fmt(value, digits: int = 1) -> str:
    if value is None:
        return "-"
    if isinstance(value, float):
        return f"{value:.{digits}f}"
    return str(value)


def plural(count: int, noun: str) -> str:
    return f"{count} {noun}" if count == 1 else f"{count} {noun}s"


def pct(value: float, base: float) -> str:
    if not base:
        return "-"
    return f"{100.0 * (value - base) / base:+.1f}%"


def completed(record: Optional[dict]) -> bool:
    return bool(record) and record.get("state") == COMPLETED and not record.get("timed_out")


def worse(value, control_value) -> bool:
    return value is not None and control_value is not None and value > control_value


def board_findings(control: Optional[dict], record: Optional[dict]) -> tuple[list, list]:
    """Regression and improvement reasons of one variant against the control on one board."""
    regressions, improvements = [], []
    if record is None or control is None:
        return regressions, improvements
    if completed(control) and not completed(record):
        regressions.append(f"fewer completed runs (state {record.get('state')})")
    elif completed(record) and not completed(control):
        improvements.append(f"completed while the control ended {control.get('state')}")
    both_completed = completed(control) and completed(record)
    if both_completed and worse(record.get("unrouted"), control.get("unrouted")):
        regressions.append(f"more unrouted ({control['unrouted']} -> {record['unrouted']})")
    elif both_completed and worse(control.get("unrouted"), record.get("unrouted")):
        improvements.append(f"fewer unrouted ({control['unrouted']} -> {record['unrouted']})")
    if worse(record.get("drc_clearance"), control.get("drc_clearance")):
        note = "" if both_completed else ", timed-out or failed run involved"
        regressions.append("more reload-DRC clearance violations "
                           f"({control['drc_clearance']} -> {record['drc_clearance']}{note})")
    if both_completed and worse(record.get("drc_unconnected"), control.get("drc_unconnected")):
        regressions.append("more reload-DRC unconnected items "
                           f"({control['drc_unconnected']} -> {record['drc_unconnected']})")
    if control.get("drc_clearance") is not None and record.get("drc_clearance") is None:
        regressions.append(f"no reload-DRC result ({record.get('drc') or 'no SES'})")
    return regressions, improvements


def write_summary_md(campaign: Campaign) -> None:
    records = campaign.load_records()
    variants = [v.name for v in campaign.variants]
    control = campaign.control
    listed = [b.name for b in campaign.boards]
    others = sorted({board for board, _ in records} - set(listed))
    boards = [b for b in listed + others if any((b, v) in records for v in variants)]
    lines = [
        "# Paired campaign summary",
        "",
        f"Generated {datetime.datetime.now(datetime.timezone.utc).isoformat(timespec='seconds')}"
        f" from `{campaign.summary_jsonl.name}`: {plural(len(boards), 'board')}, control "
        f"`{control}`.",
        "",
        f"- Common flags: `{campaign.args.flags}`; safety timeout `{campaign.args.timeout}`; "
        f"JVM options `{campaign.args.jvm_opts}`",
        f"- Evaluator (reload DRC): `{campaign.evaluator}`",
        "",
        "| variant | jar | jar sha1 | variant flags |",
        "|---|---|---|---|",
    ]
    for variant in campaign.variants:
        sha = campaign.jar_sha1.get(variant.jar, "")[:12]
        flags = f"`{shlex.join(variant.flags)}`" if variant.flags else "-"
        lines.append(f"| {variant.name} | `{variant.jar}` | {sha} | {flags} |")

    lines += ["", "## Per-board results", "",
              "Clearance = reload-DRC violations whose type contains 'clearance'; other = the "
              "remaining reload-DRC violation types. Bold = worse than the control. "
              "SES = control: byte-identical routing result.", "",
              "| board | variant | state | passes | unrouted | viol. | DRC unconn. | "
              "DRC clearance | DRC other | vias | length mm | CPU s | wall s | SES = control |",
              "|---|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|"]
    for board in boards:
        control_record = records.get((board, control))
        first = True
        for variant in variants:
            record = records.get((board, variant))
            if record is None:
                continue
            is_control = variant == control

            def cell(key, digits=1, record=record, is_control=is_control):
                text = fmt(record.get(key), digits)
                if not is_control and control_record and worse(record.get(key),
                                                               control_record.get(key)):
                    if key in ("unrouted", "drc_clearance", "drc_unconnected"):
                        return f"**{text}**"
                return text

            state = record.get("state") or "-"
            if not completed(record):
                state = f"**{state}**"
            if is_control:
                same = "(control)"
            elif control_record is None or not control_record.get("ses_sha1"):
                same = "-"
            else:
                same = "yes" if record.get("ses_sha1") == control_record.get("ses_sha1") else "no"
            lines.append(
                f"| {board if first else ''} | {variant} | {state} | {fmt(record.get('passes'))} "
                f"| {cell('unrouted')} | {fmt(record.get('violations'))} "
                f"| {cell('drc_unconnected')} | {cell('drc_clearance')} "
                f"| {fmt(record.get('drc_other'))} | {fmt(record.get('vias'))} "
                f"| {fmt(record.get('wire_length_mm'))} | {fmt(record.get('cpu_s'))} "
                f"| {fmt(record.get('wall_s'))} | {same} |")
            first = False

    comparable = [b for b in boards
                  if all(completed(records.get((b, v))) for v in variants)]
    lines += ["", "## Totals", "",
              f"Over the {plural(len(comparable), 'board')} on which every variant completed "
              f"without timing out (of {len(boards)}). Deltas are against the control.", "",
              "| variant | unrouted | vias | length mm | CPU s | wall s | DRC clearance | "
              "SES = control | runs completed | timed out / failed |",
              "|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|"]
    sums: dict[str, dict] = {}
    for variant in variants:
        total = collections.Counter()
        for board in comparable:
            record = records[(board, variant)]
            for key in ("unrouted", "vias", "wire_length_mm", "cpu_s", "wall_s",
                        "drc_clearance"):
                total[key] += record.get(key) or 0
        sums[variant] = total
    for variant in variants:
        total, base = sums[variant], sums[control]
        runs = [records.get((b, variant)) for b in boards]
        done = sum(1 for r in runs if completed(r))
        not_done = sum(1 for r in runs if r is not None and not completed(r))
        identical = sum(1 for b in boards if records.get((b, variant)) and records.get(
            (b, control)) and records[(b, variant)].get("ses_sha1") and records[
            (b, variant)].get("ses_sha1") == records[(b, control)].get("ses_sha1"))
        with_control = sum(1 for b in boards if records.get((b, variant)) and records.get(
            (b, control)))
        def delta(key, total=total, base=base, variant=variant):
            if variant == control:
                return ""
            if key in ("unrouted", "drc_clearance"):
                return f" ({total[key] - base[key]:+d})"
            return f" ({pct(total[key], base[key])})"

        lines.append(
            f"| {variant} | {total['unrouted']}{delta('unrouted')} "
            f"| {total['vias']}{delta('vias')} "
            f"| {total['wire_length_mm']:.1f}{delta('wire_length_mm')} "
            f"| {total['cpu_s']:.1f}{delta('cpu_s')} | {total['wall_s']:.1f}{delta('wall_s')} "
            f"| {total['drc_clearance']}{delta('drc_clearance')} "
            f"| {'-' if variant == control else f'{identical}/{with_control}'} "
            f"| {done}/{len([r for r in runs if r is not None])} | {not_done} |")

    regressions, improvements = [], []
    for board in boards:
        control_record = records.get((board, control))
        for variant in variants[1:]:
            bad, good = board_findings(control_record, records.get((board, variant)))
            regressions += [f"- {board} / {variant}: {reason}" for reason in bad]
            improvements += [f"- {board} / {variant}: {reason}" for reason in good]
    excluded = [f"- {b} / {v}: {records[(b, v)].get('state')}" for b in boards for v in variants
                if (b, v) in records and not completed(records[(b, v)])]
    lines += ["", "## Regressions", "",
              "Boards where a variant has more unrouted (both runs completed), more reload-DRC "
              "clearance-type violations, more reload-DRC unconnected items, or fewer completed "
              "runs than the control.", ""]
    lines += regressions or ["None."]
    lines += ["", "## Improvements", ""] + (improvements or ["None."])
    lines += ["", "## Runs excluded from comparisons (timed out, killed or failed)", ""]
    lines += excluded or ["None."]
    tmp = campaign.summary_md.with_suffix(".md.tmp")
    tmp.write_text("\n".join(lines) + "\n")
    tmp.replace(campaign.summary_md)


# ---------------------------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------------------------


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Route every board with every variant side by side and judge each result "
                    "with the evaluator's reload DRC.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument("--out", required=True, help="output directory (resumable)")
    parser.add_argument("--boards", required=True, help="boards.txt")
    parser.add_argument("--variants", required=True, help="variants.txt (first line = control)")
    parser.add_argument("--flags", default=DEFAULT_FLAGS,
                        help="flags common to all variants (JVM -D/-X options are moved before "
                             "-jar)")
    parser.add_argument("--evaluator", help="jar for the reload DRC (default: the control's jar)")
    parser.add_argument("--parallel", type=int, default=max(1, (os.cpu_count() or 2) // 2),
                        help="maximum number of routing JVMs at a time")
    parser.add_argument("--timeout", default=DEFAULT_TIMEOUT,
                        help="router job timeout (safety stop; such runs are excluded)")
    parser.add_argument("--filter",
                        help="only run boards whose boards.txt path contains this substring")
    parser.add_argument("--force", action="store_true",
                        help="rerun board/variant pairs that already have a record")
    parser.add_argument("--dry-run", action="store_true", help="print the commands only")
    parser.add_argument("--jvm-opts", default=DEFAULT_JVM_OPTS,
                        help="JVM options for every routing and DRC JVM")
    parser.add_argument("--java", help="java executable (default: $JAVA, then java on PATH)")
    parser.add_argument("--repo", default=str(REPO_ROOT),
                        help="repository root that board paths are relative to")
    parser.add_argument("--head-jar", help="jar used for the variant jar token 'head'")
    parser.add_argument("--head-commit", default="HEAD",
                        help="commit built for 'head' when --head-jar is not given")
    parser.add_argument("--head-patch", default="-",
                        help="patch applied to --head-commit when building 'head'")
    parser.add_argument("--wrap", default="",
                        help="command prefix for every JVM and build, e.g. a job-slot limiter")
    parser.add_argument("--drc-flags", default="", help="extra flags for the reload DRC run")
    parser.add_argument("--kill-after", type=float,
                        help="hard kill after this many seconds (default: 2 x timeout + 600; "
                             "no hard kill with --wrap unless given)")
    parser.add_argument("--keep-user-data", action="store_true",
                        help="keep the per-run --user_data_path directories")
    args = parser.parse_args(argv)
    if args.parallel < 1:
        parser.error("--parallel must be at least 1")
    try:
        timeout_seconds(args.timeout)
    except ValueError:
        parser.error(f"--timeout must be HH:MM:SS, got '{args.timeout}'")
    return args


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    campaign = Campaign(args)
    signal.signal(signal.SIGTERM, _raise_keyboard_interrupt)
    try:
        campaign.run()
    except KeyboardInterrupt:
        log("interrupted; running JVMs were killed (finished boards are kept in summary.jsonl)")
        return 130
    return 0


def _raise_keyboard_interrupt(signum, frame) -> None:
    raise KeyboardInterrupt()


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
