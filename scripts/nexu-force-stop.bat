@echo off
setlocal EnableExtensions

rem Usage:
rem   nexu-force-stop.bat [port] [path-to-nexu-config.properties]
rem Resolution order: port argument, NEXU_PORT, configuration file, default 9795.

set "NEXU_STOP_PORT_ARG=%~1"
set "NEXU_STOP_CONFIG_ARG=%~2"
set "NEXU_STOP_SCRIPT_DIR=%~dp0"

powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference = 'Stop';" ^
  "function Get-ConfiguredPort([string]$ExplicitPort, [string]$ExplicitConfig, [string]$ScriptDir) {" ^
  "  if (-not [string]::IsNullOrWhiteSpace($ExplicitPort)) { return $ExplicitPort }" ^
  "  if (-not [string]::IsNullOrWhiteSpace($env:NEXU_PORT)) { return $env:NEXU_PORT }" ^
  "  $candidates = @();" ^
  "  if (-not [string]::IsNullOrWhiteSpace($ExplicitConfig)) { $candidates += $ExplicitConfig }" ^
  "  $candidates += (Join-Path $ScriptDir 'nexu-config.properties');" ^
  "  $candidates += (Join-Path $ScriptDir 'app\nexu-config.properties');" ^
  "  if (-not [string]::IsNullOrWhiteSpace($env:USERPROFILE)) { $candidates += (Join-Path $env:USERPROFILE '.NexU\nexu-config.properties') }" ^
  "  foreach ($candidate in ($candidates | Select-Object -Unique)) {" ^
  "    if (-not (Test-Path -LiteralPath $candidate -PathType Leaf)) { continue }" ^
  "    $match = Select-String -LiteralPath $candidate -Pattern '^\s*binding_ports\s*=\s*([0-9]+)' | Select-Object -First 1;" ^
  "    if ($match -and $match.Matches.Count -gt 0) {" ^
  "      Write-Host ('Using binding_ports from {0}' -f $candidate);" ^
  "      return $match.Matches[0].Groups[1].Value;" ^
  "    }" ^
  "  }" ^
  "  return '9795';" ^
  "}" ^
  "function Get-ListeningPid([int]$Port) {" ^
  "  $command = Get-Command Get-NetTCPConnection -ErrorAction SilentlyContinue;" ^
  "  if ($command) {" ^
  "    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1;" ^
  "    if ($connection) { return [long]$connection.OwningProcess }" ^
  "  }" ^
  "  $pattern = ('^\s*TCP\s+\S+:{0}\s+\S+\s+LISTENING\s+(\d+)\s*$' -f $Port);" ^
  "  $line = netstat.exe -ano -p TCP 2>$null | Select-String -Pattern $pattern | Select-Object -First 1;" ^
  "  if ($line -and $line.Matches.Count -gt 0) { return [long]$line.Matches[0].Groups[1].Value }" ^
  "  return $null;" ^
  "}" ^
  "$portValue = Get-ConfiguredPort $env:NEXU_STOP_PORT_ARG $env:NEXU_STOP_CONFIG_ARG $env:NEXU_STOP_SCRIPT_DIR;" ^
  "$port = 0;" ^
  "if (-not [int]::TryParse([string]$portValue, [ref]$port) -or $port -lt 1 -or $port -gt 65535) { throw ('Invalid NexU port: {0}' -f $portValue) }" ^
  "$uri = 'http://127.0.0.1:{0}/nexu-info' -f $port;" ^
  "Write-Host ('Checking NexU endpoint {0}' -f $uri);" ^
  "try { $httpResponse = Invoke-WebRequest -UseBasicParsing -Uri $uri -Method Get -TimeoutSec 3 }" ^
  "catch { Write-Host ('No NexU instance is responding on port {0}; nothing to stop.' -f $port); exit 0 }" ^
  "try { $nexuInfo = $httpResponse.Content | ConvertFrom-Json }" ^
  "catch { throw ('Port {0} answered, but /nexu-info did not return valid JSON. Refusing to stop any process.' -f $port) }" ^
  "if ($null -eq $nexuInfo -or [string]::IsNullOrWhiteSpace([string]$nexuInfo.version)) { throw ('Port {0} answered, but /nexu-info did not contain a NexU version. Refusing to stop any process.' -f $port) }" ^
  "$ownerPid = Get-ListeningPid $port;" ^
  "if ($null -eq $ownerPid) { throw ('NexU {0} answered on port {1}, but the listening PID could not be resolved.' -f $nexuInfo.version, $port) }" ^
  "if ($ownerPid -eq $PID) { throw 'Refusing to terminate the shutdown helper process itself.' }" ^
  "$process = Get-Process -Id $ownerPid -ErrorAction Stop;" ^
  "Write-Host ('Verified NexU version {0}; force-stopping PID {1} ({2}).' -f $nexuInfo.version, $ownerPid, $process.Path);" ^
  "Stop-Process -Id $ownerPid -Force -ErrorAction Stop;" ^
  "$deadline = [DateTime]::UtcNow.AddSeconds(10);" ^
  "do {" ^
  "  Start-Sleep -Milliseconds 250;" ^
  "  $remainingPid = Get-ListeningPid $port;" ^
  "} while ($null -ne $remainingPid -and [DateTime]::UtcNow -lt $deadline);" ^
  "if ($null -ne $remainingPid) { throw ('PID {0} was stopped, but port {1} is still in use by PID {2}.' -f $ownerPid, $port, $remainingPid) }" ^
  "Write-Host ('NexU stopped successfully; port {0} is free.' -f $port);"

set "EXIT_CODE=%ERRORLEVEL%"
endlocal & exit /b %EXIT_CODE%
