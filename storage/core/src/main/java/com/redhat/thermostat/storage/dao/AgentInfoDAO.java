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

package com.redhat.thermostat.storage.dao;

import java.util.List;

import com.redhat.thermostat.annotations.Service;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Countable;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.model.AgentInformation;

/**
 * Access information about agents that agents publish to storage.
 */
@Service
public interface AgentInfoDAO extends Countable {

    static final Key<Long> START_TIME_KEY = new Key<>("startTime", false);
    static final Key<Long> STOP_TIME_KEY = new Key<>("stopTime", false);
    static final Key<Boolean> ALIVE_KEY = new Key<>("alive", false);
    static final Key<String> CONFIG_LISTEN_ADDRESS = new Key<>("configListenAddress", false);

    static final Category<AgentInformation> CATEGORY = new Category<>("agent-config", AgentInformation.class,
            Key.AGENT_ID,
            START_TIME_KEY,
            STOP_TIME_KEY,
            ALIVE_KEY,
            CONFIG_LISTEN_ADDRESS);

    /**
     * Get information about all known agents.
     *
     * @return a {@link List} of {@link AgentInformation} for all agents
     * who have published their information. Will be empty if there is no
     * information.
     */
    List<AgentInformation> getAllAgentInformation();

    /**
     * Get information about all alive agents.
     *
     * @return a {@link List} of {@link AgentInformation} for all alive
     * agents who have published their information. Will be empty if there
     * is no information or no alive agents.
     */
    List<AgentInformation> getAliveAgents();

    /**
     * Get information about a specific agent.
     *
     * @return a {@link AgentInformation} describing information about the agent
     * indicated by {@code agentRef}. {@code null} if no information about the
     * agent could be located.
     */
    AgentInformation getAgentInformation(HostRef agentRef);

    /**
     * Publish information about agent into the storage.
     */
    void addAgentInformation(AgentInformation agentInfo);

    /**
     * Update information about an existing agent. No changes will be performed
     * if there is no matching agent.
     */
    void updateAgentInformation(AgentInformation agentInfo);

    /**
     * Remove information about an agent that was published to storage.
     */
    void removeAgentInformation(AgentInformation agentInfo);

}
