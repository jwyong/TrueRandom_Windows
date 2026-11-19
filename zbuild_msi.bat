@echo off
setlocal enabledelayedexpansion
echo Starting Release Build Process for TrueRandom MSI...

set "GRADLE_FILE=composeApp\build.gradle.kts"

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