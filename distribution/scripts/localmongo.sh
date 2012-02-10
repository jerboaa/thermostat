#!/bin/bash
#
# Copyright 2012 Red Hat, Inc.
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
#####################################################################
#
# Find the directory where thermostat is installed.
# Note this will not work if there are symlinks to resolve that
# are not full paths.
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
THERM_DIR=`dirname "$( cd -P "$( dirname "$SOURCE" )" && pwd )"`
# Some variables are sourced from properties file
. "${THERM_DIR}/config/agent.properties"
# Other relevant directory/file locations
USER_THERM_DIR="${HOME}/.thermostat"
MONGO_DIR="${USER_THERM_DIR}/mongod"
MONGO_PIDDIR="${MONGO_DIR}/run"
MONGO_PIDFILE="${MONGO_PIDDIR}/mongod.pid"
MONGO_PID=`cat ${MONGO_PIDFILE}`
MONGO_LOGDIR="${MONGO_DIR}/log"
MONGO_LOGFILE="${MONGO_LOGDIR}/mongod.log"
# FIXME noauth is not really appropriate.
MONGO_ARGS="--quiet --fork --nojournal --noauth --bind_ip 127.0.0.1 \
    --dbpath ${MONGO_DIR} --logpath ${MONGO_LOGFILE} \
    --pidfilepath ${MONGO_PIDFILE} --port ${mongod_port}"

function make_directories {
  mkdir -p "${MONGO_PIDDIR}"
  mkdir -p "${MONGO_LOGDIR}"
}

function do_start {
  if [ ! -z ${MONGO_PID} ]; then
    echo "Private mongo instance already running"
    exit -1
  fi
  make_directories
  mongod ${MONGO_ARGS}
}

function do_stop {
  if [ -z ${MONGO_PID} ]; then
    echo "Private mongo instance not running"
    exit -1
  fi
  MONGO_PID=`cat ${MONGO_PIDFILE}`
  kill ${MONGO_PID}
  rm -f ${MONGO_PIDFILE}
}

function usage {
  echo "Usage:"
  echo "  localmongo.sh <start|stop>"
}

if [ $# != 1 ]; then
  usage
  exit -1
fi

if [ $1 = "start" ]; then
  do_start
elif [ $1 = "stop" ]; then
  do_stop
else
  usage
  exit -1
fi

