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

package com.redhat.thermostat.client.heap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.client.osgi.service.Filter;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.Ref;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.VmInfo;

public class HeapDumpActionTest {

    private HeapDumpAction heapDumpAction;
    private DAOFactory dao;
    private VmRef aliveVmRef;
    private VmRef deadVmRef;

    @Before
    public void setUp() {
        dao = mock(DAOFactory.class);
        VmInfoDAO vmInfoDAO = mock(VmInfoDAO.class);
        when(dao.getVmInfoDAO()).thenReturn(vmInfoDAO);

        VmInfo vmInfo1 = mock(VmInfo.class);
        when(vmInfo1.isAlive()).thenReturn(true);
        aliveVmRef = mock(VmRef.class);
        when(vmInfoDAO.getVmInfo(aliveVmRef)).thenReturn(vmInfo1);

        VmInfo vmInfo2 = mock(VmInfo.class);
        when(vmInfo2.isAlive()).thenReturn(false);
        deadVmRef = mock(VmRef.class);
        when(vmInfoDAO.getVmInfo(deadVmRef)).thenReturn(vmInfo2);

        
        BundleContext bundleContext = mock(BundleContext.class);
        heapDumpAction = new HeapDumpAction(dao, bundleContext);
    }

    @After
    public void tearDown() {
        heapDumpAction = null;
        deadVmRef = null;
        aliveVmRef = null;
        dao = null;
    }

    @Test
    public void testName() {
        assertEquals("Heap Analysis", heapDumpAction.getName());
    }

    @Test
    public void testDescription() {
        assertEquals("Heap View", heapDumpAction.getDescription());
    }

    @Test
    public void testFilter() {
        Filter filter = heapDumpAction.getFilter();
        assertTrue(filter.matches(aliveVmRef));
        assertFalse(filter.matches(deadVmRef));
        assertFalse(filter.matches(mock(Ref.class)));
    }

    @Test
    public void testExec() throws IOException {
        // TODO: Not tested yet. We cannot mock Runtime.exec(), not even with PowerMock.
        // We should create a wrapper API (that needs to remain untested, and for this
        // reason should be *really* dumb), against which we can test.
    }
}
