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
import java.util.Collection;
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
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.HostInfo;

public class HostInfoDAOImpl implements HostInfoDAO {
    
    private static final Logger logger = LoggingUtils.getLogger(HostInfoDAOImpl.class);
    private static final String QUERY_HOST_INFO = "QUERY "
            + hostInfoCategory.getName() + " WHERE " 
            + Key.AGENT_ID.getName() + " = ?s LIMIT 1";
    private static final String QUERY_ALL_HOSTS = "QUERY " + hostInfoCategory.getName();

    private final Storage storage;
    private final AgentInfoDAO agentInfoDao;


    public HostInfoDAOImpl(Storage storage, AgentInfoDAO agentInfo) {
        this.storage = storage;
        this.agentInfoDao = agentInfo;
        storage.registerCategory(hostInfoCategory);
    }

    @Override
    public HostInfo getHostInfo(HostRef ref) {
        return getHostInfo(ref.getAgentId());
    }

    private HostInfo getHostInfo(String agentId) {
        StatementDescriptor<HostInfo> desc = new StatementDescriptor<>(hostInfoCategory, QUERY_HOST_INFO);
        PreparedStatement<HostInfo> prepared;
        Cursor<HostInfo> cursor;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, agentId);
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
        Put add = storage.createAdd(hostInfoCategory);
        add.setPojo(info);
        add.apply();
    }

    @Override
    public Collection<HostRef> getHosts() {
        StatementDescriptor<HostInfo> desc = new StatementDescriptor<>(hostInfoCategory, QUERY_ALL_HOSTS);
        PreparedStatement<HostInfo> prepared;
        Cursor<HostInfo> cursor;
        try {
            prepared = storage.prepareStatement(desc);
            cursor = prepared.executeQuery();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
            return Collections.emptyList();
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
            return Collections.emptyList();
        }
        
        List<HostRef> result = new ArrayList<>();
        while (cursor.hasNext()) {
            HostInfo hostInfo = cursor.next();
            result.add(toHostRef(hostInfo));
        }
        return result;
    }

    @Override
    public Collection<HostRef> getAliveHosts() {
        List<HostRef> hosts = new ArrayList<>();
        List<AgentInformation> agentInfos = agentInfoDao.getAliveAgents();
        for (AgentInformation agentInfo : agentInfos) {
            HostInfo hostInfo = getHostInfo(agentInfo.getAgentId());
            hosts.add(toHostRef(hostInfo));
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
        return storage.getCount(hostInfoCategory);
    }
    
}

