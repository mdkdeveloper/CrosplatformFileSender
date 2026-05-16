param(
    [switch]$NoOpen
)

$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $PSScriptRoot
$OutputDir = Join-Path $RepoRoot "desktopApp\build\compose\binaries\main\deb"

if ($null -eq (Get-Command "wsl.exe" -ErrorAction SilentlyContinue)) {
    throw "WSL is required to build the Linux package from Windows. Install WSL with a Linux distro, then run this script again."
}

function Format-WslOutput {
    param($Output)

    return (($Output | Out-String) -replace "`0", "").Trim()
}

$distroCheck = & wsl.exe -l -q 2>&1
$distroDetails = Format-WslOutput $distroCheck
if ($LASTEXITCODE -ne 0 -or $distroDetails -match "no installed distributions") {
    throw "WSL is installed, but no Linux distro is available or WSL cannot start. Install a distro with: wsl.exe --install Ubuntu. Details: $distroDetails"
}

function ConvertTo-BashSingleQuoted {
    param([string]$Value)

    return "'" + $Value.Replace("'", "'\''") + "'"
}

$wslPathOutput = & wsl.exe wslpath -a "$RepoRoot" 2>&1
$wslRoot = ($wslPathOutput | Select-Object -First 1).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($wslRoot)) {
    $wslPathDetails = Format-WslOutput $wslPathOutput
    if ($wslPathDetails -match "no installed distributions") {
        throw "WSL is installed, but no Linux distro is available. Install one with: wsl.exe --install Ubuntu"
    }

    throw "Could not convert the repository path to a WSL path. Details: $wslPathDetails"
}

$openArg = if ($NoOpen) { "--no-open" } else { "--no-open" }
$command = "cd $(ConvertTo-BashSingleQuoted $wslRoot) && bash scripts/package-linux.sh $openArg"

Write-Host "Building Linux DEB through WSL..."
& wsl.exe bash -lc $command
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$artifact = Get-ChildItem -Path $OutputDir -Filter "*.deb" -File -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if ($null -eq $artifact) {
    throw "DEB was not found in $OutputDir"
}

Write-Host "Linux package:"
Write-Host $artifact.FullName

if (-not $NoOpen) {
    Invoke-Item $artifact.Directory.FullName
}
