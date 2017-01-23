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

%~dp0\thermostat-common.cmd

echo thermostat-webservice not implemented on Windows.
exit 1

set TOMCAT_DIR=tomcat

process_args() {
  while [ $# -gt 0 ]; do
    THIS_ARG=$1
    shift
    if [ $THIS_ARG = "-h" -o $THIS_ARG = "--help" -o $THIS_ARG = "help" ] ; then
      do_help
      exit
      break
    elif [ $THIS_ARG = "-t" ] ; then
      TOMCAT_DIR=$1
      shift
    elif [ $THIS_ARG = "start" -o $THIS_ARG = "stop" ] ; then
      FUNCTION=$THIS_ARG
    else
      echo "Unrecognized argument: $THIS_ARG"
      exit 1
    fi
  done
  check_valid_args
}

do_start() {
  rm -rf webapps/thermostat
  cp -r "$TH/web/war/target/thermostat-web-war-@project.version@" webapps/thermostat
  if [ $CYGWIN_MODE -eq 1 ]; then
    JAVA_OPTS="-Djava.security.auth.login.config=`cygpath -w ${TH}/distribution/target/image/etc/thermostat_jaas.conf`" ./bin/startup.sh
  else
    JAVA_OPTS="-Djava.security.auth.login.config=${TH}/distribution/target/image/etc/thermostat_jaas.conf" ./bin/startup.sh
  fi
}

do_stop() {
  ./bin/shutdown.sh
}

do_function() {
  TH="$(pwd)"
  cd %TOMCAT_DIR%
  case $1 in
    start )
      do_start
      ;;
    stop )
      do_stop
      ;;
  esac
  cd ${TH}
}

process_args $*
do_function $FUNCTION

::  Please be familiar with these privileges to decide whether this
::  set of users and roles is appropriate for you or your testing.
:: 
:: TH="$(pwd)"
:: cd path/to/tomcat
:: rm -rf webapps/thermostat
:: cp -r $TH/web/war/target/thermostat-web-war-0.16.0-SNAPSHOT webapps/thermostat
:: export JAVA_OPTS="-Djava.security.auth.login.config=${TH}/distribution/target/image/etc/thermostat_jaas.conf"
:: ./bin/startup.sh
:: cd $TH
:: ./distribution/target/image/bin/thermostat storage --start
:: mkdir -p ~/.thermostat/etc
:: echo -e "username=agent-tester\npassword=tester" > ~/.thermostat/etc/agent.auth
:: ./distribution/target/image/bin/thermostat agent -d http://127.0.0.1:8080/thermostat/storage


rem functions

:usage
  echo Usage:
  echo    thermostat-webservice [-t <root of tomcat directory>] <start|stop>
  exit /b 0


:check_valid_args
  if "x%FUNCTION%" = "x" (
    echo "You must specify either start, stop, or help."
    call usage
    exit /b 1
  )
  if not exist %TOMCAT_DIR% (
    echo "Tomcat directory does not exist: %TOMCAT_DIR%"
    call usage
    exit /b 1
  fi
  exit /b 0


print_help:
echo thermostat-webservice: A convenience script for starting and stopping
echo tomcat with thermostat web storage application deployed.
echo
echo This script is intended to be an aid for developers to start up
echo tomcat and deploy thermostat web storage app.  It is assumed that
echo there is already a backing storage running that the web app can
echo connect to, and that the web.xml file from the build image is
echo correctly configured for this backing storage connection.  See
echo the file:
echo
echo    web/war/target/thermostat-web-war-0.16.0-SNAPSHOT/WEB-INF/web.xml
echo
echo In addition, an appropriate user and role configuration must be
echo present in the build image.  See the files:
echo
echo    distribution/target/image/etc/thermostat-users.properties
echo    distribution/target/image/etc/thermostat-roles.properties
echo
echo An agent requires authentication configuration in order to connect
echo to secured storage.  See the file:
echo
echo    ~/.thermostat/etc/agent.auth
echo
echo Possible contents of thermostat-users.properties:
echo
echo    agent-tester=tester
echo    client-tester=tester
echo
echo Corresponding potential thermostat-roles.properties:
echo
echo    agent-tester=thermostat-agent, thermostat-files-grant-write-filename-ALL
echo    client-tester=thermostat-client, thermostat-cmdc-allPrivs, \\
echo                  thermostat-files-grant-read-filename-ALL
echo    thermostat-agent=thermostat-write, thermostat-prepare-statement, \\
echo                     thermostat-save-file, thermostat-purge, \\
echo                     thermostat-register-category, thermostat-cmdc-verify, \\
echo                     thermostat-login, thermostat-realm
echo    thermostat-client=thermostat-agents-grant-read-agentId-ALL, \\
echo                      thermostat-hosts-grant-read-hostname-ALL, \\
echo                      thermostat-vms-grant-read-vmId-ALL, \\
echo                      thermostat-vms-grant-read-username-ALL, thermostat-realm, \\
echo                      thermostat-login, thermostat-query, \\
echo                      thermostat-prepare-statement, thermostat-cmdc-generate, \\
echo                      thermostat-load-file, thermostat-get-count, \\
echo                      thermostat-register-category
echo    thermostat-cmdc-allPrivs = thermostat-cmdc-grant-garbage-collect, \\
echo                      thermostat-cmdc-grant-dump-heap, \\
echo                      thermostat-cmdc-grant-thread-harvester, \\
echo                      thermostat-cmdc-grant-killvm, \\
echo                      thermostat-cmdc-grant-ping, \\
echo                      thermostat-cmdc-grant-jmx-toggle-notifications
echo
echo Please be familiar with these privileges to decide whether this
echo set of users and roles is appropriate for you or your testing.
echo
echo Contents of ~\.thermostat\etc\agent.auth that would be valid with the above:
echo
echo    username=agent-tester
echo    password=tester
echo
echo Here is typical use of this script:
echo
echo    $ echo %USER_FLUFF% > distribution\target\image\etc\thermostat-users.properties
echo    $ echo %ROLE_FLUFF% > distribution\target\image\etc\thermostat-roles.properties
echo    $ echo %AGENT_FLUFF% > ~\.thermostat\etc\agent.auth
echo    $ .\distribution\target\image\bin\thermostat storage --start
echo    $ .\distribution\target\image\bin\thermostat-webservice -t \path\to\tomcat start
echo    $ .\distribution\target\image\bin\thermostat agent -d http:/127.0.0.1:8080/thermostat/storage
echo    $ .\distribution\target\image\bin\thermostat gui
echo
echo    ... and so forth.
echo
echo    .\distribution\target\image\bin\thermostat-webservice -t \path\to\tomcat stop
echo    .\distribution\target\image\bin\thermostat storage --stop
echo
echo NOTE: Use of this script to start tomcat will blow away webapps/thermostat
echo in your tomcat directory and replace it with the corresponding contents
echo from your build image.
echo
echo NOTE2: If tomcat is already running, results of this script are undefined.
echo
echo .

:done
exit /b 0

