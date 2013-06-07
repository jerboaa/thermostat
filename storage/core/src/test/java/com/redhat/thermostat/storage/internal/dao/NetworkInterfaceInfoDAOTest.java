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

package com.redhat.thermostat.storage.internal.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Replace;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.storage.model.NetworkInterfaceInfo;
import com.redhat.thermostat.storage.query.BinaryComparisonExpression;
import com.redhat.thermostat.storage.query.BinaryComparisonOperator;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;
import com.redhat.thermostat.storage.query.LiteralExpression;

public class NetworkInterfaceInfoDAOTest {

    private static final String INTERFACE_NAME = "some interface. maybe eth0";
    private static final String IPV4_ADDR = "256.256.256.256";
    private static final String IPV6_ADDR = "100:100:100::::1";

    @Test
    public void testCategory() {
        Collection<Key<?>> keys;

        assertEquals("network-info", NetworkInterfaceInfoDAO.networkInfoCategory.getName());
        keys = NetworkInterfaceInfoDAO.networkInfoCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agentId", true)));
        assertTrue(keys.contains(new Key<String>("interfaceName", true)));
        assertTrue(keys.contains(new Key<String>("ip4Addr", false)));
        assertTrue(keys.contains(new Key<String>("ip6Addr", false)));
        assertEquals(4, keys.size());
    }

    @Test
    public void testGetNetworkInterfaces() {

        NetworkInterfaceInfo niInfo = new NetworkInterfaceInfo(INTERFACE_NAME);
        niInfo.setIp4Addr(IPV4_ADDR);
        niInfo.setIp6Addr(IPV6_ADDR);

        @SuppressWarnings("unchecked")
        Cursor<NetworkInterfaceInfo> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(niInfo);

        Storage storage = mock(Storage.class);
        Query query = mock(Query.class);
        when(storage.createQuery(any(Category.class))).thenReturn(query);
        when(query.execute()).thenReturn(cursor);

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("system");

        NetworkInterfaceInfoDAO dao = new NetworkInterfaceInfoDAOImpl(storage);
        List<NetworkInterfaceInfo> netInfo = dao.getNetworkInterfaces(hostRef);

        ExpressionFactory factory = new ExpressionFactory();
        Expression expr = factory.equalTo(Key.AGENT_ID, "system");
        verify(query).where(eq(expr));
        verify(query).execute();
        verifyNoMoreInteractions(query);

        assertEquals(1, netInfo.size());

        NetworkInterfaceInfo info = netInfo.get(0);

        assertEquals(INTERFACE_NAME, info.getInterfaceName());
        assertEquals(IPV4_ADDR, info.getIp4Addr());
        assertEquals(IPV6_ADDR, info.getIp6Addr());
    }

    @Test
    public void testPutNetworkInterfaceInfo() {
        Storage storage = mock(Storage.class);
        Replace replace = mock(Replace.class);
        when(storage.createReplace(any(Category.class))).thenReturn(replace);

        NetworkInterfaceInfo info = new NetworkInterfaceInfo(INTERFACE_NAME);
        info.setIp4Addr(IPV4_ADDR);
        info.setIp6Addr(IPV6_ADDR);
        NetworkInterfaceInfoDAO dao = new NetworkInterfaceInfoDAOImpl(storage);
        dao.putNetworkInterfaceInfo(info);

        verify(storage).createReplace(NetworkInterfaceInfoDAO.networkInfoCategory);
        verify(replace).setPojo(info);
        verify(replace).apply();
    }
}

