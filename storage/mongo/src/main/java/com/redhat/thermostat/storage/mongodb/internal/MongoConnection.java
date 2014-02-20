/*
 * Copyright 2012-2014 Red Hat, Inc.
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
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.redhat.thermostat.common.ssl.SSLContextFactory;
import com.redhat.thermostat.common.ssl.SslInitException;
import com.redhat.thermostat.common.utils.HostPortPair;
import com.redhat.thermostat.common.utils.HostPortsParser;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.storage.core.Connection;
import com.redhat.thermostat.storage.core.ConnectionException;
import com.redhat.thermostat.storage.core.StorageCredentials;

class MongoConnection extends Connection {

    private static final Logger logger = LoggingUtils.getLogger(MongoConnection.class);
    static final String THERMOSTAT_DB_NAME = "thermostat";

    private MongoClient m = null;
    private DB db = null;
    private String url;
    StorageCredentials creds;
    SSLConfiguration sslConf;

    MongoConnection(String url, StorageCredentials creds, SSLConfiguration sslConf) {
        this.url = url;
        this.creds = creds;
        this.sslConf = sslConf;
    }

    @Override
    public void connect() {
        try {
            createConnection();
            authenticateIfNecessary();
            /* the mongo java driver does not ensure this connection is actually working */
            testConnection();
            connected = true;

        } catch (IOException | MongoException | InvalidConfigurationException | IllegalArgumentException e) {
            logger.log(Level.WARNING, "Failed to connect to storage", e);
            fireChanged(ConnectionStatus.FAILED_TO_CONNECT);
            throw new ConnectionException(e.getMessage(), e);
        }
        fireChanged(ConnectionStatus.CONNECTED);
    }

    private void authenticateIfNecessary() {
        String username = creds.getUsername();
        char[] password = creds.getPassword();
        try {
            if (username != null && password != null) {
                authenticate(username, password);
            }
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    private void authenticate(String username, char[] password) {
        if (! db.authenticate(username, password)) {
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
        fireChanged(ConnectionStatus.DISCONNECTED);
    }

    public DB getDB() {
        return db;
    }

    // package visibility for testing purposes.
    void createConnection() throws MongoException, InvalidConfigurationException, UnknownHostException {
        if (sslConf.enableForBackingStorage()) {
            logger.log(Level.FINE, "Using SSL socket for mongodb:// protocol");
            this.m = getSSLMongo();
        } else {
            logger.log(Level.FINE, "Using plain socket for mongodb://");
            this.m = new MongoClient(getServerAddress());
        }
        this.db = m.getDB(THERMOSTAT_DB_NAME);
    }

    MongoClient getSSLMongo() throws UnknownHostException, MongoException {
        Builder builder = new MongoClientOptions.Builder();
        SSLContext ctxt = null;
        try {
            ctxt = SSLContextFactory.getClientContext(sslConf);
        } catch (SslInitException e) {
            logger.log(Level.WARNING, "Failed to get SSL context!", e);
            throw new MongoException(e.getMessage(), e);
        }
        SSLParameters params = SSLContextFactory.getSSLParameters(ctxt);
        // Perform HTTPS compatible host name checking.
        if (!sslConf.disableHostnameVerification()) {
            params.setEndpointIdentificationAlgorithm("HTTPS");
        }
        SSLSocketFactory factory = SSLContextFactory.wrapSSLFactory(
                ctxt.getSocketFactory(), params);
        logger.log(Level.FINE, "factory is: " + factory.getClass().getName());
        builder.socketFactory(factory);
        MongoClientOptions opts = builder.build();
        MongoClient client = new MongoClient(getServerAddress(), opts);
        return client;
    }

    ServerAddress getServerAddress() throws InvalidConfigurationException, UnknownHostException {
        // Strip mongodb prefix: "mongodb://".length() == 10
        String hostPort = url.substring(10);
        HostPortsParser parser = new HostPortsParser(hostPort);
        parser.parse();
        HostPortPair ipPort = parser.getHostsPorts().get(0);
        ServerAddress addr = new ServerAddress(ipPort.getHost(), ipPort.getPort());
        return addr;
    }

    private void testConnection() {
        db.getCollection("agent-config").getCount();
    }
    
    // Testing hook
    MongoClient getMongo() {
        return this.m;
    }
}

