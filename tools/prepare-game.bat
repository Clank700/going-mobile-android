@echo off
setlocal

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0prepare-game.ps1" %*
exit /b %ERRORLEVEL%
