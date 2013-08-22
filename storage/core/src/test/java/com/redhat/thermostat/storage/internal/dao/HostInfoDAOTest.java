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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.AggregateCount;
import com.redhat.thermostat.storage.model.HostInfo;


public class HostInfoDAOTest {

    static class Triple<S, T, U> {
        final S first;
        final T second;
        final U third;

        public Triple(S first, T second, U third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }
    }

    private static final String HOST_NAME = "a host name";
    private static final String OS_NAME = "some os";
    private static final String OS_KERNEL = "some kernel";
    private static final String CPU_MODEL = "some cpu that runs fast";
    private static final int CPU_NUM = -1;
    private static final long MEMORY_TOTAL = 0xCAFEBABEl;

    @Test
    public void preparedQueryDescriptorsAreSane() {
        String expectedHostInfo = "QUERY host-info WHERE 'agentId' = ?s LIMIT 1";
        assertEquals(expectedHostInfo, HostInfoDAOImpl.QUERY_HOST_INFO);
        String expectedAllHosts = "QUERY host-info";
        assertEquals(expectedAllHosts, HostInfoDAOImpl.QUERY_ALL_HOSTS);
        String aggregateAllHosts = "QUERY-COUNT host-info";
        assertEquals(aggregateAllHosts, HostInfoDAOImpl.AGGREGATE_COUNT_ALL_HOSTS);
    }
    
    @Test
    public void testCategory() {
        assertEquals("host-info", HostInfoDAO.hostInfoCategory.getName());
        Collection<Key<?>> keys = HostInfoDAO.hostInfoCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agentId")));
        assertTrue(keys.contains(new Key<String>("hostname")));
        assertTrue(keys.contains(new Key<String>("osName")));
        assertTrue(keys.contains(new Key<String>("osKernel")));
        assertTrue(keys.contains(new Key<String>("cpuModel")));
        assertTrue(keys.contains(new Key<Integer>("cpuCount")));
        assertTrue(keys.contains(new Key<Long>("totalMemory")));
        assertEquals(7, keys.size());
    }

