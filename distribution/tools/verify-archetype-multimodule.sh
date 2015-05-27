#!/bin/bash
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
# Debugging (uncomment as needed)
#
# Treat undefined variables as errors
#set -u
# Make any failed command in this script fail this script
#set -e
# Verbose output
#set -xv

M2_REPO=$1
THERMOSTAT_HOME=$2
KEEPTEMP=$3
ARTIFACT_ID="helloworld-multimodule"

source distribution/tools/verify-archetype-functions.sh
check_usage $@

WSS_PID="$TMP_DIR/web-storage-service.pid"
WSS_OUTPUT="$TMP_DIR/wss_output.txt"
EXAMPLE_CMD_OUTPUT="$TMP_DIR/example_cmd_output.txt"


function launch_and_wait_for_web_storage() {
  $THERMOSTAT_EXE -Tbg $WSS_PID web-storage-service > $WSS_OUTPUT 2>&1
  TRIES=0
  while [[ ! -f $WSS_OUTPUT || ! `grep -e "Agent id:" < $WSS_OUTPUT` ]]; do
    # don't wait forever
    if [ $TRIES -ge 20 ]; then
      return 1
    fi
    sleep 1
    ((TRIES++))
  done
  return 0
}

function kill_and_wait_for_webstorage() {
  if [ -f $WSS_PID ]; then
    PID="$(cat $WSS_PID)"
    kill "$PID"
    TRIES=0
    while ps -p $PID > /dev/null; do
      # don't wait forever
      if [ $TRIES -ge 15 ]; then
        return 1
      fi
      sleep 1
      ((TRIES++))
    done
  fi
  return 0
}

echo Using m2 repo: $M2_REPO
echo Using THERMOSTAT_HOME to which to install plugin to: $THERMOSTAT_HOME

# BEGIN TEST

# Use the archetype in order to verify it works
pushd $TMP_DIR
mvn archetype:generate -DarchetypeCatalog=local \
                       -Dmaven.repo.local="$M2_REPO" \
                       -B -DarchetypeGroupId=com.redhat.thermostat \
                          -DarchetypeArtifactId=thermostat-maven-archetype-multimodule \
                          -DbundleSymbolicName=com.example.thermostat.helloworld \
                          -Dpackage=com.example.thermostat \
                          -DgroupId=com.example.thermostat \
                          -DartifactId="$ARTIFACT_ID" \
                          -Dversion=0.0.1-SNAPSHOT \
                          -DmoduleName="Thermostat Hello World Extension Command" \
                          -DpluginDeployDir="$ARTIFACT_ID"
exit_if_bad_return_value $? "Could not generate plugin from archetype"

# Build plugin
pushd "$ARTIFACT_ID"
mvn -Dmaven.repo.local="$M2_REPO" package
exit_if_bad_return_value $? "Could not build plugin"

# deploy.sh cleans up previously deployed bits (if any)
# pass on THERMOSTAT_HOME which deploy.sh uses.
THERMOSTAT_HOME=$THERMOSTAT_HOME bash deploy.sh
exit_if_bad_return_value $? "Could not deploy plugin"

popd # ARTIFACT_ID
popd # TMP_DIR

# Make thermostat runnable. I.e. prepare a thermostat user home
# run devsetup
$THERMOSTAT_HOME/bin/thermostat-devsetup
exit_if_bad_return_value $? "Error configuring Thermostat (devsetup)"

launch_and_wait_for_web_storage
exit_if_bad_return_value $? "$WSS_OUTPUT" "Web Storage Service not coming up"

# Why head -n4? Output looks like the following:
#
#   Please enter username for storage at http://127.0.0.1:8999/thermostat/storage:client-tester
#   Please enter password for storage at http://127.0.0.1:8999/thermostat/storage:
#   HOST_ID                              HOST                  VM_ID                                VM_PID STATUS  VM_NAME
#   1cfd3933-3fb1-4090-a377-39a7b57d48a8 localhost.localdomain 339c274e-b385-42ca-81a9-dfda4cb4ca51 21985  EXITED  com.redhat.thermostat.main.Thermostat
# 
# So the first two lines is prompt, 1 line header, one line with an agent ID
# which we are actually interested in.
AGENT_ID="$(echo -e "client-tester\ntester" | $THERMOSTAT_EXE list-vms | head -n4 | tail -n1 | cut -d' ' -f1 )"

# verify "example-command" shows up in help
$THERMOSTAT_EXE help | grep example-command
exit_if_bad_return_value $? "Plugin command not appearing in Thermostat help"

# verify "example-command" works as expected
example_cmd="echo -e \"client-tester\ntester\" | $THERMOSTAT_EXE example-command -a $AGENT_ID | grep 'Message: Hello World!'"
echo $example_cmd >> $EXAMPLE_CMD_OUTPUT 2>&1
$example_cmd >> $EXAMPLE_CMD_OUTPUT 2>&1
exit_if_bad_return_value $? "$EXAMPLE_CMD_OUTPUT" "Plugin command not working"

kill_and_wait_for_webstorage

if [ $? -ne 0 ]; then
  echo "Warning: Problem shutting down Web Storage Service"
fi

cleanup

echo "Multi-module archetype OK."

exit 0

