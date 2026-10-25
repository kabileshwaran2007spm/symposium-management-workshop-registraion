@echo off
REM CampusConnect dev launcher: backend on http://localhost:8080
REM A cryptographic JWT secret is kept locally in the gitignored .dev-secrets.

setlocal
set "SECRETS_FILE=%~dp0.dev-secrets"

if not defined JAVA_HOME if exist "%ProgramFiles%\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe" set "JAVA_HOME=%ProgramFiles%\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
if not defined JAVA_HOME (
    echo JAVA_HOME is not set. Install JDK 25 and set JAVA_HOME before starting CampusConnect.
    exit /b 1
)
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo JAVA_HOME does not point to a JDK: "%JAVA_HOME%"
    exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

set "BACKEND_PID="
for /f %%P in ('powershell -NoProfile -Command "(Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty OwningProcess)"') do set "BACKEND_PID=%%P"
if defined BACKEND_PID (
    echo Port 8080 is already in use. Current listener:
    powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter 'ProcessId = %BACKEND_PID%' | Select-Object ProcessId,Name,ExecutablePath"
    echo No process was stopped. Stop the identified CampusConnect process or choose another port.
    exit /b 1
)

if defined JWT_SECRET if not "%JWT_SECRET%"=="" (
    goto :run
)

if exist "%SECRETS_FILE%" (
    for /f "usebackq delims=" %%S in ("%SECRETS_FILE%") do set "JWT_SECRET=%%S"
    if not "%JWT_SECRET%"=="" goto :run
)

echo Creating a local development JWT secret...
powershell -NoProfile -Command "$bytes = New-Object byte[] 48; $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create(); try { $rng.GetBytes($bytes); [Convert]::ToBase64String($bytes) | Set-Content -NoNewline -Encoding ascii -LiteralPath '%SECRETS_FILE%' } finally { $rng.Dispose() }"
if errorlevel 1 (
    echo Could not create the local JWT secret.
    exit /b 1
)
for /f "usebackq delims=" %%S in ("%SECRETS_FILE%") do set "JWT_SECRET=%%S"

:run
if not defined APP_SEED_ENABLED set "APP_SEED_ENABLED=false"
if /I "%APP_SEED_ENABLED%"=="true" if not defined APP_SEED_PASSWORD (
    echo APP_SEED_ENABLED is true but APP_SEED_PASSWORD is not set.
    echo Set a private seed password in this terminal or disable seeding.
    exit /b 1
)
cd /d "%~dp0..\backend"
call mvnw.cmd -B spring-boot:run > "%TEMP%\cc-backend.log" 2>&1
