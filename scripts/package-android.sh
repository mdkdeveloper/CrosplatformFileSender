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
            echo "Unknown argument: $arg. Usage: build-android.sh [version] [--no-open]" >&2
            exit 1
            ;;
        *)
            if [[ "$version_provided" -eq 0 ]]; then
                app_version="$arg"
                version_provided=1
            else
                echo "Unknown argument: $arg. Usage: build-android.sh [version] [--no-open]" >&2
                exit 1
            fi
            ;;
    esac
done

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd -- "$script_dir/.." && pwd)"
output_dir="$repo_root/androidApp/build/outputs/apk/release"

if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    :
elif command -v java >/dev/null 2>&1; then
    java_bin="$(command -v java)"
    java_home_candidate="$(cd -- "$(dirname -- "$java_bin")/.." && pwd)"
    export JAVA_HOME="$java_home_candidate"
else
    echo "A JDK is required to build the Android APK. Install Android Studio or OpenJDK 17+ and set JAVA_HOME." >&2
    exit 1
fi

echo "Using JAVA_HOME: $JAVA_HOME"
echo "Building Android release APK version $app_version..."

if [[ -x "$repo_root/gradlew" ]]; then
    "$repo_root/gradlew" "-PappVersion=$app_version" :androidApp:assembleRelease
else
    bash "$repo_root/gradlew" "-PappVersion=$app_version" :androidApp:assembleRelease
fi

artifact="$(find "$output_dir" -maxdepth 1 -type f -name "*.apk" -printf "%T@ %p\n" 2>/dev/null | sort -nr | head -n 1 | cut -d " " -f 2-)"
if [[ -z "$artifact" ]]; then
    echo "APK was not found in $output_dir" >&2
    exit 1
fi

echo "Android APK:"
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
