/*
 * Copyright 2012-2016 Red Hat, Inc.
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.CategoryAdapter;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.AbstractDaoQuery;
import com.redhat.thermostat.storage.dao.AbstractDaoStatement;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.SimpleDaoQuery;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.AggregateCount;

public class AgentInfoDAOImpl extends BaseCountable implements AgentInfoDAO {

    private static final Logger logger = LoggingUtils.getLogger(AgentInfoDAOImpl.class);
    static final String QUERY_AGENT_INFO = "QUERY "
            + CATEGORY.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s";
    static final String QUERY_ALL_AGENTS = "QUERY "
            + CATEGORY.getName();
    // We can use AgentInfoDAO.CATEGORY.getName() here since this query
    // only changes the data class. When executed we use the adapted
    // aggregate category.
    static final String AGGREGATE_COUNT_ALL_AGENTS = "QUERY-COUNT "
            + CATEGORY.getName();
    static final String QUERY_ALIVE_AGENTS = "QUERY "
            + CATEGORY.getName() + " WHERE '"
            + ALIVE_KEY.getName() + "' = ?b";

    // ADD agent-config SET
    //                     'agentId' = ?s , \
    //                     'startTime' = ?l , \
    //                     'stopTime' = ?l , \
    //                     'alive' = ?b , \
    //                     'configListenAddress' = ?s
    static final String DESC_ADD_AGENT_INFO = "ADD " + CATEGORY.getName() + " SET " +
            "'" + Key.AGENT_ID.getName() + "' = ?s , " +
            "'" + START_TIME_KEY.getName() + "' = ?l , " +
            "'" + STOP_TIME_KEY.getName() + "' = ?l , " +
            "'" + ALIVE_KEY.getName() + "' = ?b , " +
            "'" + CONFIG_LISTEN_ADDRESS.getName() + "' = ?s";
    // REMOVE agent-config WHERE 'agentId' = ?s
    static final String DESC_REMOVE_AGENT_INFO = "REMOVE " + CATEGORY.getName() +
            " WHERE '" + Key.AGENT_ID.getName() + "' = ?s";
    // UPDATE agent-config SET
    //                       'startTime' = ?l , \
    //                       'stopTime' = ?l , \
    //                       'alive' = ?b , \
    //                       'configListenAddress' = ?s
    //                     WHERE 'agentId' = ?s
    static final String DESC_UPDATE_AGENT_INFO = "UPDATE " + CATEGORY.getName() + " SET " +
            "'" + START_TIME_KEY.getName() + "' = ?l , " +
            "'" + STOP_TIME_KEY.getName() + "' = ?l , " +
            "'" + ALIVE_KEY.getName() + "' = ?b , " +
            "'" + CONFIG_LISTEN_ADDRESS.getName() + "' = ?s " +
            "WHERE '" + Key.AGENT_ID.getName() + "' = ?s";


    private final Storage storage;
    private final Category<AggregateCount> aggregateCategory;

    public AgentInfoDAOImpl(Storage storage) {
        this.storage = storage;
        CategoryAdapter<AgentInformation, AggregateCount> adapter = new CategoryAdapter<>(CATEGORY);
        this.aggregateCategory = adapter.getAdapted(AggregateCount.class);
        storage.registerCategory(CATEGORY);
        storage.registerCategory(aggregateCategory);
    }

    @Override
    public long getCount() {
        return getCount(storage, aggregateCategory, AGGREGATE_COUNT_ALL_AGENTS);
    }

    @Override
    public List<AgentInformation> getAllAgentInformation() {
        return executeQuery(new SimpleDaoQuery<>(storage, CATEGORY, QUERY_ALL_AGENTS)).asList();
    }

    @Override
    public List<AgentInformation> getAliveAgents() {
        return executeQuery(
                new AbstractDaoQuery<AgentInformation>(storage, CATEGORY, QUERY_ALIVE_AGENTS) {
                    @Override
                    public PreparedStatement<AgentInformation> customize(PreparedStatement<AgentInformation> preparedStatement) {
                        preparedStatement.setBoolean(0, true);
                        return preparedStatement;
                    }
                }).asList();
    }

    @Override
    public AgentInformation getAgentInformation(final HostRef agentRef) {
        return executeQuery(
                new AbstractDaoQuery<AgentInformation>(storage, CATEGORY, QUERY_AGENT_INFO) {
                    @Override
                    public PreparedStatement<AgentInformation> customize(PreparedStatement<AgentInformation> preparedStatement) {
                        preparedStatement.setString(0, agentRef.getAgentId());
                        return preparedStatement;
                    }
                }).head();
    }

    @Override
    public AgentInformation getAgentInformation(final AgentId agentId) {
        return executeQuery(
                new AbstractDaoQuery<AgentInformation>(storage, CATEGORY, QUERY_AGENT_INFO) {
                    @Override
                    public PreparedStatement<AgentInformation> customize(PreparedStatement<AgentInformation> preparedStatement) {
                        preparedStatement.setString(0, agentId.get());
                        return preparedStatement;
                    }
                }).head();
    }

    @Override
    public Set<AgentId> getAgentIds() {
        return mapToIds(getAllAgentInformation());
    }

    @Override
    public Set<AgentId> getAliveAgentIds() {
        return mapToIds(getAliveAgents());
    }

    private Set<AgentId> mapToIds(Iterable<AgentInformation> agentInformations) {
        Set<AgentId> result = new HashSet<>();
        for (AgentInformation agentInformation : agentInformations) {
            result.add(toAgentId(agentInformation));
        }
        return result;
    }

    private AgentId toAgentId(AgentInformation agentInfo) {
        return new AgentId(agentInfo.getAgentId());
    }

    @Override
    public void addAgentInformation(final AgentInformation agentInfo) {
        executeStatement(new AbstractDaoStatement<AgentInformation>(storage, CATEGORY, DESC_ADD_AGENT_INFO) {
            @Override
            public PreparedStatement<AgentInformation> customize(PreparedStatement<AgentInformation> preparedStatement) {
                preparedStatement.setString(0, agentInfo.getAgentId());
                preparedStatement.setLong(1, agentInfo.getStartTime());
                preparedStatement.setLong(2, agentInfo.getStopTime());
                preparedStatement.setBoolean(3, agentInfo.isAlive());
                preparedStatement.setString(4, agentInfo.getConfigListenAddress());
                return preparedStatement;
            }
        });
    }

    @Override
    public void removeAgentInformation(final AgentInformation agentInfo) {
        executeStatement(new AbstractDaoStatement<AgentInformation>(storage, CATEGORY, DESC_REMOVE_AGENT_INFO) {
            @Override
            public PreparedStatement<AgentInformation> customize(PreparedStatement<AgentInformation> preparedStatement) {
                preparedStatement.setString(0, agentInfo.getAgentId());
                return preparedStatement;
            }
        });
    }

    @Override
    public boolean isAlive(final AgentId agentId) {
        AgentInformation info = getAgentInformation(agentId);
        return (info != null && info.isAlive());
    }

    @Override
    public void updateAgentInformation(final AgentInformation agentInfo) {
        executeStatement(new AbstractDaoStatement<AgentInformation>(storage, CATEGORY, DESC_UPDATE_AGENT_INFO) {
            @Override
            public PreparedStatement<AgentInformation> customize(PreparedStatement<AgentInformation> preparedStatement) {
                preparedStatement.setLong(0, agentInfo.getStartTime());
                preparedStatement.setLong(1, agentInfo.getStopTime());
                preparedStatement.setBoolean(2, agentInfo.isAlive());
                preparedStatement.setString(3, agentInfo.getConfigListenAddress());
                preparedStatement.setString(4, agentInfo.getAgentId());
                return preparedStatement;
            }
        });
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}

