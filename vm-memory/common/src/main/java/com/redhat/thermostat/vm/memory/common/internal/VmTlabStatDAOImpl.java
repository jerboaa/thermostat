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

package com.redhat.thermostat.vm.memory.common.internal;

import java.util.List;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmBoundaryPojoGetter;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmLatestPojoListGetter;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.core.VmTimeIntervalPojoListGetter;
import com.redhat.thermostat.storage.dao.AbstractDao;
import com.redhat.thermostat.storage.dao.AbstractDaoStatement;
import com.redhat.thermostat.vm.memory.common.VmTlabStatDAO;
import com.redhat.thermostat.vm.memory.common.model.VmTlabStat;

class VmTlabStatDAOImpl extends AbstractDao implements VmTlabStatDAO {

    private static final Logger logger = LoggingUtils.getLogger(VmTlabStatDAOImpl.class);

    static final String DESC_ADD_VM_TLAB_STAT = "ADD " + vmTlabStatsCategory.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + KEY_TOTAL_ALLOCATING_THREADS.getName() + "' = ?l , " +
                 "'" + KEY_TOTAL_ALLOCATIONS.getName() + "' = ?l , " +
                 "'" + KEY_TOTAL_REFILLS.getName() + "' = ?l , " +
                 "'" + KEY_MAX_REFILLS.getName() + "' = ?l , " +
                 "'" + KEY_TOTAL_SLOW_ALLOCATIONS.getName() + "' = ?l , " +
                 "'" + KEY_MAX_SLOW_ALLOCATIONS.getName() + "' = ?l , " +
                 "'" + KEY_TOTAL_GC_WASTE.getName() + "' = ?l , " +
                 "'" + KEY_MAX_GC_WASTE.getName() + "' = ?l , " +
                 "'" + KEY_TOTAL_SLOW_WASTE.getName() + "' = ?l , " +
                 "'" + KEY_MAX_SLOW_WASTE.getName() + "' = ?l , " +
                 "'" + KEY_TOTAL_FAST_WASTE.getName() + "' = ?l , " +
                 "'" + KEY_MAX_FAST_WASTE.getName() + "' = ?l";

    private final Storage storage;
    private final VmLatestPojoListGetter<VmTlabStat> latestGetter;
    private final VmTimeIntervalPojoListGetter<VmTlabStat> intervalGetter;
    private final VmBoundaryPojoGetter<VmTlabStat> boundaryGetter;

    VmTlabStatDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(vmTlabStatsCategory);
        latestGetter = new VmLatestPojoListGetter<>(storage, vmTlabStatsCategory);
        intervalGetter = new VmTimeIntervalPojoListGetter<>(storage, vmTlabStatsCategory);
        boundaryGetter = new VmBoundaryPojoGetter<>(storage, vmTlabStatsCategory);
    }

    @Override
    public VmTlabStat getNewestStat(VmRef ref) {
        return boundaryGetter.getNewestStat(ref);
    }

    @Override
    public VmTlabStat getOldestStat(VmRef ref) {
        return boundaryGetter.getOldestStat(ref);
    }

    @Override
    public void putStat(final VmTlabStat stat) {
        executeStatement(new AbstractDaoStatement<VmTlabStat>(storage, vmTlabStatsCategory, DESC_ADD_VM_TLAB_STAT) {
            @Override
            public PreparedStatement<VmTlabStat> customize(PreparedStatement<VmTlabStat> preparedStatement) {
                preparedStatement.setString(0, stat.getAgentId());
                preparedStatement.setString(1, stat.getVmId());
                preparedStatement.setLong(2, stat.getTimeStamp());
                preparedStatement.setLong(3, stat.getTotalAllocatingThreads());
                preparedStatement.setLong(4, stat.getTotalAllocations());
                preparedStatement.setLong(5, stat.getTotalRefills());
                preparedStatement.setLong(6, stat.getMaxRefills());
                preparedStatement.setLong(7, stat.getTotalSlowAllocations());
                preparedStatement.setLong(8, stat.getMaxSlowAllocations());
                preparedStatement.setLong(9, stat.getTotalGcWaste());
                preparedStatement.setLong(10, stat.getMaxGcWaste());
                preparedStatement.setLong(11, stat.getTotalSlowWaste());
                preparedStatement.setLong(12, stat.getMaxSlowWaste());
                preparedStatement.setLong(13, stat.getTotalFastWaste());
                preparedStatement.setLong(14, stat.getMaxFastWaste());
                return preparedStatement;
            }
        });
    }

    @Override
    public List<VmTlabStat> getLatestStats(VmRef ref, long since) {
        return latestGetter.getLatest(ref, since);
    }

    @Override
    public List<VmTlabStat> getLatestStats(AgentId agentId, VmId vmId, long since) {
        return latestGetter.getLatest(agentId, vmId, since);
    }

    @Override
    public List<VmTlabStat> getStats(VmRef ref, long since, long to) {
        return intervalGetter.getLatest(ref, since, to);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}

