#!/bin/bash

# Source thermostat-ipc-client-common
# Defines CLASSPATH
. ${thermostat.home}/bin/thermostat-ipc-client-common

# Add test-client jar to CLASSPATH
TESTCLIENT="${project.parent.basedir}/test-client/target/thermostat-dev-ipc-test-client-${project.version}.jar"
CLASSPATH="${CLASSPATH}:${TESTCLIENT}"

exec ${JAVA} -cp "${CLASSPATH}" com.redhat.thermostat.dev.ipc.test.client.internal.UnixSocketTestClient "$1" "$2"
