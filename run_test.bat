@echo off
cd /d "%~dp0backend" || exit /b 1
call .\gradlew.bat test --no-daemon --console=plain > "%~dp0gradle_result.txt" 2>&1
set "testExit=%ERRORLEVEL%"
echo EXIT_CODE=%testExit% >> "%~dp0gradle_result.txt"
exit /b %testExit%
