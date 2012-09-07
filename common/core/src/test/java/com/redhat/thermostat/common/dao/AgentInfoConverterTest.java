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

import org.junit.Test;

import com.redhat.thermostat.common.model.AgentInformation;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;

public class AgentInfoConverterTest {

    @Test
    public void testFromChunk() {
        final String AGENT_ID = "12345";
        final boolean ALIVE = true;
        final long START_TIME = 1234;
        final long STOP_TIME = 5678;
        final String CONFIG_ADDRESS = "foobar:666";

        Chunk agentInfoChunk = new Chunk(AgentInfoDAO.CATEGORY, true);
        agentInfoChunk.put(Key.AGENT_ID, AGENT_ID);
        agentInfoChunk.put(AgentInfoDAO.ALIVE_KEY, ALIVE);
        agentInfoChunk.put(AgentInfoDAO.START_TIME_KEY, START_TIME);
        agentInfoChunk.put(AgentInfoDAO.STOP_TIME_KEY, STOP_TIME);
        agentInfoChunk.put(AgentInfoDAO.CONFIG_LISTEN_ADDRESS, CONFIG_ADDRESS);

        AgentInfoConverter converter = new AgentInfoConverter();
        AgentInformation info = converter.fromChunk(agentInfoChunk);

        assertEquals(AGENT_ID, info.getAgentId());
        assertEquals(ALIVE, info.isAlive());
        assertEquals(START_TIME, info.getStartTime());
        assertEquals(STOP_TIME, info.getStopTime());
    }

    @Test
    public void testToChunk() {
        final String AGENT_ID = "12345";
        final boolean ALIVE = true;
        final long START_TIME = 1234;
        final long STOP_TIME = 5678;
        final String CONFIG_ADDRESS = "localhost:666";

        AgentInformation agentInfo = new AgentInformation();
        agentInfo.setAgentId(AGENT_ID);
        agentInfo.setAlive(ALIVE);
        agentInfo.setConfigListenAddress(CONFIG_ADDRESS);
        agentInfo.setStartTime(START_TIME);
        agentInfo.setStopTime(STOP_TIME);

        AgentInfoConverter converter = new AgentInfoConverter();
        Chunk chunk = converter.toChunk(agentInfo);

        assertEquals(AgentInfoDAO.CATEGORY, chunk.getCategory());
        assertEquals(AGENT_ID, chunk.get(Key.AGENT_ID));
        assertEquals(ALIVE, chunk.get(AgentInfoDAO.ALIVE_KEY));
        assertEquals((Long) START_TIME, chunk.get(AgentInfoDAO.START_TIME_KEY));
        assertEquals((Long) STOP_TIME, chunk.get(AgentInfoDAO.STOP_TIME_KEY));
        assertEquals((String) CONFIG_ADDRESS, chunk.get(AgentInfoDAO.CONFIG_LISTEN_ADDRESS));

    }

}
