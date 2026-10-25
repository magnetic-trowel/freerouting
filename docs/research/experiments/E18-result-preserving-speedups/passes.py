#!/usr/bin/env python3
"""usage: passes.py <console log>... : per-stage pass table (CPU per pass from cumulative CPU)."""
import re, sys
P = re.compile(r"Auto-routing pass #(\d+) .*completed in ([0-9.]+) seconds .*\((\d+) unrouted and (\d+) violations?\), using ([0-9.]+) CPU")
S = re.compile(r"Auto-routing stage started .*for (\d+) unrouted items")
F = re.compile(r"Fanout stage completed: .*completed in ([0-9.]+) seconds.*using ([0-9.]+) total CPU seconds, ([0-9.]+) GB")
FB = re.compile(r"(Routing fallback[^:]*):")
E = re.compile(r"Auto-routing stage (completed|interrupted).*using ([0-9.]+) total CPU seconds, ([0-9.]+) GB")
for path in sys.argv[1:]:
    print(f"== {path.split('/')[-2]}")
    last = 0.0; stage = 0; rows = []
    fan_cpu = 0.0
    for line in open(path, errors="replace"):
        m = F.search(line)
        if m:
            fan_cpu += float(m.group(2)); print(f"  fanout: wall {m.group(1)} s, cpu {m.group(2)} s, alloc {m.group(3)} GB"); continue
        m = S.search(line)
        if m:
            stage += 1; print(f"  stage {stage}: start {m.group(1)} unrouted items"); continue
        m = P.search(line)
        if m:
            cpu = float(m.group(5)); d = cpu - last if cpu >= last else cpu; last = cpu
            print(f"    pass {int(m.group(1)):3d}: wall {float(m.group(2)):7.2f}  dcpu {d:7.2f}  cum {cpu:8.2f}  unrouted {m.group(3)}")
            continue
        m = E.search(line)
        if m:
            print(f"  end: total cpu {m.group(2)} s, alloc {m.group(3)} GB, fanout cpu sum {fan_cpu:.1f} s")
