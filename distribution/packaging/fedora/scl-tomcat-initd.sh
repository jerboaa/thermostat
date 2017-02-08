#!/bin/bash
#
# Copyright 2012-2017 Red Hat, Inc.
#
# This file is part of Thermostat.
#
# Thermostat is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published
# by the Free Software Foundation; either version 2, or (at your
# option) any later version.
#
# Thermostat is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with Thermostat; see the file COPYING.  If not see
# <http://www.gnu.org/licenses/>.
#
# Linking this code with other modules is making a combined work
# based on this code.  Thus, the terms and conditions of the GNU
# General Public License cover the whole combination.
#
# As a special exception, the copyright holders of this code give
# you permission to link this code with independent modules to
# produce an executable, regardless of the license terms of these
# independent modules, and to copy and distribute the resulting
# executable under terms of your choice, provided that you also
# meet, for each linked independent module, the terms and conditions
# of the license of that module.  An independent module is a module
# which is not derived from or based on this code.  If you modify
# this code, you may extend this exception to your version of the
# library, but you are not obligated to do so.  If you do not wish
# to do so, delete this exception statement from your version.
#

#
# thermostat1-thermostat-tomcat    This shell script takes care of deploying SCL-ized thermostat
#                                  webapp.
#
# chkconfig: - 80 20
#
### BEGIN INIT INFO
# Provides: __service_name__
# Required-Start: $network $syslog
# Required-Stop: $network $syslog
# Default-Start:
# Default-Stop:
# Description: Thermostat tomcat start wrapper
# Short-Description: start and stop Thermostat's tomcat
### END INIT INFO
#
# Clone of tomcat6's init script.
#

## Source function library.
#. /etc/rc.d/init.d/functions
# Source LSB function library.
if [ -r /lib/lsb/init-functions ]; then
    . /lib/lsb/init-functions
else
    exit 1
fi

DISTRIB_ID=`lsb_release -i -s 2>/dev/null`

NAME="$(basename $0)"
unset ISBOOT
if [ "${NAME:0:1}" = "S" -o "${NAME:0:1}" = "K" ]; then
    NAME="${NAME:3}"
    ISBOOT="1"
fi

# For SELinux we need to use 'runuser' not 'su'
if [ -x "/sbin/runuser" ]; then
    SU="/sbin/runuser -s /bin/sh"
else
    SU="/bin/su -s /bin/sh"
fi

# Get the tomcat config (use this for environment specific settings)
TOMCAT_CFG="/etc/tomcat6/tomcat6.conf"
if [ -r "$TOMCAT_CFG" ]; then
    . $TOMCAT_CFG
fi

# Get instance specific config file
if [ -r "/etc/sysconfig/${NAME}" ]; then
    . /etc/sysconfig/${NAME}
fi

# Define which connector port to use
CONNECTOR_PORT="${CONNECTOR_PORT:-8080}"

# Path to the tomcat launch script
TOMCAT_SCRIPT="/usr/sbin/tomcat6"

# Tomcat program name
TOMCAT_PROG="${NAME}"
        
# Define the tomcat username
TOMCAT_USER="${TOMCAT_USER:-tomcat}"

# Define the tomcat log file
TOMCAT_LOG="${TOMCAT_LOG:-/var/log/${NAME}-initd.log}"

# Define the pid file name
# If change is needed, use sysconfig instead of here
export CATALINA_PID="${CATALINA_PID:-/var/run/${NAME}.pid}"

RETVAL="0"

function parseOptions() {
    options=""
    options="$options $(
                 awk '!/^#/ && !/^$/ { ORS=" "; print "export ", $0, ";" }' \
                 $TOMCAT_CFG
             )"
    if [ -r "/etc/sysconfig/${NAME}" ]; then
        options="$options $(
                     awk '!/^#/ && !/^$/ { ORS=" "; 
                                           print "export ", $0, ";" }' \
                     /etc/sysconfig/${NAME}
                 )"
    fi
    TOMCAT_SCRIPT="$options ${TOMCAT_SCRIPT}"
}

# rhbz 757632
function version() {
	parseOptions
	$SU - $TOMCAT_USER -c "${TOMCAT_SCRIPT} version" >> ${TOMCAT_LOG} 2>&1 || RETVAL="4"
}

