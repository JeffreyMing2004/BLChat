@echo off
chcp 65001 >nul
REM 一键编译全部版本并收集产物到根目录 all\ 文件夹
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\build-all-versions.ps1" %*
pause
chcp 936 >nul
