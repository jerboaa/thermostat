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

package com.redhat.thermostat.backend.system;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.dao.CpuStatDAO;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.MemoryStatDAO;
import com.redhat.thermostat.common.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.common.dao.VmClassStatDAO;
import com.redhat.thermostat.common.dao.VmCpuStatDAO;
import com.redhat.thermostat.common.dao.VmGcStatDAO;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmMemoryStatDAO;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Storage;

public class SystemBackendTest {

    private SystemBackend b;

    @Before
    public void setUp() {
        Storage s = mock(Storage.class);
        CpuStatDAO cDAO = mock(CpuStatDAO.class);
        HostInfoDAO hDAO = mock(HostInfoDAO.class);
        MemoryStatDAO mDAO = mock(MemoryStatDAO.class);
        VmCpuStatDAO vDAO = mock(VmCpuStatDAO.class);
        NetworkInterfaceInfoDAO nDAO = mock(NetworkInterfaceInfoDAO.class);
        DAOFactory df = mock(DAOFactory.class);
        when(df.getStorage()).thenReturn(s);
        when(df.getCpuStatDAO()).thenReturn(cDAO);
        when(df.getHostInfoDAO()).thenReturn(hDAO);
        when(df.getMemoryStatDAO()).thenReturn(mDAO);
        when(df.getVmCpuStatDAO()).thenReturn(vDAO);
        when(df.getNetworkInterfaceInfoDAO()).thenReturn(nDAO);
        b = new SystemBackend();
        b.setDAOFactory(df);
    }

    @Test
    public void testBasicBackend() {
        assertFalse(b.isActive());
        b.activate();
        assertTrue(b.isActive());
        b.deactivate();
        assertFalse(b.isActive());
    }

    @Test
    public void testActivateTwice() {
        b.activate();
        b.activate();
        assert(b.isActive());
    }

    @Test
    public void testDeactiateWhenNotActive() {
        b.deactivate();
        b.deactivate();
        assertFalse(b.isActive());
    }

    @Test
    public void testCategoriesAreSane() {
        Collection<Category> categories = b.getCategories();

        assertTrue(categories.contains(CpuStatDAO.cpuStatCategory));
        assertTrue(categories.contains(HostInfoDAO.hostInfoCategory));
        assertTrue(categories.contains(MemoryStatDAO.memoryStatCategory));
        assertTrue(categories.contains(NetworkInterfaceInfoDAO.networkInfoCategory));
        assertTrue(categories.contains(VmClassStatDAO.vmClassStatsCategory));
        assertTrue(categories.contains(VmCpuStatDAO.vmCpuStatCategory));
        assertTrue(categories.contains(VmGcStatDAO.vmGcStatCategory));
        assertTrue(categories.contains(VmInfoDAO.vmInfoCategory));
        assertTrue(categories.contains(VmMemoryStatDAO.vmMemoryStatsCategory));
    }

}
