/*
 * Copyright 2012-2015 Red Hat, Inc.
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.CategoryAdapter;
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

public class HostInfoDAOImpl extends BaseCountable implements HostInfoDAO {
    
    private static final Logger logger = LoggingUtils.getLogger(HostInfoDAOImpl.class);
    static final String QUERY_HOST_INFO = "QUERY "
            + hostInfoCategory.getName() + " WHERE '" 
            + Key.AGENT_ID.getName() + "' = ?s LIMIT 1";
    static final String QUERY_ALL_HOSTS = "QUERY " + hostInfoCategory.getName();
    // We can use hostInfoCategory.getName() here since this query
    // only changes the data class. When executed we use the adapted
    // aggregate category.
    static final String AGGREGATE_COUNT_ALL_HOSTS = "QUERY-COUNT " + hostInfoCategory.getName();
    // ADD host-info SET 'agentId' = ?s , \
    //                   'hostname' = ?s , \
    //                   'osName' = ?s , \
    //                   'osKernel' = ?s , \
    //                   'cpuModel' = ?s , \
    //                   'cpuCount' = ?i , \
    //                   'totalMemory' = ?l
    static final String DESC_ADD_HOST_INFO = "ADD " + hostInfoCategory.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + hostNameKey.getName() + "' = ?s , " +
                 "'" + osNameKey.getName() + "' = ?s , " +
                 "'" + osKernelKey.getName() + "' = ?s , " +
                 "'" + cpuModelKey.getName() + "' = ?s , " +
                 "'" + cpuCountKey.getName() + "' = ?i , " +
                 "'" + hostMemoryTotalKey.getName() + "' = ?l";

    private final Storage storage;
    private final AgentInfoDAO agentInfoDao;
    private final Category<AggregateCount> aggregateCategory;
    

    public HostInfoDAOImpl(Storage storage, AgentInfoDAO agentInfo) {
        this.storage = storage;
        this.agentInfoDao = agentInfo;
        // Adapt category to the aggregate form
        CategoryAdapter<HostInfo, AggregateCount> adapter = new CategoryAdapter<>(hostInfoCategory);
        this.aggregateCategory = adapter.getAdapted(AggregateCount.class);
        storage.registerCategory(hostInfoCategory);
        storage.registerCategory(aggregateCategory);
    }

    @Override
    public HostInfo getHostInfo(HostRef ref) {
        return getHostInfo(new AgentId(ref.getAgentId()));
    }

    @Override
    public HostInfo getHostInfo(final AgentId agentId) {
        StatementDescriptor<HostInfo> desc = new StatementDescriptor<>(hostInfoCategory, QUERY_HOST_INFO);
        PreparedStatement<HostInfo> prepared;
        Cursor<HostInfo> cursor;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, agentId.get());
            cursor = prepared.executeQuery();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
            return null;
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
            return null;
        }

        HostInfo result = null;
        if (cursor.hasNext()) {
            result = cursor.next();
        }
        return result;
    }

    @Override
    public void putHostInfo(HostInfo info) {
        StatementDescriptor<HostInfo> desc = new StatementDescriptor<>(hostInfoCategory, DESC_ADD_HOST_INFO);
        PreparedStatement<HostInfo> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, info.getAgentId());
            prepared.setString(1, info.getHostname());
            prepared.setString(2, info.getOsName());
            prepared.setString(3, info.getOsKernel());
            prepared.setString(4, info.getCpuModel());
            prepared.setInt(5, info.getCpuCount());
            prepared.setLong(6, info.getTotalMemory());
            prepared.execute();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }

    @Override
    public Collection<HostRef> getHosts() {
        List<HostRef> result = new ArrayList<>();
        for (HostInfo hostInfo : getHostInfos()) {
            result.add(toHostRef(hostInfo));
        }

        return result;
    }

    private Collection<HostInfo> getHostInfos() {
        Cursor<HostInfo> cursor = getAllHostInfoCursor();
        if (cursor == null) {
            return Collections.emptyList();
        }
        List<HostInfo> result = new ArrayList<>();
        while (cursor.hasNext()) {
            result.add(cursor.next());
        }

        return result;
    }

    private Cursor<HostInfo> getAllHostInfoCursor() {
        StatementDescriptor<HostInfo> desc = new StatementDescriptor<>(hostInfoCategory, QUERY_ALL_HOSTS);
        PreparedStatement<HostInfo> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            return prepared.executeQuery();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
            return null;
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
            return null;
        }
    }

    @Override
    public Collection<HostRef> getAliveHosts() {
        List<HostRef> hosts = new ArrayList<>();
        List<AgentInformation> agentInfos = agentInfoDao.getAliveAgents();
        for (AgentInformation agentInfo : agentInfos) {
            HostInfo hostInfo = getHostInfo(new AgentId(agentInfo.getAgentId()));
            // getHostInfo may return null if user is not allowed to
            // see the given host by ACL.
            if (hostInfo != null) {
                hosts.add(toHostRef(hostInfo));
            }
        }

        return hosts;
    }

    private HostRef toHostRef(HostInfo hostInfo) {
        String agentId = hostInfo.getAgentId();
        String hostName = hostInfo.getHostname();
        return new HostRef(agentId, hostName);
    }

    @Override
    public long getCount() {
        StatementDescriptor<AggregateCount> desc = new StatementDescriptor<>(
                aggregateCategory, AGGREGATE_COUNT_ALL_HOSTS);
        long count = getCount(desc, storage);
        return count;
    }
    
    @Override
    public boolean isAlive(HostRef ref) {
        AgentInformation info = agentInfoDao.getAgentInformation(ref);
        return (info != null && info.isAlive());
    }

    @Override
    public boolean isAlive(final AgentId agentId) {
        AgentInformation info = agentInfoDao.getAgentInformation(agentId);
        return (info != null && info.isAlive());
    }
}

