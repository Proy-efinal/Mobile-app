@echo off
setlocal
set "JAVA_APP=%~dp0..\..\work\jdk-21.0.13.1+2\bin\javaw.exe"
if exist "%JAVA_APP%" goto abrir
if not defined JAVA_HOME goto buscar
set "JAVA_APP=%JAVA_HOME%\bin\javaw.exe"
if exist "%JAVA_APP%" goto abrir
:buscar
where javaw.exe >nul 2>nul
if errorlevel 1 (
    echo Se necesita Java 17 o superior para abrir Facturacion.
    pause
    exit /b 1
)
set "JAVA_APP=javaw.exe"
:abrir
start "" "%JAVA_APP%" -jar "%~dp0Facturacion.jar"
