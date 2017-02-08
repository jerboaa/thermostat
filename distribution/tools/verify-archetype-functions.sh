#
# Copyright 2012-2014 Red Hat, Inc.
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
# Common functionality for verifying both simple and multi-module archetypes

THERMOSTAT_EXE="$THERMOSTAT_HOME/bin/thermostat"
TMP_DIR=$(mktemp -d)
PLUGIN_INSTALL_LOCATION="$THERMOSTAT_HOME/plugins/$ARTIFACT_ID"
USER_THERMOSTAT_HOME="$TMP_DIR/userhome"
export USER_THERMOSTAT_HOME

function is_keeptmp_ok() {
  if [ "x$1" ==  "x" -o "x$1" == "x--keepTmp" ]; then
    return 0
  fi
  return 1
}

function check_usage() {
  is_keeptmp_ok $3
  if [ $? -ne 0 -o $# -lt 2 -o $# -gt 3 ]; then
    echo "Usage: $0 <LOCAL_MVN_REPO> <THERMOSTAT_HOME> [--keepTmp]" 1>&2
    exit 1
  fi
}

function delete_dir_recursively() {
  DIR_TO_CLEAN=$1
  if [ -e $DIR_TO_CLEAN ]; then
    rm -rf $DIR_TO_CLEAN
  else
    echo Warning: cannot delete directory that does not exist: $DIR_TO_CLEAN
  fi
}

function cleanup_tempdirs() {
  for DIR_TO_CLEAN in $@
  do
    delete_dir_recursively $DIR_TO_CLEAN
  done
}

function cleanup() {
  if [ "x$KEEPTEMP" != "x--keepTemp" ]; then
    cleanup_tempdirs $TMP_DIR \
          $PLUGIN_INSTALL_LOCATION \
          $THERMOSTAT_HOME/webapp/WEB-INF/lib/"$ARTIFACT_ID"-storage-common*.jar
  fi
}

function output_fail_information() {
  cat 1>&2 <<END
Something went wrong. You may find the following info useful.

Plug-in root should be: $TMP_DIR/$ARTIFACT_ID
Recompile with:
  $ mvn -Dmaven.repo.local="$M2_REPO" clean package

USER_THERMOSTAT_HOME was:     $USER_THERMOSTAT_HOME
THERMOSTAT_HOME was:          $THERMOSTAT_HOME
Plugin install directory was: $THERMOSTAT_HOME/plugins/$PLUGIN_DIR
END
}

function exit_if_bad_return_value() {
  local rval=""
  local debugFile=""
  local errorMsg=""

  # Check if we got passed a third argument which is a file with
  # details about the failure that happened.
  if [ $# -eq 3 ]; then
    rval=$1
    debugFile=$2
    errorMsg=$3
  else
    rval=$1
    debugFile=""
    errorMsg=$2
  fi

  if [ $rval -ne 0 ]; then
    # Only print debug info if we actually have one
    if [ "_$debugFile" != "_" ]; then
      echo "------------------- Debug info start ---------------------"
      cat "$debugFile"
      echo "-------------------  Debug info end  ---------------------"
    fi
    cleanup
    echo "$errorMsg"
    output_fail_information
    exit $rval
  fi
}
