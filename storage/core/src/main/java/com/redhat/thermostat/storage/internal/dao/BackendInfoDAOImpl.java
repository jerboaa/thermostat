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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.redhat.thermostat.common.OrderedComparator;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.AbstractDao;
import com.redhat.thermostat.storage.dao.AbstractDaoQuery;
import com.redhat.thermostat.storage.dao.AbstractDaoStatement;
import com.redhat.thermostat.storage.dao.BackendInfoDAO;
import com.redhat.thermostat.storage.model.BackendInformation;

public class BackendInfoDAOImpl extends AbstractDao implements BackendInfoDAO {
    
    private static final Logger logger = LoggingUtils.getLogger(BackendInfoDAOImpl.class);
    static final String QUERY_BACKEND_INFO = "QUERY "
            + CATEGORY.getName() + " WHERE '" 
            + Key.AGENT_ID.getName() + "' = ?s";
    // ADD backend-info SET \
    //                   'agentId' = ?s , \
    //                   'name' = ?s , \
    //                   'description' = ?s , \
    //                   'observeNewJvm' = ?b , \
    //                   'pids' = ?i[ , \
    //                   'active' = ?b , \
    //                   'orderValue' = ?i
    static final String DESC_ADD_BACKEND_INFO = "ADD " + CATEGORY.getName() + " SET " +
            "'" + Key.AGENT_ID.getName() + "' = ?s , " +
            "'" + BACKEND_NAME.getName() + "' = ?s , " +
            "'" + BACKEND_DESCRIPTION.getName() + "' = ?s , " +
            "'" + SHOULD_MONITOR_NEW_PROCESSES.getName() + "' = ?b , " +
            "'" + PIDS_TO_MONITOR.getName() + "' = ?i[ , " +
            "'" + IS_ACTIVE.getName() + "' = ?b , " +
            "'" + ORDER_VALUE.getName() + "' = ?i";
    // REMOVE backend-info WHERE 'name' = ?s
    static final String DESC_REMOVE_BACKEND_INFO = "REMOVE " + CATEGORY.getName() +
            " WHERE '" + BACKEND_NAME.getName() + "' = ?s";

    private final Storage storage;

    public BackendInfoDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(CATEGORY);
    }

    @Override
    public List<BackendInformation> getBackendInformation(final HostRef host) {
        List<BackendInformation> result = executeQuery(
                new AbstractDaoQuery<BackendInformation>(storage, CATEGORY, QUERY_BACKEND_INFO) {
                    @Override
                    public PreparedStatement<BackendInformation> customize(PreparedStatement<BackendInformation> preparedStatement) {
                        preparedStatement.setString(0, host.getAgentId());
                        return preparedStatement;
                    }
                }).asList();
        List<BackendInformation> sorted = new ArrayList<>(result);
        Collections.sort(sorted, new OrderedComparator<>());
        return sorted;
    }

    @Override
    public List<BackendInformation> getBackendInformation(final AgentId agentId) {
        List<BackendInformation> result = executeQuery(
                new AbstractDaoQuery<BackendInformation>(storage, CATEGORY, QUERY_BACKEND_INFO) {
                    @Override
                    public PreparedStatement<BackendInformation> customize(PreparedStatement<BackendInformation> preparedStatement) {
                        preparedStatement.setString(0, agentId.get());
                        return preparedStatement;
                    }
                }).asList();
        List<BackendInformation> sorted = new ArrayList<>(result);
        Collections.sort(sorted, new OrderedComparator<>());
        return sorted;
    }

    @Override
    public void addBackendInformation(final BackendInformation info) {
        executeStatement(
                new AbstractDaoStatement<BackendInformation>(storage, CATEGORY, DESC_ADD_BACKEND_INFO) {
                    @Override
                    public PreparedStatement<BackendInformation> customize(PreparedStatement<BackendInformation> preparedStatement) {
                        preparedStatement.setString(0, info.getAgentId());
                        preparedStatement.setString(1, info.getName());
                        preparedStatement.setString(2, info.getDescription());
                        preparedStatement.setBoolean(3, info.isObserveNewJvm());
                        preparedStatement.setIntList(4, info.getPids());
                        preparedStatement.setBoolean(5, info.isActive());
                        preparedStatement.setInt(6, info.getOrderValue());
                        return preparedStatement;
                    }
                });
    }

    @Override
    public void removeBackendInformation(final BackendInformation info) {
        executeStatement(
                new AbstractDaoStatement<BackendInformation>(storage, CATEGORY, DESC_REMOVE_BACKEND_INFO) {
                    @Override
                    public PreparedStatement<BackendInformation> customize(PreparedStatement<BackendInformation> preparedStatement) {
                        preparedStatement.setString(0, info.getName());
                        return preparedStatement;
                    }
                });
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}

