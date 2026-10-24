# E06 Transactional last-mile repair

On stagnation, route one open connection on a copy of the board with rip-up at the pass-1 rip
cost, then re-route the nets it ripped, and adopt the copy only if the open count dropped with no
new clearance violation; otherwise drop the copy, which leaves the board exactly as it was.

## 2. Status

Designed; smoke-tested on Issue508-DAC2020_bm04 (8 passes) and the off switch on bm02/bm07; not
yet run at scale.

## 3. Hypothesis

When the loop stagnates with a handful of opens, the remaining connections usually need one cheap
rip that the late passes no longer allow (their rip cost is `startRipupCosts * passNo`), and the
rip-up that does happen is not transactional: victims stay ripped and the next pass can be worse,
which is the oscillation on bm04/bm05/Issue039/BBD Mars-64. A transaction that tries the cheap rip,
repairs the victims, and keeps the result only when the open count drops can only lower the open
count of the board it starts from. Expected: -1 to -2 opens on each oscillating board, no new
violations by construction (checked with the full clearance DRC), a few percent more CPU.

## 4. Mechanism (code at d4383b44)

- Rip cost grows with the pass (`AutorouteConnectionRouter.java:112`), so a connection that needs
  to displace another net gets harder to route in each later pass.
- The existing last-mile step (`AutorouteBatchLoop.java:517-537`, `LastMileBlockerRipup.java:29-63`)
  removes up to 24 foreign traces/vias in the airline boxes of the opens after 3 passes without
  score gain, with no routing and no rollback; it only runs from pass 8 (the stagnation block is
  guarded by `currentPass >= STOP_AT_PASS_MINIMUM`, `AutorouteBatchLoop.java:483`) and between
  board restores the no-improvement counter is reset (`AutorouteBatchLoop.java:415`), so on bm04 it
  rarely fires.
- Exact rollback: `RoutingBoard.deepCopy()` (`board/facade/RoutingBoard.java:1697`) serializes the
  whole board, including its id generator and failure log; the transaction runs on the copy, so a
  rejected transaction never touches the live board.

## 5. Change

Code, `patch.diff` (applies with `git apply` on d4383b44; commit c2f7aa67 in the design worktree):

- New `autoroute/pipeline/TransactionalLastMile.java`, called by `AutorouteBatchLoop` right after
  each pass (before the restore check, so an adopted board takes part in it). It tracks the lowest
  open count; when the current count is 1..16 and has not reached a new low for 2 passes, it lists
  the open connections (`getAutorouteItems`, minus design-blocked ones) sorted by net number and
  item id, and runs transactions in that order until one is adopted or the budget is used:
  1. `copy = board.deepCopy()`; `router.board = copy`;
  2. route the item with rip-up at pass-1 cost (`ripupPassNo = 1`);
  3. if it routed, route every queued item of the ripped nets on the copy at the current pass's
     rip cost; remove tails as at pass end;
  4. adopt the copy if the open count dropped and the full clearance-violation count
     (`BoardStatistics`, from `DesignRulesChecker.getAllClearanceViolations()`) did not grow;
     otherwise restore `router.board`, the necked-net set and the necked counters.
- Each transaction logs one INFO line (`Transactional last-mile attempt ...: kept|rolled back,
  open a -> b, clearance violations c -> d`).
- Switch: `-Dfreerouting.exp.last_mile=<max attempts>` (transactions per pass loop), read once at
  class load; default 0 = off, in which case the loop never constructs the helper and is
  byte-identical to head (checked, section 12). The existing `LastMileBlockerRipup` step is kept
  unchanged in both arms.
- Tests: `TransactionalLastMileTest` (parsing, off by default, runs only after stagnation, adopts an
  improving copy and leaves the original untouched, attempt budget).

## 6. Determinism and time-budget check

