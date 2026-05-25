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
            echo "Unknown argument: $arg. Usage: build-linux-portable.sh [version] [--no-open]" >&2
            exit 1
            ;;
        *)
            if [[ "$version_provided" -eq 0 ]]; then
                app_version="$arg"
                version_provided=1
            else
                echo "Unknown argument: $arg. Usage: build-linux-portable.sh [version] [--no-open]" >&2
                exit 1
            fi
            ;;
    esac
done

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd -- "$script_dir/.." && pwd)"
app_output_dir="$repo_root/desktopApp/build/compose/binaries/main-release/app"
archive_output_dir="$repo_root/desktopApp/build/compose/binaries/main-release/tar"
portable_name="LocalFileSender-$app_version-linux-x64"
staging_dir="$repo_root/build/tmp/linux-portable/$portable_name"
archive="$archive_output_dir/$portable_name.tar.gz"

run_gradle() {
    local wrapper_dir="$repo_root/build/tmp/wsl-gradle-wrapper"
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
elif command -v jpackage >/dev/null 2>&1 && command -v jlink >/dev/null 2>&1; then
    jpackage_bin="$(command -v jpackage)"
    java_home_candidate="$(cd -- "$(dirname -- "$jpackage_bin")/.." && pwd)"
    export JAVA_HOME="$java_home_candidate"
else
    echo "A full JDK with jpackage is required to build the Linux portable archive. Install OpenJDK 17+ and set JAVA_HOME." >&2
    exit 1
fi

echo "Using JAVA_HOME: $JAVA_HOME"
echo "Building Linux portable release archive version $app_version..."

run_gradle "-PappVersion=$app_version" :desktopApp:createReleaseDistributable

app_dir="$app_output_dir/LocalFileSender"
if [[ ! -d "$app_dir" ]]; then
    app_dir="$(find "$app_output_dir" -maxdepth 1 -type d ! -path "$app_output_dir" -printf "%T@ %p\n" 2>/dev/null | sort -nr | head -n 1 | cut -d " " -f 2-)"
fi

if [[ -z "${app_dir:-}" || ! -d "$app_dir" ]]; then
    echo "Linux app directory was not found in $app_output_dir" >&2
    exit 1
fi

rm -rf "$staging_dir"
mkdir -p "$staging_dir" "$archive_output_dir"
cp -a "$app_dir/." "$staging_dir/"
rm -f "$archive"
tar -C "$(dirname "$staging_dir")" -czf "$archive" "$(basename "$staging_dir")"

echo "Linux portable package:"
echo "$archive"

if [[ "$open_output_dir" -eq 1 ]]; then
    if command -v xdg-open >/dev/null 2>&1; then
        xdg-open "$archive_output_dir" >/dev/null 2>&1 &
    elif command -v gio >/dev/null 2>&1; then
        gio open "$archive_output_dir" >/dev/null 2>&1 &
    else
        echo "Open folder: $archive_output_dir"
    fi
fi
