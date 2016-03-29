#!/bin/bash

# Source thermostat-ipc-client-common
# Defines IPC_CLASSPATH variable with JARs necessary for the IPC service
. ${thermostat.home}/bin/thermostat-ipc-client-common

# Add test-client jar to IPC_CLASSPATH
TESTCLIENT="${project.parent.basedir}/test-client/target/thermostat-dev-ipc-test-client-${project.version}.jar"
IPC_CLASSPATH="${IPC_CLASSPATH}:${TESTCLIENT}"

exec ${JAVA} -cp "${IPC_CLASSPATH}" com.redhat.thermostat.dev.ipc.test.client.internal.UnixSocketTestClient "$1" "$2"
