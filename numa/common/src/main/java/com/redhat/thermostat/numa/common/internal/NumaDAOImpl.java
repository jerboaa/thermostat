/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.numa.common.internal;

import java.util.List;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.numa.common.NumaDAO;
import com.redhat.thermostat.numa.common.NumaHostInfo;
import com.redhat.thermostat.numa.common.NumaStat;
import com.redhat.thermostat.storage.core.HostBoundaryPojoGetter;
import com.redhat.thermostat.storage.core.HostLatestPojoListGetter;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.HostTimeIntervalPojoListGetter;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.AbstractDao;
import com.redhat.thermostat.storage.dao.AbstractDaoQuery;
import com.redhat.thermostat.storage.dao.AbstractDaoStatement;

public class NumaDAOImpl extends AbstractDao implements NumaDAO {

    private static final Logger logger = LoggingUtils.getLogger(NumaDAOImpl.class);

    static final String QUERY_NUMA_INFO = "QUERY "
            + numaHostCategory.getName() + " WHERE '" 
            + Key.AGENT_ID.getName() + "' = ?s LIMIT 1";
    // ADD numa-stat SET 'agentId' = ?s , \
    //                   'timeStamp' = ?l , \
    //                   'nodeStats' = ?p[
    static final String DESC_ADD_NUMA_STAT = "ADD " + numaStatCategory.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + nodeStats.getName() + "' = ?p[";
    // ADD numa-host-info SET 'agentId' = ?s , \
    //                        'numNumaNodes' = ?i
    static final String DESC_ADD_NUMA_HOST_INFO = "ADD " + numaHostCategory.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + hostNumNumaNodes.getName() + "' = ?i";
    
    private final Storage storage;
    private final HostLatestPojoListGetter<NumaStat> latestGetter;
    private final HostTimeIntervalPojoListGetter<NumaStat> intervalGetter;
    private final HostBoundaryPojoGetter<NumaStat> boundaryGetter;

    NumaDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(numaStatCategory);
        storage.registerCategory(numaHostCategory);
        this.latestGetter = new HostLatestPojoListGetter<>(storage, numaStatCategory);
        this.intervalGetter = new HostTimeIntervalPojoListGetter<>(storage, numaStatCategory);
        this.boundaryGetter = new HostBoundaryPojoGetter<>(storage, numaStatCategory);
    }

    @Override
    public void putNumaStat(final NumaStat stat) {
        executeStatement(new AbstractDaoStatement<NumaStat>(storage, numaStatCategory, DESC_ADD_NUMA_STAT) {
            @Override
            public PreparedStatement<NumaStat> customize(PreparedStatement<NumaStat> preparedStatement) {
                preparedStatement.setString(0, stat.getAgentId());
                preparedStatement.setLong(1, stat.getTimeStamp());
                preparedStatement.setPojoList(2, stat.getNodeStats());
                return preparedStatement;
            }
        });
    }

    @Override
    public List<NumaStat> getLatestNumaStats(HostRef ref, long lastTimeStamp) {
        return latestGetter.getLatest(ref, lastTimeStamp);
    }

    @Override
    public List<NumaStat> getNumaStats(HostRef ref, long since, long to) {
        return intervalGetter.getLatest(ref, since, to);
    }

    @Override
    public NumaStat getNewest(HostRef ref) {
        return boundaryGetter.getNewestStat(ref);
    }

    @Override
    public NumaStat getOldest(HostRef ref) {
        return boundaryGetter.getOldestStat(ref);
    }

    @Override
    public void putNumberOfNumaNodes(final NumaHostInfo numaHostInfo) {
        executeStatement(new AbstractDaoStatement<NumaHostInfo>(storage, numaHostCategory, DESC_ADD_NUMA_HOST_INFO) {
            @Override
            public PreparedStatement<NumaHostInfo> customize(PreparedStatement<NumaHostInfo> preparedStatement) {
                preparedStatement.setString(0, numaHostInfo.getAgentId());
                preparedStatement.setInt(1, numaHostInfo.getNumNumaNodes());
                return preparedStatement;
            }
        });
    }

    @Override
    public int getNumberOfNumaNodes(final HostRef ref) {
        NumaHostInfo numaHostInfo = executeQuery(new AbstractDaoQuery<NumaHostInfo>(storage, numaHostCategory, QUERY_NUMA_INFO) {
            @Override
            public PreparedStatement<NumaHostInfo> customize(PreparedStatement<NumaHostInfo> preparedStatement) {
                preparedStatement.setString(0, ref.getAgentId());
                return preparedStatement;
            }
        }).head();
        if (numaHostInfo == null) {
            return 0;
        } else {
            return numaHostInfo.getNumNumaNodes();
        }
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}

