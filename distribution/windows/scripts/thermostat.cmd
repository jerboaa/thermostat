@echo off

:: Copyright 2016 Red Hat, Inc.
::
:: This file is part of Thermostat.
::
:: Thermostat is free software; you can redistribute it and/or modify
:: it under the terms of the GNU General Public License as published
:: by the Free Software Foundation; either version 2, or (at your
:: option) any later version.
::
:: Thermostat is distributed in the hope that it will be useful, but
:: WITHOUT ANY WARRANTY; without even the implied warranty of
:: MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
:: General Public License for more details.
::
:: You should have received a copy of the GNU General Public License
:: along with Thermostat; see the file COPYING.  If not see
:: <http://www.gnu.org/licenses/>.
::
:: Linking this code with other modules is making a combined work
:: based on this code.  Thus, the terms and conditions of the GNU
:: General Public License cover the whole combination.
::
:: As a special exception, the copyright holders of this code give
:: you permission to link this code with independent modules to
:: produce an executable, regardless of the license terms of these
:: independent modules, and to copy and distribute the resulting
:: executable under terms of your choice, provided that you also
:: meet, for each linked independent module, the terms and conditions
:: of the license of that module.  An independent module is a module
:: which is not derived from or based on this code.  If you modify
:: this code, you may extend this exception to your version of the
:: library, but you are not obligated to do so.  If you do not wish
:: to do so, delete this exception statement from your version.

setlocal
setlocal EnableDelayedExpansion
set retval=0

call %~dp0\thermostat-common.cmd
if not "%errorlevel%"=="0" exit /b %errorlevel%

:: ////////////////////////////////////////////////////////////////////

:: NOTE: The following variables come from the system/user
::       profiles (if any)
::
:: THERMOSTAT_EXT_BOOT_CLASSPATH
:: THERMOSTAT_EXT_JAVA_OPTS
:: THERMOSTAT_EXT_OPTS

:: This is the minimal boot classpath thermostat needs. Other dependencies
:: will get started by the OSGi framework once that's up.
set BOOT_CLASSPATH=%THERMOSTAT_LIBS%\org.apache.felix.framework-@felix.framework.version@.jar
set BOOT_CLASSPATH=%BOOT_CLASSPATH%;%THERMOSTAT_LIBS%\thermostat-launcher-@project.version@.jar
set BOOT_CLASSPATH=%BOOT_CLASSPATH%;%THERMOSTAT_LIBS%\thermostat-main-@project.version@.jar
set BOOT_CLASSPATH=%BOOT_CLASSPATH%;%THERMOSTAT_LIBS%\thermostat-shared-config-@project.version@.jar

:: Append extra class path entries coming from the profiles
if defined THERMOSTAT_EXT_BOOT_CLASSPATH (
  set BOOT_CLASSPATH=%BOOT_CLASSPATH%;%THERMOSTAT_EXT_BOOT_CLASSPATH%
)

goto skip1
:usage
  echo %~n0 "[-J<java-opt>] [-Tbg PIDFILE] [THERMOSTAT_ARGS]"
  exit /b 1
:skip1

:: start parsing arguments, we intercept jvm arguments vs thermostat specific arguments
::echo Thermostat for Windows

set JVM_ARGS=
set CMD_ARGS=
set RUN_IN_BG=0
set PID_FILE=""

:cmdloop
if not x%1==x (
  if "x%1"=="x-Tbg" (
    shift
    set RUN_IN_BG=1
    set PID_FILE=%1
    goto argparsed
  )
  if "-J"=="%1:~0,2%" (
    set JVM_ARGS=!JAVA_ARGS! %1:~2%
    goto argparsed
  )
  set CMD_ARGS=%CMD_ARGS% %1
:argparsed
  shift
  goto cmdloop
)

:: in a VM, jline can cause 100 CPU usage on Windows without this
set THERMOSTAT_EXT_JAVA_OPTS=%THERMOSTAT_EXT_JAVA_OPTS% -Djline.terminal=jline.UnsupportedTerminal

:: Finally run thermostat (optionally in the background
if "%RUN_IN_BG%"=="1" (
    :: The thermostat-agent-sysd script uses this.
    if not defined PID_FILE (
        echo "PID_FILE must be defined"
        exit /b 1
    ) else (
        start "%JAVA%" %THERMOSTAT_EXT_JAVA_OPTS% %LOGGING_ARGS% "%JVM_ARGS%" -cp "%BOOT_CLASSPATH%" %THERMOSTAT_MAIN% %THERMOSTAT_EXT_OPTS% %CMD_ARGS%
        set retval=%ERRORLEVEL%
        rem TODO echo thejavapid to a PID_FILE
    )
) else (
    %JAVA% %THERMOSTAT_EXT_JAVA_OPTS% %LOGGING_ARGS% %JVM_ARGS% -cp %BOOT_CLASSPATH% %THERMOSTAT_MAIN% %THERMOSTAT_EXT_OPTS% %CMD_ARGS%
    set retval=%ERRORLEVEL%
)
endlocal

exit /b %retval%



