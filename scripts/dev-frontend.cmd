@echo off
REM CampusConnect dev launcher: frontend on http://localhost:5173
if not defined NODE_HOME if exist "%LOCALAPPDATA%\Programs\node-v24.22.1\node-v24.22.1-win-x64\node.exe" set "NODE_HOME=%LOCALAPPDATA%\Programs\node-v24.22.1\node-v24.22.1-win-x64"
if defined NODE_HOME if exist "%NODE_HOME%\node.exe" set "PATH=%NODE_HOME%;%PATH%"
where npm.cmd >nul 2>&1
if errorlevel 1 (
    echo npm.cmd was not found. Install Node.js 24 LTS and open a new terminal.
    exit /b 1
)
set "FRONTEND_PID="
for /f %%P in ('powershell -NoProfile -Command "(Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty OwningProcess)"') do set "FRONTEND_PID=%%P"
if defined FRONTEND_PID (
    echo Port 5173 is already in use. Current listener:
    powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter 'ProcessId = %FRONTEND_PID%' | Select-Object ProcessId,Name,ExecutablePath"
    echo No process was stopped. Stop the identified frontend process or choose another port.
    exit /b 1
)
cd /d "%~dp0..\frontend"
call npm.cmd run dev > "%TEMP%\cc-frontend.log" 2>&1
