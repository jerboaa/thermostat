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

package com.redhat.thermostat.thread.harvester.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.backend.VmUpdate;
import com.redhat.thermostat.backend.VmUpdateException;
import com.redhat.thermostat.backend.VmUpdateListener;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.SystemClock;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.thread.dao.LockInfoDao;
import com.redhat.thermostat.thread.model.LockInfo;

public class LockInfoUpdater implements VmUpdateListener {

    private static final Logger logger = LoggingUtils.getLogger(LockInfoUpdater.class);

    private final Clock clock;
    private LockInfoDao lockDao;
    private String writerId;
    private String vmId;

    public LockInfoUpdater(LockInfoDao lockDao, String writerId, String vmId) {
        this(new SystemClock(), lockDao, writerId, vmId);
    }

    LockInfoUpdater(Clock clock, LockInfoDao lockDao, String writerId, String vmId) {
        this.clock = clock;
        this.lockDao = lockDao;
        this.writerId = writerId;
        this.vmId = vmId;
    }

    @Override
    public void countersUpdated(VmUpdate update) {
        long timeStamp = clock.getRealTimeMillis();

        try {
            long contendedLockAttempts = getCounter(update, "ContendedLockAttempts");
            long deflations = getCounter(update, "Deflations");
            long emptyNotifications = getCounter(update, "EmptyNotifications");
            long failedSpins = getCounter(update, "FailedSpins");
            long futileWakeups = getCounter(update, "FutileWakeups");
            long inflations = getCounter(update, "Inflations");
            long monExtant = getCounter(update, "MonExtant");
            long monInCirculation = getCounter(update, "MonInCirculation");
            long monScavenged = getCounter(update, "MonScavenged");
            long notifications = getCounter(update, "Notifications");
            long parks = getCounter(update, "Parks");
            long privateA = getCounter(update, "PrivateA");
            long privateB = getCounter(update, "PrivateB");
            long slowEnter = getCounter(update, "SlowEnter");
            long slowExit = getCounter(update, "SlowExit");
            long slowNotify = getCounter(update, "SlowNotify");
            long slowNotifyAll = getCounter(update, "SlowNotifyAll");
            long successfulSpins = getCounter(update, "SuccessfulSpins");

            LockInfo data = new LockInfo(timeStamp, writerId, vmId,
                    contendedLockAttempts, deflations, emptyNotifications,
                    failedSpins, futileWakeups, inflations,
                    monExtant, monInCirculation, monScavenged,
                    notifications, parks, privateA, privateB,
                    slowEnter, slowExit, slowNotify, slowNotifyAll,
                    successfulSpins);

            lockDao.saveLockInfo(data);
        } catch (VmUpdateException e) {
            logger.log(Level.WARNING, "Unable to get counter", e);
        }
    }

    private long getCounter(VmUpdate update, final String value) throws VmUpdateException {
        final String PREFIX = "sun.rt._sync_";
        try {
            return update.getPerformanceCounterLong(PREFIX + value);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to read counter: " + PREFIX + value);
            return LockInfo.UNKNOWN;
        }
    }

}
