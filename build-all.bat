@echo off
REM 一键编译全部版本并把 jar 收进根目录 all\ 文件夹
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\build-all-versions.ps1" %*
pause
