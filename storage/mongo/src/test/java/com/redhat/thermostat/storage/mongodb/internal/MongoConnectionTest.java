/*
 * Copyright 2012-2015 Red Hat, Inc.
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import javax.net.SocketFactory;
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
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.redhat.thermostat.common.ssl.SSLContextFactory;
import com.redhat.thermostat.common.utils.HostPortsParser;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.storage.core.Connection;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;
import com.redhat.thermostat.storage.core.ConnectionException;
import com.redhat.thermostat.storage.core.StorageCredentials;

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
        mockSSLConf = mock(SSLConfiguration.class);
        when(mockSSLConf.enableForBackingStorage()).thenReturn(false);

        StorageCredentials creds = mock(StorageCredentials.class);
        when(creds.getUsername()).thenReturn("foo-user");
        when(creds.getPassword()).thenReturn("foo-password".toCharArray());

        conn = new MongoConnection("mongodb://127.0.0.1:27518", creds, mockSSLConf);
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
        setupDatabaseMocks(mock(DB.class), mock(MongoClient.class));

        conn.connect();

        verify(listener).changed(ConnectionStatus.CONNECTED);
    }
    
    @PrepareForTest({ MongoConnection.class })
    @Test
    public void testDisconnect() throws Exception {
        MongoClient m = mock(MongoClient.class);
        setupDatabaseMocks(mock(DB.class), m);
        conn.connect();

        verify(listener).changed(ConnectionStatus.CONNECTED);
        
        conn.disconnect();
        verify(m).close();
        verify(listener).changed(ConnectionStatus.DISCONNECTED);
    }

    @PrepareForTest({ MongoConnection.class })
    @Test
    public void testConnectIOException() throws Exception {
        IOException fakeException = new IOException();
        PowerMockito.whenNew(MongoClient.class).withParameterTypes(ServerAddress.class, List.class).withArguments(any(ServerAddress.class), any(List.class)).thenThrow(fakeException);
        boolean exceptionThrown = false;
        try {
            conn.connect();
            fail("Should have thrown IOException");
        } catch (ConnectionException ex) {
            exceptionThrown = true;
            assertTrue("Expected fake exception to be thrown. Nothing else.",
                    ex.getCause() == fakeException);
        }
        verify(listener).changed(ConnectionStatus.FAILED_TO_CONNECT);
        assertTrue(exceptionThrown);
    }

    @PrepareForTest({ MongoConnection.class })
    @Test
    public void testConnectMongoException() throws Exception {
        MongoException fakeException = new MongoException("fluff");
        PowerMockito.whenNew(MongoClient.class).withParameterTypes(ServerAddress.class, List.class).withArguments(any(ServerAddress.class), any(List.class)).thenThrow(fakeException);
        boolean exceptionThrown = false;
        try {
            conn.connect();
            fail("Should have thrown MongoException");
        } catch (ConnectionException ex) {
            exceptionThrown = true;
            assertTrue("Expected fake exception to be thrown. Nothing else.",
                    ex.getCause() == fakeException);
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
        MongoClient mockMongo = mock(MongoClient.class);
        ArgumentCaptor<MongoClientOptions> mongoOptCaptor = ArgumentCaptor.forClass(MongoClientOptions.class);
        whenNew(MongoClient.class).withParameterTypes(ServerAddress.class,
                List.class, MongoClientOptions.class).withArguments(any(ServerAddress.class),
                any(List.class), mongoOptCaptor.capture()).thenReturn(mockMongo);
        DB mockDb = mock(DB.class);
        when(mockMongo.getDB(eq(MongoConnection.THERMOSTAT_DB_NAME))).thenReturn(mockDb);
        DBCollection mockCollection = mock(DBCollection.class);
        when(mockDb.getCollection(any(String.class))).thenReturn(mockCollection);
        conn.connect();
        verify(params).setEndpointIdentificationAlgorithm("HTTPS");
        MongoClient mongo = conn.getMongo();
        assertEquals(mockMongo, mongo);
        MongoClientOptions opts = mongoOptCaptor.getValue();
        SocketFactory sockFactory = opts.getSocketFactory();
        assertTrue(sockFactory instanceof SSLSocketFactory);
        assertEquals(factory, sockFactory);
    }
    
    @PrepareForTest({ MongoConnection.class, SSLContextFactory.class, SSLContext.class, SSLSocketFactory.class })
    @Test
    public void verifyNoSSLSocketFactoryUsedIfSSLDisabled() throws Exception {
        setupDatabaseMocks(mock(DB.class), mock(MongoClient.class));

        MongoConnection connection = mock(MongoConnection.class);
        doCallRealMethod().when(connection).connect();
        doCallRealMethod().when(connection).createConnection();
        connection.sslConf = mock(SSLConfiguration.class);
        StorageCredentials c = mock(StorageCredentials.class);
        when(c.getUsername()).thenReturn("foo-user");
        when(c.getPassword()).thenReturn("foo-bar".toCharArray());
        connection.creds = c;
        connection.connect();
        verify(connection, Mockito.times(0)).getSSLMongo(any(String.class), any(char[].class));
    }
    
    @Test
    public void canGetServerAddress() {
        SSLConfiguration sslConf = mock(SSLConfiguration.class);
        StorageCredentials creds = mock(StorageCredentials.class);
        MongoConnection connection = new MongoConnection("mongodb://127.0.1.1:23452", creds, sslConf);
        ServerAddress addr = null;
        try {
            addr = connection.getServerAddress();
        } catch (UnknownHostException e) {
            fail("Should not have thrown exception!");
        }
        assertEquals(23452, addr.getPort());
        assertEquals("127.0.1.1", addr.getHost());
    }

    @PrepareForTest({ MongoConnection.class })
    @Test
    public void testConnectedUsernameIsSet() throws Exception {
        String expected = "username";

        DB db = mock(DB.class);
        setupDatabaseMocks(db, mock(MongoClient.class));

        StorageCredentials creds = mock(StorageCredentials.class);
        when(creds.getUsername()).thenReturn(expected);
        when(creds.getPassword()).thenReturn(new char[] { 'p' });

        conn = new MongoConnection("mongodb://127.0.0.1:27518", creds, mockSSLConf);
        conn.connect();

        assertEquals(expected, conn.getUsername());
    }

    @PrepareForTest({ MongoConnection.class })
    @Test
    public void testFailedConnectUsernameIsUnset() throws Exception {
        String expected = Connection.UNSET_USERNAME;
        boolean expect = false;

        DB db = mock(DB.class);
        setupDatabaseMocks(db, mock(MongoClient.class));

        StorageCredentials creds = mock(StorageCredentials.class);
        // null username is not permitted, provoking connection exception.
        when(creds.getUsername()).thenReturn(null);

        conn = new MongoConnection("mongodb://127.0.0.1:27518", creds, mockSSLConf);
        try {
            conn.connect();
        } catch (ConnectionException e) {
            //Expected a ConnectionException
            expect = true;
        }
        assertEquals(expected, conn.getUsername());
        assertTrue(expect);
    }

    @Test
    public void testDisconnectedUsernameIsUnset() throws Exception {
        String expected = Connection.UNSET_USERNAME;

        conn = new MongoConnection("mongodb://127.0.0.1:27518", mock(StorageCredentials.class), mockSSLConf);
        conn.disconnect();

        assertEquals(expected, conn.getUsername());
    }

    private void setupDatabaseMocks(DB db, MongoClient m) throws Exception {
        DBCollection collection = mock(DBCollection.class);
        when(db.getCollection("agent-config")).thenReturn(collection);

        when(m.getDB(MongoConnection.THERMOSTAT_DB_NAME)).thenReturn(db);
        PowerMockito.whenNew(MongoClient.class).withParameterTypes(ServerAddress.class, List.class).withArguments(any(ServerAddress.class), any(List.class)).thenReturn(m);
    }
}

