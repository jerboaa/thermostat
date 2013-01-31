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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.storage.core.Connection;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.DbService;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.testutils.StubBundleContext;

public class DbServiceImplTest {
    
    private Connection connection;
    private Storage storage;
    private DbService dbService;
    private StubBundleContext context;
    
    @Before
    public void setup() {
        context = new StubBundleContext();
        connection = mock(Connection.class);

        storage = mock(Storage.class);
        when(storage.getConnection()).thenReturn(connection);

        dbService = new DbServiceImpl(context, storage, "http://someUrl.ignored.com");
    }
    
    @After
    public void teardown() {
        dbService = null;
        context = null;
    }

    @Test
    public void testConnect() {
        dbService.connect();

        verify(connection).connect();
    }
    
    @Test
    public void testConnectRegistersDbService() {
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
        dbService.connect();

        verify(connection).connect();
        @SuppressWarnings("rawtypes")
        ServiceReference storageRef = context.getServiceReference(Storage.class);
        // connect registers DbService
        assertNotNull(storageRef);
        // make sure we really get the same instance
        assertTrue(storage.equals(context.getService(storageRef)));
    }

    @Test
    public void testDisconnect() {
        dbService.connect();
        assertNotNull(context.getServiceReference(DbService.class));
        
        dbService.disconnect();

        verify(connection).disconnect();
    }

    @Test
    public void testDisconnectUnregistersDbService() {
        dbService.connect();
        assertNotNull(context.getServiceReference(DbService.class));
        
        dbService.disconnect();

        verify(connection).disconnect();
        // disconnect unregisters DbService
        assertNull(context.getServiceReference(DbService.class));
    }
    
    @Test
    public void testDisconnectUnregistersStorage() {
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

        dbService = new DbServiceImpl(context, null, connectionURL);
        assertEquals(connectionURL, dbService.getConnectionUrl());
    }
    
    @Test
    public void testAddListener() {
        ConnectionListener listener = mock(ConnectionListener.class);
        dbService.addConnectionListener(listener);
        verify(connection).addListener(listener);
    }
    
    @Test
    public void testRemoveListener() {
        // Remove called regardless of listener actually being added
        ConnectionListener listener = mock(ConnectionListener.class);
        dbService.removeConnectionListener(listener);
        verify(connection).removeListener(listener);
    }
}

