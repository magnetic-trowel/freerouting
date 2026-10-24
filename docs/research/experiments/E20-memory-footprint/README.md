# E20 Memory footprint: what holds the live heap, and compressing stored boards

Measure the live heap of a routing job board by board, attribute it, and test a result-preserving
reduction: keep the board-history snapshots and the routing-fallback input Deflate-compressed.

## 2. Status

Designed; smoke-tested on Issue508-DAC2020_bm02, Issue217-8088sbc and
Issue157-TeamAdapt-LinePCB; not yet run at scale.

## 3. Hypothesis

The "peak heap usage" Freerouting prints is heap *occupancy* (`MemoryMXBean` used bytes,
garbage included), so it mostly shows how far G1 let the heap grow. The live set (what survives a
full GC) is an order of magnitude smaller on the smoke boards: 9 MB (ldo-led), 12 MB (bm02),
31 MB (8088sbc, reported 135 MB), 62 MB (TeamAdapt bounded, reported 270-1020 MB). The board
history holds one serialized board per pass (up to 30, 0.3-0.5 MB each here); Deflate level 1
shrinks them 3.7-4.5x. Expected effect of the change: peak live heap lower by up to
`entries x (1 - 1/ratio) x serialized size` (about 2 MB on 8088sbc at 10 passes, about 9 MB on
TeamAdapt at 25 passes, i.e. 5-15% of the live peak), with byte-identical SES and CPU within noise.
Larger boards with more items have larger entries, so the share should grow with board size, but
it is bounded by the 30-entry cap.

## 4. Mechanism (code at d4383b44)

- `AutorouteBatchLoop.java:83` creates one `BoardHistory` per pass loop; `:353` adds the board
  before every pass; `:684` clears it at the end. `BoardHistory.java:235` stores each entry as
  `board.serialize(false)` (complete Java serialization of the board, undo bookkeeping included;
  search trees are transient), capped at `MAX_HISTORY_SIZE = 30` (`:30`). Restores
  (`:111`, `:187`) deserialize the bytes. Entries are only read back by restore.
- `RoutingPipeline.java:119` serializes the input board once more for the routing fallback and
  keeps the bytes for the whole default pass loop and all alternatives
  (`RoutingFallbackRunner.java:273`).
- Heap attribution at the sampled live peak (full-GC histograms every 1.5-3 s, in-process probe):
  geometry objects of the live boards dominate (TeamAdapt: `IntPoint` 9.6 MB, `FloatPoint`
  6.1 MB, `Line` 4.7 MB, `IntDirection` 4.5 MB, `IntOctagon` 4.2 MB of 65 MB), then maze search
  state (`MazeListElement`, `MazeSearchElement`, `ExpansionDoor`: 3-5 MB), `byte[]` 6.3-6.6 MB
  (history entries, fallback input, log buffers), the shove-check memo (`ShoveCheckCache` keys,
  entries and regions: 2-6 MB; bounded at 65,536 entries per thread, `ShoveCheckCache.java:41`)
  and search-tree nodes (`ShapeTree$Leaf`/`InnerNode`, about 80 bytes per leaf per tree).
- Search trees: one general tree (`SearchTreeManager.java:33`, 45-degree bounding octagons) plus
  one autoroute tree per clearance class used (`:162-196`), of the angle restriction's type
  (`ShapeSearchTree45Degree` for 45-degree boards). 8088sbc keeps 3 trees (cl 0, 1, 3; 2,129
  leaves each); TeamAdapt keeps 1 (cl 0, 4,260 leaves). Each tree also caches its own tile
  shapes on every item. A deserialized board copy costs 1.7 MB (8088sbc) to 7.5 MB (TeamAdapt)
  with the general tree only, +0.4 MB per autoroute tree on 8088sbc.
- Undo snapshots do not accumulate in batch routing: each connection attempt pops or undoes its
  snapshot (`AutorouteConnectionRouter.java:72-90`).
- Optimizer: one board copy per candidate item from a `BoardImage` (`BatchOptimizer.java:1508`),
  so the live set is about (threads + 2) boards; not measured here (optimizer off).

## 5. Change

Code experiment, `patch.diff`, switched by `-Dfreerouting.exp.compress_boards=true` (read once
into a `static final` at class load; default `false` = head behaviour).

- New `app.freerouting.autoroute.StoredBoard`: holds the bytes of `BasicBoard.serialize(false)`,
  either as given or Deflate level 1 (`BEST_SPEED`) compressed; `restore()` inflates to exactly
  the original bytes and calls `BasicBoard.deserialize`, so a compressed and an uncompressed
  entry restore the same board.
