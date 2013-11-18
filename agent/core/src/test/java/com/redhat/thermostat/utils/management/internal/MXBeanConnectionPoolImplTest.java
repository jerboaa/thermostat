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

package com.redhat.thermostat.utils.management.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Test;

import com.redhat.thermostat.utils.management.MXBeanConnection;
import com.redhat.thermostat.utils.management.internal.MXBeanConnectionPoolImpl.ConnectorCreator;

public class MXBeanConnectionPoolImplTest {

    private File binDir = mock(File.class);

    @Test
    public void testAcquire() throws Exception {
        MXBeanConnectionImpl toReturn = mock(MXBeanConnectionImpl.class);
        MXBeanConnector connector = mock(MXBeanConnector.class);
        ConnectorCreator creator = mock(ConnectorCreator.class);

        when(creator.create(any(RMIRegistry.class), anyInt(), any(File.class))).thenReturn(connector);
        when(connector.connect()).thenReturn(toReturn);

        RMIRegistry registry = mock(RMIRegistry.class);
        MXBeanConnectionPoolImpl pool = new MXBeanConnectionPoolImpl(creator, registry, binDir);

        MXBeanConnection connection = pool.acquire(0);

        assertNotNull(connection);
        assertEquals(connection, toReturn);

        verify(connector).attach();
        verify(connector).connect();
        verify(connector).close();
    }

    @Test
    public void testAcquireTwice() throws Exception {
        MXBeanConnectionImpl toReturn = mock(MXBeanConnectionImpl.class);
        MXBeanConnector connector = mock(MXBeanConnector.class);
        ConnectorCreator creator = mock(ConnectorCreator.class);

        when(creator.create(any(RMIRegistry.class), anyInt(), any(File.class))).thenReturn(connector);
        when(connector.connect()).thenReturn(toReturn);

        RMIRegistry registry = mock(RMIRegistry.class);
        MXBeanConnectionPoolImpl pool = new MXBeanConnectionPoolImpl(creator, registry, binDir);

        MXBeanConnection connection1 = pool.acquire(0);

        verify(connector).attach();
        verify(connector).connect();
        verify(connector).close();

        MXBeanConnection connection2 = pool.acquire(0);

        assertEquals(connection1, toReturn);
        assertEquals(connection2, toReturn);

        verifyNoMoreInteractions(connector);
    }

    @Test
    public void testRelease() throws Exception {
        MXBeanConnectionImpl actualConnection = mock(MXBeanConnectionImpl.class);
        MXBeanConnector connector = mock(MXBeanConnector.class);
        ConnectorCreator creator = mock(ConnectorCreator.class);

        when(creator.create(any(RMIRegistry.class), anyInt(), any(File.class))).thenReturn(connector);
        when(connector.connect()).thenReturn(actualConnection);

        RMIRegistry registry = mock(RMIRegistry.class);
        MXBeanConnectionPoolImpl pool = new MXBeanConnectionPoolImpl(creator, registry, binDir);

        MXBeanConnection connection = pool.acquire(0);

        verify(actualConnection, never()).close();

        pool.release(0, connection);

        verify(actualConnection).close();
    }

    @Test
    public void testReleaseTwice() throws Exception {
        MXBeanConnectionImpl actualConnection = mock(MXBeanConnectionImpl.class);
        MXBeanConnector connector = mock(MXBeanConnector.class);
        ConnectorCreator creator = mock(ConnectorCreator.class);

        when(creator.create(any(RMIRegistry.class), anyInt(), any(File.class))).thenReturn(connector);
        when(connector.connect()).thenReturn(actualConnection);

        RMIRegistry registry = mock(RMIRegistry.class);
        MXBeanConnectionPoolImpl pool = new MXBeanConnectionPoolImpl(creator, registry, binDir);

        // connection1 == connection1 == actualConnection
        MXBeanConnection connection1 = pool.acquire(0);
        MXBeanConnection connection2 = pool.acquire(0);

        pool.release(0, connection1);

        verify(actualConnection, never()).close();

        pool.release(0, connection2);

        verify(actualConnection).close();

    }
    
    @Test
    public void testShutdown() throws Exception {
        RMIRegistry registry = mock(RMIRegistry.class);
        ConnectorCreator creator = mock(ConnectorCreator.class);
        MXBeanConnectionPoolImpl pool = new MXBeanConnectionPoolImpl(creator, registry, binDir);
        verify(registry).start();
        
        pool.shutdown();
        verify(registry).stop();
    }
}
