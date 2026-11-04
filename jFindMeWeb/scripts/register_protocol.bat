@echo off
title Register jFindMe Protocol Handler
cd /d "%~dp0"

echo [SYSTEM] Registering jfindme:// protocol handler for current user...

set BATCH_PATH=%~dp0launch_server.bat

reg add "HKCU\Software\Classes\jfindme" /ve /t REG_SZ /d "URL:jFindMe Protocol" /f >nul
reg add "HKCU\Software\Classes\jfindme" /v "URL Protocol" /t REG_SZ /d "" /f >nul
reg add "HKCU\Software\Classes\jfindme\shell\open\command" /ve /t REG_SZ /d "\"cmd.exe\" /c \"\"%BATCH_PATH%\"\"" /f >nul

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Protocol handler jfindme:// successfully registered!
) else (
    echo [ERROR] Failed to register protocol handler.
)
