@echo off
title jFindMe_PROFILER_LAUNCHER [jfindme-3b15c]
color 0A
set PORT=8080

echo [SYSTEM] Purging ghost handshakes...
taskkill /F /IM node.exe /T >nul 2>&1
cls

echo ==========================================
echo       jFindMe ENTERPRISE FORENSIC HUB
echo ==========================================
echo.
echo [1/3] INITIALIZING_CLOUD_PROXY...
start "jFindMe_PROXY_ENGINE" cmd /c "node js/server.js || pause"

echo [2/3] STARTING_STATIC_WEB_MATRIX...
start "jFindMe_WEB_SERVER" cmd /c "npx -y http-server . -p %PORT% --cors -c-1 -s"

echo [3/3] VERIFYING_BRIDGE_LIFESIGN...
timeout /t 5 /nobreak >nul

echo [INFO] UI available at: http://127.0.0.1:%PORT%
echo [INFO] Proxy available at: http://127.0.0.1:3000
echo.
echo [SUCCESS] MISSION_START: Projecting Dashboard...
start "" "http://127.0.0.1:%PORT%"

echo.
echo ------------------------------------------
echo  MONITORING_ACTIVE: Close this window to 
echo  terminate the entire forensic session.
echo ------------------------------------------
pause
taskkill /F /IM node.exe /T >nul 2>&1
exit
