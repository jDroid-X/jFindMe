@echo off
title jFindMe_PROFILER_LAUNCHER [jfindme-3b15c]
color 0A
cd /d "%~dp0"

set WEB_PORT=8080
set PROXY_PORT=3000

echo [SYSTEM] Purging ghost handshakes...
taskkill /F /IM node.exe /T >nul 2>&1
cls

echo ==========================================
echo       jFindMe ENTERPRISE FORENSIC HUB
echo ==========================================
echo.
echo [1/3] INITIALIZING_CLOUD_PROXY (Port %PROXY_PORT%)...
start "jFindMe_PROXY_ENGINE" cmd /c "set PORT=%PROXY_PORT% && node js/server.js || pause"

echo [2/3] STARTING_STATIC_WEB_MATRIX (Port %WEB_PORT% with API Reverse Proxy)...
start "jFindMe_WEB_SERVER" cmd /c "set WEB_PORT=%WEB_PORT% && set PROXY_PORT=%PROXY_PORT% && node js/static_server.js || pause"

echo [3/3] VERIFYING_BRIDGE_LIFESIGN...
timeout /t 3 /nobreak >nul

echo [INFO] UI available at: http://127.0.0.1:%WEB_PORT%
echo [INFO] Proxy available at: http://127.0.0.1:%PROXY_PORT%
echo.
echo [SUCCESS] MISSION_START: Projecting Dashboard...
start "" "http://127.0.0.1:%WEB_PORT%"

echo.
echo ------------------------------------------
echo  MONITORING_ACTIVE: Keep this window open.
echo  Closing this window will stop the servers.
echo ------------------------------------------
pause
taskkill /F /IM node.exe /T >nul 2>&1
exit
