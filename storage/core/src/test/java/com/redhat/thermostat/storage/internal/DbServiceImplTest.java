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

package com.redhat.thermostat.storage.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.storage.core.Connection;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;
import com.redhat.thermostat.storage.core.ConnectionException;
import com.redhat.thermostat.storage.core.DbService;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.storage.core.StorageProvider;
import com.redhat.thermostat.testutils.StubBundleContext;

public class DbServiceImplTest {
    
    // Stub connection which is always successful and
    // firesChanged immediately.
    private ImmediateConnection connection = new ImmediateConnection();
    private StorageProvider storageProvider;
    private Storage storage;
    private StubBundleContext context;
    
    @Before
    public void setup() {
        context = new StubBundleContext();

        storage = mock(Storage.class);
        when(storage.getConnection()).thenReturn(connection);

        storageProvider = mock(StorageProvider.class);
        when(storageProvider.canHandleProtocol()).thenReturn(true);
        when(storageProvider.createStorage()).thenReturn(storage);
        context.registerService(StorageProvider.class, storageProvider, null);
    }

    @Test
    public void testNoStorageProvider() {
        context = new StubBundleContext();

        try {
            new DbServiceImpl(context, "ignore", "ignore", "http://ignored.example.com");
            fail("exception expected");
        } catch (StorageException se) {
            assertEquals("No storage provider available", se.getMessage());
        }
    }

    @Test
    public void testNoStorageProviderCanHandleStorageUrl() {
        when(storageProvider.canHandleProtocol()).thenReturn(false);

        try {
            new DbServiceImpl(context, "ignore", "ignore", "http://ignored.example.com");
            fail("exception expected");
        } catch (StorageException se) {
            assertEquals("No storage found for URL http://ignored.example.com", se.getMessage());
        }
    }

    @Test
    public void testConnect() {
        DbService dbService = new DbServiceImpl(context, storage);
        try {
            dbService.connect();
            // pass
        } catch (ConnectionException e) {
            fail();
        }
        assertTrue(connection.connectCalled);
    }
    
    @Test
    public void connectEnsuresPostConditionOnDelayedException() {
        Storage mockStorage = mock(Storage.class);
        DelayedConnection connection = new DelayedConnection();
        when(mockStorage.getConnection()).thenReturn(connection);
        DbService dbService = new DbServiceImpl(context, mockStorage);
        
        try {
            dbService.connect();
            fail("Should have thrown ConnectionException!");
        } catch (ConnectionException e) {
            // pass
        }
        assertTrue(connection.connectCalled);
        assertTrue(connection.delayAwaited);
        assertFalse(context.isServiceRegistered(Storage.class.getName(), mockStorage.getClass()));
        assertFalse(context.isServiceRegistered(DbService.class.getName(), DbServiceImpl.class));
    }
    
    @Test
    public void disconnectAwaitsConnectionDisconnect() {
        Storage mockStorage = mock(Storage.class);
        DelayedConnection connection = new DelayedConnection() {
            @Override
            public void connect() {
                fireChanged(ConnectionStatus.CONNECTED);
                connectCalled = true;
            }
        };
        when(mockStorage.getConnection()).thenReturn(connection);
        DbService dbService = new DbServiceImpl(context, mockStorage);
        
        try {
            dbService.connect();
            // pass
        } catch (ConnectionException e) {
            fail();
        }
        assertTrue(connection.connectCalled);
        connection.connectCalled = false;
        assertFalse(connection.delayAwaited);
        assertFalse(connection.disconnectCalled);
        assertNotNull(context.getServiceReference(DbService.class));
        assertNotNull(context.getServiceReference(Storage.class));
        
        try {
            dbService.disconnect();
        } catch (ConnectionException e) {
            fail();
        }
        assertTrue(connection.disconnectCalled);
        assertFalse(connection.connectCalled);
        assertTrue(connection.delayAwaited);
        assertNull(context.getServiceReference(DbService.class));
        assertNull(context.getServiceReference(Storage.class));
    }

