# E08 Parallel fallback alternatives

Run the routing fallback's alternative strategies on worker threads and fold their results in
strategy order, so the chosen board (and the SES) is exactly the sequential one while wall time
drops.

## Status

Designed; smoke-tested on Issue367-Charger (p0 vs p2 SES identical); Issue313-FastTest and
Issue508-DAC2020_bm06 runs queued (see "Smoke-test results"); not yet run at scale.

## Hypothesis

When the default pass loop stalls, `RoutingFallbackRunner` re-routes the input once per
alternative (`direct-first, fanout-retry, short-escape, open-escape, no-fanout`), each a full pass
loop, one after the other. They all start from the same serialized input, so they can run at the
same time. Expected: identical SES on every board (any difference is a bug); wall time of the
fallback phase divided by up to min(threads, alternatives that run before the fold stops); total
CPU unchanged or higher (attempts after the point where the sequential loop would stop are
computed and thrown away). On bm06 (61% of the job in the fallback, three alternatives before
short-escape wins) the job wall time should drop by roughly 30-45% at 4 threads. Routing quality
does not change by construction.

## Mechanism (commit d4383b44)

- `RoutingFallbackRunner.java:98-141`: the alternatives loop. Each `runAttempt` (`:267-299`)
  deserializes `input` (`:273`), clones `job.routerSettings` and applies the strategy (`:277-278`).
  Nothing an attempt computes depends on an earlier attempt except:
  - `passLoopStarts` (`:23`, filled at `:88` and `:123`): fanout-only strategies (`fanout-retry`,
    `short-escape`) stop after the fanout when the post-fanout board hash equals one an earlier pass
    loop started on (`:317-320`, `AutorouteBatchLoop.java:299-305`), and
  - the fold itself: `replaces()` (`:335-340`), the stop checks at `:99` and `:138`
    (`stopAlternatives`, `:342-347`), and `job.board` bookkeeping (`:136`).
- Shared-state blockers, all in the attempt path:
  - `runPrepared` (`:301-333`) writes `job.board` / `job.routerSettings` (`:310-311`) and resets the
    job thread's auto-router stop flag (`:309`); the pass loop stops itself through that shared flag
    (`AutorouteBatchLoop.java:774`), writes `job.board` (`:634`), `job.setCurrentPass` (`:348`) and
    `job.resultPhaseMetrics` (`:257`, `:661`), and reads `job.routerSettings` for scoring.
  - `PerformanceProfiler` is process-wide; each pass loop overwrites its configuration
    (`AutorouteBatchLoop.java:94`) and clears its totals at the end (`:697`).
  - Logging: `job.logInfo` lines of concurrent attempts would interleave.
  - Listeners copied onto each attempt (`:316`) write the job's intermediate output.

## Change (code, `patch.diff`)

Switch: `-Dfreerouting.exp.parallel_fallback=<threads>`, read once at class load
(`ParallelFallbackAttempts.CONFIGURED_THREADS`). Default 0 = the existing sequential loop; the
property-off code path performs exactly the same calls as head (the loop body moved to
`runAlternatives` with the stop reason held in a local). Effective workers =
min(property, `router.autorouter.max_threads` (set by `-mt`; the autorouter pass never reads it, see
`RouterSettings.getAutorouterMaxThreads`), number of alternatives); fewer than 2 = sequential.

