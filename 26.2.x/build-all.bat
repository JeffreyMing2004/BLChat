@echo off
setlocal

set BASE_DIR=%~dp0
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

set PROJECTS=forge-26.2

echo ============================================================
echo Building BLChat for Minecraft 26.2.x
echo ============================================================

for %%P in (%PROJECTS%) do (
    echo.
    echo Building %%P...
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
echo All builds complete.
echo Jars:
echo ============================================================
for %%P in (%PROJECTS%) do (
    if exist "%BASE_DIR%\%%P\build\libs\*.jar" (
        dir /b "%BASE_DIR%\%%P\build\libs\*.jar"
    )
)

endlocal
