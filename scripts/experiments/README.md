# Experiment harness

Scripts for paired A/B routing experiments: every board is routed with every variant side by
side, each result is judged by one evaluator jar (DSN + SES reload DRC), and the outcome is
summarised against a control variant. Experiment packages under `docs/research/experiments/`
are written against this interface.

| File | Purpose |
|---|---|
| `build_variant.sh` | Build an executable jar from a commit plus an optional patch |
| `paired_campaign.py` | Run a paired campaign and write `summary.jsonl` / `summary.md` |
| `selftest.sh` | Two-variant smoke test of the harness (about a minute) |

Requirements: bash (Linux or macOS), git, Python 3.9 or later (standard library only), and a
Java 25 runtime (`java` on `PATH`, or `$JAVA`, or `--java`).

## Building variants

```bash
scripts/experiments/build_variant.sh <commit> <patch.diff|-> <out.jar>
```

The script creates a temporary sparse worktree of the repository that contains it (without
`scripts/benchmark/` and `integrations/`), applies the patch with `git apply` (it stops with an
error if the patch does not apply), runs `./gradlew --no-daemon -q executableJar`, copies
`build/libs/freerouting-current-executable.jar` to `<out.jar>` and removes the worktree. It
also writes `<out.jar>.provenance` (commit, patch path and sha1, jar sha1). The checkout you run
it from is never modified.

Optional environment variables: `FR_REPO` (repository to build from), `FR_BUILD_TMP` (parent
directory of the temporary worktree, default `$TMPDIR` or `/tmp`) and `GRADLE_ARGS` (extra
Gradle arguments, for example `GRADLE_ARGS=-Dorg.gradle.jvmargs=-Xmx1g` on a small machine).

```bash
scripts/experiments/build_variant.sh d4383b44 - jars/control.jar
scripts/experiments/build_variant.sh d4383b44 docs/research/experiments/x/patch.diff jars/x.jar
```

## Input files

`boards.txt`, one board per line; `#` starts a comment:

```text
# <fixture path relative to the repository root> [extra flags for this board]
fixtures/Issue143-rpi_splitter.dsn
fixtures/Issue508-DAC2020_bm02.dsn
fixtures/Issue297-myboard.dsn -mp 10
```

`variants.txt`, one variant per line; the first line is the control:

```text
# <name> <jar path | head> [JVM -D/-X options and Freerouting flags]
control jars/control.jar
v250    scripts/benchmark/binaries/freerouting-2.5.0.jar
exp     jars/x.jar -Dfreerouting.exp.my_switch=true
```

- Names may contain letters, digits, `.`, `_` and `-`; they become directory names.
- Relative jar paths are looked up in the current directory, then next to `variants.txt`,
  then in the repository root. `$VARS` and `~` are expanded.
- The jar token `head` means a jar built from the commit under test: pass it with
  `--head-jar <jar>`, or let the campaign build it with `build_variant.sh` from
  `--head-commit` (default `HEAD`) plus `--head-patch` (default `-`) into `<out>/jars/`.
- Flags are combined as common (`--flags`) < board < variant; later flags win because
  Freerouting applies its arguments in order. JVM options (`-D...`, `-X...`, `-ea`,
  `-javaagent:...`, or any option written as `jvm:<option>`) are moved before `-jar`; all other
  tokens go after the jar.

## Running a campaign

```bash
python3 scripts/experiments/paired_campaign.py \
  --out results/x --boards docs/research/experiments/x/boards.txt \
  --variants docs/research/experiments/x/variants.txt
```

Main options (all optional except the three above; `--help` lists everything):

| Option | Default | Meaning |
|---|---|---|
| `--flags "<flags>"` | `-mt 1 -mp 25 --router.optimizer.enabled=false` | flags common to all variants |
| `--evaluator <jar>` | the control's jar | jar that runs the reload DRC on every result |
| `--parallel N` | CPUs // 2, at least 1 | maximum number of routing JVMs at a time |
| `--timeout HH:MM:SS` | `00:20:00` | router job timeout, a safety stop only |
| `--filter <text>` | | only boards whose boards.txt path contains the text |
| `--force` | | rerun pairs that already have a record |
| `--dry-run` | | print the routing and DRC commands, run nothing |
| `--jvm-opts=<opts>` | `-Xmx3g` | JVM options of every routing and DRC JVM |
| `--wrap "<cmd>"` | | prefix for every JVM and build, e.g. a job-slot limiter |

Write option values that start with a dash and contain no space with `=`, for example
`--jvm-opts=-Xmx1500m` (argparse would otherwise read the value as an option).

