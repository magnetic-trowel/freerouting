# Shove Check Cache

`TraceShover.check` answers "can this trace shape be pushed through here?" and is the largest single
cost of maze routing (20-33% of autorouter CPU in JFR profiles). `ShoveCheckCache` memoises the
answers exactly: a hit replays the answer and every side effect of the check (IDs taken from the ID
generator, writes to the board's shove-failure record), so routing with the cache is byte-identical
to routing without it.

## Validity

Each entry carries the region its check read and the board's change sequence at the time:

- **Read region.** `QueryRegionRecorder` collects, per thread, the bounding box and layer bits of
  every default-tree query made while a check runs (`MinAreaTree.overlaps*`, room completion).
  Frames nest; a nested check that hits the cache adds its entry's region to the enclosing frame.
  A query of any other tree, and any iteration of the item list (`UndoableObjects.startReadObject`,
  `ShapeTree.toArray`), counts as a read of the whole board.
- **Change grid.** `SearchTreeChangeGrid` (one per board, created on first use) stamps a 64 x 64
  grid per layer with a new sequence number for every change: all default-tree entries of an item
  before and after `ShapeSearchTree` inserts, removes, changes, merges or splits them, and the
  bounding box (plus the largest clearance) of an item on `UndoableObjects.saveForUndo`, `insert`,
  `delete` and `noteObjectChanged`.
- **Unlocated changes.** `noteContentChange` (net or clearance-class reassignment), `undo` and
  `redo` start a new grid generation. A new generation, a changed clearance matrix
  (`ClearanceMatrix.getModificationCount`) or trace angle restriction, or a different board clears
  the cache.

An entry is valid while no cell of its region carries a stamp newer than the entry. Entries are
evicted least-recently-used beyond `MAX_ENTRIES`.

## Invariant for new code

Any change to an item that a shove check can observe must reach the change grid. Geometry changes
go through the search trees and are covered automatically. Other state must either call
`itemList.saveForUndo(item)` before the change (as undoable edits already do) or
`itemList.noteObjectChanged(item)` after it, as `Item.setFixedState` and `Item.unfix` do. The fixed
state was the one such field: it is not counted by the content version, so before this change even
the per-version cache could return an answer computed with stale fixed states (for example after
`DirectPinRouting` restores the fixed states it changed).

## Diagnostics

- `-Dfreerouting.shove_cache.verify=true` recomputes every hit that was kept across a board change,
  compares answer and side effects, logs mismatches and prints totals at exit.
- `-Dfreerouting.shove_cache.stats=true` prints hit, retained-hit, miss and drop counts at exit.
- `-Dfreerouting.shove_cache.retain=false` keeps only entries computed since the last board change
  (the earlier behaviour).

Measured on the A/B suite (bm01, BBD Mars-64, FHT-8086, GP8B, hw48na, interf_u, bm05,
rpi_splitter): identical SES files and board-hash sequences, misses on bm01 down 20%, total CPU
-4.6% (bm01 -6.8%, Mars-64 -5.7%). The verify mode recomputed about one million retained hits on six
boards, including the optimizer with three threads, without a mismatch.
