#!/usr/bin/env bash
# Self-test of the experiment harness.
#
# Usage: selftest.sh [jar] [extra paired_campaign.py options...]
#
# Runs a 2-variant campaign on fixtures/Issue143-rpi_splitter.dsn with -mp 3. Both variants use
# the same jar; the second adds a no-op JVM system property, which also checks that -D options are
# placed before -jar. The test passes when both runs complete, their SES files are byte-identical,
# summary.md is produced, a second invocation resumes without rerunning anything, and --dry-run
# prints the commands. Without a jar argument, a jar is built from HEAD with build_variant.sh.
#
# Example on a shared machine: selftest.sh my.jar --wrap /path/to/slot-limiter
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd -P)
repo=$(cd "$script_dir/../.." && pwd -P)
work=$(mktemp -d "${TMPDIR:-/tmp}/fr-harness-selftest.XXXXXX")

fail() {
  echo "SELFTEST FAILED: $*" >&2
  echo "(outputs kept in $work)" >&2
  exit 1
}

jar=""
if [ "$#" -gt 0 ] && [ "${1#-}" = "$1" ]; then
  jar=$1
  shift
fi
if [ -z "$jar" ]; then
  jar="$work/head.jar"
  "$script_dir/build_variant.sh" HEAD - "$jar" || fail "could not build a jar from HEAD"
fi
[ -f "$jar" ] || fail "jar '$jar' does not exist"
jar=$(cd "$(dirname "$jar")" && printf '%s/%s\n' "$(pwd -P)" "$(basename "$jar")")

echo "fixtures/Issue143-rpi_splitter.dsn" >"$work/boards.txt"
{
  echo "# name jar flags (first line = control)"
  echo "control $jar"
  echo "noop $jar -Dfreerouting.exp.selftest_noop=true"
} >"$work/variants.txt"

campaign=(python3 "$script_dir/paired_campaign.py" --out "$work/out" --boards "$work/boards.txt"
  --variants "$work/variants.txt" --flags "-mt 1 -mp 3 --router.optimizer.enabled=false"
  --timeout 00:05:00 --parallel 2 --repo "$repo")

"${campaign[@]}" "$@" || fail "campaign exited with an error"

[ -s "$work/out/summary.md" ] || fail "summary.md was not produced"
python3 - "$work/out/summary.jsonl" <<'EOF' || fail "result check"
import json, sys
records = [json.loads(line) for line in open(sys.argv[1]) if line.strip()]
by_variant = {r["variant"]: r for r in records}
assert len(records) == 2 and set(by_variant) == {"control", "noop"}, records
for r in records:
    assert r["state"] == "COMPLETED", (r["variant"], r["state"])
    assert r["unrouted"] is not None and r["ses_sha1"], r
    assert r["drc"] == "ok" and r["vias"] is not None and r["wire_length_mm"], r
    print(f"{r['variant']}: state {r['state']}, unrouted {r['unrouted']}, vias {r['vias']}, "
          f"length {r['wire_length_mm']} mm, passes {r['passes']}, DRC clearance "
          f"{r['drc_clearance']}, cpu {r['cpu_s']} s, ses {r['ses_sha1'][:12]}")
assert by_variant["control"]["ses_sha1"] == by_variant["noop"]["ses_sha1"], "SES differ"
print("SES identical")
EOF
cmp "$work/out/runs/Issue143-rpi_splitter/control/out.ses" \
  "$work/out/runs/Issue143-rpi_splitter/noop/out.ses" || fail "SES files differ"
grep -q '| noop | COMPLETED |' "$work/out/summary.md" || fail "summary.md lacks the noop row"
grep -A3 '^| board | variant' "$work/out/summary.md" | grep -q '| yes |' \
  || fail "summary.md does not report identical SES"

# Resume: nothing is left to do, so no JVM runs and no record is added.
"${campaign[@]}" "$@" >"$work/resume.log" 2>&1 || fail "resume run exited with an error"
grep -q "0 boards / 0 runs to do" "$work/resume.log" || fail "resume did not skip finished runs"
[ "$(grep -c . "$work/out/summary.jsonl")" -eq 2 ] || fail "resume added records"

# Dry run with --force prints both routing and both DRC commands; the -D option precedes -jar.
"${campaign[@]}" "$@" --force --dry-run >"$work/dry.txt" || fail "dry run exited with an error"
[ "$(grep -c -- ' -jar ' "$work/dry.txt")" -eq 4 ] || fail "dry run did not print 4 commands"
grep -q -- '-Dfreerouting.exp.selftest_noop=true .*-jar ' "$work/dry.txt" \
  || fail "the -D option is not placed before -jar"

echo
cat "$work/out/summary.md"
echo
echo "SELFTEST PASSED (outputs in $work)"
