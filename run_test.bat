@echo off
cd /d T:\backend\intern-pilot-backend
call .\gradlew.bat test --no-daemon --console=plain > T:\gradle_result.txt 2>&1
echo EXIT_CODE=%ERRORLEVEL% >> T:\gradle_result.txt