- `BoardHistory.BoardHistoryEntry` stores a `StoredBoard` instead of a `byte[]` and reuses the
  hash `add()` already computed (the old constructor called `getHash()` a second time, which
  serializes the basic trace profile again; same value, one serialization less per pass).
- `RoutingPipeline.runRoutingStage` keeps the fallback input as a `StoredBoard`;
  `RoutingFallbackRunner.run/runAttempt` restore from it.
- Unit test `StoredBoardTest` (round trip is byte-exact, compressed restore equals plain restore,
  compression is at least 2x on a real board, null handling).

With the property off the stored bytes are the same array `serialize(false)` returned and
`restore()` is `BasicBoard.deserialize(bytes)`, as before, so the property-off arm is byte-identical
to head (checked: see section 13).

## 6. Determinism and time-budget check

Compression is a pure function of the serialized bytes and inflation returns them exactly, so
every restore yields the same board as head and no routing decision changes. Nothing reads a clock
or the remaining budget. The only effect is CPU time (Deflate per pass, Inflate per restore) and
heap.

## 7. Boards

`boards.txt`: two cheap smoke boards (ldo-led, bm02), three mid-size two-layer boards (RelayModule,
8088sbc, Mars-64), TeamAdapt-LinePCB (bounded by items; it runs the routing fallback with
continuations and five alternatives, so it exercises the fallback input and history restores), and
three larger boards (RoyalBlue54L 8-layer, Issue297-myboard, DAC2020_bm01) bounded by passes,
where serialized boards and the history are largest. All are must-not-regress boards: the change
must not alter any SES.

## 8. Variants

`variants.txt`: `control head` and `compress head -Dfreerouting.exp.compress_boards=true`.

## 9. How to run

```
E=docs/research/experiments/E20-memory-footprint
scripts/experiments/build_variant.sh d4383b44 $E/patch.diff /tmp/e20.jar

# (a) result identity, CPU and wall time: the compress arm must give the control's SES everywhere
python3 scripts/experiments/paired_campaign.py --out /tmp/e20-camp \
  --boards $E/boards.txt --variants $E/variants.txt --parallel 4 --timeout 00:20:00

# (b) peak live heap, per board and arm (each histogram forces a full GC, so its total is the
#     live set at that moment; the run keeps the largest one as peak_histo.txt)
while read -r board flags; do
  case "$board" in ''|'#'*) continue;; esac
  b=$(basename "$board" .dsn)
  $E/measure_heap.sh /tmp/e20.jar "$board" /tmp/e20-heap/control/$b 1500m 2 \
    -mt 1 --router.optimizer.enabled=false --router.job_timeout=00:20:00 $flags
  JVMEXTRA=-Dfreerouting.exp.compress_boards=true $E/measure_heap.sh /tmp/e20.jar "$board" \
    /tmp/e20-heap/compress/$b 1500m 2 \
    -mt 1 --router.optimizer.enabled=false --router.job_timeout=00:20:00 $flags
done < $E/boards.txt
grep -h . /tmp/e20-heap/*/*/result.txt
```

