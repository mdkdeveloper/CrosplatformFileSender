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
        throw "Unknown argument: $Argument. Usage: build-windows-portable.bat [version] [--no-open]"
    }
}

$RepoRoot = Split-Path -Parent $PSScriptRoot
$GradleWrapper = Join-Path $RepoRoot "gradlew.bat"
$AppOutputDir = Join-Path $RepoRoot "desktopApp\build\compose\binaries\main-release\app"
$ArchiveOutputDir = Join-Path $RepoRoot "desktopApp\build\compose\binaries\main-release\zip"
$PortableName = "LocalFileSender-$AppVersion-windows-x64"
$StagingRoot = Join-Path $RepoRoot "build\tmp\windows-portable"
$StagingDir = Join-Path $StagingRoot $PortableName
$ArchivePath = Join-Path $ArchiveOutputDir "$PortableName.zip"

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

    throw "A full JDK with jpackage is required to build the Windows portable archive. Install Temurin/OpenJDK 17+ or set JAVA_HOME to a full JDK."
}

$env:JAVA_HOME = Resolve-JPackageHome
Write-Host "Using JAVA_HOME: $env:JAVA_HOME"
Write-Host "Building Windows portable release archive version $AppVersion..."

Push-Location $RepoRoot
try {
    & $GradleWrapper "-PappVersion=$AppVersion" ":desktopApp:createReleaseDistributable"
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
finally {
    Pop-Location
}

$appDir = Join-Path $AppOutputDir "LocalFileSender"
if (-not (Test-Path $appDir)) {
    $appDir = Get-ChildItem -Path $AppOutputDir -Directory -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1 -ExpandProperty FullName
}

if ([string]::IsNullOrWhiteSpace($appDir) -or -not (Test-Path $appDir)) {
    throw "Windows app directory was not found in $AppOutputDir"
}

if (Test-Path $StagingDir) {
    Remove-Item -LiteralPath $StagingDir -Recurse -Force
}

New-Item -ItemType Directory -Path $StagingDir -Force | Out-Null
New-Item -ItemType Directory -Path $ArchiveOutputDir -Force | Out-Null
Copy-Item -Path (Join-Path $appDir "*") -Destination $StagingDir -Recurse -Force

if (Test-Path $ArchivePath) {
    Remove-Item -LiteralPath $ArchivePath -Force
}

Compress-Archive -LiteralPath $StagingDir -DestinationPath $ArchivePath -CompressionLevel Optimal

Write-Host "Windows portable package:"
Write-Host $ArchivePath

if (-not $NoOpen) {
    Invoke-Item $ArchiveOutputDir
}
