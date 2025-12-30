@echo off
REM Quick client launcher - skips full asset validation
cd /d "%~dp0"
.\gradlew.bat --no-daemon runClient -x getAssetIndex 2>&1
pause
