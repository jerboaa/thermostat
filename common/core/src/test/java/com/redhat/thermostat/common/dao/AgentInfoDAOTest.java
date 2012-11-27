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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.core.Query.Criteria;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.test.MockQuery;

public class AgentInfoDAOTest {

    private AgentInformation agentInfo1;
    private AgentInformation agent1;

    @Before
    public void setUp() {
        agentInfo1 = new AgentInformation();
        agentInfo1.setAgentId("1234");
        agentInfo1.setAlive(true);
        agentInfo1.setConfigListenAddress("foobar:666");
        agentInfo1.setStartTime(100);
        agentInfo1.setStopTime(10);

        agent1 = new AgentInformation();
        agent1.setAgentId("1234");
        agent1.setAlive(true);
        agent1.setConfigListenAddress("foobar:666");
        agent1.setStartTime(100);
        agent1.setStopTime(10);
    }

    @Test
    public void verifyCategoryName() {
        Category category = AgentInfoDAO.CATEGORY;
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
    public void verifyGetAllAgentInformationWithOneAgentInStorage() {
        @SuppressWarnings("unchecked")
        Cursor<AgentInformation> agentCursor = mock(Cursor.class);
        when(agentCursor.hasNext()).thenReturn(true).thenReturn(false);
        when(agentCursor.next()).thenReturn(agent1).thenReturn(null);

        Storage storage = mock(Storage.class);
        when(storage.findAllPojos(any(Query.class), same(AgentInformation.class))).thenReturn(agentCursor);
        MockQuery query = new MockQuery();
        when(storage.createQuery()).thenReturn(query);
        AgentInfoDAOImpl dao = new AgentInfoDAOImpl(storage);

        List<AgentInformation> allAgentInfo = dao.getAllAgentInformation();

        assertEquals(1, allAgentInfo.size());

        AgentInformation result = allAgentInfo.get(0);
        AgentInformation expected = agentInfo1;
        assertEquals(expected, result);
    }

    @Test
    public void verifyGetAliveAgent() {
        @SuppressWarnings("unchecked")
        Cursor<AgentInformation> agentCursor = mock(Cursor.class);
        when(agentCursor.hasNext()).thenReturn(true).thenReturn(false);
        when(agentCursor.next()).thenReturn(agent1).thenReturn(null);

        MockQuery query = new MockQuery();
        Storage storage = mock(Storage.class);
        when(storage.createQuery()).thenReturn(query);
        when(storage.findAllPojos(query, AgentInformation.class)).thenReturn(agentCursor);

        AgentInfoDAO dao = new AgentInfoDAOImpl(storage);
        List<AgentInformation> aliveAgents = dao.getAliveAgents();

        assertEquals(AgentInfoDAO.CATEGORY, query.getCategory());
        assertTrue(query.hasWhereClause(AgentInfoDAO.ALIVE_KEY, Criteria.EQUALS, true));

        assertEquals(1, aliveAgents.size());

        AgentInformation result = aliveAgents.get(0);
        AgentInformation expected = agentInfo1;
        assertEquals(expected, result);
    }

    @Test
    public void verifyGetAgentInformationWhenStorageCantFindIt() {
        HostRef agentRef = mock(HostRef.class);

        MockQuery query = new MockQuery();
        Storage storage = mock(Storage.class);
        when(storage.createQuery()).thenReturn(query);

        AgentInfoDAO dao = new AgentInfoDAOImpl(storage);

        AgentInformation computed = dao.getAgentInformation(agentRef);

        assertEquals(null, computed);
    }

    @Test
    public void verifyGetAgentInformation() {
        HostRef agentRef = mock(HostRef.class);
        when(agentRef.getAgentId()).thenReturn(agentInfo1.getAgentId());

        Storage storage = mock(Storage.class);
        MockQuery query = new MockQuery();
        when(storage.createQuery()).thenReturn(query);
        when(storage.findPojo(query, AgentInformation.class)).thenReturn(agentInfo1);
        AgentInfoDAO dao = new AgentInfoDAOImpl(storage);

        AgentInformation computed = dao.getAgentInformation(agentRef);

        assertEquals(AgentInfoDAO.CATEGORY, query.getCategory());
        assertTrue(query.hasWhereClause(Key.AGENT_ID, Criteria.EQUALS, agentInfo1.getAgentId()));

        AgentInformation expected = agentInfo1;
        assertSame(expected, computed);
    }

    @Test
    public void verifyAddAgentInformation() {
        Storage storage = mock(Storage.class);
        AgentInfoDAO dao = new AgentInfoDAOImpl(storage);

        dao.addAgentInformation(agentInfo1);

        verify(storage).putPojo(AgentInfoDAO.CATEGORY, true, agentInfo1);

    }

    @Test
    public void verifyUpdateAgentInformation() {

        Update mockUpdate = QueryTestHelper.createMockUpdate();
        Storage storage = mock(Storage.class);
        when(storage.createUpdate()).thenReturn(mockUpdate);
        AgentInfoDAO dao = new AgentInfoDAOImpl(storage);

        dao.updateAgentInformation(agentInfo1);

        verify(mockUpdate).from(AgentInfoDAO.CATEGORY);
        verify(mockUpdate).where(Key.AGENT_ID, "1234");
        verify(mockUpdate).set(AgentInfoDAO.START_TIME_KEY, 100L);
        verify(mockUpdate).set(AgentInfoDAO.STOP_TIME_KEY, 10L);
        verify(mockUpdate).set(AgentInfoDAO.CONFIG_LISTEN_ADDRESS, "foobar:666");
        verify(mockUpdate).set(AgentInfoDAO.ALIVE_KEY, true);
        verifyNoMoreInteractions(mockUpdate);
        verify(storage).updatePojo(mockUpdate);

    }

    @Test
    public void verifyRemoveAgentInformation() {
        Remove mockRemove = QueryTestHelper.createMockRemove();
        Storage storage = mock(Storage.class);
        when(storage.createRemove()).thenReturn(mockRemove);
        AgentInfoDAO dao = new AgentInfoDAOImpl(storage);

        dao.removeAgentInformation(agentInfo1);

        verify(storage).removePojo(mockRemove);
        verify(mockRemove).from(AgentInfoDAO.CATEGORY);
        verify(mockRemove).where(Key.AGENT_ID, "1234");
    }

}
