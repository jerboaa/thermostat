/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.common.dao;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.storage.Connection;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.common.storage.StorageProvider;

public class MongoDAOFactoryTest {

    private Storage storage;
    private Connection connection;
    private StorageProvider provider;
    private DAOFactory daoFactory;
    HostRef hostRef;
    VmRef vmRef;

    @Before
    public void setUp() {
        storage = mock(Storage.class);
        connection = mock(Connection.class);
        when(storage.getConnection()).thenReturn(connection);
        when(connection.isConnected()).thenReturn(true);
        provider = mock(StorageProvider.class);
        when(provider.createStorage()).thenReturn(storage);
        hostRef = mock(HostRef.class);
        vmRef = mock(VmRef.class);
        daoFactory = new MongoDAOFactory(provider);
    }

    @Test
    public void testGetConnection() {
        assertSame(storage, daoFactory.getStorage());
    }

    @Test
    public void testGetVmClassStatsDAO() {
        VmClassStatDAO dao = daoFactory.getVmClassStatsDAO();
        assertNotNull(dao);
    }

    @Test
    public void testGetVmGcStatDAO() {
        VmGcStatDAO dao = daoFactory.getVmGcStatDAO();
        assertNotNull(dao);
    }

    @Test
    public void testGetVmInfoDAO() {
        VmInfoDAO dao = daoFactory.getVmInfoDAO();
        assertNotNull(dao);
    }

    @Test
    public void testGetVmMemoryStatDAO() {
        VmMemoryStatDAO dao = daoFactory.getVmMemoryStatDAO();
        assertNotNull(dao);
    }

    @Test
    public void testGetHostInfoDAO() {
        HostInfoDAO dao = daoFactory.getHostInfoDAO();
        assertNotNull(dao);
    }

    @Test
    public void testGetCpuStatDAO() {
        CpuStatDAO dao = daoFactory.getCpuStatDAO();
        assertNotNull(dao);
    }

    @Test
    public void testGetMemoryStatDAO() {
        MemoryStatDAO dao = daoFactory.getMemoryStatDAO();
        assertNotNull(dao);
    }

    @Test
    public void testGetNetworkInterfaceInfoDAO() {
        NetworkInterfaceInfoDAO dao = daoFactory.getNetworkInterfaceInfoDAO();
        assertNotNull(dao);
    }
}