All variants of one board start together so they share the same machine load; when
`--parallel` is at least twice the number of variants, several boards run at once
(`parallel // variants` boards). Each routing JVM gets `--router.job_timeout=<timeout>` before
the other flags (a board or variant flag can override it), and a hard kill after twice the
timeout plus ten minutes guards against hangs (`--kill-after <seconds>`; off by default with
`--wrap`, since a wrapper may queue the JVM for an unknown time). CPU time is the child's user + system time from
`os.wait4`; wall time is measured around the process (with `--wrap`, it includes the time spent
waiting for a slot; `stage_s` in the JSON is the router's own stage time).

On a shared machine with a job-slot limiter, either pass it with `--wrap` (each JVM then takes
its own slot, so the variants of a board may no longer start at the same moment) or run the
whole campaign as one limited job with `--parallel 1`.

### Resuming

Records are appended to `<out>/summary.jsonl` after each board. Rerunning the same command with
the same `--out` skips every board/variant pair that already has a record, so an interrupted
campaign (Ctrl-C or SIGTERM kills the running JVMs; their boards are not recorded) continues
where it stopped. Adding a variant line or a board line later runs only the new pairs. Use
`--force` to rerun; the newest record of a pair wins. The campaign warns when a recorded pair
was produced by a jar with a different sha1 than the one now listed.

## Outputs

- `<out>/runs/<board>/<variant>/`: `command.txt`, `console.log`, `out.ses`, `drc_command.txt`,
  `drc.log`, `drc.json` (the `--user_data_path` directories are deleted unless
  `--keep-user-data`).
- `<out>/summary.jsonl`: one JSON record per board and variant with `state` (job final state,
  or `KILLED` / `FAILED`), `timed_out`, `unrouted` and `violations` (the router's final line
  "Auto-routing stage completed ... (N unrouted and M violation(s))"), `passes`, `score`,
  `stage_s`, `cpu_s`, `wall_s`, `rc`, `ses_sha1`, `vias`, `wire_length_mm` (sum of the path
  segment lengths in the SES `network_out`, converted with the SES resolution),
  `drc_unconnected`, `drc_violations` (counts by type), `drc_clearance` (types containing
  "clearance", i.e. `clearance` and `holeClearance`), `drc_other`, plus jar and evaluator paths
  and sha1, flags, host and CPU count.
- `<out>/summary.md`, regenerated after each board:
  - per-board table with all variants (bold marks a run that did not complete, or more
    unrouted / clearance / unconnected than the control) and an identical-SES column against
    the control;
  - totals per variant (unrouted, vias, length, CPU, wall, clearance) over the boards on which
    every variant completed without timing out, with deltas against the control;
  - **Regressions**: every board where a variant has more unrouted (both runs completed), more
    reload-DRC clearance-type violations, more reload-DRC unconnected items, or fewer completed
    runs than the control;
  - Improvements, and the list of runs excluded from comparisons.

Timed-out, killed and failed runs are excluded from the totals and from the unrouted
comparison: their result depends on machine load, not only on the algorithm. Prefer boards and
pass limits (`-mp`, `--router.autorouter.max_items`) that finish well inside the timeout.

## System-property switched experiments

Code experiments should be switchable with a JVM system property
`-Dfreerouting.exp.<name>=...`, read once at startup and defaulting to the current behaviour.
Then one jar built with `build_variant.sh <commit> patch.diff exp.jar` runs both arms:

```text
control exp.jar
exp     exp.jar -Dfreerouting.exp.my_switch=true
```

With the property off, the result must be byte-identical to a jar built from the commit without
the patch. Check it once with a third variant `base base.jar` (built with `-` as patch): the
identical-SES column must say `yes` for `base` on every board.

## Determinism checks

The router is deterministic: the same jar, input and flags give a byte-identical SES
(`ses_sha1`). To check a jar, list it twice with identical flags (for example `control a.jar`
and `control2 a.jar`); every board must show `yes` in the identical-SES column. A `no` there
is a determinism bug, independent of the experiment. `selftest.sh` does exactly this with an
extra no-op system property.

## Self-test

```bash
scripts/experiments/selftest.sh [jar] [extra paired_campaign.py options...]
```

Routes `fixtures/Issue143-rpi_splitter.dsn` with `-mp 3` using the same jar twice (the second
variant adds `-Dfreerouting.exp.selftest_noop=true`) and checks that both runs complete, the SES
files are identical, `summary.md` is produced, a rerun resumes without routing, and `--dry-run`
places the `-D` option before `-jar`. Without a jar argument it builds one from `HEAD`.
