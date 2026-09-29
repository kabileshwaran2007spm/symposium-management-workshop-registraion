@echo off
REM CampusConnect dev launcher: backend on http://localhost:8080
REM
REM The JWT secret is NOT stored in this file. If JWT_SECRET is already set in
REM the environment it is used; otherwise a random secret is generated once and
REM kept in scripts\.dev-secrets (gitignored) so tokens survive dev restarts
REM without a secret ever being committed.

setlocal enabledelayedexpansion
set "SECRETS_FILE=%~dp0.dev-secrets"

if defined JWT_SECRET if not "%JWT_SECRET%"=="" (
    echo Using JWT_SECRET from environment.
    goto :run
)

if exist "%SECRETS_FILE%" (
    for /f "usebackq delims=" %%S in ("%SECRETS_FILE%") do set "JWT_SECRET=%%S"
    if not "%JWT_SECRET%"=="" goto :run
)

echo Generating a new development JWT secret...
powershell -NoProfile -Command "[guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')" > "%SECRETS_FILE%"
for /f "usebackq delims=" %%S in ("%SECRETS_FILE%") do set "JWT_SECRET=%%S"

:run
set APP_SEED_ENABLED=true
set APP_SEED_PASSWORD=Campus@123
cd /d "%~dp0..\backend"
java -jar target\campusconnect-0.0.1-SNAPSHOT.jar > "%TEMP%\cc-backend.log" 2>&1
