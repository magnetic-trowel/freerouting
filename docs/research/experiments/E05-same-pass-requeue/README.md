# E05 Same-pass requeue of ripped nets

After a successful attempt, one item per broken component of each net it ripped is queued for a
retry within the same pass (at most twice per net and pass), so victims are re-routed while the
space around them is still the one they were ripped from.

## 2. Status

Designed; smoke-tested on Issue508-DAC2020_bm04 (8 passes) and the off switch on bm02/bm07; not
yet run at scale.

## 3. Hypothesis

Today a victim waits for the next pass (`AutoroutePassRunner.java:102-201` iterates a list fixed at
pass start), by which time later items of the same pass have taken its space, and the next pass
rips again at a higher cost. Re-routing the victims immediately should complete more connections
per pass and need fewer rip cycles. An earlier implementation (no patch survived) measured on 14
boards, 25 passes: open connections 11 -> 10 (bm04 2 -> 0, bm05 9 -> 10), vias 773 -> 716 (-7%),
BBD Mars-64 4 -> 1 open but it hit the 15-minute limit under heavier load (see
`docs/research/routing-performance-and-correctness-review.md`, "Measured and not adopted").
Expected now: the same direction (fewer opens on bm04, BBD Mars-64; -5% vias), with the risk of
more CPU per pass.

## 4. Mechanism (code at d4383b44)

- The pass list is built once by `BatchAutorouter.getAutorouteItems` (`BatchAutorouter.java:457-543`)
  and reordered (`AutoroutePassRunner.java:257-322`); the loop at `AutoroutePassRunner.java:102`
  never adds to it.
- A ROUTED attempt leaves its victims ripped (`AutorouteConnectionRouter.java:28-35`); they are only
  retried in the next pass, whose rip cost is `startRipupCosts * (passNo + 1)`
  (`AutorouteConnectionRouter.java:112`).
- The oscillation on bm04 (8,10,9,11,8,8,8,5,9,11,3,... open per pass at 25 passes) shows victims
  that fail in the pass after they are ripped and then rip their rippers back.

## 5. Change

Code, `patch.diff` (applies with `git apply` on d4383b44; commit b2ae9c3f in the design worktree):

- New `autoroute/pipeline/SameNetRequeue.java` (one instance per pass). After a ROUTED attempt
  with a non-empty ripped list, for each net of the ripped items (ascending net number) whose
  requeue count this pass is below 2: split the net's connectable items (ascending id) into
  connected components; if there are at least two, append one start item per component to the
  pass list (its lowest-id pin, else its lowest-id non-trace connectable item; components of a
  plane net that already touch the plane are skipped, as `getAutorouteItems` does), skipping
  items still pending later in the pass.
- `AutoroutePassRunner.runSingleThread`: the item loop is index based so appended items are
  reached (identical iteration when nothing is appended). A failed same-pass retry counts as not
  routed but is not recorded in the failure log and does not trigger the failed-net rip
  (`AutoroutePassRunner.java:166-195`): the item's regular attempt in the next pass does that, so
  the failure log still sees at most one failure per item and pass.
- Switch: `-Dfreerouting.exp.requeue_ripped=true`, read once at class load; default false. Off,
  no item is ever appended and the loop is the same as before, so the off arm is byte-identical to
  head (checked, section 12).
- Tests: `SameNetRequeueTest` (off by default, one start item per component until the net is
  connected, at most twice per net and pass, pending items not duplicated).

## 6. Determinism and time-budget check

Nets are visited in ascending net number, items and components in ascending item id, and the
pass list is appended in that order; the per-net budget is a count. No clock or budget is read.
Retries consume the `router.max_items` budget like other attempts (the item limit is a work bound,
not time).

## 7. Boards

As E04 (`boards.txt`): the oscillating boards, the other boards with opens, design-blocked boards
and boards that complete today; smoke boards bm02, bm07.

## 8. Variants

`variants.txt`: `control`, `requeue` (`-Dfreerouting.exp.requeue_ripped=true`).

## 9. How to run

```
scripts/experiments/build_variant.sh d4383b44 docs/research/experiments/E05-same-pass-requeue/patch.diff build/E05.jar
python3 scripts/experiments/paired_campaign.py --out logs/E05 \
  --boards docs/research/experiments/E05-same-pass-requeue/boards.txt \
  --variants docs/research/experiments/E05-same-pass-requeue/variants.txt \
  --head-jar build/E05.jar \
  --flags "-mt 1 -mp 25 --router.optimizer.enabled=false" \
  --parallel 4 --timeout 00:30:00
```
(Without `--head-jar`, `--head-commit d4383b44 --head-patch
docs/research/experiments/E05-same-pass-requeue/patch.diff` makes the harness build the jar itself.) All variants
run with the same jar, so the identical-SES matrix also checks the off switch on every board: the
control must equal a run of a jar built from plain d4383b44 (add `plain <that jar>` as a second
line to `variants.txt` on a few boards to see it in the matrix).