- `ParallelFallbackAttempts` (new): submits every alternative, in strategy order, to a fixed pool;
  settings copies are made on the job thread before launch. Each worker routes through a private
  `AttemptJob` (a `RoutingJob` with its own board, settings, phase metrics and an `AttemptStop`
  flag). `AttemptStop` keeps the attempt's own auto-router stop private and follows the real job's
  stop request and `TIMED_OUT` state, so a timeout still only stops work. Log lines are buffered and
  replayed through the real job when the fold takes the attempt, so the log reads in sequential
  order. Workers get no board/task listeners (no intermediate output from worker attempts; the
  job's output keeps showing the incumbent). `BatchAutorouter.detachedAttempt` makes worker pass
  loops leave the global `PerformanceProfiler` configuration and totals alone.
- Fold (`RoutingFallbackRunner.runAlternatives`): takes results in order with the unchanged
  `replaces()`/`stopAlternatives()`/`stopped()` checks. A fanout-only worker only knew the pass-loop
  starts present at launch, so the fold re-checks its post-fanout hash against `passLoopStarts` as
  folded so far; a late match is treated as the sequential "stopped after the fanout" (log replayed
  up to the hash check, then the same skip line). When the fold stops, unconsumed workers are asked
  to stop and joined before the final continuations run.
- When the residual continuation runs first on the job thread, the pool starts with one worker
  fewer and grows to the full count afterwards, so at most `max_threads` attempts route at once.
- Tests: `ParallelFallbackAttemptsTest` (property parsing, thread cap, ordered log replay, private
  stop flag that follows the job's stop and timeout). Existing `RoutingFallbackTest` passes.

## Determinism and time-budget check

Each attempt is a pure function of (input bytes, settings copy, known pass-loop starts at launch);
the fold order and every decision are those of the sequential loop, so the result does not depend
on thread scheduling. The one cross-attempt dependency (`passLoopStarts`) is resolved at fold time
against the same map the sequential loop would see. No decision reads elapsed time or budget; a
timeout or cancel only stops workers (as it stops the sequential attempt), and, as today, a run that
hits the timeout is not comparable. Shared read-only objects: the input `byte[]`, the input
`ClearanceBaseline` (immutable), and the job's `resourceUsage` (read for log text only).

## Boards

`boards.txt`: the three identity boards from the task (cheap to mid), the other fallback boards
from `SP/camp3/run1/master` (wall-time gain), Issue110 (fallback with zero alternatives: workers
never start), and three no-fallback boards (inert).

## Variants

`variants.txt`: `control` (property off), `par2`, `par4`. All arms must run with the same `-mt`,
which must be at least the largest thread count (`-mt 4`).

## How to run

```
scripts/experiments/build_variant.sh d4383b44 docs/research/experiments/E08-parallel-fallback/patch.diff out/e08.jar
python3 scripts/experiments/paired_campaign.py --out out/e08 \
  --boards docs/research/experiments/E08-parallel-fallback/boards.txt \
  --variants docs/research/experiments/E08-parallel-fallback/variants.txt \
  --flags "-mt 4 -mp 25 --router.optimizer.enabled=false" --parallel 1
```
(`head` in variants.txt = the jar built with the patch.) Use `--parallel 1` (or a machine with at
least 4 free cores per job): the par4 arm uses up to 4 cores, and wall time is the metric.

## Metrics and decision rule

- Hard gate: SES sha1 of `par2` and `par4` equal to `control` on every board that does not time
  out. One difference rejects the change (it would be a determinism bug, not a trade-off).
- Report per board: wall s, CPU s (process rusage), passes in the log, fallback winner.
- Adopt (as a default worker count derived from `max_threads`) if the total wall time over the
  fallback boards drops by at least 20% at 2 threads with total CPU up by at most 25%.

## Cost estimate

control CPU per board from SP/camp3/run1/master: bm06 82 s, Issue313 40 s, Issue367 16 s,
Issue015 101 s, Issue157 124 s, Issue684 322 s, Issue110 69 s, bm02 8 s, Issue155 12 s, bm05 116 s
= about 890 CPU s per arm; par arms up to about 1.3x that (discarded attempts). Three arms: about
0.9 CPU-hours, about 0.5-0.7 wall-hours with `--parallel 1` on an idle 4-core machine.

## Smoke-test results

Jar: `SP/ab/jars/fix9.jar` with the patched classes compiled by javac 25 against it (the touched
head classes compile to bytecode identical to fix9's, checked with `javap -c -p`). Flags:
`-mt 4 -mp 25 --router.optimizer.enabled=false --router.job_timeout=00:20:00`, through the slot
limiter on a shared, overloaded 4-core machine (wall times are noisy).

| board | arm | wall s | CPU s | passes | chose | final | vias | SES sha1 |
|---|---|---|---|---|---|---|---|---|
| Issue367-Charger | p0 | 25.8 | 15.9 | 7 | default | 6 unrouted and 1 violation | 15 | 5bfc1c897df7 |
| Issue367-Charger | p2 | 17.8 | 13.7 | 7 | default | 6 unrouted and 1 violation | 15 | 5bfc1c897df7 |

Issue367: p0 and p2 give the same SES (5bfc1c897df7, also the camp3 master SES), the same chosen
strategy and the same log order. The remaining identity runs (Issue367 p4 and head; Issue313 and
bm06, arms p0, p2, p4 and head) were queued behind about 40 other jobs on the machine's 3-slot
limiter and had not started when this package was committed; they keep running in the background
and append to `SP/agent-exp-fallback/runs/results.jsonl` (render with
`python3 SP/agent-exp-fallback/tools/table.py SP/agent-exp-fallback/runs/results.jsonl`; each run's
console log and SES are in `SP/agent-exp-fallback/runs/<board>/<arm>/`). Until they land, the
identity claim is proven on Issue367 only (where the fallback runs 5 alternatives, two of them
fanout-only repeats, and keeps the default).

The replayed fallback log of Issue367 p2 is line-for-line the p0 log except the per-pass "using N
CPU seconds and the job allocated" figures, which come from the job's sampled resource usage and
are stale for worker attempts.

## Risks and follow-ups

- CPU waste: attempts after the winner are computed and discarded (bm06 at 4 threads: open-escape, and
  no-fanout once a worker frees up, start before short-escape wins). A "speculate only k ahead" pool (k = threads) bounds it; the
  pool is FIFO so later strategies start last.
- Memory: up to `threads` extra boards and search trees live at once; check peak heap on the
  largest boards (Issue684, Issue157) before raising the default.
- Workers send no progress events: the GUI and the API's intermediate output do not show worker
  attempts, and the per-pass CPU/memory figures in their replayed log lines are stale.
- Static engine state: the engine is already used by parallel optimizer workers (thread-local search
  stacks and shove cache, synchronized `BoardStatisticsBoundsCalculator` cache), but nothing
  enforces that for the full pass loop; keep the SES identity gate on every board.
- Continuations stay sequential (each depends on the previous incumbent).
- The fold's late-repeat handling drops the end-of-loop summary lines of an attempt the sequential
  loop would have stopped after its fanout; log-scraping metrics that count those lines differ.
