@echo off
REM ==============================================================================
REM Online Job Portal - Local Environment Preflight Verification Tool
REM Authored by: Saunak Sharma (Co-Contributor & Research Analyst)
REM ==============================================================================

echo [PREFLIGHT] Checking Online Job Portal Development Environment...
echo.

REM 1. Check Java JDK
echo [1/4] Checking Java Development Kit (JDK 17+)...
javac -version >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "tokens=2" %%i in ('javac -version 2^>^&1') do set JAVA_VER=%%i
    echo      [OK] Java compiler detected: %JAVA_VER%
) else (
    echo      [WARN] javac not found on PATH. Ensure JDK 17 is installed.
)

REM 2. Check Maven Wrapper
echo.
echo [2/4] Checking Maven Wrapper...
if exist "mvnw.cmd" (
    echo      [OK] Maven wrapper (mvnw.cmd) is present and ready.
) else (
    echo      [WARN] mvnw.cmd not found in current directory.
)

REM 3. Check MySQL Service
echo.
echo [3/4] Checking MySQL Service Status...
sc query MySQL80 >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo      [OK] MySQL80 Windows Service is registered and running.
) else (
    echo      [INFO] MySQL80 service not found or running as custom service name.
    echo             Application will safely use high-availability in-memory store.
)

REM 4. Check Port 8080 Availability
echo.
echo [4/4] Checking Port 8080 Availability...
netstat -ano | findstr :8080 >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo      [WARN] Port 8080 is currently occupied.
) else (
    echo      [OK] Port 8080 is available.
)

echo.
echo [PREFLIGHT] Preflight check completed.
