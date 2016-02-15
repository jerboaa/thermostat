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

package com.redhat.thermostat.host.cpu.common.internal;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.host.cpu.common.CpuStatDAO;
import com.redhat.thermostat.host.cpu.common.model.CpuStat;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostBoundaryPojoGetter;
import com.redhat.thermostat.storage.core.HostLatestPojoListGetter;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.HostTimeIntervalPojoListGetter;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.AbstractDao;
import com.redhat.thermostat.storage.dao.AbstractDaoStatement;
import com.redhat.thermostat.storage.model.Pojo;

class CpuStatDAOImpl extends AbstractDao implements CpuStatDAO {

    private static final Logger logger = LoggingUtils.getLogger(CpuStatDAOImpl.class);
    // ADD cpu-stats SET 'agentId' = ?s , \
    //                   'perProcessorUsage' = ?d[ , \
    //                   'timeStamp' = ?l
    static final String DESC_ADD_CPU_STAT = "ADD " + cpuStatCategory.getName() +
                           " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                                "'" + cpuLoadKey.getName() + "' = ?d[ , " +
                                "'" + Key.TIMESTAMP.getName() + "' = ?l";

    private final Storage storage;

    private final HostLatestPojoListGetter<CpuStat> latestGetter;
    private final HostTimeIntervalPojoListGetter<CpuStat> intervalGetter;
    private final HostBoundaryPojoGetter<CpuStat> boundaryGetter;

    CpuStatDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(cpuStatCategory);
        this.latestGetter = new HostLatestPojoListGetter<>(storage, cpuStatCategory);
        this.intervalGetter = new HostTimeIntervalPojoListGetter<>(storage, cpuStatCategory);
        this.boundaryGetter = new HostBoundaryPojoGetter<>(storage, cpuStatCategory);
    }

    @Override
    public List<CpuStat> getLatestCpuStats(HostRef ref, long lastTimeStamp) {
        return latestGetter.getLatest(ref, lastTimeStamp);
    }

    @Override
    public void putCpuStat(final CpuStat stat) {
        executeStatement(new AbstractDaoStatement<CpuStat>(storage, cpuStatCategory, DESC_ADD_CPU_STAT) {
            @Override
            public PreparedStatement<CpuStat> customize(PreparedStatement<CpuStat> preparedStatement) {
                preparedStatement.setString(0, stat.getAgentId());
                preparedStatement.setDoubleList(1, stat.getPerProcessorUsage());
                preparedStatement.setLong(2, stat.getTimeStamp());
                return preparedStatement;
            }
        });
    }

    @Override
    public List<CpuStat> getCpuStats(HostRef ref, long since, long to) {
        return intervalGetter.getLatest(ref, since, to);
    }

    @Override
    public CpuStat getOldest(HostRef ref) {
        return boundaryGetter.getOldestStat(ref);
    }

    @Override
    public CpuStat getNewest(HostRef ref) {
        return boundaryGetter.getNewestStat(ref);
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

}

