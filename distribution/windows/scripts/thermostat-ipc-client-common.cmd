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
:: <http:\\www.gnu.org\licenses\>.
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

rem setlocal

call %~dp0\thermostat-common.cmd

rem endlocal & ( set THERMOSTAT_LIBS=%THERMOSTAT_LIBS% )

rem Build classpath shared by all IPC clients

set IPC_CLASSPATH=%THERMOSTAT_LIBS%\thermostat-agent-ipc-client-@project.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\thermostat-agent-ipc-common-@project.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\thermostat-agent-ipc-tcpsocket-client-@project.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\thermostat-agent-ipc-tcpsocket-common-@project.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\thermostat-agent-ipc-winpipes-client-@project.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\thermostat-agent-ipc-winpipes-common-@project.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\jnr-enxio-@jnr-enxio.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\jnr-constants-@jnr-constants.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\jnr-posix-@jnr-posix.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\jnr-ffi-@jnr-ffi.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\jnr-x86asm-@jnr-x86asm.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\jffi-@jffi.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\jffi-@jffi.version@-native.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\asm-@asm.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\asm-commons-@asm.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\asm-util-@asm.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\asm-analysis-@asm.version@.jar
set IPC_CLASSPATH=%IPC_CLASSPATH%;%THERMOSTAT_LIBS%\asm-tree-@asm.version@.jar

rem set THERMOSTAT_LIBS=

rem echo %IPC_CLASSPATH%

exit /b 0
