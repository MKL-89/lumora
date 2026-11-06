@echo off
REM ============================================================================
REM  LUMORA - standalone launcher
REM
REM  Needs ONLY Java 17 or newer. No MySQL, no XAMPP, no Maven.
REM  The database is an embedded H2 file created automatically in .\lumora-data
REM
REM  Just double-click this file. When you see "Started LumoraApplication",
REM  open a browser at:   http://localhost:8081
REM  Sign in with:        principal / Lumora@123
REM ============================================================================
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo.
echo  ==========================================================
echo    LUMORA SCHOOL MANAGEMENT SYSTEM  (standalone mode)
echo  ==========================================================
echo.

REM ---- Find Java -------------------------------------------------------------
set JAVA_CMD=
where java >nul 2>&1
if %errorlevel%==0 (
    set JAVA_CMD=java
) else (
    REM Java is not on PATH - look in the usual install locations.
    for %%D in (
        "C:\Program Files\Eclipse Adoptium"
        "C:\Program Files\Java"
        "C:\Program Files\Microsoft\jdk"
        "C:\Program Files\Amazon Corretto"
        "C:\Program Files\Zulu"
    ) do (
        if exist "%%~D" (
            for /f "delims=" %%J in ('dir /b /ad "%%~D" 2^>nul') do (
                if exist "%%~D\%%J\bin\java.exe" set JAVA_CMD="%%~D\%%J\bin\java.exe"
            )
        )
    )
)

if "%JAVA_CMD%"=="" (
    echo  [X] Java was not found on this computer.
    echo.
    echo      Install Java 17 or newer, then run this file again.
    echo      Free download:  https://adoptium.net/temurin/releases/?version=17
    echo      Pick the Windows .msi installer and tick "Add to PATH".
    echo.
    pause
    exit /b 1
)

echo  [OK] Using Java: %JAVA_CMD%
%JAVA_CMD% -version
echo.

REM ---- Find the application jar ----------------------------------------------
set JAR=lumora.jar
if not exist "%JAR%" (
    if exist "target\lumora-school-management-system-0.0.1-SNAPSHOT.jar" (
        set JAR=target\lumora-school-management-system-0.0.1-SNAPSHOT.jar
    ) else (
        echo  [X] lumora.jar was not found next to this script.
        echo      Make sure you copied the whole folder, including lumora.jar.
        echo.
        pause
        exit /b 1
    )
)

echo  [OK] Starting Lumora. This takes about 30-40 seconds the first time.
echo.
echo       When you see  "Started LumoraApplication"  open your browser at:
echo.
echo            http://localhost:8081
echo.
echo       Sign in with   principal / Lumora@123
echo       (other roles: bursar, librarian, grace.wanjiru, student, parent - same password)
echo.
echo       Keep this window open. Close it to stop the server.
echo  ----------------------------------------------------------
echo.

%JAVA_CMD% -jar "%JAR%" --spring.profiles.active=standalone,demo

echo.
echo  The server has stopped.
pause
endlocal
