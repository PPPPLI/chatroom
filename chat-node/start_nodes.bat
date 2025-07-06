@echo off
setlocal enabledelayedexpansion

REM === Java & JavaFX Setup ===
set JAVA_EXE="D:\Java\bin\java.exe"
set JAVAFX_LIB="D:\openjfx-24.0.1_windows-x64_bin-sdk\openjfx-17.0.15_windows-x64_bin-sdk\javafx-sdk-17.0.15\lib"
set MODULES=javafx.controls,javafx.fxml

REM === Main Class & Base Port ===
set MAIN_CLASS=com.cloud.chat.MainApp
set BASE_PORT=5000
set COUNT=3

echo Starting !COUNT! chat nodes...

for /L %%i in (0,1,%COUNT%-1) do (
    set /A PORT=!BASE_PORT! + %%i
    start "Node_%%i" %JAVA_EXE% --module-path %JAVAFX_LIB% --add-modules %MODULES% ^
        -Dserver.port=!PORT! -cp target\classes %MAIN_CLASS%
)

echo All nodes started.
pause
