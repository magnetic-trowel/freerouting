#!/usr/bin/env python3
"""Aggregate a JFR recording (streams `jfr print` text output, low memory).

usage:
  jfragg.py <rec.jfr> [--top N] [--thread SUBSTR]          summary report
  jfragg.py <rec.jfr> --callers <method substr> [--depth D] caller chains of a method (CPU)
  jfragg.py <rec.jfr> --alloc-callers <substr> [--depth D]  allocation weight by caller chain
  jfragg.py <rec.jfr> --timeline <seconds per bucket>       phase shares over time (CPU)

Summary: phase buckets (innermost matching category), outer phases, self time, self time
attributed to the first app frame, inclusive app frames, allocation by class and by site.
"""
import collections
import re
import subprocess
import sys

# Innermost-first phase buckets: the first category (in this order) whose pattern occurs anywhere
# in the stack wins.
BUCKETS = [
    ("drc", ("drc.DesignRulesChecker", "ClearanceViolations")),
    ("snapshot_hash", ("BoardSnapshotManager", "BoardHistory")),
    ("statistics", ("BoardStatistics", "RatsNest", "AutorouteUnroutedReport",
                    "AutorouteAirlineCalculator", "NetRoutingLedger", "NetIncompletes")),
    ("shove_check", ("ShoveCheckCache", "ForcedPadRouter.check", "ForcedViaInserter.check",
                     "DrillItemMover.check", "TraceShover.check", "MazeTraceShover")),
    ("room_completion", ("AutorouteEngine.completeExpansionRoom",
                         "AutorouteEngine.completeNeighbourRooms",
                         "ShapeSearchTree45Degree.completeShape",
                         "ShapeSearchTree90Degree.completeShape",
                         "ShapeSearchTree.completeShape", "AutorouteEngine.calculateDoors",
                         "SortedRoomNeighbours", "Sorted45DegreeRoomNeighbours",
                         "SortedOrthogonalRoomNeighbours", "AutorouteEngine.addCompleteRoom")),
    ("insertion", ("FoundConnectionInserter", "FoundConnectionLocator", "normalizeTraces",
                   "PolylineTraceNormalization", "BasicBoard.insert", "RoutingBoard.insert",
                   "PolylineTrace.combine", "RoutingBoard.fanout", "TraceTightener")),
    ("ripup_restore", ("RoutingBoardUndoFacade", "UndoableObjects", "BasicBoard.removeItems",
                       "MazeRipupResolver")),
    ("maze_search", ("MazeSearchEngine", "MazeExpansionEngine", "DrillPage",
                     "DestinationDistance", "MazeListElement")),
    ("autoroute_other", ("autoroute.",)),
    ("board_other", ("board.",)),
    ("io", ("io.specctra", "DsnReader", "SesWriter")),
]

# Outer phase: the first category (in this order) whose pattern occurs anywhere in the stack.
OUTER = [
    ("load", ("DsnReader", "io.specctra.parser")),
    ("fallback", ("RoutingFallback",)),
    ("fanout", ("BatchFanout",)),
    ("route_pass", ("BatchAutorouter.autoroutePass", "AutoroutePassRunner")),
    ("batch_loop_other", ("AutorouteBatchLoop",)),
    ("optimizer", ("BatchOptimizer",)),
    ("pipeline_other", ("RoutingPipeline",)),
    ("drc_job", ("DesignRulesChecker",)),
]

FRAME_RE = re.compile(r"^\s+([\w$.<>/]+)\((.*)\)\s+line:")
THREAD_RE = re.compile(r'^\s+(?:sampledThread|eventThread) = "([^"]*)"')
CLASS_RE = re.compile(r"^\s+objectClass = ([^\s]+)")
WEIGHT_RE = re.compile(r"^\s+weight = ([0-9.]+)\s*(bytes|kB|MB|GB)")
TIME_RE = re.compile(r"^\s+startTime = (\d+):(\d+):([0-9.]+)")
UNIT = {"bytes": 1, "kB": 1e3, "MB": 1e6, "GB": 1e9}


def events(path, event):
    """Yields dicts with thread, frames, cls, weight, t for each event."""
    proc = subprocess.Popen(
        ["jfr", "print", "--stack-depth", "128", "--events", event, path],
        stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True, bufsize=1 << 20)
    cur = None
    for line in proc.stdout:
        if line.startswith(event) and line.rstrip().endswith("{"):
            cur = {"thread": "?", "frames": [], "cls": "?", "weight": 0.0, "t": 0.0}
            continue
        if cur is None:
            continue
        if line.startswith("}"):
            yield cur
            cur = None
            continue
        m = FRAME_RE.match(line)
        if m:
            cur["frames"].append(m.group(1).replace("/", ".").replace("app.freerouting.", ""))
            continue
        m = THREAD_RE.match(line)
        if m:
            cur["thread"] = m.group(1)
            continue
        m = CLASS_RE.match(line)
        if m:
            cur["cls"] = m.group(1).replace("app.freerouting.", "")
            continue
        m = WEIGHT_RE.match(line)
        if m:
            cur["weight"] = float(m.group(1)) * UNIT[m.group(2)]
            continue
        m = TIME_RE.match(line)
        if m:
            cur["t"] = int(m.group(1)) * 3600 + int(m.group(2)) * 60 + float(m.group(3))
    proc.wait()


