@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
set "JAR=%SCRIPT_DIR%..\lib\spad\spad-tools.jar"
java -cp "%JAR%" dragon.DragonMain %*
