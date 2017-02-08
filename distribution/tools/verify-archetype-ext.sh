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
KEEPTMP=$3
ARTIFACT_ID=helloworld

source distribution/tools/verify-archetype-functions.sh
check_usage $@

function install_plugin() {
  # install built hello-world plugin
  # put a filtered thermostat-plugin.xml in target
  mkdir $PLUGIN_INSTALL_LOCATION
  exit_if_bad_return_value $? "Plugin directory already exists."
  cp target/helloworld-0.0.1-SNAPSHOT.jar target/classes/thermostat-plugin.xml $PLUGIN_INSTALL_LOCATION
}

echo Using m2 repo: $M2_REPO
echo Using THERMOSTAT_HOME to which to install plugin to: $THERMOSTAT_HOME

# Use the archetype in order to verify it works
pushd $TMP_DIR
mvn archetype:generate -DarchetypeCatalog=local \
                       -Dmaven.repo.local="$M2_REPO" \
                       -B -DarchetypeGroupId=com.redhat.thermostat \
                          -DarchetypeArtifactId=thermostat-maven-archetype-ext \
                          -DbundleSymbolicName=com.example.thermostat.helloworld \
                          -Dpackage=com.example.thermostat \
                          -DgroupId=com.example.thermostat \
                          -DartifactId="$ARTIFACT_ID" \
                          -Dversion=0.0.1-SNAPSHOT \
                          -DmoduleName="Thermostat Hello World Extension Command"
exit_if_bad_return_value $? "Could not generate plugin from archetype"

# Build plugin
pushd ${ARTIFACT_ID}
mvn -Dmaven.repo.local="$M2_REPO" package
exit_if_bad_return_value $? "Could not build plugin"

install_plugin

popd # ARTIFACT_ID
popd # TMP_DIR

# Make thermostat runnable. I.e. prepare a thermostat user home
# run devsetup
$THERMOSTAT_HOME/bin/thermostat-devsetup
exit_if_bad_return_value $? "Error configuring Thermostat (devsetup)"

# verify "example-command" shows up in help
$THERMOSTAT_EXE help | grep example-command
exit_if_bad_return_value $? "Plugin command not appearing in Thermostat help"

# verify "example-command" works as expected
$THERMOSTAT_HOME/bin/thermostat example-command | grep "Hello World!"
exit_if_bad_return_value $? "Plugin command not working"

cleanup

echo "Simple archetype OK"

exit 0
