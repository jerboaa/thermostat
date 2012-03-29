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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.mongodb.DB;

public class MongoDAOFactoryTest {

    private MongoConnection conn;
    private ConnectionProvider connProvider;
    private DAOFactory daoFactory;
    private DB db;
    HostRef hostRef;
    VmRef vmRef;

    @Before
    public void setUp() {
        conn = mock(MongoConnection.class);
        connProvider = mock(ConnectionProvider.class);
        when(connProvider.createConnection()).thenReturn(conn);
        db = mock(DB.class);
        when(conn.getDB()).thenReturn(db);
        hostRef = mock(HostRef.class);
        vmRef = mock(VmRef.class);
        daoFactory = new MongoDAOFactory(connProvider);
    }

    @Test
    public void testGetConnection() {
        assertSame(conn, daoFactory.getConnection());
    }

    @Test
    public void testGetVmClassStatsDAO() {
        VmClassStatDAO dao = daoFactory.getVmClassStatsDAO(vmRef);
        assertNotNull(dao);
    }

    @Test
    public void testGetVmGcStatDAO() {
        VmGcStatDAO dao = daoFactory.getVmGcStatDAO(vmRef);
        assertNotNull(dao);
    }

    @Test
    public void testGetVmInfoDAO() {
        VmInfoDAO dao = daoFactory.getVmInfoDAO(vmRef);
        assertNotNull(dao);
    }

    @Test
    public void testGetVmMemoryStatDAO() {
        VmMemoryStatDAO dao = daoFactory.getVmMemoryStatDAO(vmRef);
        assertNotNull(dao);
    }

    @Test
    public void testGetHostInfoDAO() {
        HostInfoDAO dao = daoFactory.getHostInfoDAO(hostRef);
        assertNotNull(dao);
    }

    @Test
    public void testGetCpuStatDAO() {
        CpuStatDAO dao = daoFactory.getCpuStatDAO(hostRef);
        assertNotNull(dao);
    }

    @Test
    public void testGetMemoryStatDAO() {
        MemoryStatDAO dao = daoFactory.getMemoryStatDAO(hostRef);
        assertNotNull(dao);
    }

    @Test
    public void testGetNetworkInterfaceInfoDAO() {
        NetworkInterfaceInfoDAO dao = daoFactory.getNetworkInterfaceInfoDAO(hostRef);
        assertNotNull(dao);
    }

    @Test
    public void testGetHostRefDAO() {
        HostRefDAO dao = daoFactory.getHostRefDAO();
        assertNotNull(dao);
    }

    @Test
    public void testGetVmRefDAO() {
        VmRefDAO dao = daoFactory.getVmRefDAO();
        assertNotNull(dao);
    }
}
