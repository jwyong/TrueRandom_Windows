@echo off
setlocal enabledelayedexpansion
echo Starting Release Build Process for TrueRandom MSI...

set "GRADLE_FILE=composeApp\build.gradle.kts"

powershell -NoProfile -Command ^
  "$path = '%GRADLE_FILE%';" ^
  "$content = Get-Content -Raw -Path $path;" ^
  "$match = [regex]::Match($content, 'packageVersion\s*=\s*\"([^\"]+)\"');" ^
  "if (-not $match.Success) { Write-Error 'packageVersion not found'; exit 1 };" ^
  "$ver = $match.Groups[1].Value;" ^
  "$parts = $ver.Split('.');" ^
  "if ($parts.Length -eq 3) { $newVer = \"$ver.1\" } " ^
  "elseif ($parts.Length -eq 4) { $last = [int]$parts[3] + 1; $newVer = \"$($parts[0]).$($parts[1]).$($parts[2]).$last\" } " ^
  "else { Write-Error \"Unexpected version format: $ver\"; exit 1 };" ^
  "$updated = [regex]::Replace($content, '(packageVersion\s*=\s*\")[^\"]+(\")', \"${1}$newVer${2}\", 1);" ^
  "Set-Content -Path $path -Value $updated -NoNewline;" ^
  "Write-Host \"Bumped packageVersion: $ver -> $newVer\""

if errorlevel 1 (
    echo Failed to increment packageVersion.
    pause
    exit /b 1
)

call ./gradlew clean
call ./gradlew packageReleaseMsi

echo.
echo Build Complete!
echo Your installer is in: composeApp\build\compose\binaries\main-release\msi\

explorer "composeApp\build\compose\binaries\main-release\msi"

pause