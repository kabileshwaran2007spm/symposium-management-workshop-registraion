@echo off
REM CampusConnect dev launcher: frontend on http://localhost:5173
set PATH=%PATH%;C:\Users\KABI\AppData\Local\Microsoft\WinGet\Packages\OpenJS.NodeJS.LTS_Microsoft.Winget.Source_8wekyb3d8bbwe\node-v24.19.0-win-x64
cd /d "%~dp0..\frontend"
npm run dev > "%TEMP%\cc-frontend.log" 2>&1
