# E07 Fallback strategy order

Settings-only sweep of the routing fallback's strategy order and subset, and of the residual
continuation threshold, to find a cheaper order that loses nothing.

## Status

Designed; smoke-tested on Issue508-DAC2020_bm06, Issue313-FastTest and Issue367-Charger (earlier
runs from the bm06 bisect, see below);
not yet run at scale.

## Hypothesis

bm06 spends 61% of its job in the fallback (92 passes; v2.5.0 needs 7). The default order runs
`direct-first` and `fanout-retry` (full pass loops) before `short-escape`, the strategy that wins.
Putting the cheap fanout-only strategies first, or dropping strategies that never win
(`no-fanout` never won on the camp3 boards), should cut fallback CPU by 30-50% on the boards that
enter the fallback, with no board worse in completion, vias or reload-DRC violations. The risk is
the other direction: on Issue684 `direct-first` is the winner, and an order that reaches it later
(or a subset without it) costs more or loses completion there.

## Mechanism (commit d4383b44)

- `DefaultSettings.java:132-133`: `DEFAULT_FALLBACK_STRATEGIES =
  "direct-first,fanout-retry,short-escape,open-escape,no-fanout"`; `:197`
  `residualRepairMaxOpen = 4`. Flags: `--router.autorouter.fallback_strategies`,
  `--router.autorouter.residual_repair_max_open` (`AutorouterSettings.java:68-90`).
- `RoutingFallback.java:130-154` parses the list in order (`none` disables it); `:104-124` drops
  fanout strategies on boards without SMD fanout; `:56-97` is the catalog (`fanout-retry` and
  `short-escape` are fanout-only: they stop after the fanout when it repeats an earlier pass-loop
  start, `RoutingFallbackRunner.java:106-124`).
- `RoutingFallbackRunner.java:94-97`: with 1..`residual_repair_max_open` open connections the board
  is continued first; `:98-141`: alternatives in order, each a full pass loop from the input; the
  loop stops at the first complete-and-clean result (`:99`, `:138`), so the order decides how many
  pass loops run. `:133` `replaces()` keeps the first strictly better result (`RoutingFallback.java:
  280-300`: fewer opens, then fewer new violations, then fewer violations, then higher score), so
  the order can also change the winner when two strategies tie on opens and violations.
- `RoutingSupervisor.java:145-151`: `residual()`; with 0 there is no continuation before or after
  the alternatives (the post-alternative `continue` also breaks on non-residual,
  `RoutingFallbackRunner.java:231-233`).
- `RoutingFallbackTest.defaultStrategiesStartWithDirectFirst` pins the current default; adopting an
  order changes that test (`SP/agent-bm06/patches/fixD-fallback-cheap-first.diff` shows the edit).

## Change (settings only)

`--router.autorouter.fallback_strategies=<order>` and `--router.autorouter.residual_repair_max_open=<n>`;
see `variants.txt`. No code. The fallback only runs after a stagnation/no-improvement/completed/error
stop with open connections or new violations, so boards that finish clean or stop at the pass limit
must be byte-identical across all variants (a free consistency check).

## Determinism and time-budget check

Only the order and membership of deterministic attempts change; no decision depends on time. A run
that hits `--timeout` is reported separately (Issue297 timed out in camp3 master).

## Boards

`boards.txt`: every board that entered the fallback in `SP/camp3/run1/master` (bm06, Issue313,
Issue367, Issue015, Issue157, Issue684, Issue110, Issue297), and six that do not (bm02, bm07,
Issue155, Issue558, bm11, bm05) as identity checks. Cheapest smoke boards: Issue367 (16 CPU s),
Issue313 (40 CPU s).

## Variants

`variants.txt`, first line control (head default). `short-first` is the fixD order;
`fanout-only-first` puts both fanout-only strategies first; `short-direct` keeps direct-first
second; `drop-no-fanout` and `short-direct-open` are subsets; `direct-only` and `fallback-none`
bound the cost; `residual0`/`residual8` move the continuation threshold; `short-first-residual0`
combines the two cheapest settings seen on bm06.

## How to run

