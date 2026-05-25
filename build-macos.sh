#!/usr/bin/env bash
set -euo pipefail

app_version="1.0.0"
version_provided=0
open_output_dir=1

for arg in "$@"; do
    case "$arg" in
        --no-open)
            open_output_dir=0
            ;;
        -*)
            echo "Unknown argument: $arg. Usage: build-macos.sh [version] [--no-open]" >&2
            exit 1
            ;;
        *)
            if [[ "$version_provided" -eq 0 ]]; then
                app_version="$arg"
                version_provided=1
            else
                echo "Unknown argument: $arg. Usage: build-macos.sh [version] [--no-open]" >&2
                exit 1
            fi
            ;;
    esac
done

if [[ "$(uname -s)" != "Darwin" ]]; then
    echo "macOS app packaging must be run on macOS." >&2
    exit 1
fi

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$script_dir"
output_dir="$repo_root/desktopApp/build/compose/binaries/main-release/dmg"

run_gradle() {
    local wrapper_dir="$repo_root/build/tmp/shell-gradle-wrapper"
    local wrapper="$wrapper_dir/gradlew"

    mkdir -p "$wrapper_dir"
    tr -d '\r' < "$repo_root/gradlew" > "$wrapper"
    chmod +x "$wrapper"

    if [[ ! -e "$wrapper_dir/gradle" ]]; then
        ln -s "$repo_root/gradle" "$wrapper_dir/gradle" 2>/dev/null || cp -R "$repo_root/gradle" "$wrapper_dir/gradle"
    fi

    "$wrapper" "$@"
}

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
echo "Building macOS release DMG version $app_version..."
echo "Running: ./gradlew -PappVersion=$app_version :desktopApp:packageReleaseDmg --console=plain --info --stacktrace --no-daemon"

run_gradle "-PappVersion=$app_version" :desktopApp:packageReleaseDmg --console=plain --info --stacktrace --no-daemon

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