    @Test
    public void testConnectRegistersDbService() {
        DbService dbService = new DbServiceImpl(context, "ignore", "ignore", "http://ignored.example.com");

        try {
            dbService.connect();
            // pass
        } catch (ConnectionException e) {
            fail();
        }
        assertTrue(connection.connectCalled);
        @SuppressWarnings("rawtypes")
        ServiceReference dbServiceRef = context.getServiceReference(DbService.class);
        // connect registers DbService
        assertNotNull(dbServiceRef);
        // make sure we really get the same instance
        assertTrue(dbService.equals(context.getService(dbServiceRef)));
    }
    
    @Test
    public void testConnectRegistersStorage() {
        DbService dbService = new DbServiceImpl(context, "ignore", "ignore", "http://ignored.example.com");

        try {
            dbService.connect();
            // pass
        } catch (ConnectionException e) {
            fail();
        }
        assertTrue(connection.connectCalled);
        @SuppressWarnings("rawtypes")
        ServiceReference storageRef = context.getServiceReference(Storage.class);
        // connect registers DbService
        assertNotNull(storageRef);
        // make sure we really get the same instance
        assertTrue(storage.equals(context.getService(storageRef)));
    }
    
    @SuppressWarnings("rawtypes")
    @Test
    public void testConnectEnforcesPreCond() {
        DbService dbService = new DbServiceImpl(context, "ignore", "ignore", "http://ignored.example.com");

        ServiceRegistration reg = context.registerService(DbService.class, dbService, null);
        try {
            dbService.connect();
            fail("connect should check if db service is already registered");
        } catch (IllegalStateException e) {
            // pass
            reg.unregister();
        }
        reg = context.registerService(Storage.class, storage, null);
        try {
            dbService.connect();
            fail("connect should check if storage service is already registered");
        } catch (IllegalStateException e) {
            // pass
            reg.unregister();
        }
    }
    
    @SuppressWarnings("rawtypes")
    @Test
    public void testDisConnectEnforcesPreCond() {
        DbService dbService = new DbServiceImpl(context, "ignore", "ignore", "http://ignored.example.com");

        ServiceRegistration reg = context.registerService(DbService.class, dbService, null);
        try {
            // Storage == null
            dbService.disconnect();
            fail("disconnect should check if storage service is already registered");
        } catch (IllegalStateException e) {
            // pass
            reg.unregister();
        }
        reg = context.registerService(Storage.class, storage, null);
        try {
            // DbService == null
            dbService.disconnect();
            fail("disconnect should check if db service is already registered");
        } catch (IllegalStateException e) {
            // pass
            reg.unregister();
        }
    }

    @Test
    public void testDisconnect() {
        DbService dbService = new DbServiceImpl(context, "ignore", "ignore", "http://ignored.example.com");

        try {
            dbService.connect();
            // pass
        } catch (ConnectionException e) {
            fail();
        }
        assertTrue(connection.connectCalled);
        assertNotNull(context.getServiceReference(DbService.class));
        
        dbService.disconnect();

        assertTrue(connection.disconnectCalled);
    }

    @Test
    public void testDisconnectUnregistersDbService() {
        DbService dbService = new DbServiceImpl(context, "ignore", "ignore", "http://ignored.example.com");

        try {
            dbService.connect();
            // pass
        } catch (ConnectionException e) {
            fail();
        }
        assertTrue(connection.connectCalled);
        assertNotNull(context.getServiceReference(DbService.class));
        
        dbService.disconnect();

        assertTrue(connection.disconnectCalled);
        // disconnect unregisters DbService
        assertNull(context.getServiceReference(DbService.class));
    }
    
    @Test
    public void testDisconnectUnregistersStorage() {
        DbService dbService = new DbServiceImpl(context, "ignore", "ignore", "http://ignored.example.com");

        try {
            dbService.connect();
            // pass
        } catch (ConnectionException e) {
            fail();
        }
        assertTrue(connection.connectCalled);
        assertNotNull(context.getServiceReference(Storage.class));
        
        dbService.disconnect();

        assertTrue(connection.disconnectCalled);
        
        // disconnect unregisters Storage
        assertNull(context.getServiceReference(Storage.class));
    }
    
