@echo off
chcp 65001 >nul
title blog-cloud one-click STOP
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-all.ps1" %*
echo.
pause
