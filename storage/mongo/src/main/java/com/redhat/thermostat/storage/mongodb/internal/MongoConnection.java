/*
 * Copyright 2012-2016 Red Hat, Inc.
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
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

    private final String url;
    private final StorageCredentials creds;
    private final SSLConfiguration sslConf;
    private final MongoClientOptions.Builder builder;
    private MongoClient m = null;
    private MongoDatabase db = null;

    MongoConnection(String url, StorageCredentials creds, SSLConfiguration sslConf) {
        this(url, creds, sslConf, new MongoClientOptions.Builder());
    }
    
    MongoConnection(String url, StorageCredentials creds, SSLConfiguration sslConf, MongoClientOptions.Builder builder) {
        this.url = url;
        this.creds = creds;
        this.sslConf = sslConf;
        this.builder = builder;
    }

    @Override
    public void connect() {
        try {
            createConnection();
            /* the mongo java driver does not ensure this connection is actually working */
            testConnection();
            connected = true;

        } catch (IOException | MongoException | InvalidConfigurationException | IllegalArgumentException e) {
            logger.log(Level.WARNING, "Failed to connect to storage", e);
            setUsername(Connection.UNSET_USERNAME);
            fireChanged(ConnectionStatus.FAILED_TO_CONNECT);
            throw new ConnectionException(e.getMessage(), e);
        }
        fireChanged(ConnectionStatus.CONNECTED);
    }

    @Override
    public void disconnect() {
        connected = false;
        db = null;
        if (m != null) {
            m.close();
        }
        setUsername(Connection.UNSET_USERNAME);
        fireChanged(ConnectionStatus.DISCONNECTED);
    }

    MongoDatabase getDatabase() {
        return db;
    }

    // package visibility for testing purposes.
    void createConnection() throws MongoException, InvalidConfigurationException, UnknownHostException {
        String username = creds.getUsername();
        setUsername(username);
        char[] password = creds.getPassword();
        try {
            this.m = createMongoClient(username, password, builder);
            this.db = m.getDatabase(THERMOSTAT_DB_NAME);
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    MongoClient createMongoClient(String username, char[] password, Builder builder) throws UnknownHostException, MongoException {
        if (sslConf.enableForBackingStorage()) {
            logger.log(Level.FINE, "Using SSL socket for mongodb:// protocol");
            SSLSocketFactory factory = createSSLSocketFactory();
            builder.socketFactory(factory);
        } else {
            logger.log(Level.FINE, "Using plain socket for mongodb://");
        }
        MongoCredential creds = MongoCredential.createCredential(username, THERMOSTAT_DB_NAME, password);
        CodecRegistry defaultCodecRegistry = MongoClient.getDefaultCodecRegistry();
        CodecRegistry arrayCodecRegistry = CodecRegistries.fromCodecs(
                                                new DoubleArrayCodec(),
                                                new IntegerArrayCodec(),
                                                new StringArrayCodec());
        CodecRegistry ourCodecRegistry = CodecRegistries.fromRegistries(
                                                defaultCodecRegistry,
                                                arrayCodecRegistry);
        builder.codecRegistry(ourCodecRegistry);
        MongoClientOptions opts = builder.build();
        return getMongoClientInstance(getServerAddress(), Arrays.asList(creds), opts);
    }
    
    MongoClient getMongoClientInstance(ServerAddress address, List<MongoCredential> creds, MongoClientOptions opts) {
        return new MongoClient(address, creds, opts);
    }

    private SSLSocketFactory createSSLSocketFactory() {
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
        return factory;
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
        db.getCollection("agent-config").count();
    }
    
    // Testing hook
    MongoClient getMongo() {
        return this.m;
    }
}