```
python3 scripts/experiments/paired_campaign.py --out out/e07 \
  --boards docs/research/experiments/E07-fallback-order/boards.txt \
  --variants docs/research/experiments/E07-fallback-order/variants.txt \
  --flags "-mt 1 -mp 25 --router.optimizer.enabled=false" --parallel 3
```
`head` = a jar built from d4383b44 (`scripts/experiments/build_variant.sh d4383b44 - out/head.jar`).
Which strategy won: grep `Routing fallback chose '` in each run's console log (the harness stores
the logs per board and variant); passes: count `Auto-routing pass #`.

## Metrics and decision rule

Per board and variant: router unrouted/violations, reload-DRC unconnected and violations by type,
vias, wire length, CPU s, passes, fallback winner, SES sha1.
- Adopt an order (as the new default) only if, on every board that does not time out, it is not
  worse than control in reload-DRC unconnected count, reload-DRC violations (any type), and router
  unrouted, and has at most control's via count on boards where completion is equal; and the total
  CPU over the fallback boards drops by at least 15%. Report the CPU saving per board.
- Non-fallback boards must be byte-identical to control; a difference means the settings leaked
  outside the fallback (investigate before reading anything else).
- A subset that drops a strategy is adoptable only if that strategy never wins on these boards and
  on a second sample (for example the PCBench tier-A boards) before it becomes the default.

## Cost estimate

control CPU from SP/camp3/run1/master: bm06 82, Issue313 40, Issue367 16, Issue015 101, Issue157
124, Issue684 322, Issue110 69, bm02 8, bm07 12, Issue155 12, Issue558 19, bm11 57, bm05 116 s =
about 980 CPU s, plus Issue297 up to 1460 CPU s (timeout). Variants are cheaper or equal except
where a later winner is reached later. 11 variants: about 3 CPU-hours without Issue297, about 7.5
with it (drop it with a `#` for a quick run). About 1.2 wall-hours at `--parallel 3` without
Issue297.

## Smoke-test results

bm06, earlier runs from `SP/agent-bm06/runs/Issue508-DAC2020_bm06/` (jar 5b9bbd4b = the camp3 master
jar, standard flags `-mt 1 -mp 25`, optimizer off; all complete with 0 violations, reload DRC clean):

| variant | CPU s | wall s | passes | winner | vias | SES |
|---|---|---|---|---|---|---|
| control (default order) | 85.9 | 67.8 | 92 | short-escape | 26 | 3bb0bc3321 |
| short-escape first (m-F1) | 57.6 | 89.4 | 46 | short-escape | 26 | 3bb0bc3321 |
| short-escape first, residual 0 (m-F12) | 42.9 | 69.6 | 21 | short-escape | 26 | 3bb0bc3321 |
| direct-first then fanout-retry then short-escape, residual 0 (m-F2) | 74.2 | 109.4 | 67 | short-escape | 26 | 3bb0bc3321 |
| none (m-fbnone) | 39.6 | 57.2 | 18 | - | 29 | 3 unrouted |

(wall times are from an overloaded machine; compare CPU.) The SES is identical in all four
fallback variants: on bm06 the reorder and the residual threshold only remove work.

Same jar and flags, short-escape first with residual 0 (m-F12) against control (m-1):

| board | control CPU s / passes | m-F12 CPU s / passes | SES |
|---|---|---|---|
| Issue313-FastTest | 36.7 / 108 | 30.3 / 90 | identical (ca2a8be952), 2 open, 122 holeClearance in both |
| Issue367-Charger | 15.2 / 7 | 14.2 / 7 | identical (5bfc1c897d) |

Issue367-Charger with the head-equivalent jar (fix9 classes) and `-mt 4`, control: 15.9 CPU s,
7 passes, default kept, SES 5bfc1c897df7 (same as the camp3 master jar). The full sweep (other
fallback boards, the subsets, Issue684 where direct-first wins) has not been run.

## Risks and follow-ups

- Issue684 is the only board where `direct-first` wins; orders that reach it later will cost more
  there, and subsets without it may lose completion. Treat a regression there as a veto.
- Tie-breaking: `replaces()` keeps the first strictly better result, so with ties the order picks
  the winner (different SES with equal metrics). Report those cases; they are not regressions by
  the rule but they change outputs users see.
- The camp3 sample is small (8 fallback boards); confirm on PCBench before changing the default.
- Combine with E08 (parallel fallback): with parallel attempts the order matters less for wall time
  but still sets the CPU spent.
