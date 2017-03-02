@echo off

set CMD_DIR=%~dp0
set CYGWIN_DIR=c:\cygwin64
set PATH=%PATH%;%CYGWIN_DIR%\bin

for /f "delims=" %%a in ('%CYGWIN_DIR%\bin\cygpath -u %CMD_DIR%') do @set CMD_DIR=%%a

%CYGWIN_DIR%\bin\bash -c %CMD_DIR%verify-bash-completion-agent.sh

rem echo XXX ERRR = %errorlevel%
exit /b %errorlevel%
