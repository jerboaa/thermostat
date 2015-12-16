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

package com.redhat.thermostat.vm.compiler.common.internal;

import java.util.List;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmBoundaryPojoGetter;
import com.redhat.thermostat.storage.core.VmLatestPojoListGetter;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.core.VmTimeIntervalPojoListGetter;
import com.redhat.thermostat.storage.dao.AbstractDao;
import com.redhat.thermostat.storage.dao.AbstractDaoStatement;
import com.redhat.thermostat.vm.compiler.common.VmCompilerStat;
import com.redhat.thermostat.vm.compiler.common.VmCompilerStatDao;

class VmCompilerStatDaoImpl extends AbstractDao implements VmCompilerStatDao {

    private static final Logger logger = LoggingUtils.getLogger(VmCompilerStatDaoImpl.class);

    static final String DESC_ADD_VM_COMPILER_STAT = "ADD " + vmCompilerStatsCategory.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + totalCompilesKey.getName() + "' = ?l , " +
                 "'" + totalBailoutsKey.getName() + "' = ?l , " +
                 "'" + totalInvalidatesKey.getName() + "' = ?l , " +
                 "'" + compilationTimeKey.getName() + "' = ?l , " +
                 "'" + lastSizeKey.getName() + "' = ?l , " +
                 "'" + lastTypeKey.getName() + "' = ?l , " +
                 "'" + lastMethodKey.getName() + "' = ?s , " +
                 "'" + lastFailedTypeKey.getName() + "' = ?l , " +
                 "'" + lastFailedMethodKey.getName() + "' = ?s";

    private final Storage storage;
    private final VmLatestPojoListGetter<VmCompilerStat> latestGetter;
    private final VmTimeIntervalPojoListGetter<VmCompilerStat> intervalGetter;
    private final VmBoundaryPojoGetter<VmCompilerStat> boundaryGetter;

    VmCompilerStatDaoImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(vmCompilerStatsCategory);
        this.latestGetter = new VmLatestPojoListGetter<>(storage, vmCompilerStatsCategory);
        this.intervalGetter = new VmTimeIntervalPojoListGetter<>(storage, vmCompilerStatsCategory);
        this.boundaryGetter = new VmBoundaryPojoGetter<>(storage, vmCompilerStatsCategory);
    }

    @Override
    public List<VmCompilerStat> getLatestCompilerStats(VmRef ref, long lastUpdateTime) {
        return latestGetter.getLatest(ref, lastUpdateTime);
    }

    @Override
    public List<VmCompilerStat> getCompilerStats(VmRef ref, long since, long to) {
        return intervalGetter.getLatest(ref, since, to);
    }

    @Override
    public void putVmCompilerStat(final VmCompilerStat stat) {
        executeStatement(new AbstractDaoStatement<VmCompilerStat>(storage, vmCompilerStatsCategory, DESC_ADD_VM_COMPILER_STAT) {
            @Override
            public PreparedStatement<VmCompilerStat> customize(PreparedStatement<VmCompilerStat> preparedStatement) {
                preparedStatement.setString(0, stat.getAgentId());
                preparedStatement.setString(1, stat.getVmId());
                preparedStatement.setLong(2, stat.getTimeStamp());
                preparedStatement.setLong(3, stat.getTotalCompiles());
                preparedStatement.setLong(4, stat.getTotalBailouts());
                preparedStatement.setLong(5, stat.getTotalInvalidates());
                preparedStatement.setLong(6, stat.getCompilationTime());
                preparedStatement.setLong(7, stat.getLastSize());
                preparedStatement.setLong(8, stat.getLastType());
                preparedStatement.setString(9, stat.getLastMethod());
                preparedStatement.setLong(10, stat.getLastFailedType());
                preparedStatement.setString(11, stat.getLastFailedMethod());
                return preparedStatement;
            }
        });
    }

    @Override
    public VmCompilerStat getOldest(final VmRef ref) {
        return boundaryGetter.getOldestStat(ref);
    }

    @Override
    public VmCompilerStat getNewest(final VmRef ref) {
        return boundaryGetter.getNewestStat(ref);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}

