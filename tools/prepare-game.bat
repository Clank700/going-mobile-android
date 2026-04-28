@echo off
setlocal

echo Ratchet ^& Clank: Going Mobile Android Port Kit
echo.
echo Drag-and-drop mode is supported:
echo   Drop your Going Mobile 1.1.0.jar onto this file.
echo.
echo This will prepare the project and try to build a debug APK.
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0prepare-game.ps1" -Build %*
set EXIT_CODE=%ERRORLEVEL%

echo.
if "%EXIT_CODE%"=="0" (
    echo Preparation finished successfully.
    echo If Android build tools were found, the APK is here:
    echo   %~dp0..\going-mobile\app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo You can also open this project folder in Android Studio:
    echo   %~dp0..
) else (
    echo Preparation failed. Read the message above for details.
)
echo.
echo This window will stay open so you can read the result.
pause
exit /b %EXIT_CODE%
