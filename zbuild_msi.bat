@echo off
echo Starting Build Process for TrueRandom MSI...

:: 1. Clean old builds to prevent cache/identity hash issues
call ./gradlew clean

:: 2. Package the MSI
call ./gradlew packageMsi

echo.
echo Build Complete!
echo Your installer is in: composeApp\build\compose\binaries\main\msi\

:: 3. Launch the output directory
explorer "composeApp\build\compose\binaries\main\msi"

pause