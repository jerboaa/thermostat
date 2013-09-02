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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.AggregateCount;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;

public class AgentInfoDAOTest {

    private AgentInformation agentInfo1;
    private AgentInformation agent1;
    private ExpressionFactory factory;

    @Before
    public void setUp() {
        agentInfo1 = new AgentInformation("1234");
        agentInfo1.setAlive(true);
        agentInfo1.setConfigListenAddress("foobar:666");
        agentInfo1.setStartTime(100);
        agentInfo1.setStopTime(10);

        agent1 = new AgentInformation("1234");
        agent1.setAlive(true);
        agent1.setConfigListenAddress("foobar:666");
        agent1.setStartTime(100);
        agent1.setStopTime(10);
        factory = new ExpressionFactory();
    }

    @Test
    public void preparedQueryDescriptorsAreSane() {
        String expectedAgentInfo = "QUERY agent-config WHERE 'agentId' = ?s";
        assertEquals(expectedAgentInfo, AgentInfoDAOImpl.QUERY_AGENT_INFO);
        String expectedAllAgents = "QUERY agent-config";
        assertEquals(expectedAllAgents, AgentInfoDAOImpl.QUERY_ALL_AGENTS);
        String expectedAliveAgents = "QUERY agent-config WHERE 'alive' = ?b";
        assertEquals(expectedAliveAgents, AgentInfoDAOImpl.QUERY_ALIVE_AGENTS);
        String aggregateAllAgents = "QUERY-COUNT agent-config";
        assertEquals(aggregateAllAgents, AgentInfoDAOImpl.AGGREGATE_COUNT_ALL_AGENTS);
    }
    
    @Test
    public void verifyCategoryName() {
        Category<AgentInformation> category = AgentInfoDAO.CATEGORY;
        assertEquals("agent-config", category.getName());
    }

    @Test
    public void verifyKeyNames() {
        assertEquals("agentId", Key.AGENT_ID.getName());
        assertEquals("alive", AgentInfoDAO.ALIVE_KEY.getName());
        assertEquals("startTime", AgentInfoDAO.START_TIME_KEY.getName());
        assertEquals("stopTime", AgentInfoDAO.STOP_TIME_KEY.getName());
        assertEquals("configListenAddress", AgentInfoDAO.CONFIG_LISTEN_ADDRESS.getName());
    }

    @Test
    public void verifyCategoryHasAllKeys() {
        Collection<Key<?>> keys = AgentInfoDAO.CATEGORY.getKeys();

        assertTrue(keys.contains(Key.AGENT_ID));
        assertTrue(keys.contains(AgentInfoDAO.ALIVE_KEY));
        assertTrue(keys.contains(AgentInfoDAO.START_TIME_KEY));
        assertTrue(keys.contains(AgentInfoDAO.STOP_TIME_KEY));
        assertTrue(keys.contains(AgentInfoDAO.CONFIG_LISTEN_ADDRESS));
    }

