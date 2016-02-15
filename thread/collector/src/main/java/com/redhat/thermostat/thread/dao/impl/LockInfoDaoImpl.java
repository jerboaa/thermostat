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

package com.redhat.thermostat.thread.dao.impl;

import java.util.List;
import java.util.logging.Logger;

import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmBoundaryPojoGetter;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.core.VmTimeIntervalPojoListGetter;
import com.redhat.thermostat.storage.dao.AbstractDao;
import com.redhat.thermostat.storage.dao.AbstractDaoStatement;
import com.redhat.thermostat.thread.dao.LockInfoDao;
import com.redhat.thermostat.thread.model.LockInfo;

public class LockInfoDaoImpl extends AbstractDao implements LockInfoDao {

    private static final Logger logger = LoggingUtils.getLogger(LockInfoDaoImpl.class);

    static final String ADD_LOCK_INFO = "ADD " + LOCK_INFO_CATEGORY.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + CONTENDED_LOCK_ATTEMPTS_KEY.getName() + "' = ?l , " +
                 "'" + DEFLATIONS_KEY.getName() + "' = ?l , " +
                 "'" + EMPTY_NOTIFICATIONS_KEY.getName() + "' = ?l , " +
                 "'" + FAILED_SPINS_KEY.getName() + "' = ?l , " +
                 "'" + FUTILE_WAKEUPS_KEY.getName() + "' = ?l , " +
                 "'" + INFLATIONS_KEY.getName() + "' = ?l , " +
                 "'" + MONITORS_EXTANT_KEY.getName() + "' = ?l , " +
                 "'" + MONITORS_IN_CIRCULATION_KEY.getName() + "' = ?l , " +
                 "'" + MONITORS_SCAVENGED_KEY.getName() + "' = ?l , " +
                 "'" + NOTIFICATIONS_KEY.getName() + "' = ?l , " +
                 "'" + PARKS_KEY.getName() + "' = ?l , " +
                 "'" + PRIVATE_A_KEY.getName() + "' = ?l , " +
                 "'" + PRIVATE_B_KEY.getName() + "' = ?l , " +
                 "'" + SLOW_ENTER_KEY.getName() + "' = ?l , " +
                 "'" + SLOW_EXIT_KEY.getName() + "' = ?l , " +
                 "'" + SLOW_NOTIFY_KEY.getName() + "' = ?l , " +
                 "'" + SLOW_NOTIFY_ALL_KEY.getName() + "' = ?l , " +
                 "'" + SUCCESSFUL_SPINS_KEY.getName() + "' = ?l";

    private final Storage storage;

    private final VmBoundaryPojoGetter<LockInfo> boundaryGetter;

    private VmTimeIntervalPojoListGetter<LockInfo> intervalGetter;

    public LockInfoDaoImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(LOCK_INFO_CATEGORY);

        boundaryGetter = new VmBoundaryPojoGetter<>(storage, LOCK_INFO_CATEGORY);
        intervalGetter = new VmTimeIntervalPojoListGetter<>(storage, LOCK_INFO_CATEGORY);
    }

    @Override
    public void saveLockInfo(final LockInfo info) {
        executeStatement(new AbstractDaoStatement<LockInfo>(storage, LOCK_INFO_CATEGORY, ADD_LOCK_INFO) {
            @Override
            public PreparedStatement<LockInfo> customize(PreparedStatement<LockInfo> preparedStatement) {
                preparedStatement.setString(0, info.getAgentId());
                preparedStatement.setString(1, info.getVmId());
                preparedStatement.setLong(2, info.getTimeStamp());
                preparedStatement.setLong(3, info.getContendedLockAttempts());
                preparedStatement.setLong(4, info.getDeflations());
                preparedStatement.setLong(5, info.getEmptyNotifications());
                preparedStatement.setLong(6, info.getFailedSpins());
                preparedStatement.setLong(7, info.getFutileWakeups());
                preparedStatement.setLong(8, info.getInflations());
                preparedStatement.setLong(9, info.getMonExtant());
                preparedStatement.setLong(10, info.getMonInCirculation());
                preparedStatement.setLong(11, info.getMonScavenged());
                preparedStatement.setLong(12, info.getNotifications());
                preparedStatement.setLong(13, info.getParks());
                preparedStatement.setLong(14, info.getPrivateA());
                preparedStatement.setLong(15, info.getPrivateB());
                preparedStatement.setLong(16, info.getSlowEnter());
                preparedStatement.setLong(17, info.getSlowExit());
                preparedStatement.setLong(18, info.getSlowNotify());
                preparedStatement.setLong(19, info.getSlowNotifyAll());
                preparedStatement.setLong(20, info.getSuccessfulSpins());

                return preparedStatement;
            }
        });
    }

    @Override
    public List<LockInfo> getLockInfo(VmRef ref, Range<Long> range) {
        return intervalGetter.getLatest(ref, range.getMin(), range.getMax());
    }

    @Override
    public LockInfo getLatestLockInfo(VmRef ref) {
        return boundaryGetter.getNewestStat(ref);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

}
