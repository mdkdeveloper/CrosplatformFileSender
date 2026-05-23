#!/usr/bin/env bash
set -euo pipefail

open_output_dir=1
if [[ "${1:-}" == "--no-open" ]]; then
    open_output_dir=0
fi

if [[ "$(uname -s)" != "Darwin" ]]; then
    echo "macOS DMG packaging must be run on macOS." >&2
    exit 1
fi

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd -- "$script_dir/.." && pwd)"
output_dir="$repo_root/desktopApp/build/compose/binaries/main/dmg"

if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/jpackage" && -x "$JAVA_HOME/bin/jlink" ]]; then
    :
elif command -v jpackage >/dev/null 2>&1 && command -v jlink >/dev/null 2>&1; then
    jpackage_bin="$(command -v jpackage)"
    java_home_candidate="$(cd -- "$(dirname -- "$jpackage_bin")/.." && pwd)"
    export JAVA_HOME="$java_home_candidate"
else
    echo "A full JDK with jpackage is required to build the macOS DMG. Install OpenJDK 17+ and set JAVA_HOME." >&2
    exit 1
fi

echo "Using JAVA_HOME: $JAVA_HOME"
echo "Building macOS DMG..."

if [[ -x "$repo_root/gradlew" ]]; then
    "$repo_root/gradlew" :desktopApp:packageDmg
else
    bash "$repo_root/gradlew" :desktopApp:packageDmg
fi

artifact=""
latest_mtime=0
shopt -s nullglob
for candidate in "$output_dir"/*.dmg; do
    candidate_mtime="$(stat -f "%m" "$candidate")"
    if (( candidate_mtime > latest_mtime )); then
        artifact="$candidate"
        latest_mtime="$candidate_mtime"
    fi
done
shopt -u nullglob

if [[ -z "$artifact" ]]; then
    echo "DMG was not found in $output_dir" >&2
    exit 1
fi

echo "macOS package:"
echo "$artifact"

if [[ "$open_output_dir" -eq 1 ]]; then
    open "$output_dir"
fi
