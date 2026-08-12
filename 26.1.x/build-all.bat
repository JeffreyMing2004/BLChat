@echo off
REM Build all BLChat jars for Minecraft 26.1.x versions.
REM Each sub-project is an independent Gradle project targeting a specific MC version range.
REM
REM Usage: build-all.bat
REM Output: build/libs/*.jar in each sub-project directory

setlocal enabledelayedexpansion

set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

set BASE_DIR=%~dp0
set PROJECTS=forge-26.1

echo ============================================================
echo  BLChat 26.1.x Multi-Version Build
echo  Java: %JAVA_HOME%
echo  Projects: %PROJECTS%
echo ============================================================

for %%P in (%PROJECTS%) do (
    echo.
    echo ---- Building %%P ----
    pushd "%BASE_DIR%\%%P"
    call gradlew.bat build --no-daemon
    if errorlevel 1 (
        echo [FAIL] %%P build failed
        popd
        exit /b 1
    )
    echo [ OK  ] %%P built successfully
    popd
)

echo.
echo ============================================================
echo  All builds complete.
echo  Jars:
echo ============================================================
for %%P in (%PROJECTS%) do (
    if exist "%BASE_DIR%\%%P\build\libs\*.jar" (
        dir /b "%BASE_DIR%\%%P\build\libs\*.jar"
    ) else (
        echo [WARN] No jar found in %%P\build\libs
    )
)

endlocal
