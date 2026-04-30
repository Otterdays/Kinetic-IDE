@echo off
setlocal EnableExtensions
cd /d "%~dp0." || exit /b 1

REM Debug APK by default (no release keystore in repo). Pass: release
set "TASK=:app:assembleDebug"
set "APK_DIR=app\build\outputs\apk\debug"
if /i "%~1"=="release" (
    set "TASK=:app:assembleRelease"
    set "APK_DIR=app\build\outputs\apk\release"
)

echo Running gradlew.bat %TASK% ...
call gradlew.bat %TASK%
if errorlevel 1 (
    echo Build failed.
    exit /b 1
)

if not exist "%APK_DIR%" (
    echo APK output folder missing: %APK_DIR%
    exit /b 1
)

set "DEST=BUILT"
if not exist "%DEST%" mkdir "%DEST%"

copy /Y "%APK_DIR%\*.apk" "%DEST%\" >nul
if errorlevel 1 (
    echo No .apk found under %APK_DIR%
    exit /b 1
)

for %%F in ("%APK_DIR%\*.apk") do echo Copied %%~nxF -^> %DEST%\
exit /b 0
