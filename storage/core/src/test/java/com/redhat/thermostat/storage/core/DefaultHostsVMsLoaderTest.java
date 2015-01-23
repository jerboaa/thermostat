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

package com.redhat.thermostat.storage.core;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.storage.core.DefaultHostsVMsLoader;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;

public class DefaultHostsVMsLoaderTest {

    private HostInfoDAO mockHostsDAO;
    private VmInfoDAO mockVmsDAO;
    private DefaultHostsVMsLoader loader;
    
    @Before
    public void setUp() throws Exception {
        mockHostsDAO = mock(HostInfoDAO.class);
        mockVmsDAO = mock(VmInfoDAO.class);
    }

    @After
    public void tearDown() throws Exception {
        mockHostsDAO = null;
        mockVmsDAO = null;
        loader = null;
    }

    @Test
    public void canGetHosts() {
        Collection<HostRef> expectedHosts = new ArrayList<>();
        expectedHosts.add(new HostRef("123", "fluffhost1"));
        expectedHosts.add(new HostRef("456", "fluffhost2"));
        expectedHosts.add(new HostRef("007", "deadHost"));

        loader = new DefaultHostsVMsLoader(mockHostsDAO, mockVmsDAO, false);
        when(mockHostsDAO.getHosts()).thenReturn(expectedHosts);
        assertEquals(loader.getHosts(), expectedHosts);
        
        loader = new DefaultHostsVMsLoader(mockHostsDAO, mockVmsDAO, true);
        Collection<HostRef> aliveHosts = new ArrayList<>();
        expectedHosts.add(new HostRef("123", "fluffhost1"));
        expectedHosts.add(new HostRef("456", "fluffhost2"));
        when(mockHostsDAO.getAliveHosts()).thenReturn(aliveHosts);
        assertEquals(loader.getHosts(), aliveHosts);
    }
    
    @Test
    public void canGetVms() {
        HostRef hostR = mock(HostRef.class);
        Collection<VmRef> expectedVms = new ArrayList<>();
        expectedVms.add(new VmRef(hostR, "321", 1, "test1"));
        expectedVms.add(new VmRef(hostR, "654", 2, "test2"));

        loader = new DefaultHostsVMsLoader(mockHostsDAO, mockVmsDAO, false /* irrelevant */);
        when(mockVmsDAO.getVMs(hostR)).thenReturn(expectedVms);
        assertEquals(loader.getVMs(hostR), expectedVms);
        
        assertEquals(0, loader.getVMs(mock(HostRef.class)).size());
    }

}