Candidates are sorted by (net, item id), victims follow `getAutorouteItems` order, the board copy is
a deterministic serialization, and the trigger counts passes, not time. The attempt budget is a
count. The connection router's existing per-attempt `TimeLimit` (`AutorouteConnectionRouter.java:138-140`,
100 s and more) is a safety stop that does not fire on these boards.

## 7. Boards

As E04 (`boards.txt`). The transaction only runs with 1..16 opens, so boards that complete early
are not affected except through the trigger (they must stay byte-identical; the identical-SES
matrix checks it).

## 8. Variants

`variants.txt`: `control`, `last_mile4`, `last_mile8`, `last_mile16`.

## 9. How to run

```
scripts/experiments/build_variant.sh d4383b44 docs/research/experiments/E06-transactional-last-mile/patch.diff build/E06.jar
python3 scripts/experiments/paired_campaign.py --out logs/E06 \
  --boards docs/research/experiments/E06-transactional-last-mile/boards.txt \
  --variants docs/research/experiments/E06-transactional-last-mile/variants.txt \
  --head-jar build/E06.jar \
  --flags "-mt 1 -mp 25 --router.optimizer.enabled=false" \
  --parallel 4 --timeout 00:30:00
```
(Without `--head-jar`, `--head-commit d4383b44 --head-patch
docs/research/experiments/E06-transactional-last-mile/patch.diff` makes the harness build the jar itself.) All variants
run with the same jar, so the identical-SES matrix also checks the off switch on every board: the
control must equal a run of a jar built from plain d4383b44 (add `plain <that jar>` as a second
line to `variants.txt` on a few boards to see it in the matrix).

The per-pass open counts, which show the oscillation, are in the router log:
`grep -o 'pass #[0-9]* .*unrouted' logs/E06/runs/<board>/<variant>/console.log`.

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
per variant, roughly 0.9 CPU-hours. A transaction costs one board copy, one or a few maze searches
and one full clearance DRC (well under a pass on the boards here); boards that complete early never
run one. Four variants: about 3.8 CPU-hours plus 0.2 hours of reload DRC; at `--parallel 4` about
1 hour wall.

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
| last_mile=8 | 8,10,**8**,11,**7**,8,**7**,7 | 7 | 0 | 62 | 82.7 (119) |

(Bold: passes after which a transaction was adopted; the pass line is printed after the
transaction.) Three transactions ran, after passes 3, 5 and 7, all on the same connection (net #1,
item 124), and all three were kept (open 9 -> 8, 8 -> 7, 8 -> 7; clearance violations 0 -> 0).
The pass that followed each one broke the connection again, so the transaction does not remove
the cause of the cycle, and the changed boards sent the last pass to 7 open instead of the
control's 5. The rollback path was not exercised on this board (no transaction was rejected), and
`TransactionalLastMileTest` covers only the adopt case and the untouched original; check the
`rolled back` log lines on the campaign boards where it fires. Follow-up worth testing with the
campaign: skip a connection whose transaction was adopted earlier in the same pass loop (it is
being re-broken), or run the transaction after the board restore instead of before it.

## 13. Risks and follow-ups

- Each transaction costs one board serialization, one maze search at pass-1 cost, the victims'
  searches and one full clearance DRC. With 16 attempts on a large board this is a few passes'
  worth of CPU; the attempt budget bounds it.
- The acceptance test uses the router's open count and the full clearance DRC; it does not look at
  via count or length, so an adopted transaction may add vias. If vias grow, require score
  improvement (`BoardStatistics.getRouterScore`) as well.
- Only one connection per transaction: two opens that block each other need a pair transaction
  (route A then B, judge both); a follow-up if single transactions are often rejected with
  "open n -> n".
- Interaction with board restores: an adopted board is added to the board history at the next
  pass start like any other; the final best-board restore can still pick an earlier board if it
  scores higher (for example fewer vias), which is the intended behaviour.
- Candidates are tried in (net, item id) order, so on boards with more than the budget of opens
  the low-numbered nets always go first; a rotation by pass number would spread the attempts.
