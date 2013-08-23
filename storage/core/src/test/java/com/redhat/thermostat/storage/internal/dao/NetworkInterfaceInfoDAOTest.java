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
import static org.junit.Assert.assertNull;
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

import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Replace;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.storage.model.NetworkInterfaceInfo;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;

public class NetworkInterfaceInfoDAOTest {

    private static final String INTERFACE_NAME = "some interface. maybe eth0";
    private static final String IPV4_ADDR = "256.256.256.256";
    private static final String IPV6_ADDR = "100:100:100::::1";

    @Test
    public void testCategory() {
        Collection<Key<?>> keys;

        assertEquals("network-info", NetworkInterfaceInfoDAO.networkInfoCategory.getName());
        keys = NetworkInterfaceInfoDAO.networkInfoCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agentId")));
        assertTrue(keys.contains(new Key<String>("interfaceName")));
        assertTrue(keys.contains(new Key<String>("ip4Addr")));
        assertTrue(keys.contains(new Key<String>("ip6Addr")));
        assertEquals(4, keys.size());
    }
    
    @Test
    public void preparedQueryDescriptorsAreSane() {
        String expectedNetworkInfo = "QUERY network-info WHERE 'agentId' = ?s";
        assertEquals(expectedNetworkInfo, NetworkInterfaceInfoDAOImpl.QUERY_NETWORK_INFO);
    }

    @Test
    public void testGetNetworkInterfaces() throws DescriptorParsingException, StatementExecutionException {

        NetworkInterfaceInfo niInfo = new NetworkInterfaceInfo(INTERFACE_NAME);
        niInfo.setIp4Addr(IPV4_ADDR);
        niInfo.setIp6Addr(IPV6_ADDR);

        @SuppressWarnings("unchecked")
        Cursor<NetworkInterfaceInfo> cursor = (Cursor<NetworkInterfaceInfo>) mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(niInfo);

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<NetworkInterfaceInfo> stmt = (PreparedStatement<NetworkInterfaceInfo>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(cursor);

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("system");

        NetworkInterfaceInfoDAO dao = new NetworkInterfaceInfoDAOImpl(storage);
        List<NetworkInterfaceInfo> netInfo = dao.getNetworkInterfaces(hostRef);

        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, "system");
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);

        assertEquals(1, netInfo.size());

        NetworkInterfaceInfo info = netInfo.get(0);

        assertEquals(INTERFACE_NAME, info.getInterfaceName());
        assertEquals(IPV4_ADDR, info.getIp4Addr());
        assertEquals(IPV6_ADDR, info.getIp6Addr());
    }

    @SuppressWarnings("unchecked")
    private StatementDescriptor<NetworkInterfaceInfo> anyDescriptor() {
        return (StatementDescriptor<NetworkInterfaceInfo>) any(StatementDescriptor.class);
    }

    @Test
    public void testPutNetworkInterfaceInfo() {
        String agentId = "fooAgent";
        doTestPutNetworkInerfaceInfo(false, agentId);
    }
    
    @Test
    public void testPutNetworkInterfaceInfoWithoutAgentIdInInfo() {
        String agentId = "fooStorageAgentId";
        doTestPutNetworkInerfaceInfo(true, agentId);
    }
    
    private void doTestPutNetworkInerfaceInfo(boolean agentIdFromStorage, String agentId) {
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        Replace<NetworkInterfaceInfo> replace = mock(Replace.class);
        when(storage.createReplace(eq(NetworkInterfaceInfoDAO.networkInfoCategory))).thenReturn(replace);
        if (agentIdFromStorage) {
            when(storage.getAgentId()).thenReturn(agentId);
        }

        NetworkInterfaceInfo info = new NetworkInterfaceInfo(INTERFACE_NAME);
        info.setIp4Addr(IPV4_ADDR);
        info.setIp6Addr(IPV6_ADDR);
        if (!agentIdFromStorage) {
            info.setAgentId(agentId);
        } else {
            // case where agentId gets replaced by the DAO
            // with the one set for storage.
            assertNull(info.getAgentId());
        }
        ExpressionFactory factory = new ExpressionFactory();
        Expression left = factory.equalTo(Key.AGENT_ID, agentId);
        Expression right = factory.equalTo(NetworkInterfaceInfoDAO.ifaceKey, INTERFACE_NAME);
        Expression expected = factory.and(left, right);
        NetworkInterfaceInfoDAO dao = new NetworkInterfaceInfoDAOImpl(storage);
        dao.putNetworkInterfaceInfo(info);

        verify(storage).createReplace(NetworkInterfaceInfoDAO.networkInfoCategory);
        verify(replace).setPojo(info);
        verify(replace).where(expected);
        verify(replace).apply();
    }
    
}

