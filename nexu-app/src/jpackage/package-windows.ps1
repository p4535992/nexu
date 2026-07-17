param(
    [string]$JarPath,
    [string]$Destination,
    [string]$AppVersion = "1.24.0"
)

$ErrorActionPreference = "Stop"
$ScriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = (Resolve-Path (Join-Path $ScriptDirectory "..\..\..")).Path

if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $JarPath = Join-Path $ProjectRoot "nexu-app\target\nexu-app.jar"
}
if ([string]::IsNullOrWhiteSpace($Destination)) {
    $Destination = Join-Path $ProjectRoot "nexu-app\target\jpackage"
}

$JarPath = [System.IO.Path]::GetFullPath($JarPath)
$Destination = [System.IO.Path]::GetFullPath($Destination)
$Jpackage = Join-Path $env:JAVA_HOME "bin\jpackage.exe"
$Modules = (Get-Content (Join-Path $ScriptDirectory "modules.txt") -Raw).Trim()
$AppName = "NexU"
$InputDirectory = Join-Path $Destination "input"
$AppImage = Join-Path $Destination $AppName
$Architecture = $env:PROCESSOR_ARCHITECTURE.ToLowerInvariant()
$PortableArchive = Join-Path $Destination "nexu-$AppVersion-windows-$Architecture-portable.zip"
$UpgradeUuid = "5d8fbe17-6f31-4a42-9b87-6b7d3c2f4e10"

if (-not (Test-Path -LiteralPath $Jpackage -PathType Leaf)) {
    throw "jpackage.exe not found under JAVA_HOME: $Jpackage"
}
if (-not (Test-Path -LiteralPath $JarPath -PathType Leaf)) {
    throw "Executable JAR not found: $JarPath"
}

if (Test-Path -LiteralPath $Destination) {
    Remove-Item -LiteralPath $Destination -Recurse -Force
}
New-Item -ItemType Directory -Path $InputDirectory | Out-Null
Copy-Item -LiteralPath $JarPath -Destination (Join-Path $InputDirectory "nexu-app.jar")

$ImageArguments = @(
    "--type", "app-image",
    "--name", $AppName,
    "--app-version", $AppVersion,
    "--vendor", "NexU Community",
    "--description", "Local smart-card signing agent",
    "--dest", $Destination,
    "--input", $InputDirectory,
    "--main-jar", "nexu-app.jar",
    "--add-modules", $Modules,
    "--java-options", "--add-exports=jdk.crypto.cryptoki/sun.security.pkcs11.wrapper=ALL-UNNAMED",
    "--java-options", "--add-opens=jdk.crypto.cryptoki/sun.security.pkcs11=ALL-UNNAMED"
)

& $Jpackage @ImageArguments
if ($LASTEXITCODE -ne 0) {
    throw "jpackage app-image failed with exit code $LASTEXITCODE"
}

Copy-Item -LiteralPath (Join-Path $ProjectRoot "LICENSE") -Destination (Join-Path $AppImage "LICENSE")
Copy-Item -LiteralPath (Join-Path $ProjectRoot "THIRD_PARTY_NOTICES.md") -Destination (Join-Path $AppImage "THIRD_PARTY_NOTICES.md")
Copy-Item -LiteralPath (Join-Path $ProjectRoot "nexu-app\src\main\resources\nexu-config.properties") `
    -Destination (Join-Path $AppImage "nexu-config.properties")
Copy-Item -LiteralPath (Join-Path $ScriptDirectory "LOGS.txt") -Destination (Join-Path $AppImage "LOGS.txt")
Copy-Item -LiteralPath (Join-Path $ProjectRoot "licenses") -Destination (Join-Path $AppImage "licenses") -Recurse

if (Test-Path -LiteralPath $PortableArchive) {
    Remove-Item -LiteralPath $PortableArchive -Force
}
Compress-Archive -Path $AppImage -DestinationPath $PortableArchive -CompressionLevel Optimal

$InstallerArguments = @(
    "--type", "exe",
    "--name", $AppName,
    "--app-version", $AppVersion,
    "--vendor", "NexU Community",
    "--description", "Local smart-card signing agent",
    "--dest", $Destination,
    "--app-image", $AppImage,
    "--license-file", (Join-Path $ProjectRoot "LICENSE"),
    "--win-menu",
    "--win-menu-group", "NexU",
    "--win-shortcut",
    "--win-dir-chooser",
    "--win-per-user-install",
    "--win-upgrade-uuid", $UpgradeUuid
)

& $Jpackage @InstallerArguments
if ($LASTEXITCODE -ne 0) {
    throw "jpackage EXE generation failed with exit code $LASTEXITCODE"
}

Remove-Item -LiteralPath $InputDirectory -Recurse -Force

Write-Host "Application image: $AppImage"
Write-Host "Portable archive: $PortableArchive"
Write-Host "Diagnostic log guide: $(Join-Path $AppImage 'LOGS.txt')"
Get-ChildItem -LiteralPath $Destination -Filter "*.exe" | ForEach-Object {
    Write-Host "Windows installer: $($_.FullName)"
}
