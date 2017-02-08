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
