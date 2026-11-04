@echo off
title jFindMe_WEB_LAUNCHER [jfindme-3b15c]
color 0A
cd /d "%~dp0"

echo ===================================================
echo     jFindMe Web & Node Server Launcher
echo ===================================================

call scripts\launch_server.bat

echo [OPENING] Directing browser to http://127.0.0.1:3000...
timeout /t 2 /nobreak >nul
start "" "http://127.0.0.1:3000"
exit /b 0
