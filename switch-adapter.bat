@echo off
REM Simple ReactiveMusic adapter switcher
REM Usage: switch-adapter.bat [1_20_4|1_21_1|latest]

if "%1"=="" (
    echo Current adapter setting:
    findstr "reactivemusic.adapter=" gradle.properties 2>nul || echo reactivemusic.adapter=not set
    echo.
    echo Available adapters: 1_20_4, 1_21_1, latest
    echo Usage: switch-adapter.bat [adapter]
    goto :eof
)

if "%1"=="1_20_4" goto :switch
if "%1"=="1_21_1" goto :switch  
if "%1"=="latest" goto :switch
echo Invalid adapter: %1
echo Valid options: 1_20_4, 1_21_1, latest
goto :eof

:switch
powershell -Command "(Get-Content gradle.properties) -replace '^reactivemusic\.adapter=.*', 'reactivemusic.adapter=%1' | Set-Content gradle.properties"
echo Switched to adapter: %1
echo.
echo Generating IDE context...
gradlew -b ide-context.gradle generateContext --quiet
echo.
echo Next steps:
echo 1. Reload/refresh your IDE project  
echo 2. Verify that common module classes are now recognized