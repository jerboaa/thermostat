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
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.redhat.thermostat.common.model.NetworkInterfaceInfo;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;

public class NetworkInterfaceInfoConverterTest {

    @Test
    public void testNetworkInfoToChunk() {
        NetworkInterfaceInfo info = new NetworkInterfaceInfo("eth0");
        info.setIp4Addr("4");
        info.setIp6Addr("6");

        Chunk chunk = new NetworkInterfaceInfoConverter().networkInfoToChunk(info);

        assertEquals("network-info", chunk.getCategory().getName());
        assertEquals("eth0", chunk.get(new Key<String>("iface", true)));
        assertEquals("4", chunk.get(new Key<String>("ipv4addr", false)));
        assertEquals("6", chunk.get(new Key<String>("ipv6addr", false)));

    }

    @Test
    public void testChunkToNetworkInfo() {
        final String INTERFACE_NAME = "some interface. maybe eth0";
        final String IPV4_ADDR = "256.256.256.256";
        final String IPV6_ADDR = "100:100:100::::1";

        Chunk chunk = new Chunk(NetworkInterfaceInfoDAO.networkInfoCategory, false);
        chunk.put(NetworkInterfaceInfoDAO.ifaceKey, INTERFACE_NAME);
        chunk.put(NetworkInterfaceInfoDAO.ip4AddrKey, IPV4_ADDR);
        chunk.put(NetworkInterfaceInfoDAO.ip6AddrKey, IPV6_ADDR);

        NetworkInterfaceInfo info = new NetworkInterfaceInfoConverter().chunkToNetworkInfo(chunk);
        assertNotNull(info);
        assertEquals(INTERFACE_NAME, info.getInterfaceName());
        assertEquals(IPV4_ADDR, info.getIp4Addr());
        assertEquals(IPV6_ADDR, info.getIp6Addr());
    }

}
