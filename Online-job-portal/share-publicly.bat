@echo off
title JobSphere - Public Link Generator
echo ========================================================
echo   JobSphere Online Job Portal - Live Public Sharing
echo ========================================================
echo.

if not exist "%TEMP%\cloudflared.exe" (
    echo Downloading Cloudflare Tunnel tool...
    powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe' -OutFile '$env:TEMP\cloudflared.exe' -UseBasicParsing"
)

echo Starting secure public link for http://localhost:8080 ...
echo.
echo [1] Custom Named Link: https://online-job-portal.loca.lt
echo [2] Cloudflare High-Speed Link
echo.
echo Starting custom named link (https://online-job-portal.loca.lt)...
call npx --yes localtunnel --port 8080 --subdomain online-job-portal
pause