    @Test
    public void testGetHostInfo() throws DescriptorParsingException, StatementExecutionException {
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<HostInfo> prepared = (PreparedStatement<HostInfo>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(prepared);

        HostInfo info = new HostInfo(HOST_NAME, OS_NAME, OS_KERNEL, CPU_MODEL, CPU_NUM, MEMORY_TOTAL);
        @SuppressWarnings("unchecked")
        Cursor<HostInfo> cursor = (Cursor<HostInfo>) mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(info).thenReturn(null);
        when(prepared.executeQuery()).thenReturn(cursor);
        AgentInfoDAO agentInfoDao = mock(AgentInfoDAO.class);

        HostInfo result = new HostInfoDAOImpl(storage, agentInfoDao).getHostInfo(new HostRef("some uid", HOST_NAME));
        
        verify(storage).prepareStatement(anyDescriptor());
        verify(prepared).setString(0, "some uid");
        verify(prepared).executeQuery();
        assertSame(result, info);
    }
    
    @SuppressWarnings("unchecked")
    private StatementDescriptor<HostInfo> anyDescriptor() {
        return (StatementDescriptor<HostInfo>) any(StatementDescriptor.class);
    }

    @Test
    public void testGetHostsSingleHost() throws DescriptorParsingException, StatementExecutionException {

        Storage storage = setupStorageForSingleHost();
        AgentInfoDAO agentInfo = mock(AgentInfoDAO.class);

        HostInfoDAO hostsDAO = new HostInfoDAOImpl(storage, agentInfo);
        Collection<HostRef> hosts = hostsDAO.getHosts();

        assertEquals(1, hosts.size());
        assertTrue(hosts.contains(new HostRef("123", "fluffhost1")));
    }

    private Storage setupStorageForSingleHost() throws DescriptorParsingException, StatementExecutionException {
        HostInfo hostConfig = new HostInfo("fluffhost1", OS_NAME, OS_KERNEL, CPU_MODEL, CPU_NUM, MEMORY_TOTAL);
        hostConfig.setAgentId("123");

        @SuppressWarnings("unchecked")
        Cursor<HostInfo> cursor = (Cursor<HostInfo>) mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(hostConfig);

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<HostInfo> stmt = (PreparedStatement<HostInfo>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(cursor);
        return storage;
    }

    @Test
    public void testGetHosts3Hosts() throws DescriptorParsingException, StatementExecutionException {

        Storage storage = setupStorageFor3Hosts();
        AgentInfoDAO agentInfo = mock(AgentInfoDAO.class);

        HostInfoDAO hostsDAO = new HostInfoDAOImpl(storage, agentInfo);
        Collection<HostRef> hosts = hostsDAO.getHosts();

        assertEquals(3, hosts.size());
        assertTrue(hosts.contains(new HostRef("123", "fluffhost1")));
        assertTrue(hosts.contains(new HostRef("456", "fluffhost2")));
        assertTrue(hosts.contains(new HostRef("789", "fluffhost3")));
    }

    private Storage setupStorageFor3Hosts() throws DescriptorParsingException, StatementExecutionException {

        HostInfo hostConfig1 = new HostInfo("fluffhost1", OS_NAME, OS_KERNEL, CPU_MODEL, CPU_NUM, MEMORY_TOTAL);
        hostConfig1.setAgentId("123");
        HostInfo hostConfig2 = new HostInfo("fluffhost2", OS_NAME, OS_KERNEL, CPU_MODEL, CPU_NUM, MEMORY_TOTAL);
        hostConfig2.setAgentId("456");
        HostInfo hostConfig3 = new HostInfo("fluffhost3", OS_NAME, OS_KERNEL, CPU_MODEL, CPU_NUM, MEMORY_TOTAL);
        hostConfig3.setAgentId("789");


        @SuppressWarnings("unchecked")
        Cursor<HostInfo> cursor = (Cursor<HostInfo>) mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(hostConfig1).thenReturn(hostConfig2).thenReturn(hostConfig3);

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<HostInfo> stmt = (PreparedStatement<HostInfo>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(cursor);
        
        return storage;
    }

    @Test
    public void testPutHostInfo() {
        Storage storage = mock(Storage.class);
        Add add = mock(Add.class);
        when(storage.createAdd(any(Category.class))).thenReturn(add);

        AgentInfoDAO agentInfo = mock(AgentInfoDAO.class);

        HostInfo info = new HostInfo(HOST_NAME, OS_NAME, OS_KERNEL, CPU_MODEL, CPU_NUM, MEMORY_TOTAL);
        HostInfoDAO dao = new HostInfoDAOImpl(storage, agentInfo);
        dao.putHostInfo(info);

        verify(storage).createAdd(HostInfoDAO.hostInfoCategory);
        verify(add).setPojo(info);
        verify(add).apply();
    }

    @Test
    public void testGetCount() throws DescriptorParsingException,
            StatementExecutionException {
        AggregateCount count = new AggregateCount();
        count.setCount(2);

        @SuppressWarnings("unchecked")
        Cursor<AggregateCount> countCursor = (Cursor<AggregateCount>) mock(Cursor.class);
        when(countCursor.next()).thenReturn(count);

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<AggregateCount> stmt = (PreparedStatement<AggregateCount>) mock(PreparedStatement.class);
        @SuppressWarnings("unchecked")
        StatementDescriptor<AggregateCount> desc = any(StatementDescriptor.class);
        when(storage.prepareStatement(desc)).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(countCursor);
        HostInfoDAOImpl dao = new HostInfoDAOImpl(storage, null);

        assertEquals(2, dao.getCount());
    }
    
    @Test
    public void getAliveHostSingle() throws DescriptorParsingException, StatementExecutionException {
        Triple<Storage, AgentInfoDAO, PreparedStatement<HostInfo>> setup = setupForSingleAliveHost();
        Storage storage = setup.first;
        AgentInfoDAO agentInfoDao = setup.second;
        PreparedStatement<HostInfo> stmt = setup.third;

        HostInfoDAO hostsDAO = new HostInfoDAOImpl(storage, agentInfoDao);
        Collection<HostRef> hosts = hostsDAO.getAliveHosts();

        assertEquals(1, hosts.size());
        assertTrue(hosts.contains(new HostRef("123", "fluffhost1")));
        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, "123");
        verify(stmt).executeQuery();
    }
    
    @Test
    public void getAliveHostsEmptyDueToHostInfoBeingNull() throws DescriptorParsingException, StatementExecutionException {
        Triple<Storage, AgentInfoDAO, PreparedStatement<HostInfo>> setup = setupForNullHostInfo();
        Storage storage = setup.first;
        AgentInfoDAO agentInfoDao = setup.second;
        PreparedStatement<HostInfo> stmt = setup.third;

        HostInfoDAO hostsDAO = new HostInfoDAOImpl(storage, agentInfoDao);
        Collection<HostRef> hosts = hostsDAO.getAliveHosts();

        assertEquals(0, hosts.size());
        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, "123");
        verify(stmt).executeQuery();
    }
    
    private Triple<Storage, AgentInfoDAO, PreparedStatement<HostInfo>> setupForSingleAliveHost()
            throws DescriptorParsingException, StatementExecutionException {
        
        // agents

        AgentInformation agentInfo1 = new AgentInformation();
        agentInfo1.setAgentId("123");
        agentInfo1.setAlive(true);
        
        // hosts
        
        HostInfo hostConfig1 = new HostInfo();
        hostConfig1.setHostname("fluffhost1");
        hostConfig1.setAgentId("123");
        
        HostInfo hostConfig2 = new HostInfo();
        hostConfig2.setHostname("fluffhost2");
        hostConfig2.setAgentId("456");
        
        // cursor

        @SuppressWarnings("unchecked")
        Cursor<HostInfo> cursor1 = mock(Cursor.class);
        when(cursor1.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor1.next()).thenReturn(hostConfig1);

        // storage
        
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<HostInfo> stmt = (PreparedStatement<HostInfo>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(cursor1);

        AgentInfoDAO agentDao = mock(AgentInfoDAO.class);
        when(agentDao.getAliveAgents()).thenReturn(Arrays.asList(agentInfo1));

        return new Triple<>(storage, agentDao, stmt);
    }
    
    private Triple<Storage, AgentInfoDAO, PreparedStatement<HostInfo>> setupForNullHostInfo()
            throws DescriptorParsingException, StatementExecutionException {
        
        // agents

        AgentInformation agentInfo1 = new AgentInformation();
        agentInfo1.setAgentId("123");
        agentInfo1.setAlive(true);
        
        // cursor

        @SuppressWarnings("unchecked")
        Cursor<HostInfo> cursor1 = mock(Cursor.class);
        when(cursor1.hasNext()).thenReturn(false);

        // storage
        
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<HostInfo> stmt = (PreparedStatement<HostInfo>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(cursor1);

        AgentInfoDAO agentDao = mock(AgentInfoDAO.class);
        when(agentDao.getAliveAgents()).thenReturn(Arrays.asList(agentInfo1));

        return new Triple<>(storage, agentDao, stmt);
    }

    @Test
    public void getAliveHost3() throws DescriptorParsingException, StatementExecutionException {
        Triple<Storage, AgentInfoDAO, PreparedStatement<HostInfo>> setup = setupForAliveHost3();
        Storage storage = setup.first;
        AgentInfoDAO agentInfoDao = setup.second;
        PreparedStatement<HostInfo> stmt = setup.third;

        HostInfoDAO hostsDAO = new HostInfoDAOImpl(storage, agentInfoDao);
        Collection<HostRef> hosts = hostsDAO.getAliveHosts();

        // cursor 3 from the above storage should not be used
        assertEquals(3, hosts.size());
        assertTrue(hosts.contains(new HostRef("123", "fluffhost1")));
        assertTrue(hosts.contains(new HostRef("456", "fluffhost2")));
        assertTrue(hosts.contains(new HostRef("678", "fluffhost3")));
        verify(storage, atLeast(3)).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, "123");
        verify(stmt).setString(0, "456");
        verify(stmt).setString(0, "678");
        verify(stmt, atLeast(3)).executeQuery();
    }
    
    private Triple<Storage, AgentInfoDAO, PreparedStatement<HostInfo>> setupForAliveHost3()
            throws DescriptorParsingException, StatementExecutionException {
        
        // agents
        AgentInformation agentInfo1 = new AgentInformation();
        agentInfo1.setAgentId("123");
        agentInfo1.setAlive(true);

        AgentInformation agentInfo2 = new AgentInformation();
        agentInfo2.setAgentId("456");
        agentInfo2.setAlive(true);

        AgentInformation agentInfo3 = new AgentInformation();
        agentInfo3.setAgentId("678");
        agentInfo3.setAlive(true);
        
        // hosts
        
        HostInfo hostConfig1 = new HostInfo();
        hostConfig1.setHostname("fluffhost1");
        hostConfig1.setAgentId("123");

        HostInfo hostConfig2 = new HostInfo();
        hostConfig2.setHostname("fluffhost2");
        hostConfig2.setAgentId("456");

        HostInfo hostConfig3 = new HostInfo();
        hostConfig3.setHostname("fluffhost3");
        hostConfig3.setAgentId("678");

        @SuppressWarnings("unchecked")
        Cursor<HostInfo> cursor1 = mock(Cursor.class);
        when(cursor1.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor1.next()).thenReturn(hostConfig1);

        @SuppressWarnings("unchecked")
        Cursor<HostInfo> cursor2 = mock(Cursor.class);
        when(cursor2.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor2.next()).thenReturn(hostConfig2);

        @SuppressWarnings("unchecked")
        Cursor<HostInfo> cursor3 = mock(Cursor.class);
        when(cursor3.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor3.next()).thenReturn(hostConfig3);

        // storage
        
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<HostInfo> stmt = (PreparedStatement<HostInfo>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(cursor1).thenReturn(cursor2).thenReturn(cursor3);
        
        AgentInfoDAO agentDao = mock(AgentInfoDAO.class);
        when(agentDao.getAliveAgents()).thenReturn(Arrays.asList(agentInfo1, agentInfo2, agentInfo3));

        return new Triple<>(storage, agentDao, stmt);
    }
}