    @Test
    public void canGetStorageUrl() {
        String connectionURL = "http://test.example.com:8082";

        DbService dbService = new DbServiceImpl(context, "ignore", "ignore", connectionURL);
        assertEquals(connectionURL, dbService.getConnectionUrl());
    }
    
    @Test
    public void testAddListener() {
        ConnectionListener listener = mock(ConnectionListener.class);
        Connection connection = mock(Connection.class);
        when(storage.getConnection()).thenReturn(connection);
        DbService dbService = new DbServiceImpl(context, "ignore", "ignore", "http://ignored.example.com");

        dbService.addConnectionListener(listener);
        verify(connection).addListener(listener);
    }
    
    @Test
    public void testListenerGetsEvent() {
        ConnectingConnectionListener listener = new ConnectingConnectionListener();
        DbService dbService = new DbServiceImpl(context, "ignore", "ignore", "http://ignored.example.com");

        ConnectingConnection connection = new ConnectingConnection();
        when(storage.getConnection()).thenReturn(connection);
        dbService.addConnectionListener(listener);
        assertFalse(listener.eventReceived);
        try {
            dbService.connect();
        } catch (ConnectionException e) {
            fail();
        }
        assertTrue(connection.connectCalled);
        assertTrue(listener.eventReceived);
        listener.eventReceived = false;
        dbService.removeConnectionListener(listener);
        try {
            dbService.disconnect();
            dbService.connect();
        } catch (ConnectionException e) {
            fail();
        }
        assertFalse(listener.eventReceived);
    }
    
    @Test
    public void testRemoveListener() {
        // Remove called regardless of listener actually being added
        ConnectionListener listener = mock(ConnectionListener.class);
        Connection connection = mock(Connection.class);
        when(storage.getConnection()).thenReturn(connection);
        DbService dbService = new DbServiceImpl(context, "ignore", "ignore", "http://ignored.example.com");
        dbService.removeConnectionListener(listener);
        verify(connection).removeListener(listener);
    }
    
    static class DelayedConnection extends Connection {

        boolean connectCalled = false;
        boolean disconnectCalled = false;
        private Thread thread;
        boolean delayAwaited = false;
        
        @Override
        public void connect() {
            // delay connection and then fail
            Runnable runnable = new Runnable() {
                
                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                        // This makes the connection fail (although delayed).
                        delayAwaited = true;
                        fireChanged(ConnectionStatus.FAILED_TO_CONNECT);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            };
            thread = new Thread(runnable);
            thread.start();
            connectCalled = true;
        }

        @Override
        public void disconnect() {
            Runnable runnable = new Runnable() {
                
                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                        delayAwaited = true;
                        fireChanged(ConnectionStatus.DISCONNECTED);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            };
            thread = new Thread(runnable);
            thread.start();
            disconnectCalled = true;
        }

    }
    
    static class ImmediateConnection extends Connection {

        boolean connectCalled = false;
        boolean disconnectCalled = false;

        @Override
        public void connect() {
            connectCalled = true;
            fireChanged(ConnectionStatus.CONNECTED);
        }

        @Override
        public void disconnect() {
            disconnectCalled = true;
            fireChanged(ConnectionStatus.DISCONNECTED);
        }

    }
    
    static class ConnectingConnectionListener implements ConnectionListener {
        
        boolean eventReceived = false;

        @Override
        public void changed(ConnectionStatus newStatus) {
            if (newStatus == ConnectionStatus.CONNECTING) {
                eventReceived = true;
            }
        }
        
    }
    
    static class ConnectingConnection extends ImmediateConnection {
        @Override
        public void connect() {
            fireChanged(ConnectionStatus.CONNECTING);
            super.connect();
        }
    }
}

