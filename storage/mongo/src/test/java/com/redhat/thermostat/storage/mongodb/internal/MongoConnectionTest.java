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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.IOException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoOptions;
import com.mongodb.MongoURI;
import com.mongodb.ServerAddress;
import com.redhat.thermostat.common.ssl.SSLContextFactory;
import com.redhat.thermostat.common.utils.HostPortsParser;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.storage.config.StartupConfiguration;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;
import com.redhat.thermostat.storage.core.ConnectionException;

@RunWith(PowerMockRunner.class)
// There is a bug (resolved as wontfix) in powermock which results in
// java.lang.LinkageError if javax.management.* classes aren't ignored by
// Powermock. More here: http://code.google.com/p/powermock/issues/detail?id=277
// SSL tests need this and having that annotation on method level doesn't seem
// to solve the issue.
@PowerMockIgnore( {"javax.management.*"})
public class MongoConnectionTest {

    private MongoConnection conn;
    private ConnectionListener listener;
    private SSLConfiguration mockSSLConf;

    @Before
    public void setUp() {
        StartupConfiguration conf = mock(StartupConfiguration.class);
        mockSSLConf = mock(SSLConfiguration.class);
        when(mockSSLConf.enableForBackingStorage()).thenReturn(false);
        when(conf.getDBConnectionString()).thenReturn("mongodb://127.0.0.1:27518");
        
        conn = new MongoConnection(conf, mockSSLConf);
        listener = mock(ConnectionListener.class);
        conn.addListener(listener);
    }

    @After
    public void tearDown() {
        conn = null;
    }

    @PrepareForTest({ MongoConnection.class })
    @Test
    public void testConnectSuccess() throws Exception {
        DBCollection collection = mock(DBCollection.class);
        DB db = mock(DB.class);
        when(db.getCollection("agent-config")).thenReturn(collection);
        Mongo m = mock(Mongo.class);
        when(m.getDB(MongoConnection.THERMOSTAT_DB_NAME)).thenReturn(db);
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        conn.connect();

        verify(listener).changed(ConnectionStatus.CONNECTED);
    }
    
    @PrepareForTest({ MongoConnection.class })
    @Test
    public void testDisconnect() throws Exception {
        DBCollection collection = mock(DBCollection.class);
        DB db = mock(DB.class);
        when(db.getCollection("agent-config")).thenReturn(collection);
        Mongo m = mock(Mongo.class);
        when(m.getDB(MongoConnection.THERMOSTAT_DB_NAME)).thenReturn(db);
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        conn.connect();

        verify(listener).changed(ConnectionStatus.CONNECTED);
        
        conn.disconnect();
        verify(m).close();
        verify(listener).changed(ConnectionStatus.DISCONNECTED);
    }

    @PrepareForTest({ MongoConnection.class })
    @Test
    public void testConnectIOException() throws Exception {
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenThrow(new IOException());
        boolean exceptionThrown = false;
        try {
            conn.connect();
        } catch (ConnectionException ex) {
            exceptionThrown = true;
        }
        verify(listener).changed(ConnectionStatus.FAILED_TO_CONNECT);
        assertTrue(exceptionThrown);
    }

    @PrepareForTest({ MongoConnection.class })
    @Test
    public void testConnectMongoException() throws Exception {
        PowerMockito.whenNew(Mongo.class).withParameterTypes(ServerAddress.class).withArguments(any(ServerAddress.class)).thenThrow(new MongoException("fluff"));
        boolean exceptionThrown = false;
        try {
            conn.connect();
        } catch (ConnectionException ex) {
            exceptionThrown = true;
        }

        verify(listener).changed(ConnectionStatus.FAILED_TO_CONNECT);
        assertTrue(exceptionThrown);
    }
    
    @PrepareForTest({ MongoConnection.class, HostPortsParser.class })
    @Test
    public void testConnectInvalidConfigurationexception() throws Exception {
        HostPortsParser failingParser = mock(HostPortsParser.class);
        doThrow(new InvalidConfigurationException("let's pretend the configuration is invalid"))
                .when(failingParser).parse();

        PowerMockito
            .whenNew(HostPortsParser.class)
            .withParameterTypes(String.class)
            .withArguments(anyString())
            .thenReturn(failingParser);

        boolean exceptionThrown = false;
        try {
            conn.connect();
        } catch (ConnectionException ex) {
            exceptionThrown = true;
        }

        verify(listener).changed(ConnectionStatus.FAILED_TO_CONNECT);
        assertTrue(exceptionThrown);
    }

