@echo off
::
:: Copyright 2012-2016 Red Hat, Inc.
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
::
::####################################################################
::
:: Environment variables for the system Thermostat profile. You
:: can assume that THERMOSTAT_HOME variable is correctly set and you
:: may override system variables via a user profile in
:: USER_THERMOSTAT_HOME/etc/thermostatrc.
::
::####################################################################

::
:: Use a different JDK for running thermostat
::
rem JAVA_HOME=C:\java-1.8.0-openjdk

::
:: Extra jar files which need to be on the classpath when
:: Thermostat boots.
::
set THERMOSTAT_EXT_BOOT_CLASSPATH=%JAVA_HOME%\lib\tools.jar%

:: FIXME: Remove once jfreechart is a real OSGi bundle upstream
set THERMOSTAT_EXT_BOOT_CLASSPATH=%THERMOSTAT_EXT_BOOT_CLASSPATH%;%THERMOSTAT_HOME%\libs\jfreechart-@jfreechart.version@.jar
set THERMOSTAT_EXT_BOOT_CLASSPATH=%THERMOSTAT_EXT_BOOT_CLASSPATH%;%THERMOSTAT_HOME%\libs\jcommon-@jcommon.version@.jar
:: Needed to parse web.xml files without network connection See PR 2029.
set THERMOSTAT_EXT_BOOT_CLASSPATH=%THERMOSTAT_EXT_BOOT_CLASSPATH%;%THERMOSTAT_HOME%\plugins\embedded-web-endpoint\jetty-schemas-@jetty-schemas.version@.jar

::
:: Extra java options
::
rem set THERMOSTAT_EXT_JAVA_OPTS="-Xint -ea"

::
:: Extra options passed on to the Thermostat main class
::
rem set THERMOSTAT_EXT_OPTS=--ignore-bundle-versions


