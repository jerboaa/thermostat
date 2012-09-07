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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.model.AgentInformation;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.common.storage.Query.Criteria;
import com.redhat.thermostat.test.MockQuery;

public class AgentInfoDAOTest {

    private AgentInformation agentInfo1;
    private Chunk agentChunk1;

    @Before
    public void setUp() {
        agentInfo1 = new AgentInformation();
        agentInfo1.setAgentId("1234");
        agentInfo1.setAlive(true);
        agentInfo1.setConfigListenAddress("foobar:666");
        agentInfo1.setStartTime(100);
        agentInfo1.setStopTime(10);

        agentChunk1 = new AgentInfoConverter().toChunk(agentInfo1);
    }

    @Test
    public void verifyCategoryName() {
        Category category = AgentInfoDAO.CATEGORY;
        assertEquals("agent-config", category.getName());
    }

    @Test
    public void verifyKeyNames() {
        assertEquals("agent-id", Key.AGENT_ID.getName());
        assertEquals("alive", AgentInfoDAO.ALIVE_KEY.getName());
        assertEquals("start-time", AgentInfoDAO.START_TIME_KEY.getName());
        assertEquals("stop-time", AgentInfoDAO.STOP_TIME_KEY.getName());
        assertEquals("config-listen-address", AgentInfoDAO.CONFIG_LISTEN_ADDRESS.getName());
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
        Cursor agentCursor = mock(Cursor.class);
        when(agentCursor.hasNext()).thenReturn(true).thenReturn(false);
        when(agentCursor.next()).thenReturn(agentChunk1).thenReturn(null);

        Storage storage = mock(Storage.class);
        when(storage.findAllFromCategory(AgentInfoDAO.CATEGORY)).thenReturn(agentCursor);

        AgentInfoDAOImpl dao = new AgentInfoDAOImpl(storage);

        List<AgentInformation> allAgentInfo = dao.getAllAgentInformation();

        assertEquals(1, allAgentInfo.size());

        AgentInformation result = allAgentInfo.get(0);
        AgentInformation expected = agentInfo1;
        assertEquals(expected, result);
    }

    @Test
    public void verifyGetAliveAgent() {
        Cursor agentCursor = mock(Cursor.class);
        when(agentCursor.hasNext()).thenReturn(true).thenReturn(false);
        when(agentCursor.next()).thenReturn(agentChunk1).thenReturn(null);

        MockQuery query = new MockQuery();
        Storage storage = mock(Storage.class);
        when(storage.createQuery()).thenReturn(query);
        when(storage.findAll(query)).thenReturn(agentCursor);

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
        when(storage.find(query)).thenReturn(agentChunk1);
        AgentInfoDAO dao = new AgentInfoDAOImpl(storage);

        AgentInformation computed = dao.getAgentInformation(agentRef);

        assertEquals(AgentInfoDAO.CATEGORY, query.getCategory());
        assertTrue(query.hasWhereClause(Key.AGENT_ID, Criteria.EQUALS, agentInfo1.getAgentId()));

        AgentInformation expected = agentInfo1;
        assertEquals(expected, computed);
    }

    @Test
    public void verifyAddAgentInformation() {
        Storage storage = mock(Storage.class);
        AgentInfoDAO dao = new AgentInfoDAOImpl(storage);

        dao.addAgentInformation(agentInfo1);

        ArgumentCaptor<Chunk> chunkCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(storage).putChunk(chunkCaptor.capture());

        Chunk insertedChunk = chunkCaptor.getValue();
        Chunk expectedChunk = agentChunk1;

        assertEquals(expectedChunk, insertedChunk);
    }

    @Test
    public void verifyUpdateAgentInformation() {
        Storage storage = mock(Storage.class);
        AgentInfoDAO dao = new AgentInfoDAOImpl(storage);

        dao.updateAgentInformation(agentInfo1);

        ArgumentCaptor<Chunk> chunkCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(storage).updateChunk(chunkCaptor.capture());

        Chunk updatedChunk = chunkCaptor.getValue();
        Chunk expectedChunk = agentChunk1;

        assertEquals(expectedChunk, updatedChunk);
    }

    @Test
    public void verifyRemoveAgentInformation() {
        Storage storage = mock(Storage.class);
        AgentInfoDAO dao = new AgentInfoDAOImpl(storage);

        dao.removeAgentInformation(agentInfo1);

        ArgumentCaptor<Chunk> queryCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(storage).removeChunk(queryCaptor.capture());

        Chunk removeQuery = queryCaptor.getValue();
        Chunk expectedQuery = new Chunk(AgentInfoDAO.CATEGORY, true);
        expectedQuery.put(Key.AGENT_ID, agentInfo1.getAgentId());

        assertEquals(expectedQuery, removeQuery);
    }

}
