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
import com.redhat.thermostat.storage.core.DbService;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.storage.core.StorageProvider;
import com.redhat.thermostat.testutils.StubBundleContext;

public class DbServiceImplTest {
    
    private Connection connection;
    private StorageProvider storageProvider;
    private Storage storage;
    private StubBundleContext context;
    
    @Before
    public void setup() {
        context = new StubBundleContext();
        connection = mock(Connection.class);

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
        DbService dbService = new DbServiceImpl(context, "ignore", "ignore", "http://ignored.example.com");

        dbService.connect();

        verify(connection).connect();
    }
    
    @Test
    public void testConnectRegistersDbService() {
        DbService dbService = new DbServiceImpl(context, "ignore", "ignore", "http://ignored.example.com");

        dbService.connect();

        verify(connection).connect();
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

        dbService.connect();

        verify(connection).connect();
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

        dbService.connect();
        assertNotNull(context.getServiceReference(DbService.class));
        
        dbService.disconnect();

        verify(connection).disconnect();
    }

    @Test
    public void testDisconnectUnregistersDbService() {
        DbService dbService = new DbServiceImpl(context, "ignore", "ignore", "http://ignored.example.com");

        dbService.connect();
        assertNotNull(context.getServiceReference(DbService.class));
        
        dbService.disconnect();

        verify(connection).disconnect();
        // disconnect unregisters DbService
        assertNull(context.getServiceReference(DbService.class));
    }
    
    @Test
    public void testDisconnectUnregistersStorage() {
        DbService dbService = new DbServiceImpl(context, "ignore", "ignore", "http://ignored.example.com");

        dbService.connect();
        assertNotNull(context.getServiceReference(Storage.class));
        
        dbService.disconnect();

        verify(connection).disconnect();
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
        DbService dbService = new DbServiceImpl(context, "ignore", "ignore", "http://ignored.example.com");

        dbService.addConnectionListener(listener);
        verify(connection).addListener(listener);
    }
    
    @Test
    public void testRemoveListener() {
        // Remove called regardless of listener actually being added
        ConnectionListener listener = mock(ConnectionListener.class);
        DbService dbService = new DbServiceImpl(context, "ignore", "ignore", "http://ignored.example.com");
        dbService.removeConnectionListener(listener);
        verify(connection).removeListener(listener);
    }
}

