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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Put;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;

public class AgentInfoDAOImpl implements AgentInfoDAO {

    private static final Logger logger = LoggingUtils.getLogger(AgentInfoDAOImpl.class);
    static final String QUERY_AGENT_INFO = "QUERY "
            + CATEGORY.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s";
    static final String QUERY_ALL_AGENTS = "QUERY "
            + CATEGORY.getName();
    static final String QUERY_ALIVE_AGENTS = "QUERY "
            + CATEGORY.getName() + " WHERE '" 
            + ALIVE_KEY.getName() + "' = ?b";
    
    private final Storage storage;
    private final ExpressionFactory factory;

    public AgentInfoDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(CATEGORY);
        this.factory = new ExpressionFactory();
    }

    @Override
    public long getCount() {
        return storage.getCount(CATEGORY);
    }

    @Override
    public List<AgentInformation> getAllAgentInformation() {
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(CATEGORY, QUERY_ALL_AGENTS);
        PreparedStatement<AgentInformation> prepared = null;
        Cursor<AgentInformation> agentCursor = null;
        try {
            prepared = storage.prepareStatement(desc);
            agentCursor = prepared.executeQuery();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
            return Collections.emptyList();
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
            return Collections.emptyList();
        }
        List<AgentInformation> results = new ArrayList<>();

        while (agentCursor.hasNext()) {
            AgentInformation agentInfo = agentCursor.next();
            results.add(agentInfo);
        }
        return results;
    }

    @Override
    public List<AgentInformation> getAliveAgents() {
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(CATEGORY, QUERY_ALIVE_AGENTS);
        PreparedStatement<AgentInformation> prepared = null;
        Cursor<AgentInformation> agentCursor = null;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setBoolean(0, true);
            agentCursor = prepared.executeQuery();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
            return Collections.emptyList();
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
            return Collections.emptyList();
        }
        List<AgentInformation> results = new ArrayList<>();

        while (agentCursor.hasNext()) {
            AgentInformation agentInfo = agentCursor.next();
            results.add(agentInfo);
        }
        return results;
    }

    @Override
    public AgentInformation getAgentInformation(HostRef agentRef) {
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(CATEGORY, QUERY_AGENT_INFO);
        PreparedStatement<AgentInformation> prepared;
        Cursor<AgentInformation> agentCursor;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, agentRef.getAgentId());
            agentCursor = prepared.executeQuery();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
            return null;
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
            return null;
        }
        
        AgentInformation result = null;
        if (agentCursor.hasNext()) {
            result = agentCursor.next();
        }
        return result;
    }

    @Override
    public void addAgentInformation(AgentInformation agentInfo) {
        Put replace = storage.createReplace(CATEGORY);
        replace.setPojo(agentInfo);
        replace.apply();
    }

    @Override
    public void removeAgentInformation(AgentInformation agentInfo) {
        Expression expr = factory.equalTo(Key.AGENT_ID, agentInfo.getAgentId());
        Remove remove = storage.createRemove();
        remove.from(CATEGORY);
        remove.where(expr);
        remove.apply();
    }

    @Override
    public void updateAgentInformation(AgentInformation agentInfo) {
        Update update = storage.createUpdate(CATEGORY);
        Expression expr = factory.equalTo(Key.AGENT_ID, agentInfo.getAgentId());
        update.where(expr);
        update.set(START_TIME_KEY, agentInfo.getStartTime());
        update.set(STOP_TIME_KEY, agentInfo.getStopTime());
        update.set(ALIVE_KEY, agentInfo.isAlive());
        update.set(CONFIG_LISTEN_ADDRESS, agentInfo.getConfigListenAddress());
        update.apply();
    }

}

