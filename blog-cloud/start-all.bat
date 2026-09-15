@echo off
chcp 65001 >nul
title blog-cloud one-click START
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-all.ps1"
echo.
pause
