/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.dev.populator.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.dev.populator.config.ConfigItem;
import com.redhat.thermostat.dev.populator.dependencies.ProcessedRecords;
import com.redhat.thermostat.dev.populator.dependencies.SharedState;
import com.redhat.thermostat.dev.populator.impl.NetworkInfoPopulator.NetworkInterfaceDAOCountable;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.model.NetworkInterfaceInfo;

public class NetworkInfoPopulatorTest {

    @Test
    public void canHandleCorrectCollection() {
        NetworkInfoPopulator populator = new NetworkInfoPopulator();
        assertEquals("network-info", populator.getHandledCollection());
    }
    
    @Test
    public void testPopulation() {
        NetworkInterfaceDAOCountable dao = mock(NetworkInterfaceDAOCountable.class);
        NetworkInfoPopulator populator = new NetworkInfoPopulator(dao);
        
        String[] agentIds = new String[] {
                "testAgent1", "testAgent2", "testAgent3", "testAgent4"
        };
        int perAgentItems = 100;
        int totalRecords = perAgentItems * agentIds.length;
        when(dao.getCount()).thenReturn(0L).thenReturn((long)totalRecords);
        List<String> agents = Arrays.asList(agentIds);
        SharedState state = new SharedState();
        state.addProcessedRecords("agentId", new ProcessedRecords<>(agents));
        ConfigItem config = new ConfigItem(perAgentItems, ConfigItem.UNSET, "network-info");
        populator.addPojos(mock(Storage.class), config, state);
        ArgumentCaptor<NetworkInterfaceInfo> captor = ArgumentCaptor.forClass(NetworkInterfaceInfo.class);
        verify(dao, times(totalRecords)).putNetworkInterfaceInfo(captor.capture());
        List<NetworkInterfaceInfo> list = captor.getAllValues();
        // expected agentId + iface name to be unique (since REPLACE otherwise replaces some random values)
        Set<String> uniqueSet = new HashSet<>();
        for (NetworkInterfaceInfo info: list) {
            String agentId = info.getAgentId();
            String ifaceName = info.getInterfaceName();
            String key = agentId + ifaceName;
            if (uniqueSet.contains(key)) {
                throw new AssertionError("Expected interface names to be unique per agent");
            } else {
                uniqueSet.add(key);
            }
        }
    }
    
    @Test
    public void iPv6HextetRollOver() {
        String ipv6 = new NetworkInfoPopulator().getIpv6Hextet(Integer.parseInt("fffe", 16));
        assertEquals("fffe", ipv6);
        ipv6 = new NetworkInfoPopulator().getIpv6Hextet(Integer.parseInt("ffff", 16));
        assertEquals("1", ipv6);
        ipv6 = new NetworkInfoPopulator().getIpv6Hextet(Integer.parseInt("eee1", 16));
        assertEquals("eee1", ipv6);
    }
    
    @Test
    public void iPv4OctetsRollOver() {
        int octet = new NetworkInfoPopulator().getIpv4Octet(255);
        assertEquals(1, octet);
        octet = new NetworkInfoPopulator().getIpv4Octet(256);
        assertEquals(1, octet);
        octet = new NetworkInfoPopulator().getIpv4Octet(114);
        assertEquals(114, octet);
    }
    
    @Test
    public void stringFormatIpv6Template() {
        String formattedString = String.format(NetworkInfoPopulator.IPV6_FORMATS[1], "ff", "scope");
        assertEquals("fe80::56ee:75ff:fe35:ff%scope", formattedString);
    }
}