    @PrepareForTest({ MongoConnection.class,
        SSLContextFactory.class, SSLContext.class, SSLSocketFactory.class })
    @Test
    public void verifySSLSocketFactoryUsedIfSSLEnabled() throws Exception {
        when(mockSSLConf.enableForBackingStorage()).thenReturn(true);
        
        PowerMockito.mockStatic(SSLContextFactory.class);
        // SSL classes need to be mocked with PowerMockito
        SSLContext context = PowerMockito.mock(SSLContext.class);
        when(SSLContextFactory.getClientContext(isA(SSLConfiguration.class))).thenReturn(context);
        SSLSocketFactory factory = PowerMockito.mock(SSLSocketFactory.class);
        when(SSLContextFactory.wrapSSLFactory(any(SSLSocketFactory.class), any(SSLParameters.class))).thenReturn(factory);
        SSLParameters params = mock(SSLParameters.class);
        when(SSLContextFactory.getSSLParameters(context)).thenReturn(params);
        Mongo mockMongo = mock(Mongo.class);
        ArgumentCaptor<MongoOptions> mongoOptCaptor = ArgumentCaptor.forClass(MongoOptions.class);
        whenNew(Mongo.class).withParameterTypes(ServerAddress.class,
                MongoOptions.class).withArguments(any(ServerAddress.class),
                mongoOptCaptor.capture()).thenReturn(mockMongo);
        DB mockDb = mock(DB.class);
        when(mockMongo.getDB(eq(MongoConnection.THERMOSTAT_DB_NAME))).thenReturn(mockDb);
        DBCollection mockCollection = mock(DBCollection.class);
        when(mockDb.getCollection(any(String.class))).thenReturn(mockCollection);
        conn.connect();
        verify(params).setEndpointIdentificationAlgorithm("HTTPS");
        Mongo mongo = conn.getMongo();
        assertEquals(mockMongo, mongo);
        MongoOptions opts = mongoOptCaptor.getValue();
        assertTrue(opts.socketFactory instanceof SSLSocketFactory);
        assertEquals(factory, opts.socketFactory);
    }
    
    @PrepareForTest({ MongoConnection.class, SSLContextFactory.class, SSLContext.class, SSLSocketFactory.class })
    @Test
    public void verifyNoSSLSocketFactoryUsedIfSSLDisabled() throws Exception {
        DBCollection collection = mock(DBCollection.class);
        DB db = mock(DB.class);
        when(db.getCollection("agent-config")).thenReturn(collection);
        Mongo mockMongo = mock(Mongo.class);
        when(mockMongo.getDB(MongoConnection.THERMOSTAT_DB_NAME)).thenReturn(db);
        PowerMockito.whenNew(Mongo.class).withParameterTypes(ServerAddress.class)
                .withArguments(any(ServerAddress.class)).thenReturn(mockMongo);
        MongoConnection connection = mock(MongoConnection.class);
        doCallRealMethod().when(connection).connect();
        doCallRealMethod().when(connection).createConnection();
        connection.sslConf = mock(SSLConfiguration.class);
        connection.connect();
        verify(connection, Mockito.times(0)).getSSLMongo();
    }
    
    @Test
    public void canGetServerAddress() {
        StartupConfiguration config = new StartupConfiguration() {
            
            @Override
            public String getDBConnectionString() {
                return "mongodb://127.0.1.1:23452";
            }
        };
        SSLConfiguration sslConf = mock(SSLConfiguration.class);
        MongoConnection connection = new MongoConnection(config, sslConf);
        ServerAddress addr = null;
        try {
            addr = connection.getServerAddress();
        } catch (UnknownHostException e) {
            fail("Should not have thrown exception!");
        }
        assertEquals(23452, addr.getPort());
        assertEquals("127.0.1.1", addr.getHost());
        
        config = new StartupConfiguration() {
            
            @Override
            public String getDBConnectionString() {
                return "fluff://willnotwork.com:23452";
            }
        };
        connection = new MongoConnection(config, sslConf);
        try {
            connection.getServerAddress();
            fail("should not have been able to parse address");
        } catch (UnknownHostException e) {
            // pass
        }
    }
    
}

