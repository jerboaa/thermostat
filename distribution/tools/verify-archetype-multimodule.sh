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
PLUGIN_DIR="foo-bar"
ARTIFACT_ID="helloworld-multimodule"

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
                          -DarchetypeArtifactId=thermostat-maven-archetype-multimodule \
                          -DbundleSymbolicName=com.example.thermostat.helloworld \
                          -Dpackage=com.example.thermostat \
                          -DgroupId=com.example.thermostat \
                          -DartifactId="$ARTIFACT_ID" \
                          -Dversion=0.0.1-SNAPSHOT \
                          -DmoduleName="Thermostat Hello World Extension Command" \
                          -DpluginDeployDir="$PLUGIN_DIR"

# Build plugin
pushd "$ARTIFACT_ID"
mvn -Dmaven.repo.local="$M2_REPO" package

# deploy.sh cleans up previously deployed bits (if any)
# pass on THERMOSTAT_HOME which deploy.sh uses.
THERMOSTAT_HOME=$THERMOSTAT_HOME bash deploy.sh
popd # ARTIFACT_ID dir

# Make thermostat runnable. I.e. prepare a thermostat user home
# run devsetup
USER_THERMOSTAT_HOME=$(mktemp -d)
export USER_THERMOSTAT_HOME
$THERMOSTAT_HOME/bin/thermostat-devsetup

# launch web-storage-service
WSS_OUTPUT="$(mktemp)"
$THERMOSTAT_HOME/bin/thermostat -Tbg $TMP_DIR/web-storage-service.pid web-storage-service > WSS_OUTPUT 2>&1 

# Wait for web-storage-service to come up
COUNT=0
while [[ ! -f WSS_OUTPUT || ! `grep -e "Agent id:" < WSS_OUTPUT` ]]; do 
	sleep 1
	if [ $COUNT -ge 10 ]; then
		break
	fi
	((COUNT++))
done

AGENT_ID="$(echo -e "client-tester\ntester" | $THERMOSTAT_HOME/bin/thermostat list-vms | grep "localhost" | head -1 | cut -d' ' -f1 )"
popd # TMP_DIR

# verify "example-command" shows up and works
retval=0
$THERMOSTAT_HOME/bin/thermostat help | grep example-command
retval=$(( $retval + $? ))
echo -e "client-tester\ntester" | $THERMOSTAT_HOME/bin/thermostat example-command -a $AGENT_ID | grep 'Message: Hello World!'
retval=$(( $retval + $? ))

# kill the process web-storage-service spawned earlier
MONGO_PROCESSES="$(echo $(ps aux | grep mongod | grep -v grep | wc -l))"
kill "$(cat $TMP_DIR/web-storage-service.pid)"

# ensure web-storage-service closes before cleanup 
COUNT=0
MONGO_EXPECTED=$((MONGO_PROCESSES-1))
while [[ $MONGO_PROCESSES -gt $MONGO_EXPECTED ]]; do 
	sleep 1
	MONGO_PROCESSES="$(echo $(ps aux | grep mongod | grep -v grep | wc -l))"
	((COUNT++))
	if [ $COUNT -ge 15 ]; then
		retval=$(($retval+1))
		break
	fi
done

if [ $retval -eq 0 ]; then
  rm -rf $TMP_DIR
  rm -rf $USER_THERMOSTAT_HOME
  # clean-up plugin artifacts from THERMOSTAT_HOME which we've used for
  # installation.
  rm -rf $THERMOSTAT_HOME/plugins/$PLUGIN_DIR
  rm -rf $THERMOSTAT_HOME/webapp/WEB-INF/lib/"$ARTIFACT_ID"-storage-common*.jar
else
  cat 1>&2 <<END
Something went wrong. You may find the following info useful.

Plug-in root should be: $TMP_DIR/$ARTIFACT_ID
Recompile with:
  $ mvn -Dmaven.repo.local="$M2_REPO" clean package

USER_THERMOSTAT_HOME was:     $USER_THERMOSTAT_HOME
THERMOSTAT_HOME was:          $THERMOSTAT_HOME
Plugin install directory was: $THERMOSTAT_HOME/plugins/$PLUGIN_DIR
END
fi

exit $retval
