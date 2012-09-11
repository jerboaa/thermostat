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

import java.util.ArrayList;
import java.util.List;

import com.redhat.thermostat.common.model.AgentInformation;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Query;
import com.redhat.thermostat.common.storage.Query.Criteria;
import com.redhat.thermostat.common.storage.Storage;

public class AgentInfoDAOImpl implements AgentInfoDAO {

    private final Storage storage;
    private final AgentInfoConverter converter = new AgentInfoConverter();

    public AgentInfoDAOImpl(Storage storage) {
        this.storage = storage;
        storage.createConnectionKey(CATEGORY);
    }

    @Override
    public long getCount() {
        return storage.getCount(CATEGORY);
    }

    @Override
    public List<AgentInformation> getAllAgentInformation() {
        Cursor agentCursor = storage.findAllFromCategory(CATEGORY);

        List<AgentInformation> results = new ArrayList<>();

        while (agentCursor.hasNext()) {
            Chunk agentChunk = agentCursor.next();
            results.add(converter.fromChunk(agentChunk));
        }
        return results;
    }

    @Override
    public List<AgentInformation> getAliveAgents() {
        Query query = storage.createQuery()
                .from(CATEGORY)
                .where(AgentInfoDAO.ALIVE_KEY, Criteria.EQUALS, true);

        Cursor agentCursor = storage.findAll(query);

        List<AgentInformation> results = new ArrayList<>();

        while (agentCursor.hasNext()) {
            Chunk agentChunk = agentCursor.next();
            results.add(converter.fromChunk(agentChunk));
        }
        return results;
    }

    @Override
    public AgentInformation getAgentInformation(HostRef agentRef) {
        Query query = storage.createQuery()
                .from(CATEGORY)
                .where(Key.AGENT_ID, Criteria.EQUALS, agentRef.getAgentId());

        return storage.findPojo(query, AgentInformation.class);
    }

    @Override
    public void addAgentInformation(AgentInformation agentInfo) {
        storage.putPojo(AgentInfoDAO.CATEGORY, true, agentInfo);
    }

    @Override
    public void removeAgentInformation(AgentInformation agentInfo) {
        Chunk chunk = new Chunk(CATEGORY, true);
        chunk.put(Key.AGENT_ID, agentInfo.getAgentId());

        storage.removeChunk(chunk);
    }

    @Override
    public void updateAgentInformation(AgentInformation agentInfo) {
        storage.updateChunk(converter.toChunk(agentInfo));
    }

}