    @Test
    public void verifyGetAllAgentInformationWithOneAgentInStorage()
            throws DescriptorParsingException, StatementExecutionException {
        @SuppressWarnings("unchecked")
        Cursor<AgentInformation> agentCursor = (Cursor<AgentInformation>) mock(Cursor.class);
        when(agentCursor.hasNext()).thenReturn(true).thenReturn(false);
        when(agentCursor.next()).thenReturn(agent1).thenReturn(null);

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<AgentInformation> stmt = (PreparedStatement<AgentInformation>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(agentCursor);
        AgentInfoDAOImpl dao = new AgentInfoDAOImpl(storage);

        List<AgentInformation> allAgentInfo = dao.getAllAgentInformation();

        assertEquals(1, allAgentInfo.size());

        AgentInformation result = allAgentInfo.get(0);
        AgentInformation expected = agentInfo1;
        assertEquals(expected, result);
    }
    
    @Test
    public void testGetCount()
            throws DescriptorParsingException, StatementExecutionException {
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
        AgentInfoDAOImpl dao = new AgentInfoDAOImpl(storage);

        assertEquals(2, dao.getCount());
    }
    
    @SuppressWarnings("unchecked")
    private StatementDescriptor<AgentInformation> anyDescriptor() {
        return (StatementDescriptor<AgentInformation>) any(StatementDescriptor.class);
    }

    @Test
    public void verifyGetAliveAgent() throws DescriptorParsingException, StatementExecutionException {
        @SuppressWarnings("unchecked")
        Cursor<AgentInformation> agentCursor = (Cursor<AgentInformation>) mock(Cursor.class);
        when(agentCursor.hasNext()).thenReturn(true).thenReturn(false);
        when(agentCursor.next()).thenReturn(agent1).thenReturn(null);

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<AgentInformation> stmt = (PreparedStatement<AgentInformation>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(agentCursor);

        AgentInfoDAO dao = new AgentInfoDAOImpl(storage);
        List<AgentInformation> aliveAgents = dao.getAliveAgents();

        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).executeQuery();
        verify(stmt).setBoolean(0, true);
        verifyNoMoreInteractions(stmt);

        assertEquals(1, aliveAgents.size());

        AgentInformation result = aliveAgents.get(0);
        AgentInformation expected = agentInfo1;
        assertEquals(expected, result);
    }

    @Test
    public void verifyGetAgentInformationWhenStorageCantFindIt() throws DescriptorParsingException, StatementExecutionException {
        HostRef agentRef = mock(HostRef.class);

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<AgentInformation> stmt = (PreparedStatement<AgentInformation>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        @SuppressWarnings("unchecked")
        Cursor<AgentInformation> cursor = (Cursor<AgentInformation>) mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(false);
        when(cursor.next()).thenReturn(null);
        when(stmt.executeQuery()).thenReturn(cursor);

        AgentInfoDAO dao = new AgentInfoDAOImpl(storage);

        AgentInformation computed = dao.getAgentInformation(agentRef);

        assertEquals(null, computed);
    }

    @Test
    public void verifyGetAgentInformation() throws StatementExecutionException, DescriptorParsingException {
        HostRef agentRef = mock(HostRef.class);
        when(agentRef.getAgentId()).thenReturn(agentInfo1.getAgentId());

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<AgentInformation> stmt = (PreparedStatement<AgentInformation>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        @SuppressWarnings("unchecked")
        Cursor<AgentInformation> cursor = (Cursor<AgentInformation>) mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(agentInfo1).thenReturn(null);
        when(stmt.executeQuery()).thenReturn(cursor);
        AgentInfoDAO dao = new AgentInfoDAOImpl(storage);

        AgentInformation computed = dao.getAgentInformation(agentRef);

        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, agentInfo1.getAgentId());
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);
        AgentInformation expected = agentInfo1;
        assertSame(expected, computed);
    }

    @Test
    public void verifyAddAgentInformation() {
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        Add<AgentInformation> add = mock(Add.class);
        when(storage.createAdd(eq(AgentInfoDAO.CATEGORY))).thenReturn(add);

        AgentInfoDAO dao = new AgentInfoDAOImpl(storage);

        dao.addAgentInformation(agentInfo1);

        verify(storage).createAdd(AgentInfoDAO.CATEGORY);
        verify(add).setPojo(agentInfo1);
        verify(add).apply();
    }

    @Test
    public void verifyUpdateAgentInformation() {

        @SuppressWarnings("unchecked")
        Update<AgentInformation> mockUpdate = mock(Update.class);
        Storage storage = mock(Storage.class);
        when(storage.createUpdate(eq(AgentInfoDAO.CATEGORY))).thenReturn(mockUpdate);
        AgentInfoDAO dao = new AgentInfoDAOImpl(storage);

        dao.updateAgentInformation(agentInfo1);

        verify(storage).createUpdate(AgentInfoDAO.CATEGORY);
        Expression expr = factory.equalTo(Key.AGENT_ID, "1234");
        verify(mockUpdate).where(eq(expr));
        verify(mockUpdate).set(AgentInfoDAO.START_TIME_KEY, 100L);
        verify(mockUpdate).set(AgentInfoDAO.STOP_TIME_KEY, 10L);
        verify(mockUpdate).set(AgentInfoDAO.CONFIG_LISTEN_ADDRESS, "foobar:666");
        verify(mockUpdate).set(AgentInfoDAO.ALIVE_KEY, true);
        verify(mockUpdate).apply();
        verifyNoMoreInteractions(mockUpdate);

    }

    @Test
    public void verifyRemoveAgentInformation() {
        @SuppressWarnings("unchecked")
        Remove<AgentInformation> mockRemove = mock(Remove.class);
        Storage storage = mock(Storage.class);
        when(storage.createRemove(eq(AgentInfoDAO.CATEGORY))).thenReturn(mockRemove);
        AgentInfoDAO dao = new AgentInfoDAOImpl(storage);

        dao.removeAgentInformation(agentInfo1);

        verify(mockRemove).apply();
        Expression expr = factory.equalTo(Key.AGENT_ID, "1234");
        verify(mockRemove).where(eq(expr));
    }

}