`measure_heap.sh <jar> <dsn> <outdir> <xmx> <histogram interval s|0> [flags]` writes `result.txt`
(`histo_peak_MB`, `gc_max_after_MB`, SES sha1, the router's final line), `peak_histo.txt`,
`histo_series.txt` (time, live bytes) and `gc.log`. It needs `jcmd` from the same JDK. The
histogram sampling only costs time; it cannot change the result (no routing decision reads a
clock).

In-process probes (`probes/`; compile with `javac -proc:none -cp <jar> -d <dir> probes/*.java`,
run with `java -Xmx1500m -cp <dir>:<jar> ...`):
`MemProbe <histogram ms> <dsn> <repeats> <cli flags>` routes a board `<repeats>` times in one JVM
through the scheduler and prints the sampled live peak with its histogram, the serialized and
Deflate sizes, the search trees with leaf counts, the live cost of a deserialized copy, and the
heap retained after each job is removed (cross-job leak check); `LoadProbe <dsn> <repeats>`
times DSN loading (alternating `DsnReader` and `HeadlessBoardManager` with the post-load scan)
with allocation and retained heap; `LeafProbe <dsn>` counts default-tree shapes per item class.

Optional minimal-heap check (binary search on `-Xmx` with the same SES): rerun (b) with
interval `0` and decreasing `<xmx>` until the run fails or the SES changes.

## 10. Metrics and decision rule

- Must hold (adoption blockers): for every board, the compress arm's SES sha1 equals the
  control's (the identical-SES matrix in `summary.md` is all "same"); hence also no change in
  unrouted, reload-DRC violations or completion. Any difference means a bug in the patch.
- Must improve: peak live heap (`histo_peak_MB`) of the compress arm, summed over the boards that
  finish, is at least 5% lower than the control's. Single-board peaks from a 2 s histogram
  interval are noisy by several MB (8088sbc above: +2.8 MB for an expected -2 MB), so judge the
  sum, repeat each heap run three times and use the median per board.
- Must not regress: total CPU seconds of the compress arm within +2% of the control.
- Adopt (make it the default and drop the switch) when all three hold; otherwise keep head.

## 11. Cost estimate

Per arm, from the smoke timings on the loaded 4-core machine (CPU seconds): ldo-led 1, bm02 1.5,
8088sbc (-mp 10) 15, TeamAdapt (bounded) 94; estimated from campaign logs: RelayModule 25,
Mars-64 (-mp 8) 60, bm01 (-mp 6) 150, Issue297 (-mp 6) 230, RoyalBlue54L (-mp 6) 600. About
0.33 CPU-hours per arm; campaign (2 arms) plus heap runs (2 arms, histogram sampling adds about
20%) is about 1.5 CPU-hours. RoyalBlue54L and Issue297 dominate; drop them for a quick run.

## 12. Smoke-test results

Jar: `fix9.jar` (head behaviour) with the four patched classes compiled against it (the
unpatched head sources of those classes compile to bytecode identical to fix9's). Flags
`-mt 1 --router.optimizer.enabled=false` plus the board flags of `boards.txt`.

Result identity (SES sha1, first 12 hex digits):

| Board | control (fix9) | compress | property off |
|---|---|---|---|
| Issue508-DAC2020_bm02 (-mp 25) | 125cea8b3db0 | 125cea8b3db0 | 125cea8b3db0 |
| Issue217-8088sbc (-mp 10) | 9717764282b5 | 9717764282b5 | - |
| Issue157-TeamAdapt-LinePCB (-mp 25, max_items 300; runs the fallback: continuation + 5 alternatives, history restores) | df835b84167d | df835b84167d | - |

Peak live heap, TeamAdapt (CLI, full-GC histogram every 3 s, 38-41 samples): control 69.4 MB,
compress 63.4 MB (-6.0 MB, -8.6%); the five largest samples were 61.7/62.2/63.6/68.7/69.4 MB vs
60.0/60.1/61.4/63.4/63.4 MB. CPU 93.5 s vs 94.2 s (+0.8%, noise on a shared machine). The
sampling interval makes single peaks noisy by a few MB: treat this as one paired observation,
not a measurement of the effect size. bm02 (5 passes of 0.13 MB entries, histogram every 1 s):
14 MB in both arms (`byte[]` at the peak 6.49 vs 6.26 MB), i.e. no measurable difference on a
board this small. 8088sbc (10 passes of 0.28 MB entries, expected saving about 2 MB): control 33.2 MB, compress
36.0 MB (2 s interval, 14 samples each; `byte[]` at the peak 7.26 vs 6.59 MB); CPU 15.7 vs
15.3 s. The expected saving is below the noise of a 2 s sampled peak here.

Live heap per board (in-process probe, fix9, full-GC histogram every 1.5-3 s) versus the
"peak heap usage" the router prints for the same kind of run:

| Board | items | serialized board | Deflate-1 | trees (leaves each) | deserialized copy | peak live | printed "peak heap" |
|---|---|---|---|---|---|---|---|
| Issue957-smoke-ldo-led | 101 | 0.03 MB | 2.9x | 2 (259) | 0.4 MB | 9.2 MB | 15 MB* |
| Issue508-DAC2020_bm02 | 568 | 0.13 MB | 3.4x | 2 (449) | 0.7 MB | 12.2 MB | 55-130 MB |
| Issue110-RelayModule | 1125 | 0.22 MB | 3.8x | 2 (833) | 0.9 MB | 31.3 MB | 152 MB* |
| Issue102-Mars-64 (-mp 4) | 1364 | 0.21 MB | 4.3x | 2 (1219) | 1.1 MB | 40.3 MB | 190 MB* |
| Issue217-8088sbc (-mp 10) | 1498 | 0.28 MB | 3.7x | 3 (2129) | 1.7 MB | 31.0 MB | 135 MB |
| Issue157-TeamAdapt (bounded) | 2650 | 0.48 MB | 4.5x | 1 (4260) | 7.5 MB | 62-69 MB | 311-342 MB |

\* printed by the probe run itself, whose histogram sampling forces a full GC every 1.5-2.5 s, so
it understates a normal run; the other values are from the plain CLI runs above (`-Xmx1500m`).

At the sampled peak the largest classes were: RelayModule `byte[]` 10.0 MB (history: 0.22 MB x
up to 25 entries, plus fallback input and log buffers); Mars-64 the shove-check memo at its cap
(65,536 `ShoveCheckCache$Key` 6.3 MB + `Entry` 2.6 MB + `QueryRegionRecorder$Region` 2.6 MB +
`LinkedHashMap$Entry` 2.8 MB + key `int[]`, about 15-20 MB of 40 MB); TeamAdapt the board geometry
of the boards alive during the fallback (about 30 MB of 69 MB).

Minimal `-Xmx` with the same SES (fix9, no histograms): bm02 64m and 48m complete with SES
125cea8b3db0, 32m fails; 8088sbc 96m, 64m and 48m complete with SES 9717764282b5, 40m fails. Both
failures happen 3 s after start, before the board is read, on a G1 humongous allocation with
11-12 MB live ("Terminating due to java.lang.OutOfMemoryError: Java heap space" right after the
settings are parsed), so the floor of about 40-48 MB is a startup allocation, not routing; the
routing working set of these boards fits well under it. Not yet identified which startup
allocation it is (rerun with `-Xmx40m -XX:+HeapDumpOnOutOfMemoryError`).

Cross-job retention (same JVM, job removed from the scheduler and its session, then full GC):
ldo-led three runs 11.5/11.4/11.5 MB, bm02 two runs 11.1/11.1 MB, against 6.5 MB before the
first job (class metadata, logger and static caches). No growth from run to run; no leak found.
Four loads of Issue208 in one JVM retained 108/106/106/106 MB while each board was referenced and
did not accumulate.

Load-time case, `fixtures/Issue208-freerouting.dsn` (see the report; not addressed by this
patch): `DsnReader.readBoard` takes 12-14 s and allocates 3.2 GB, the loaded board is 106 MB live;
the `HeadlessBoardManager` path adds the deferred post-load scan, 34 s and 3.8 GB in total.
48 keepout `ObstacleArea`s (4 per component image, 12 placements; `(circle F.Cu 501100 ...)`, a
501 mm diameter keepout) are cut into 393,648 tree shapes of at most 5 mm
(`ShapeSearchTree.java:1243-1256`, `TileShape.java:900`); the default tree has 398,286 leaves.

## 13. Risks and follow-ups

- The saving is bounded by the history size: at most 30 entries of one serialized board per
  pass loop, plus one fallback input. On the boards measured here that is a modest share of the
  live peak; the bulk of the live heap is the board itself (items, cached shapes, search trees),
  the per-attempt board copies of the routing fallback and the maze/shove working state. If the
  scale run confirms only single-digit percentages, the change is still free (identical results,
  negligible CPU) but it is not where the memory goes.
- CPU: one Deflate per pass (13-14 ms for 0.3-0.5 MB on the loaded smoke machine) and one Inflate
  per restore. Passes take seconds on these boards; check the CPU totals anyway.
- Follow-ups, larger but riskier (each needs its own identity proof): (1) stream the
  serialization straight into a `DeflaterOutputStream` so the uncompressed array is never
  allocated (less allocation churn, same bytes after inflation); (2) the default router and its board stay
  reachable through `RoutingFallbackRunner.listenerSource` (`RoutingFallbackRunner.java:24`) and
  `RoutingPipeline.autorouter` for the whole fallback, also after an alternative replaced the
  default result; keeping only the listener list would free one board while alternatives run; (3) the reported "peak heap usage" is
  `MemoryMXBean` heap *occupancy* (garbage included,
  `RoutingJobSchedulerActionThread.java:343-351`), so it mostly reflects the G1 heap sizing, not
  what the job needs; reporting the live set after a GC (or the old-generation occupancy after
  the last mixed collection) would make user memory reports meaningful;
  (4) build the default (cl=0) search tree lazily during batch autorouting if the autoroute trees
  cover all queries (needs an audit of every `getDefaultTree()` caller; not result-preserving by
  construction); (5) the shove-check memo reached its 65,536-entry cap on Mars-64 and was the
  largest single item there (about 15-20 MB of a 40 MB live peak, per routing thread). It is an
  exact memo (hits replay the answer and its side effects, `ShoveCheckCache.java:27-35`), so a
  smaller `MAX_ENTRIES` (`:41`) or a compact key (one `int[]` instead of 19 fields plus a
  separate coordinate array) should not change any SES and only trade hit rate for heap; make
  `MAX_ENTRIES` a `-Dfreerouting.exp.shove_cache_entries` switch and measure 16k/32k/64k;
  (6) obstacle areas far larger than the board (Issue208: 48 keepouts of 501 mm diameter) are cut
  into 5 mm tree tiles over their whole bounding box (`ShapeSearchTree.java:1243-1256`), 393,648
  tiles, 106 MB live and 3.2 GB of allocation at load. Clipping the tile grid to the board's
  bounding box enlarged by the largest clearance would bound this by the board area; it changes
  tree contents, so it needs its own SES check.
