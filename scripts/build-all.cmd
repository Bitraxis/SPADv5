@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "ROOT_DIR=%SCRIPT_DIR%.."
for %%I in ("%ROOT_DIR%") do set "ROOT_DIR=%%~fI"

set "BUILD_DIR=%ROOT_DIR%\build\tools"
set "CLASS_DIR=%BUILD_DIR%\classes"
set "DIST_DIR=%ROOT_DIR%\dist"
set "LIB_DIR=%DIST_DIR%\lib\spad"
set "BIN_DIR=%DIST_DIR%\bin"

if exist "%CLASS_DIR%" rmdir /s /q "%CLASS_DIR%"
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"

mkdir "%CLASS_DIR%" || exit /b 1
mkdir "%LIB_DIR%" || exit /b 1
mkdir "%BIN_DIR%" || exit /b 1

pushd "%ROOT_DIR%" >nul
javac -d "%CLASS_DIR%" main\*.java dragon\*.java
if errorlevel 1 (
	popd >nul
	exit /b 1
)
popd >nul

jar --create --file "%LIB_DIR%\spad-tools.jar" -C "%CLASS_DIR%" .
if errorlevel 1 exit /b 1

copy /y "%ROOT_DIR%\scripts\launchers\dragon" "%BIN_DIR%\dragon" >nul
copy /y "%ROOT_DIR%\scripts\launchers\spad" "%BIN_DIR%\spad" >nul
copy /y "%ROOT_DIR%\scripts\launchers\dragon.cmd" "%BIN_DIR%\dragon.cmd" >nul
copy /y "%ROOT_DIR%\scripts\launchers\spad.cmd" "%BIN_DIR%\spad.cmd" >nul

echo Build complete
echo - Jar: %LIB_DIR%\spad-tools.jar
echo - Launchers: %BIN_DIR%
