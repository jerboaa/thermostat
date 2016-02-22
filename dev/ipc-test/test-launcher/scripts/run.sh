#!/bin/bash

# Source thermostat-common to setup environment
. ${thermostat.home}/bin/thermostat-common

CLASSPATH=".:${thermostat.home}/libs/org.apache.felix.framework-${felix.framework.version}.jar"
CLASSPATH="${CLASSPATH}:${thermostat.home}/libs/jffi-${jffi.version}-native.jar"
CLASSPATH="${CLASSPATH}:${project.parent.basedir}/test-server/target/thermostat-dev-ipc-test-server-${project.version}.jar"
exec ${JAVA} -cp "${CLASSPATH}" com.redhat.thermostat.dev.ipc.test.launcher.internal.TestLauncher
