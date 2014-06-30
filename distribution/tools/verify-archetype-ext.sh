#!/bin/bash
#
# Debugging (uncomment as needed)
#
# Treat undefined variables as errors
#set -u
# Make any failed command in this script fail this script
#set -e
# Verbose output
#set -xv

if [ $# -ne 2 ]; then
  echo "Usage: $0 LOCAL_MVN_REPO THERMOSTAT_HOME" 1>&2
  exit 1
fi

M2_REPO=$1
THERMOSTAT_HOME=$2

echo Using m2 repo: $M2_REPO
echo Using THERMOSTAT_HOME to which to install plugin to: $THERMOSTAT_HOME


find "$M2_REPO" -name '*thermostat*' -print0 | xargs -0 rm -rf;
mvn -Dmaven.test.skip=true -Dmaven.repo.local="$M2_REPO" clean install

# Use the archetype in order to verify it works
TMP_DIR=$(mktemp -d)
pushd $TMP_DIR
mvn archetype:generate -DarchetypeCatalog=local \
                       -Dmaven.repo.local="$M2_REPO" \
                       -B -DarchetypeGroupId=com.redhat.thermostat \
                          -DarchetypeArtifactId=thermostat-maven-archetype-ext \
                          -DbundleSymbolicName=com.example.thermostat.helloworld \
                          -Dpackage=com.example.thermostat \
                          -DgroupId=com.example.thermostat \
                          -DartifactId=helloworld \
                          -Dversion=0.0.1-SNAPSHOT \
                          -DmoduleName="Thermostat Hello World Extension Command"

# Build plugin
pushd helloworld
mvn -Dmaven.repo.local="$M2_REPO" package

# install built hello-world plugin
# put a filtered thermostat-plugin.xml in target
if [ -e $THERMOSTAT_HOME/plugins/helloworld ]; then
  rm -rf $THERMOSTAT_HOME/plugins/helloworld
fi
mkdir $THERMOSTAT_HOME/plugins/helloworld
cp target/helloworld-0.0.1-SNAPSHOT.jar target/classes/thermostat-plugin.xml $THERMOSTAT_HOME/plugins/helloworld
popd # plugin-build

popd

# Make thermostat runnable. I.e. prepare a thermostat user home
# run devsetup
USER_THERMOSTAT_HOME=$(mktemp -d)
export USER_THERMOSTAT_HOME
$THERMOSTAT_HOME/bin/thermostat-devsetup

# verify "example-command" shows up and works
retval=0
$THERMOSTAT_HOME/bin/thermostat help | grep example-command
retval=$(( $retval + $? ))
$THERMOSTAT_HOME/bin/thermostat example-command | grep "Hello World!"
retval=$(( $retval + $? ))

# Clean up if all went fine
if [ $retval -eq 0 ]; then
  rm -rf $TMP_DIR
  rm -rf $USER_THERMOSTAT_HOME
  rm -rf $THERMOSTAT_HOME/plugins/helloworld
else
  cat 1>&2 <<END
Something went wrong. You may find the following info useful.

Plug-in root should be: $TMP_DIR/helloworld
Recompile with:
  $ mvn -Dmaven.repo.local="$M2_REPO" clean package

USER_THERMOSTAT_HOME was:     $USER_THERMOSTAT_HOME
THERMOSTAT_HOME was:          $THERMOSTAT_HOME
Plugin install directory was: $THERMOSTAT_HOME/plugins/helloworld
END
fi

exit $retval