# See how we were called.
function start() {
  
   echo -n "Starting ${TOMCAT_PROG}: "
   if [ "$RETVAL" != "0" ]; then 
     log_failure_msg
     return
   fi
   if [ -f "/var/lock/subsys/${NAME}" ]; then
        if [ -f "${CATALINA_PID}" ]; then
            read kpid < ${CATALINA_PID}
#           if checkpid $kpid 2>&1; then
            if [ -d "/proc/${kpid}" ]; then
                log_success_msg
                if [ "$DISTRIB_ID" = "MandrivaLinux" ]; then
                    echo
                fi
                RETVAL="0"
                return
            fi
        fi
    fi
    # fix permissions on the log and pid files
    touch $CATALINA_PID 2>&1 || RETVAL="4"
    if [ "$RETVAL" -eq "0" -a "$?" -eq "0" ]; then 
      chown ${TOMCAT_USER}:${TOMCAT_USER} $CATALINA_PID
    fi
    parseOptions
    if [ "$RETVAL" -eq "0" -a "$SECURITY_MANAGER" = "true" ]; then
        $SU - $TOMCAT_USER -c "${TOMCAT_SCRIPT} start-security" \
            >> ${TOMCAT_LOG} 2>&1 || RETVAL="4"
    else
       
       [ "$RETVAL" -eq "0" ] && $SU - $TOMCAT_USER -c "${TOMCAT_SCRIPT} start" >> ${TOMCAT_LOG} 2>&1 || RETVAL="4"
    fi
    if [ "$RETVAL" -eq "0" ]; then 
        log_success_msg
        touch /var/lock/subsys/${NAME}
    else
        log_failure_msg "Error code ${RETVAL}"
    fi
    if [ "$DISTRIB_ID" = "MandrivaLinux" ]; then
        echo
    fi
}

function stop() {
    echo -n "Stopping ${TOMCAT_PROG}: "
    if [ -f "/var/lock/subsys/${NAME}" ]; then
      parseOptions
      if [ "$RETVAL" -eq "0" ]; then
         touch /var/lock/subsys/${NAME} 2>&1 || RETVAL="4"
         [ "$RETVAL" -eq "0" ] && $SU - $TOMCAT_USER -c "${TOMCAT_SCRIPT} stop" >> ${TOMCAT_LOG} 2>&1 || RETVAL="4"
      fi
      if [ "$RETVAL" -eq "0" ]; then
         count="0"
         if [ -f "${CATALINA_PID}" ]; then
            read kpid < ${CATALINA_PID}
            until [ "$(ps --pid $kpid | grep -c $kpid)" -eq "0" ] || \
                      [ "$count" -gt "$SHUTDOWN_WAIT" ]; do
                    if [ "$SHUTDOWN_VERBOSE" = "true" ]; then
                        echo "waiting for processes $kpid to exit"
                    fi
                    sleep 1
                    let count="${count}+1"
                done
                if [ "$count" -gt "$SHUTDOWN_WAIT" ]; then
                    if [ "$SHUTDOWN_VERBOSE" = "true" ]; then
                        log_warning_msg "killing processes which did not stop after ${SHUTDOWN_WAIT} seconds"
                    fi
                    kill -9 $kpid
                fi
                log_success_msg
            fi
            rm -f /var/lock/subsys/${NAME} ${CATALINA_PID}
        else
            log_failure_msg
            RETVAL="4"
        fi
    else
        log_success_msg
        RETVAL="0"
    fi
    if [ "$DISTRIB_ID" = "MandrivaLinux" ]; then
        echo
    fi
}

function status()
{
   checkpidfile 
   if [ "$RETVAL" -eq "0" ]; then
      log_success_msg "${NAME} (pid ${kpid}) is running..."
   elif [ "$RETVAL" -eq "1" ]; then
      log_failure_msg "PID file exists, but process is not running"
   else 
      checklockfile
      if [ "$RETVAL" -eq "2" ]; then
         log_failure_msg "${NAME} lockfile exists but process is not running"
      else
         pid="$(/usr/bin/pgrep -u ${TOMCAT_USER} -f tomcat)"
         if [ -z "$pid" ]; then
             log_success_msg "${NAME} is stopped"
             RETVAL="3"
         else
             log_success_msg "${NAME} (pid $pid) is running..."
             RETVAL="0"
         fi
      fi
  fi
}

function checklockfile()
{
   if [ -f /var/lock/subsys/${NAME} ]; then
      pid="$(/usr/bin/pgrep -u ${TOMCAT_USER} -f tomcat)"
# The lockfile exists but the process is not running
      if [ -z "$pid" ]; then
         RETVAL="2"
      fi
   fi
}

function checkpidfile()
{
   if [ -f "${CATALINA_PID}" ]; then
      read kpid < ${CATALINA_PID}
      if [ -d "/proc/${kpid}" ]; then
# The pid file exists and the process is running
          RETVAL="0"
      else
# The pid file exists but the process is not running
         RETVAL="1"
         return
      fi
   fi
# pid file does not exist and program is not running
   RETVAL="3"
}

function usage()
{
   echo "Usage: $0 {start|stop|restart|condrestart|try-restart|reload|force-reload|status|version}"
   RETVAL="2"
}

# See how we were called.
RETVAL="0"
case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        stop
        start
        ;;
    condrestart|try-restart)
        if [ -f "/var/run/${NAME}.pid" ]; then
            stop
            start
        fi
        ;;
    reload)
        RETVAL="3"
        ;;
    force-reload)
        if [ -f "/var/run/${NAME}.pid" ]; then
            stop
            start
        fi
        ;;
    status)
        status
        ;;
    version)
	 	version
#        ${TOMCAT_SCRIPT} version
        ;;
    *)
      usage
      ;;
esac

exit $RETVAL
