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
            echo "Unknown argument: $arg. Usage: build-linux.sh [version] [--no-open]" >&2
            exit 1
            ;;
        *)
            if [[ "$version_provided" -eq 0 ]]; then
                app_version="$arg"
                version_provided=1
            else
                echo "Unknown argument: $arg. Usage: build-linux.sh [version] [--no-open]" >&2
                exit 1
            fi
            ;;
    esac
done

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd -- "$script_dir/.." && pwd)"
output_dir="$repo_root/desktopApp/build/compose/binaries/main-release/deb"

if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/jpackage" && -x "$JAVA_HOME/bin/jlink" ]]; then
    :
elif command -v jpackage >/dev/null 2>&1 && command -v jlink >/dev/null 2>&1; then
    jpackage_bin="$(command -v jpackage)"
    java_home_candidate="$(cd -- "$(dirname -- "$jpackage_bin")/.." && pwd)"
    export JAVA_HOME="$java_home_candidate"
else
    echo "A full JDK with jpackage is required to build the Linux DEB. Install OpenJDK 17+ and set JAVA_HOME." >&2
    exit 1
fi

echo "Using JAVA_HOME: $JAVA_HOME"
echo "Building Linux release DEB version $app_version..."

if [[ -x "$repo_root/gradlew" ]]; then
    "$repo_root/gradlew" "-PappVersion=$app_version" :desktopApp:packageReleaseDeb
else
    bash "$repo_root/gradlew" "-PappVersion=$app_version" :desktopApp:packageReleaseDeb
fi

artifact="$(find "$output_dir" -maxdepth 1 -type f -name "*.deb" -printf "%T@ %p\n" 2>/dev/null | sort -nr | head -n 1 | cut -d " " -f 2-)"
if [[ -z "$artifact" ]]; then
    echo "DEB was not found in $output_dir" >&2
    exit 1
fi

echo "Linux package:"
echo "$artifact"

if [[ "$open_output_dir" -eq 1 ]]; then
    if command -v xdg-open >/dev/null 2>&1; then
        xdg-open "$output_dir" >/dev/null 2>&1 &
    elif command -v gio >/dev/null 2>&1; then
        gio open "$output_dir" >/dev/null 2>&1 &
    else
        echo "Open folder: $output_dir"
    fi
fi