def is_app(f):
    return not f.startswith(("java.", "jdk.", "sun.", "com.", "org."))


def classify(frames, table):
    joined = "\n".join(frames)
    for name, pats in table:
        if any(p in joined for p in pats):
            return name
    return "other"


def pct(c, n):
    return f"{100.0 * c / n:6.1f}%" if n else "   n/a"


def summary(path, top, thread):
    selfc, selfapp, incl = collections.Counter(), collections.Counter(), collections.Counter()
    buckets, outer, oi, threads = (collections.Counter() for _ in range(4))
    n = 0
    for e in events(path, "jdk.ExecutionSample"):
        threads[e["thread"]] += 1
        if thread and thread not in e["thread"]:
            continue
        fr = e["frames"]
        if not fr:
            continue
        n += 1
        selfc[fr[0]] += 1
        app = next((f for f in fr if is_app(f)), None)
        if app:
            selfapp[app] += 1
        for f in set(fr):
            if is_app(f):
                incl[f] += 1
        b = classify(fr, BUCKETS)
        o = classify(fr, OUTER)
        buckets[b] += 1
        outer[o] += 1
        oi[(o, b)] += 1
    print(f"# {path}\nexecution samples: {n} (threads: {dict(threads.most_common(5))})\n")
    print("## phase buckets (innermost category)")
    for b, c in buckets.most_common():
        print(f"{pct(c, n)}  {c:7d}  {b}")
    print("\n## outer phase")
    for b, c in outer.most_common():
        print(f"{pct(c, n)}  {c:7d}  {b}")
    print("\n## outer x inner (>= 0.5%)")
    for (o, b), c in oi.most_common():
        if c * 200 >= n:
            print(f"{pct(c, n)}  {o:16s} {b}")
    print("\n## self time (top frame)")
    for f, c in selfc.most_common(top):
        print(f"{pct(c, n)}  {f}")
    print("\n## self time attributed to the first app frame")
    for f, c in selfapp.most_common(top):
        print(f"{pct(c, n)}  {f}")
    print("\n## inclusive (app frames)")
    for f, c in incl.most_common(top + 25):
        print(f"{pct(c, n)}  {f}")
    bycls, bysite = collections.Counter(), collections.Counter()
    tot = 0.0
    for e in events(path, "jdk.ObjectAllocationSample"):
        if thread and thread not in e["thread"]:
            continue
        w = e["weight"]
        tot += w
        bycls[e["cls"]] += w
        site = next((f for f in e["frames"] if is_app(f)), "?")
        bysite[(site, e["cls"])] += w
    print(f"\n## allocation by class (sampled weight {tot / 1e9:.2f} GB)")
    for c, w in bycls.most_common(top):
        print(f"{pct(w, tot)}  {w / 1e9:7.2f} GB  {c}")
    print("\n## allocation by site (first app frame, class)")
    for (s, c), w in bysite.most_common(top):
        print(f"{pct(w, tot)}  {w / 1e9:7.2f} GB  {s}  [{c}]")


def callers(path, needle, depth, alloc):
    chains = collections.Counter()
    n = hit = selfhit = 0
    ev = "jdk.ObjectAllocationSample" if alloc else "jdk.ExecutionSample"
    for e in events(path, ev):
        fr = e["frames"]
        w = e["weight"] if alloc else 1
        n += w
        idx = next((i for i, f in enumerate(fr) if needle in f), None)
        if idx is None:
            continue
        hit += w
        if idx == 0:
            selfhit += w
        chain = [f for f in fr[idx + 1:] if is_app(f)][:depth]
        chains[" <- ".join(chain)] += w
    print(f"total {n:.0f}; containing '{needle}': {pct(hit, n)}; as top frame {pct(selfhit, n)}")
    for c, k in chains.most_common(25):
        print(f"{pct(k, n)}  {c}")


def timeline(path, step, thread):
    rows = collections.defaultdict(collections.Counter)
    t0 = None
    for e in events(path, "jdk.ExecutionSample"):
        if thread and thread not in e["thread"]:
            continue
        if not e["frames"]:
            continue
        t0 = e["t"] if t0 is None else min(t0, e["t"])
        rows[e["t"]][classify(e["frames"], BUCKETS)] += 1
    bins = collections.defaultdict(collections.Counter)
    for t, c in rows.items():
        bins[int((t - t0) // step)].update(c)
    names = [b for b, _ in BUCKETS] + ["other"]
    print("t_start    n  " + " ".join(f"{x[:10]:>10s}" for x in names))
    for k in sorted(bins):
        c = bins[k]
        n = sum(c.values())
        print(f"{k * step:7.0f} {n:4d}  " + " ".join(f"{100.0 * c[x] / n:10.1f}" for x in names))


def main():
    args = sys.argv[1:]
    path = args[0]
    opt = {args[i]: (args[i + 1] if i + 1 < len(args) else None) for i in range(1, len(args))}
    top = int(opt.get("--top") or 30)
    depth = int(opt.get("--depth") or 4)
    thread = opt.get("--thread")
    if "--callers" in opt:
        callers(path, opt["--callers"], depth, False)
    elif "--alloc-callers" in opt:
        callers(path, opt["--alloc-callers"], depth, True)
    elif "--timeline" in opt:
        timeline(path, float(opt["--timeline"]), thread)
    else:
        summary(path, top, thread)


if __name__ == "__main__":
    main()
