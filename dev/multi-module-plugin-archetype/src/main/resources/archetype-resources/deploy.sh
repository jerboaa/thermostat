#/bin/bash
if [ x"$THERMOSTAT_DEV_HOME" = x ] ; then
    echo "variable THERMOSTAT_DEV_HOME is not set, should point to the thermostat src directory"
    exit -1
fi

PLUGIN_DIR=$THERMOSTAT_DEV_HOME/distribution/target/image/plugins

TARGET_DIR="${pluginDeployDir}"

if [ -e $PLUGIN_DIR/$TARGET_DIR ]; then
    rm -rf $PLUGIN_DIR/$TARGET_DIR
fi

DISTRO_ZIP=$(pwd)/distribution/target/${artifactId}-distribution*.zip
pushd $PLUGIN_DIR
unzip $DISTRO_ZIP

WEBLIB_DIR=$THERMOSTAT_DEV_HOME/distribution/target/image/webapp/WEB-INF/lib
pushd $TARGET_DIR
cp *-storage-common-*.jar $WEBLIB_DIR
