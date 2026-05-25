$ErrorActionPreference = "Stop"

$AppVersion = "1.0.0"
$VersionProvided = $false
$NoOpen = $false

foreach ($Argument in $args) {
    if ($Argument -eq "--no-open" -or $Argument -eq "-NoOpen") {
        $NoOpen = $true
    }
    elseif (-not $VersionProvided -and -not $Argument.StartsWith("-")) {
        $AppVersion = $Argument
        $VersionProvided = $true
    }
    else {
        throw "Unknown argument: $Argument. Usage: build-android.bat [version] [--no-open]"
    }
}

$RepoRoot = Split-Path -Parent $PSScriptRoot
$GradleWrapper = Join-Path $RepoRoot "gradlew.bat"
$OutputDir = Join-Path $RepoRoot "androidApp\build\outputs\apk\release"

function Test-JavaHome {
    param([string]$Path)

    return -not [string]::IsNullOrWhiteSpace($Path) -and
        (Test-Path (Join-Path $Path "bin\java.exe"))
}

function Resolve-JavaHome {
    if (Test-JavaHome $env:JAVA_HOME) {
        return $env:JAVA_HOME
    }

    $candidates = @(
        "C:\Program Files\Android\Android Studio\jbr",
        "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
    )

    foreach ($candidate in $candidates) {
        if (Test-JavaHome $candidate) {
            return $candidate
        }
    }

    $globCandidate = Get-ChildItem -Path "C:\Program Files\Eclipse Adoptium\jdk-*" -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-JavaHome $_.FullName } |
        Sort-Object FullName -Descending |
        Select-Object -First 1

    if ($null -ne $globCandidate) {
        return $globCandidate.FullName
    }

    throw "A JDK is required to build the Android APK. Install Android Studio or set JAVA_HOME."
}

$env:JAVA_HOME = Resolve-JavaHome
Write-Host "Using JAVA_HOME: $env:JAVA_HOME"
Write-Host "Building Android release APK version $AppVersion..."

Push-Location $RepoRoot
try {
    & $GradleWrapper "-PappVersion=$AppVersion" ":androidApp:assembleRelease"
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
finally {
    Pop-Location
}

$artifact = Get-ChildItem -Path $OutputDir -Filter "*.apk" -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notmatch "unsigned" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if ($null -eq $artifact) {
    throw "APK was not found in $OutputDir"
}

Write-Host "Android APK:"
Write-Host $artifact.FullName

if (-not $NoOpen) {
    Invoke-Item $artifact.Directory.FullName
}
