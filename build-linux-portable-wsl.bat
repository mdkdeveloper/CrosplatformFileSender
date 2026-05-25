@echo off
setlocal

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\package-linux-portable-wsl.ps1" %*
exit /b %ERRORLEVEL%
