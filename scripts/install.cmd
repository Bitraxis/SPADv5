@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "ROOT_DIR=%SCRIPT_DIR%.."
for %%I in ("%ROOT_DIR%") do set "ROOT_DIR=%%~fI"

set "PREFIX=%USERPROFILE%\.spad"
set "ADD_PATH=0"

:parse_args
if "%~1"=="" goto args_done
if /I "%~1"=="--add-path" goto opt_add_path
if /I "%~1"=="--prefix" goto opt_prefix
if /I "%~1"=="--help" goto usage
if /I "%~1"=="-h" goto usage
set "PREFIX=%~1"
shift
goto parse_args

:opt_add_path
set "ADD_PATH=1"
shift
goto parse_args

:opt_prefix
shift
if "%~1"=="" (
  echo Missing value for --prefix
  exit /b 1
)
set "PREFIX=%~1"
shift
goto parse_args

:args_done

if not exist "%ROOT_DIR%\dist\lib\spad\spad-tools.jar" (
  call "%ROOT_DIR%\scripts\build-all.cmd"
  if errorlevel 1 exit /b 1
)

mkdir "%PREFIX%\bin" 2>nul
mkdir "%PREFIX%\lib\spad" 2>nul

copy /y "%ROOT_DIR%\dist\lib\spad\spad-tools.jar" "%PREFIX%\lib\spad\spad-tools.jar" >nul
copy /y "%ROOT_DIR%\dist\bin\dragon.cmd" "%PREFIX%\bin\dragon.cmd" >nul
copy /y "%ROOT_DIR%\dist\bin\spad.cmd" "%PREFIX%\bin\spad.cmd" >nul

if "%ADD_PATH%"=="1" (
  call :add_path "%PREFIX%\bin"
)

echo Installed SPAD/Dragon tools to %PREFIX%
echo Add this to PATH if needed: %PREFIX%\bin
if "%ADD_PATH%"=="1" echo Updated user PATH and current session when needed
echo Try: dragon help
exit /b 0

:usage
echo Usage: %~nx0 [--add-path] [--prefix ^<dir^>] [install-dir]
exit /b 0

:add_path
set "BIN_DIR=%~1"
set "CURRENT_PATH="
for /f "tokens=2,*" %%A in ('reg query "HKCU\Environment" ^| findstr /I /R "^[ ]*Path[ ]*REG_"') do set "CURRENT_PATH=%%B"
if not defined CURRENT_PATH set "CURRENT_PATH=%PATH%"

echo(!CURRENT_PATH!; | findstr /I /C:";%BIN_DIR%;" >nul
if errorlevel 1 (
  if defined CURRENT_PATH (
    set "NEW_PATH=%CURRENT_PATH%;%BIN_DIR%"
  ) else (
    set "NEW_PATH=%BIN_DIR%"
  )
  reg add "HKCU\Environment" /v Path /t REG_EXPAND_SZ /d "%NEW_PATH%" /f >nul
  if errorlevel 1 (
    echo Failed to update user PATH
    exit /b 1
  )
  set "PATH=%BIN_DIR%;%PATH%"
)
exit /b 0
