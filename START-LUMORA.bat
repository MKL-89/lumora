@echo off
REM ============================================================================
REM  LUMORA - start from the project folder
REM
REM  Needs only Java 17+. If the app has already been built it starts instantly
REM  in standalone mode (embedded database, no MySQL). If it has not been built
REM  yet, it builds itself first using the bundled Maven wrapper - still no need
REM  to install Maven.
REM
REM  When you see "Started LumoraApplication", open:  http://localhost:8081
REM  Sign in with:  principal / Lumora@123
REM ============================================================================
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo.
echo  ==========================================================
echo    LUMORA SCHOOL MANAGEMENT SYSTEM
echo  ==========================================================
echo.

REM ---- Find Java -------------------------------------------------------------
where java >nul 2>&1
if not %errorlevel%==0 (
    echo  [X] Java was not found on this computer.
    echo      Install Java 17 or newer from:
    echo        https://adoptium.net/temurin/releases/?version=17
    echo      Choose the Windows .msi and tick "Add to PATH", then run this again.
    echo.
    pause
    exit /b 1
)
echo  [OK] Java found.

REM ---- Locate an already-built jar -------------------------------------------
set JAR=
if exist "dist\lumora.jar" set JAR=dist\lumora.jar
if "%JAR%"=="" if exist "target\lumora-school-management-system-0.0.1-SNAPSHOT.jar" set JAR=target\lumora-school-management-system-0.0.1-SNAPSHOT.jar

REM ---- Build it if necessary (uses the Maven wrapper, no Maven install) -------
if "%JAR%"=="" (
    echo  [..] First run - building the application. This can take a few minutes.
    call mvnw.cmd -q -DskipTests package
    if not !errorlevel!==0 (
        echo  [X] The build failed. Make sure you are online for the first build
        echo      so the wrapper can download its dependencies, then try again.
        echo.
        pause
        exit /b 1
    )
    set JAR=target\lumora-school-management-system-0.0.1-SNAPSHOT.jar
)

echo  [OK] Starting Lumora in standalone mode (embedded database).
echo.
echo       When you see  "Started LumoraApplication"  open your browser at:
echo            http://localhost:8081
echo       Sign in with   principal / Lumora@123
echo.
echo       Keep this window open. Close it to stop the server.
echo  ----------------------------------------------------------
echo.

java -jar "%JAR%" --spring.profiles.active=standalone,demo

echo.
echo  The server has stopped.
pause
endlocal
