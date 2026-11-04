@echo off
title jFindMe_NODE_SERVER_LAUNCHER [jfindme-3b15c]
color 0A

:: Resolve absolute path of jFindMeWeb directory
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "APP_DIR=%%~fI"

cd /d "%APP_DIR%"

:: Ensure node is found in PATH
where node >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    set "PATH=C:\jDroid-X-AI-Tools\NodeJS;%PATH%"
)

set PORT=3000
set PROXY_PORT=3000
set USE_LOCAL_MOCK=false

echo ===================================================
echo     jFindMe AUTO-NODE-SERVER BOOTSTRAPPER (PORT 3000)
echo ===================================================
echo Root: %APP_DIR%

:: Check if unified port 3000 is already active
netstat -ano | findstr ":3000" | findstr "LISTENING" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [INFO] Unified Web & Node Server is ALREADY running on port 3000.
) else (
    echo [STARTING] Initializing Unified Node Server js/server.js on port 3000...
    start "jFindMe_UNIFIED_SERVER" /D "%APP_DIR%" cmd /c "node js/server.js || pause"
)

echo [SUCCESS] Unified Server operational at http://127.0.0.1:3000
exit /b 0
