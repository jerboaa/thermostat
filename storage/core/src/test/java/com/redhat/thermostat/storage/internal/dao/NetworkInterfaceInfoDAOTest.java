/*
 * Copyright 2012-2017 Red Hat, Inc.
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.storage.model.AggregateCount;
import com.redhat.thermostat.storage.model.NetworkInterfaceInfo;

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
        String aggregateCountAllNetworkInterfaces = "QUERY-COUNT network-info";
        assertEquals(aggregateCountAllNetworkInterfaces,
                NetworkInterfaceInfoDAOImpl.AGGREGATE_COUNT_ALL_NETWORK_INTERFACES);
        String replaceNetworkInfo = "REPLACE network-info SET 'agentId' = ?s , " +
            "'interfaceName' = ?s , " +
            "'ip4Addr' = ?s , " +
            "'ip6Addr' = ?s WHERE " +
            "'agentId' = ?s AND 'interfaceName' = ?s";
        assertEquals(replaceNetworkInfo, NetworkInterfaceInfoDAOImpl.DESC_REPLACE_NETWORK_INFO);
    }

    @Test
    public void testGetNetworkInterfaces() throws DescriptorParsingException, StatementExecutionException {

        NetworkInterfaceInfo niInfo = new NetworkInterfaceInfo("foo-agent", INTERFACE_NAME);
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

    @SuppressWarnings("unchecked")
    @Test
    public void testPutNetworkInterfaceInfo()
            throws DescriptorParsingException, StatementExecutionException {
        String agentId = "fooAgent";
        Storage storage = mock(Storage.class);
        PreparedStatement<NetworkInterfaceInfo> replace = mock(PreparedStatement.class);
        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(replace);

        NetworkInterfaceInfo info = new NetworkInterfaceInfo(agentId, INTERFACE_NAME);
        info.setIp4Addr(IPV4_ADDR);
        info.setIp6Addr(IPV6_ADDR);
        
        NetworkInterfaceInfoDAO dao = new NetworkInterfaceInfoDAOImpl(storage);
        dao.putNetworkInterfaceInfo(info);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<StatementDescriptor> captor = ArgumentCaptor.forClass(StatementDescriptor.class);
        
        verify(storage).prepareStatement(captor.capture());
        
        StatementDescriptor<?> desc = captor.getValue();
        assertEquals(NetworkInterfaceInfoDAOImpl.DESC_REPLACE_NETWORK_INFO, desc.getDescriptor());
        
        verify(replace).setString(0, info.getAgentId());
        verify(replace).setString(1, info.getInterfaceName());
        verify(replace).setString(2, info.getIp4Addr());
        verify(replace).setString(3, info.getIp6Addr());
        verify(replace).setString(4, info.getAgentId());
        verify(replace).setString(5, info.getInterfaceName());
        verify(replace).execute();
        verifyNoMoreInteractions(replace);
    }

    @Test
    public void testGetCount()
            throws DescriptorParsingException, StatementExecutionException {
        AggregateCount count = new AggregateCount();
        count.setCount(2);

        @SuppressWarnings("unchecked")
        Cursor<AggregateCount> c = (Cursor<AggregateCount>) mock(Cursor.class);
        when(c.hasNext()).thenReturn(true).thenReturn(false);
        when(c.next()).thenReturn(count).thenThrow(new NoSuchElementException());

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<AggregateCount> stmt = (PreparedStatement<AggregateCount>) mock(PreparedStatement.class);
        @SuppressWarnings("unchecked")
        StatementDescriptor<AggregateCount> desc = any(StatementDescriptor.class);
        when(storage.prepareStatement(desc)).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(c);
        NetworkInterfaceInfoDAOImpl dao = new NetworkInterfaceInfoDAOImpl(storage);

        assertEquals(2, dao.getCount());
    }
}

