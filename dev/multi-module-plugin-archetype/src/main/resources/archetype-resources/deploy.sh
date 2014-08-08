#/bin/bash
if [ x"$THERMOSTAT_HOME" = x ] ; then
    echo "Variable THERMOSTAT_HOME is not set. It should point to the installation of thermostat (e.g. THERMOSTAT_SOURCE/distribution/target/image if built from source)"
    exit -1
fi

PLUGIN_DIR=$THERMOSTAT_HOME/plugins

TARGET_DIR="${pluginDeployDir}"

WEBLIB_DIR=$THERMOSTAT_HOME/webapp/WEB-INF/lib

# Clean up artifacts from previous runs
if [ -e $PLUGIN_DIR/$TARGET_DIR ]; then
    rm -rf $PLUGIN_DIR/$TARGET_DIR
fi
pushd $WEBLIB_DIR
if [ -e ${artifactId}-storage-common-*.jar ]; then
    rm -rf ${artifactId}-storage-common-*.jar
fi
popd #WEBLIB_DIR

DISTRO_ZIP=$(pwd)/distribution/target/${artifactId}-distribution*.zip
pushd $PLUGIN_DIR
unzip $DISTRO_ZIP
popd # PLUGIN_DIR

# Deploy relevant add-ons for the web-app
cp $(pwd)/storage-common/target/${artifactId}-storage-common-*.jar $WEBLIB_DIR
