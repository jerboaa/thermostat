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

package com.redhat.thermostat.backend.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.Test;

import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.storage.model.NetworkInterfaceInfo;

public class NetworkInfoBuilderTest {

    @Test
    public void testBuilder() {

        WriterID id = mock(WriterID.class);
        
        NetworkInfoBuilder builder = new NetworkInfoBuilder(id);
        List<NetworkInterfaceInfo> info = builder.build();
        assertNotNull(info);
        for (NetworkInterfaceInfo iface: info) {
            assertNotNull(iface);
            assertNotNull(iface.getInterfaceName());
            if (iface.getIp4Addr() != null) {
                // ipv4 address matches the form XX.XX.XX.XX
                assertTrue(iface.getIp4Addr().matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"));
            }
            if (iface.getIp6Addr() != null) {
                validateIpv6Address(iface.getIp6Addr());
            }
        }

    }

    private void validateIpv6Address(String address) {
        // ipv6 addresses may contain a scope id
        if (address.contains("%")) {
            int index = address.indexOf("%");
            assertTrue(index >= 0);
            String scopeId = address.substring(index);
            assertFalse(scopeId.isEmpty());
            address = address.substring(0, index);
        }

        String[] parts = address.split(":");
        assertEquals(8, parts.length);

        for (String part : parts) {
            assertNotNull(part);
            if (!part.isEmpty()) {
                assertTrue(part.matches("[0-9a-f]*"));
            }
        }
    }

}

