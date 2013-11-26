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

package com.redhat.thermostat.backend.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.VmBlacklist;
import com.redhat.thermostat.agent.utils.username.UserNameUtil;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;

public class SystemBackendTest {

    private static final String VERSION = "0.0.0";
    private SystemBackend b;

    @Before
    public void setUp() {
        HostInfoDAO hDAO = mock(HostInfoDAO.class);
        NetworkInterfaceInfoDAO nDAO = mock(NetworkInterfaceInfoDAO.class);
        VmInfoDAO vmInfoDAO = mock(VmInfoDAO.class);

        Version version = mock(Version.class);
        when(version.getVersionNumber()).thenReturn(VERSION);
        
        VmStatusChangeNotifier notifier = mock(VmStatusChangeNotifier.class);
        UserNameUtil util = mock(UserNameUtil.class);

        WriterID id = mock(WriterID.class);
        VmBlacklist blacklist = mock(VmBlacklist.class);
        b = new SystemBackend(hDAO, nDAO, vmInfoDAO, version, notifier, util, id, blacklist);
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
    public void testVersion() {
        assertEquals(VERSION, b.getVersion());
    }

}

