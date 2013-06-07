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
import java.util.List;

import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Put;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;

public class HostInfoDAOImpl implements HostInfoDAO {

    private final Storage storage;
    private final AgentInfoDAO agentInfoDao;


    public HostInfoDAOImpl(Storage storage, AgentInfoDAO agentInfo) {
        this.storage = storage;
        this.agentInfoDao = agentInfo;
        storage.registerCategory(hostInfoCategory);
    }

    @Override
    public HostInfo getHostInfo(HostRef ref) {
        Query<HostInfo> query = storage.createQuery(hostInfoCategory);
        ExpressionFactory factory = new ExpressionFactory();
        Expression expr = factory.equalTo(Key.AGENT_ID, ref.getAgentId());
        query.where(expr);
        query.limit(1);
        HostInfo result = query.execute().next();
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
        Query<HostInfo> allHosts = storage.createQuery(hostInfoCategory);
        return getHosts(allHosts);
    }

    @Override
    public Collection<HostRef> getAliveHosts() {
        List<HostRef> hosts = new ArrayList<>();
        List<AgentInformation> agentInfos = agentInfoDao.getAliveAgents();
        for (AgentInformation agentInfo : agentInfos) {
            Query<HostInfo> filter = storage.createQuery(hostInfoCategory);
            ExpressionFactory factory = new ExpressionFactory();
            Expression expr = factory.equalTo(Key.AGENT_ID, agentInfo.getAgentId());
            filter.where(expr);
            hosts.addAll(getHosts(filter));
        }

        return hosts;
    }


    private Collection<HostRef> getHosts(Query<HostInfo> filter) {
        Collection<HostRef> hosts = new ArrayList<HostRef>();
        
        Cursor<HostInfo> hostsCursor = filter.execute();
        while(hostsCursor.hasNext()) {
            HostInfo host = hostsCursor.next();
            String agentId = host.getAgentId();
            String hostName = host.getHostname();
            hosts.add(new HostRef(agentId, hostName));
        }
        return hosts;
    }

    @Override
    public long getCount() {
        return storage.getCount(hostInfoCategory);
    }
}

