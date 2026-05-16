@echo off
setlocal

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\package-android.ps1" %*
exit /b %ERRORLEVEL%
