@echo off
title jFindMe_WEB_LAUNCHER [jfindme-3b15c]
color 0A
cd /d "%~dp0"

echo ===================================================
echo     jFindMe Web & Node Server Launcher
echo ===================================================

call scripts\launch_server.bat

echo [OPENING] Launching Matrix Heartbeat Splash Screen...
start "" "splash.html"
exit /b 0
