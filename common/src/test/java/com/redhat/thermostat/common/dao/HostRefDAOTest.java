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

import java.util.Collection;

import org.junit.Test;

import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Storage;
import static org.mockito.Mockito.*;

public class HostRefDAOTest {

    @Test
    public void testKeys() {
        assertEquals(new Key<String>("agent-id", false), HostRefDAO.agentIdKey);
    }

    @Test
    public void testAgentConfigCategory() {
        assertEquals("agent-config", HostRefDAO.agentConfigCategory.getName());
        assertEquals(1, HostRefDAO.agentConfigCategory.getKeys().size());
        assertTrue(HostRefDAO.agentConfigCategory.getKeys().contains(HostRefDAO.agentIdKey));
    }

    @Test
    public void testGetHostsSingleHost() {

        Storage storage = setupStorageForSingleHost();

        HostRefDAO hostsVMsDAO = new HostRefDAOImpl(storage);
        Collection<HostRef> hosts = hostsVMsDAO.getHosts();

        assertEquals(1, hosts.size());
        assertTrue(hosts.contains(new HostRef("123", "fluffhost1")));
    }

    private Storage setupStorageForSingleHost() {
        
        Chunk agentConfig = new Chunk(HostRefDAO.agentConfigCategory, false);
        agentConfig.put(HostRefDAO.agentIdKey, "123");

        Cursor agentsCursor = mock(Cursor.class);
        when(agentsCursor.hasNext()).thenReturn(true).thenReturn(false);
        when(agentsCursor.next()).thenReturn(agentConfig);

        Chunk hostConfig = new Chunk(HostInfoDAO.hostInfoCategory, false);
        hostConfig.put(HostInfoDAO.hostNameKey, "fluffhost1");
        hostConfig.put(HostRefDAO.agentIdKey, "123");

        Storage storage = mock(Storage.class);
        when(storage.findAllFromCategory(HostRefDAO.agentConfigCategory)).thenReturn(agentsCursor);
        Chunk hostsQuery = new Chunk(HostInfoDAO.hostInfoCategory, false);
        hostsQuery.put(HostRefDAO.agentIdKey, "123");
        when(storage.find(hostsQuery)).thenReturn(hostConfig);

        return storage;
    }

    @Test
    public void testGetHosts3Hosts() {

        Storage storage = setupStorageFor3Hosts();

        HostRefDAO hostsVMsDAO = new HostRefDAOImpl(storage);
        Collection<HostRef> hosts = hostsVMsDAO.getHosts();

        assertEquals(3, hosts.size());
        assertTrue(hosts.contains(new HostRef("123", "fluffhost1")));
        assertTrue(hosts.contains(new HostRef("456", "fluffhost2")));
        assertTrue(hosts.contains(new HostRef("789", "fluffhost3")));
    }

    private Storage setupStorageFor3Hosts() {
        Chunk agentConfig1 = new Chunk(HostRefDAO.agentConfigCategory, false);
        agentConfig1.put(HostRefDAO.agentIdKey, "123");
        Chunk agentConfig2 = new Chunk(HostRefDAO.agentConfigCategory, false);
        agentConfig2.put(HostRefDAO.agentIdKey, "456");
        Chunk agentConfig3 = new Chunk(HostRefDAO.agentConfigCategory, false);
        agentConfig3.put(HostRefDAO.agentIdKey, "789");

        Cursor agentsCursor = mock(Cursor.class);
        when(agentsCursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(agentsCursor.next()).thenReturn(agentConfig1).thenReturn(agentConfig2).thenReturn(agentConfig3);

        Chunk hostConfig1 = new Chunk(HostInfoDAO.hostInfoCategory, false);
        hostConfig1.put(HostInfoDAO.hostNameKey, "fluffhost1");
        hostConfig1.put(HostRefDAO.agentIdKey, "123");
        Chunk hostConfig2 = new Chunk(HostInfoDAO.hostInfoCategory, false);
        hostConfig2.put(HostInfoDAO.hostNameKey, "fluffhost2");
        hostConfig2.put(HostRefDAO.agentIdKey, "456");
        Chunk hostConfig3 = new Chunk(HostInfoDAO.hostInfoCategory, false);
        hostConfig3.put(HostInfoDAO.hostNameKey, "fluffhost3");
        hostConfig3.put(HostRefDAO.agentIdKey, "789");

        Storage storage = mock(Storage.class);
        when(storage.findAllFromCategory(HostRefDAO.agentConfigCategory)).thenReturn(agentsCursor);
        Chunk hostsQuery1 = new Chunk(HostInfoDAO.hostInfoCategory, false);
        hostsQuery1.put(HostRefDAO.agentIdKey, "123");
        when(storage.find(hostsQuery1)).thenReturn(hostConfig1);
        Chunk hostsQuery2 = new Chunk(HostInfoDAO.hostInfoCategory, false);
        hostsQuery2.put(HostRefDAO.agentIdKey, "456");
        when(storage.find(hostsQuery2)).thenReturn(hostConfig2);
        Chunk hostsQuery3 = new Chunk(HostInfoDAO.hostInfoCategory, false);
        hostsQuery3.put(HostRefDAO.agentIdKey, "789");
        when(storage.find(hostsQuery3)).thenReturn(hostConfig3);

        return storage;
    }
}
