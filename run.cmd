@echo off
rem Run a Money Wrapped command on Windows, for example:  run structure
rem Put --reference first to run the instructor's build:  run --reference structure
setlocal
cd /d "%~dp0"
set "PROFILE="
if /i "%~1"=="--reference" (
  set "PROFILE=-Preference"
  shift
)
set "ARGS="
:loop
if "%~1"=="" goto go
set "ARGS=%ARGS% %~1"
shift
goto loop
:go
call mvnw.cmd -q %PROFILE% compile exec:java "-Dexec.args=%ARGS%"
