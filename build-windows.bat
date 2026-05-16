@echo off
setlocal

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\package-windows.ps1" %*
exit /b %ERRORLEVEL%
