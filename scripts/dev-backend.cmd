@echo off
REM CampusConnect dev launcher: backend on http://localhost:8080
set JWT_SECRET=CampusConnectDevSecretKeyForHs256SigningMustBeLongEnough
set APP_SEED_ENABLED=true
set APP_SEED_PASSWORD=Campus@123
cd /d "%~dp0..\backend"
java -jar target\campusconnect-0.0.1-SNAPSHOT.jar > "%TEMP%\cc-backend.log" 2>&1
