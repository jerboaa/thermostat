@echo off

:: Copyright 2017 Red Hat, Inc.
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

if "%4"=="" goto usage
if not "%5"=="" goto usage

 goto skipfuncdefs

 :usage
   echo "usage: %~f0 <pidOfTargetJvm> <userNameOfJvmOwner> <ipcConfigFile> <ipcServerName>"
   exit /b 1

 :skipfuncdefs

set TARGET_PID=%1
set TARGET_USER=%2
set CONFIG_FILE=%3
set IPC_SERVER_NAME=%4

:: Source thermostat-ipc-client-common from same directory as this script
:: Defines IPC_CLASSPATH variable with JARs necessary for the IPC service

call %~dp0\thermostat-ipc-client-common.cmd
if not "%errorlevel%"=="0" exit /b %errorlevel%

:: Ensure thermostat-ipc-client-common sourced correctly
if not defined IPC_CLASSPATH (
  echo "Classpath not properly defined for agent proxy"
  exit /b 1
)

:: Need tools from the JVM
set TOOLS_JAR=%JAVA_HOME%\lib\tools.jar

:: Additional JARs necessary for the agent proxy
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\thermostat-common-core-@project.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\thermostat-shared-config-@project.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\thermostat-agent-proxy-server-@project.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\gson-@gson.version@.jar
set IPC_CLASSPATH=%TOOLS_JAR%;%IPC_CLASSPATH%

set AGENT_PROXY_CLASS=com.redhat.thermostat.agent.proxy.server.AgentProxy

:: Set this to remote debug
if defined THERMOSTAT_DEBUG (
  set DEBUG_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=1082
)

:: Start server
:: within the server, consider adjusting toek priviledge to disable debug, etc
set CONFIG_FILE_ARG=-DipcConfigFile=%CONFIG_FILE%

%JAVA% -cp %IPC_CLASSPATH% %CONFIG_FILE_ARG% %LOGGING_ARGS% %DEBUG_OPTS% %AGENT_PROXY_CLASS% %TARGET_PID% %IPC_SERVER_NAME% %TARGET_USER%


