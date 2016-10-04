@echo off

:: Copyright 2016 Red Hat, Inc.
::
:: This file is part of Thermostat.
::
:: Thermostat is free software; you can redistribute it and\or modify
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

if "%3"=="" goto usage
if not "%4"=="" goto usage

goto skipfuncdefs

:usage
  echo "usage: %~f0 <hostname> <port> <ipcConfigFile>"
  exit /b 1

:skipfuncdefs

set HOSTNAME=%1
set PORT=%2
set CONFIG_FILE=%3

:: Source thermostat-ipc-client-common from same directory as this script
:: Defines IPC_CLASSPATH variable with JARs necessary for the IPC service

call %~dp0\thermostat-ipc-client-common.cmd
if not "%errorlevel%"=="0" exit /b %errorlevel%

:: Ensure thermostat-ipc-client-common sourced correctly
if not defined IPC_CLASSPATH (
  echo "Classpath not properly defined for command channel" 
  exit /b 1
)

:: Additional JARs necessary for the server
set IPC_CLASSPATH=%IPC_CLASSPATH%:%THERMOSTAT_LIBS%\thermostat-common-core-@project.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%:%THERMOSTAT_LIBS%\thermostat-shared-config-@project.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%:%THERMOSTAT_LIBS%\thermostat-agent-command-@project.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%:%THERMOSTAT_LIBS%\thermostat-common-command-@project.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%:%THERMOSTAT_LIBS%\thermostat-agent-command-server-@project.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%:%THERMOSTAT_LIBS%\netty-buffer-@netty.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%:%THERMOSTAT_LIBS%\netty-common-@netty.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%:%THERMOSTAT_LIBS%\netty-transport-@netty.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%:%THERMOSTAT_LIBS%\netty-codec-@netty.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%:%THERMOSTAT_LIBS%\netty-handler-@netty.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%:%THERMOSTAT_LIBS%\gson-@gson.version@.jar

set CMD_CHANNEL_CLASS=com.redhat.thermostat.agent.command.server.internal.CommandChannelServerMain

:: Set this to remote debug
if defined THERMOSTAT_CMDC_DEBUG (
  set DEBUG_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,quiet=y,address=1083
)


:: Start server

set CONFIG_FILE_ARG=-DipcConfigFile=%CONFIG_FILE%
%JAVA% %CONFIG_FILE_ARG% %LOGGING_ARGS% -cp %IPC_CLASSPATH% %DEBUG_OPTS% %CMD_CHANNEL_CLASS% %HOSTNAME% %PORT%



