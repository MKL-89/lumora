@echo off
REM ============================================================================
REM  LUMORA - launcher for a REAL school (no demo data, no demo passwords)
REM
REM  Needs ONLY Java 17 or newer.
REM  The school's data is kept in .\school-data next to this file. It is separate
REM  from the demo (.\lumora-data), so demonstrating Lumora never touches it.
REM
REM  The very first time, the browser shows "Set up your school": enter the school
REM  details and choose the administrator's username and password there.
REM
REM  BACK UP the school-data folder regularly (copy it to a flash disk or cloud
REM  drive while Lumora is stopped). It holds every learner, mark and payment.
REM ============================================================================
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo.
echo  ==========================================================
echo    LUMORA SCHOOL MANAGEMENT SYSTEM  (school mode)
echo  ==========================================================
echo.

set JAVA_CMD=
where java >nul 2>&1
if %errorlevel%==0 (
    set JAVA_CMD=java
) else (
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
    echo      Install Java 17 or newer from https://adoptium.net and tick "Add to PATH".
    echo.
    pause
    exit /b 1
)

if not exist "lumora.jar" (
    echo  [X] lumora.jar was not found next to this script.
    echo.
    pause
    exit /b 1
)

echo  [OK] Starting Lumora. When you see "Started LumoraApplication" open:
echo.
echo            http://localhost:8081
echo.
echo       First time: you will be asked to set up the school.
echo       Keep this window open. Close it to stop the server.
echo  ----------------------------------------------------------
echo.

REM One login per browser, ordinary cookies (per-tab sessions are a demo feature).
set PER_TAB_SESSIONS=false
set MAX_SESSIONS_PER_USER=3

%JAVA_CMD% -jar lumora.jar --spring.profiles.active=standalone ^
  "--spring.datasource.url=jdbc:h2:file:./school-data/lumora;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE" ^
  --app.media.dir=./school-data/media

echo.
echo  The server has stopped.
pause
endlocal
