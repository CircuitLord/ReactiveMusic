@echo off
REM MochaMix Project and Adapter Switcher
REM Usage: switch-project.bat [project] [adapter]
REM Example: switch-project.bat mochamix v1_21_1

if "%1"=="" (
    echo Current project settings:
    findstr "mochamix.project=" gradle.properties 2>nul || echo mochamix.project=not set
    findstr "mochamix.adapter=" gradle.properties 2>nul || echo mochamix.adapter=not set
    echo.
    echo Available projects: mochamix, logical-loadouts, voidloom
    echo Available adapters: v1_21_1, v1_21_5
    echo Usage: switch-project.bat [project] [adapter]
    echo Example: switch-project.bat mochamix v1_21_1
    goto :eof
)

if "%2"=="" (
    echo Error: Both project and adapter must be specified
    echo Usage: switch-project.bat [project] [adapter]
    echo Available projects: mochamix, logical-loadouts, voidloom
    echo Available adapters: v1_21_1, v1_21_5
    goto :eof
)

REM Validate project
if "%1"=="mochamix" goto :checkAdapter
if "%1"=="logical-loadouts" goto :checkAdapter
if "%1"=="voidloom" goto :checkAdapter
echo Invalid project: %1
echo Valid options: mochamix, logical-loadouts, voidloom
goto :eof

:checkAdapter
REM Validate adapter
if "%2"=="v1_21_1" goto :switch
if "%2"=="v1_21_5" goto :switch
echo Invalid adapter: %2
echo Valid options: v1_21_1, v1_21_5
goto :eof

:switch
REM Update both project and adapter settings
powershell -Command "(Get-Content gradle.properties) -replace '^mochamix\.project=.*', 'mochamix.project=%1' | Set-Content gradle.properties"
powershell -Command "(Get-Content gradle.properties) -replace '^mochamix\.adapter=.*', 'mochamix.adapter=%2' | Set-Content gradle.properties"

echo Switched to project: %1, adapter: %2
echo.

REM Verify the project/adapter directory exists
if exist "projects\%1\%2" (
    echo Project directory verified: projects\%1\%2
) else (
    echo WARNING: Project directory not found: projects\%1\%2
    echo Please ensure the directory exists before building
)

echo.
echo Generating IDE context...
gradlew -b ide-context.gradle generateContext --quiet
echo.
echo Next steps:
echo 1. Reload/refresh your IDE project  
echo 2. Verify that project classes are now recognized
echo 3. Build with: gradlew clean build