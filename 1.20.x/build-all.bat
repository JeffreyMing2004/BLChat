@echo off
REM Build all BLChat jars for Minecraft 1.20.x versions.
REM Each sub-project is an independent Gradle project targeting a specific MC version range.
REM
REM Usage: build-all.bat
REM Output: build/libs/*.jar in each sub-project directory

setlocal enabledelayedexpansion

set JAVA_HOME=C:\Users\Administrator\.gradle\jdks\jdk-21\jdk-21.0.12+8
set PATH=%JAVA_HOME%\bin;%PATH%

set BASE_DIR=%~dp0
set PROJECTS=forge-1.20 forge-1.20.2 forge-1.20.6

echo ============================================================
echo  BLChat 1.20.x Multi-Version Build
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
