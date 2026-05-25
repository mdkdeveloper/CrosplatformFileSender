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
        throw "Unknown argument: $Argument. Usage: build-windows.bat [version] [--no-open]"
    }
}

$RepoRoot = Split-Path -Parent $PSScriptRoot
$GradleWrapper = Join-Path $RepoRoot "gradlew.bat"
$OutputDir = Join-Path $RepoRoot "desktopApp\build\compose\binaries\main-release\msi"

function Test-JPackageHome {
    param([string]$Path)

    return -not [string]::IsNullOrWhiteSpace($Path) -and
        (Test-Path (Join-Path $Path "bin\java.exe")) -and
        (Test-Path (Join-Path $Path "bin\jpackage.exe")) -and
        (Test-Path (Join-Path $Path "bin\jlink.exe"))
}

function Resolve-JPackageHome {
    if (Test-JPackageHome $env:JAVA_HOME) {
        return $env:JAVA_HOME
    }

    $patterns = @(
        "C:\Program Files\Eclipse Adoptium\jdk-*",
        "C:\Program Files\Java\jdk-*",
        "C:\Program Files\Microsoft\jdk-*",
        "C:\Program Files\Zulu\zulu-*"
    )

    foreach ($pattern in $patterns) {
        $candidate = Get-ChildItem -Path $pattern -Directory -ErrorAction SilentlyContinue |
            Where-Object { Test-JPackageHome $_.FullName } |
            Sort-Object FullName -Descending |
            Select-Object -First 1

        if ($null -ne $candidate) {
            return $candidate.FullName
        }
    }

    throw "A full JDK with jpackage is required to build the Windows MSI. Install Temurin/OpenJDK 17+ or set JAVA_HOME to a full JDK."
}

$env:JAVA_HOME = Resolve-JPackageHome
Write-Host "Using JAVA_HOME: $env:JAVA_HOME"
Write-Host "Building Windows release MSI version $AppVersion..."

Push-Location $RepoRoot
try {
    & $GradleWrapper "-PappVersion=$AppVersion" ":desktopApp:packageReleaseMsi"
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
finally {
    Pop-Location
}

$artifact = Get-ChildItem -Path $OutputDir -Filter "*.msi" -File -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if ($null -eq $artifact) {
    throw "MSI was not found in $OutputDir"
}

Write-Host "Windows package:"
Write-Host $artifact.FullName

if (-not $NoOpen) {
    Invoke-Item $artifact.Directory.FullName
}