The per-pass open counts, which show the oscillation, are in the router log:
`grep -o 'pass #[0-9]* .*unrouted' logs/E05/runs/<board>/<variant>/console.log`.

## 10. Metrics and decision rule

Per board and variant, from `summary.jsonl`: router unrouted and violations, reload-DRC
unconnected and clearance violations by type, vias, wire length, CPU seconds, passes. Runs that hit
the 30-minute safety stop are reported separately and excluded.

- Must not regress (any single board): reload-DRC clearance violations never increase; router
  unrouted and reload-DRC unconnected never increase; a board that is complete in the control
  stays complete.
- Must improve: the sum of open connections (reload-DRC unconnected, design-blocked opens
  excluded by comparing against the control) over the oscillating and open boards drops by at
  least 3, or at least two boards become complete, with no board worse.
- Secondary (tie-breakers, may not regress by more than the stated amount): vias +3%, wire length
  +2%, CPU time +25% summed over the boards that finish inside the time limit.
- Adopt (with a `docs/research` note and the setting made default or removed) only if the rule
  holds; otherwise record the numbers under "Measured and not adopted".

## 11. Cost estimate

From the head-like master timings in `SP/camp3/run1` (one thread, optimizer off) scaled to the pass
limits in `boards.txt`, plus BBD Mars-64 at 12 passes (about 250 s): about 3,250 s of wall time
per variant, roughly 0.9 CPU-hours. Retries add maze searches; budget up to +30% for the requeue
arm (see section 12 for the measured bm04 overhead). Two variants: about 2 CPU-hours plus 0.1 hours
of reload DRC; at `--parallel 4` about 35 minutes wall.

## 12. Smoke-test results

All runs: one jar built from d4383b44 plus the three experiment patches (`combined.diff`; every
switch off by default), `-mt 1 --router.optimizer.enabled=false --router.job_timeout=00:20:00
-Xmx1500m`, run one at a time through the slot limiter on the shared 4-core machine (wall times
are load-dependent). Router counts are the job's own final line; no reload DRC was run for the
smoke (the router reported 0 clearance violations in every run).

Off switch (byte-identical to head): with no property set, the jar's SES equals the one from
`SP/ab/jars/fix9.jar` (head behaviour) on bm02 (sha1 125cea8b3db0, 0 open, 16 vias) and bm07 (sha1
366fba4d3405, 0 open, 31 vias), `-mp 8`. On bm04 `-mp 8` the control's per-pass open counts
(8,10,9,11,8,8,8,5) equal the first eight passes of the head-like master in `SP/camp3/run1`.

Determinism of the switched-on code: bm07 `-mp 8` with all three switches on
(`rip_history=1 requeue_ripped=true last_mile=8`) twice gave the same SES (sha1 8289c499821f; open
counts 1,0,0 against the control's 4,4,1,0,0, and 28 vias instead of 31).

Issue508-DAC2020_bm04, `-mp 8` (8 passes; unrouted is the router's count after each pass):

| run | unrouted after pass 1..8 | final unrouted | violations | vias | CPU s (wall s) |
|---|---|---|---|---|---|
| control (all off) | 8,10,9,11,8,8,8,5 | 5 | 0 | 62 | 85.1 (121) |
| requeue_ripped=true | 9,4,4,4,4,10,1,1 | **1** | 0 | 65 | 78.8 (109) |

The requeue cut the open count after pass 2 from 10 to 4 and ended at 1 open instead of 5, with 3
more vias (+5%) and 7% less CPU over the same 8 passes. Pass 6 still jumped to 10 (the
oscillation is damped, not gone). One board and 8 passes is not evidence of a gain; the campaign
decides.

## 13. Risks and follow-ups

- More attempts per pass: each retry is a maze search with the current rip cost, so CPU per pass
  grows (bounded by 2 requeues per net and pass, one item per component). Watch CPU on bm01 and
  BBD Mars-64; the earlier BBD Mars-64 run hit the time limit.
- A retry can rip the item that just routed (ping-pong inside the pass); the per-net budget of 2
  bounds it, and E04 (rip history) is the natural complement to try in combination
  (`-Dfreerouting.exp.rip_history=1 -Dfreerouting.exp.requeue_ripped=true`; the combined patch is
  `SP/agent-exp-ripup/patches/combined.diff`).
- Retries do not record failures, so `dropDesignBlockedItems` and the failed-net rip see the same
  failure history as today; a variant that records them would rip more aggressively.
- Plane nets: components touching the plane are skipped; if plane boards (Issue093, tairakb) move,
  check the plane-net path first.
