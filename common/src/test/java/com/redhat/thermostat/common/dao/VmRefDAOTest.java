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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Storage;

public class VmRefDAOTest {

    private VmRefDAO dao;

    @Before
    public void setUp() {
        Storage storage = setupStorageForSingleVM();
        dao = new VmRefDAOImpl(storage);
    }

    @After
    public void tearDown() {
        dao = null;
    }

    private Storage setupStorageForSingleVM() {
        Chunk query1 = new Chunk(VmInfoDAO.vmInfoCategory, false);
        query1.put(HostRefDAO.agentIdKey, "123");

        Chunk query2 = new Chunk(VmInfoDAO.vmInfoCategory, false);
        query2.put(HostRefDAO.agentIdKey, "456");

        Chunk vm1 = new Chunk(VmInfoDAO.vmInfoCategory, false);
        vm1.put(VmInfoDAO.vmIdKey, 123);
        vm1.put(VmInfoDAO.mainClassKey, "mainClass1");

        Chunk vm2 = new Chunk(VmInfoDAO.vmInfoCategory, false);
        vm2.put(VmInfoDAO.vmIdKey, 456);
        vm2.put(VmInfoDAO.mainClassKey, "mainClass2");

        Cursor singleVMCursor = mock(Cursor.class);
        when(singleVMCursor.hasNext()).thenReturn(true).thenReturn(false);
        when(singleVMCursor.next()).thenReturn(vm1);

        Cursor multiVMsCursor = mock(Cursor.class);
        when(multiVMsCursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(multiVMsCursor.next()).thenReturn(vm1).thenReturn(vm2);

        Storage storage = mock(Storage.class);
        when(storage.findAll(query1)).thenReturn(singleVMCursor);
        when(storage.findAll(query2)).thenReturn(multiVMsCursor);
        return storage;
    }

    @Test
    public void testSingleVM() {
        HostRef host = new HostRef("123", "fluffhost");

        Collection<VmRef> vms = dao.getVMs(host);

        assertCollection(vms, new VmRef(host, 123, "mainClass1"));
    }

    @Test
    public void testMultiVMs() {
        HostRef host = new HostRef("456", "fluffhost");

        Collection<VmRef> vms = dao.getVMs(host);

        assertCollection(vms, new VmRef(host, 123, "mainClass1"), new VmRef(host, 456, "mainClass2"));
    }

    private void assertCollection(Collection<VmRef> vms, VmRef... expectedVMs) {
        assertEquals(expectedVMs.length, vms.size());
        for (VmRef expectedVM : expectedVMs) {
            assertTrue(vms.contains(expectedVM));
        }
    }

}
