#!/bin/bash
#set -x

if [ $# -ne 2 ]; then
  echo 1>&2 "usage: $0 <ip-of-storage-host> <path-to-thermostat-home>" 
  exit 1
fi

STORAGE_IP=$1

THERMOSTAT_HOME="$2"
THERMOSTAT_USER="t"
THERMOSTAT_PWD="t"
MONGO_ADMIN_USER="admin"
MONGO_ADMIN_PWD="admin"

$THERMOSTAT_HOME/bin/thermostat storage --start
cat > /tmp/thermostat_setup.txt <<EOF
db.addUser("$MONGO_ADMIN_USER", "$MONGO_ADMIN_PWD");
db.auth("$MONGO_ADMIN_USER", "$MONGO_ADMIN_PWD");
db = db.getMongo().getDB( "thermostat" );
db.addUser("$THERMOSTAT_USER", "$THERMOSTAT_PWD");
EOF

mongo localhost:27518/admin /tmp/thermostat_setup.txt
$THERMOSTAT_HOME/bin/thermostat storage --stop

cat > $THERMOSTAT_HOME/storage/db.properties <<EOF
PORT=27518
BIND=$STORAGE_IP
PROTOCOL=mongodb
EOF
$THERMOSTAT_HOME/bin/thermostat storage --start

exit 0
