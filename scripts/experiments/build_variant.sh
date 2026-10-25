#!/usr/bin/env bash
# Build a Freerouting executable jar from a commit plus an optional patch.
#
# Usage: build_variant.sh <commit> <patch.diff|-> <out.jar>
#
# The build runs in a temporary sparse worktree of the repository that contains this script
# (scripts/benchmark/ and integrations/ are left out to save disk space), so the caller's checkout
# is never modified. The worktree is removed when the script exits, whether the build succeeded
# or not. A provenance file <out.jar>.provenance records the commit and the patch checksum.
#
# Optional environment variables:
#   FR_REPO       repository to build from (default: the one containing this script)
#   FR_BUILD_TMP  parent directory for the temporary worktree (default: $TMPDIR or /tmp)
#   GRADLE_ARGS   extra Gradle arguments, e.g. "-Dorg.gradle.jvmargs=-Xmx1g"
#
# Works with bash 3.2 (macOS) and later.
set -euo pipefail

die() {
  echo "build_variant.sh: ERROR: $*" >&2
  exit 1
}

abspath() {
  # Portable replacement for 'realpath' (not available on older macOS).
  local dir base
  dir=$(dirname "$1")
  base=$(basename "$1")
  (cd "$dir" 2>/dev/null && printf '%s/%s\n' "$(pwd -P)" "$base") || return 1
}

sha1_of() {
  if command -v sha1sum >/dev/null 2>&1; then
    sha1sum "$1" | cut -d' ' -f1
  else
    shasum -a 1 "$1" | cut -d' ' -f1
  fi
}

if [ "$#" -ne 3 ]; then
  echo "Usage: $0 <commit> <patch.diff|-> <out.jar>" >&2
  exit 2
fi
commit_arg=$1
patch_arg=$2
out_arg=$3

script_dir=$(cd "$(dirname "$0")" && pwd -P)
repo=${FR_REPO:-$(git -C "$script_dir" rev-parse --show-toplevel)}
[ -d "$repo" ] || die "repository '$repo' does not exist"

sha=$(git -C "$repo" rev-parse --verify --quiet "${commit_arg}^{commit}") \
  || die "'$commit_arg' is not a commit in $repo"

patch_path=""
patch_sha1="-"
if [ "$patch_arg" != "-" ]; then
  [ -f "$patch_arg" ] || die "patch file '$patch_arg' does not exist"
  patch_path=$(abspath "$patch_arg") || die "cannot resolve patch path '$patch_arg'"
  patch_sha1=$(sha1_of "$patch_path")
fi

out_dir=$(dirname "$out_arg")
mkdir -p "$out_dir"
out_jar=$(abspath "$out_arg") || die "cannot resolve output path '$out_arg'"

tmp_parent=${FR_BUILD_TMP:-${TMPDIR:-/tmp}}
mkdir -p "$tmp_parent"
tmp=$(mktemp -d "${tmp_parent%/}/fr-variant.XXXXXX")
wt="$tmp/wt"

cleanup() {
  local rc=$?
  if [ -e "$wt" ]; then
    git -C "$repo" worktree remove --force "$wt" >/dev/null 2>&1 || true
  fi
  rm -rf "$tmp"
  git -C "$repo" worktree prune >/dev/null 2>&1 || true
  exit "$rc"
}
trap cleanup EXIT
trap 'exit 130' INT TERM

echo "build_variant.sh: commit $sha, patch ${patch_path:-none}, output $out_jar" >&2
git -C "$repo" worktree add --quiet --no-checkout --detach "$wt" "$sha"
git -C "$wt" sparse-checkout init --no-cone
printf '/*\n!/scripts/benchmark/\n!/integrations/\n' \
  | git -C "$wt" sparse-checkout set --no-cone --stdin
git -C "$wt" checkout --quiet

if [ -n "$patch_path" ]; then
  if ! git -C "$wt" apply --check "$patch_path"; then
    die "patch '$patch_path' does not apply to $sha"
  fi
  echo "build_variant.sh: applying $(git -C "$wt" apply --stat "$patch_path" | tail -n 1)" >&2
  git -C "$wt" apply "$patch_path"
fi

chmod +x "$wt/gradlew"
# GRADLE_ARGS is intentionally word-split so several arguments can be passed.
# shellcheck disable=SC2086
(cd "$wt" && ./gradlew --no-daemon -q ${GRADLE_ARGS:-} executableJar) \
  || die "Gradle build failed for $sha${patch_path:+ + $patch_path}"

built="$wt/build/libs/freerouting-current-executable.jar"
[ -f "$built" ] || die "the build did not produce $built"
cp "$built" "$out_jar"
{
  echo "commit=$sha"
  echo "patch=${patch_path:--}"
  echo "patch_sha1=$patch_sha1"
  echo "jar_sha1=$(sha1_of "$out_jar")"
  echo "built_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
} >"$out_jar.provenance"
echo "build_variant.sh: wrote $out_jar" >&2
