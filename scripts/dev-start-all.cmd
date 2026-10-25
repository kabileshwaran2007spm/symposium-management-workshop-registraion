@echo off
REM CampusConnect - All-in-One Dev Launcher
echo.
echo ================================================================
echo  CampusConnect All-in-One Development Launcher
echo ================================================================

REM --- MongoDB ---
echo.
echo Checking MongoDB service...
sc query mongodb | findstr /I "RUNNING" >nul 2>&1
if not errorlevel 1 (
    echo MongoDB service is already running. ✅
) else (
    sc query mongodb >nul 2>&1
    if errorlevel 1 (
        echo MongoDB service was not found. Start MongoDB manually before using the app.
    ) else (
        echo Starting MongoDB service...
        net start mongodb
    )
)

REM --- Backend (Spring Boot) ---
echo.
echo Starting Backend (Spring Boot on :8080)...
start "CampusConnect Backend" /min "%~dp0dev-backend.cmd"
echo Backend launcher started in background.

REM --- Frontend (Vite) ---
echo.
echo Starting Frontend (Vite on :5173)...
start "CampusConnect Frontend" /min "%~dp0dev-frontend.cmd"
echo Frontend launcher started in background.

echo.
echo ================================================================
echo  All services launched.
echo  • Backend:   http://localhost:8080  (check %TEMP%\cc-backend.log)
echo  • Frontend:  http://localhost:5173  (check %TEMP%\cc-frontend.log)
echo  • MongoDB:   127.0.0.1:27017
echo.
echo  To stop all: scripts/dev-stop-all.cmd
echo.
echo ================================================================
pause