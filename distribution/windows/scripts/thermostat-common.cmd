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

set THERMOSTAT_HOME=%~dp0..
set THERMOSTAT_LIBS=%THERMOSTAT_HOME%\libs
set USER_THERMOSTAT_HOME=%USERPROFILE%\.thermostat

:: Duplicated in ThermostatVmMainLabelDecorator
set THERMOSTAT_MAIN=com.redhat.thermostat.main.Thermostat

if not defined JAVA_HOME (
  set jdk_home_candidate="@thermostat.jdk.home@"
  if exist %jdk_home_candidate%\bin\javac.exe (
    set JAVA_HOME=%jdk_home_candidate%
  ) else (
    :: Got likely a JRE, but thermostat expects a full JDK, try
    :: one level up and hope this will work. We check
    :: if JAVA_HOME is a valid value below.
    set JAVA_HOME=%jdk_home_candidate%\..
  )
)

if exist %THERMOSTAT_HOME%\etc\thermostatrc.cmd      call %THERMOSTAT_HOME%\etc\thermostatrc.cmd
if exist %USER_THERMOSTAT_HOME%\etc\thermostatrc.cmd call %USER_THERMOSTAT_HOME%\etc\thermostatrc.cmd

:: Verify that JAVA_HOME is a real JDK
if not exist %JAVA_HOME%\bin\javac.exe (
  echo JAVA_HOME does not seem to be a JDK. Thermostat needs a JDK to run.
  echo JAVA_HOME was set to '%JAVA_HOME%'
  exit /b 2
)

set JAVA=%JAVA_HOME%\bin\java

set SYSTEM_LOG_CONFIG_FILE=%THERMOSTAT_HOME%\etc\logging.properties
if exist %SYSTEM_LOG_CONFIG_FILE% (
    set LOGGING_ARGS=-Djava.util.logging.config.file=%SYSTEM_LOG_CONFIG_FILE%
)

set USER_LOG_CONFIG_FILE=%USER_THERMOSTAT_HOME%\etc\logging.properties
if exist %USER_LOG_CONFIG_FILE% (
    set LOGGING_ARGS=-Djava.util.logging.config.file=%USER_LOG_CONFIG_FILE%
)

set LOGGING_ARGS=%LOGGING_ARGS% -Djline.log.jul=true

exit /b 0