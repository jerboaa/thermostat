/*
 * Copyright 2012, 2013 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.storage.mongodb.internal;

import java.io.IOException;
import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoURI;
import com.redhat.thermostat.storage.config.AuthenticationConfiguration;
import com.redhat.thermostat.storage.config.StartupConfiguration;
import com.redhat.thermostat.storage.core.Connection;
import com.redhat.thermostat.storage.core.ConnectionException;

class MongoConnection extends Connection {

    static final String THERMOSTAT_DB_NAME = "thermostat";

    private Mongo m = null;
    private DB db = null;
    private StartupConfiguration conf;

    MongoConnection(StartupConfiguration conf) {
        this.conf = conf;
    }

    @Override
    public void connect() {
        try {
            createConnection();
            authenticateIfNecessary();
            /* the mongo java driver does not ensure this connection is actually working */
            testConnection();
            connected = true;

        } catch (IOException | MongoException | IllegalArgumentException e) {
            e.printStackTrace();
            fireChanged(ConnectionStatus.FAILED_TO_CONNECT);
            throw new ConnectionException(e.getMessage(), e);
        }
        fireChanged(ConnectionStatus.CONNECTED);
    }

    private void authenticateIfNecessary() {
        if (conf instanceof AuthenticationConfiguration) {
            AuthenticationConfiguration authConf = (AuthenticationConfiguration) conf;
            String username = authConf.getUsername();
            if (username != null && ! username.equals("")) {
                authenticate(username, authConf.getPassword());
            }
        }
    }

    private void authenticate(String username, String password) {
        if (! db.authenticate(username, password.toCharArray())) {
            throw new MongoException("Invalid username/password");
        }
    }

    @Override
    public void disconnect() {
        connected = false;
        db = null;
        if (m != null) {
            m.close();
        }
    }

    public DB getDB() {
        return db;
    }

    private void createConnection() throws MongoException, UnknownHostException {
        this.m = new Mongo(getMongoURI());
        this.db = m.getDB(THERMOSTAT_DB_NAME);
    }

    private MongoURI getMongoURI() {
        String url = conf.getDBConnectionString();
        MongoURI uri = new MongoURI(url);
        return uri;
    }

    private void testConnection() {
        db.getCollection("agent-config").getCount();
    }
}

