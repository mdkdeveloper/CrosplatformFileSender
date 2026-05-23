#!/usr/bin/env bash
set -euo pipefail

open_output_dir=1
if [[ "${1:-}" == "--no-open" ]]; then
    open_output_dir=0
fi

if [[ "$(uname -s)" != "Darwin" ]]; then
    echo "macOS app packaging must be run on macOS." >&2
    exit 1
fi

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$script_dir"
output_dir="$repo_root/desktopApp/build/compose/binaries/main/app"

if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/jpackage" && -x "$JAVA_HOME/bin/jlink" ]]; then
    :
elif java_home_candidate="$(/usr/libexec/java_home 2>/dev/null)" \
        && [[ -x "$java_home_candidate/bin/jpackage" && -x "$java_home_candidate/bin/jlink" ]]; then
    # On macOS the /usr/bin/{jpackage,jlink} entries are Apple stubs, so resolving
    # JAVA_HOME from their location yields /usr (a broken JDK home that hangs the JVM).
    # Always resolve the real JDK home via java_home instead.
    export JAVA_HOME="$java_home_candidate"
else
    echo "A full JDK with jpackage is required to build the macOS app. Install OpenJDK 17+ and set JAVA_HOME." >&2
    echo "Check Java on macOS with: java -version" >&2
    echo "Check installed JDKs with: /usr/libexec/java_home -V" >&2
    exit 1
fi

echo "Using JAVA_HOME: $JAVA_HOME"
echo "Building macOS app..."
echo "Running: ./gradlew :desktopApp:createDistributable --console=plain --info --stacktrace --no-daemon"

if [[ -x "$repo_root/gradlew" ]]; then
    "$repo_root/gradlew" :desktopApp:createDistributable --console=plain --info --stacktrace --no-daemon
else
    bash "$repo_root/gradlew" :desktopApp:createDistributable --console=plain --info --stacktrace --no-daemon
fi

artifact=""
latest_mtime=0
shopt -s nullglob
for candidate in "$output_dir"/*.app; do
    candidate_mtime="$(stat -f "%m" "$candidate")"
    if (( candidate_mtime > latest_mtime )); then
        artifact="$candidate"
        latest_mtime="$candidate_mtime"
    fi
done
shopt -u nullglob

if [[ -z "$artifact" ]]; then
    echo "App was not found in $output_dir" >&2
    exit 1
fi

echo "macOS app:"
echo "$artifact"

if [[ "$open_output_dir" -eq 1 ]]; then
    open "$output_dir"
fi
