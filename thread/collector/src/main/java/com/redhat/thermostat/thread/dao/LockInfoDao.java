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

package com.redhat.thermostat.thread.dao;

import java.util.Arrays;
import java.util.List;

import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.thread.model.LockInfo;

public interface LockInfoDao {

    /*
     * vm-thread-lock schema
     */

    static final Key<Long> CONTENDED_LOCK_ATTEMPTS_KEY = new Key<>("contendedLockAttempts");
    static final Key<Long> DEFLATIONS_KEY = new Key<>("deflations");
    static final Key<Long> EMPTY_NOTIFICATIONS_KEY = new Key<>("emptyNotifications");
    static final Key<Long> FAILED_SPINS_KEY = new Key<>("failedSpins");
    static final Key<Long> FUTILE_WAKEUPS_KEY = new Key<>("futileWakeups");
    static final Key<Long> INFLATIONS_KEY = new Key<>("inflations");
    static final Key<Long> MONITORS_EXTANT_KEY = new Key<>("monExtant");
    static final Key<Long> MONITORS_IN_CIRCULATION_KEY = new Key<>("monInCirculation");
    static final Key<Long> MONITORS_SCAVENGED_KEY = new Key<>("monScavenged");
    static final Key<Long> NOTIFICATIONS_KEY = new Key<>("notifications");
    static final Key<Long> PARKS_KEY = new Key<>("parks");
    static final Key<Long> PRIVATE_A_KEY = new Key<>("privateA");
    static final Key<Long> PRIVATE_B_KEY = new Key<>("privateB");
    static final Key<Long> SLOW_ENTER_KEY = new Key<>("slowEnter");
    static final Key<Long> SLOW_EXIT_KEY = new Key<>("slowExit");
    static final Key<Long> SLOW_NOTIFY_KEY = new Key<>("slowNotify");
    static final Key<Long> SLOW_NOTIFY_ALL_KEY = new Key<>("slowNotifyAll");
    static final Key<Long> SUCCESSFUL_SPINS_KEY = new Key<>("successfulSpins");

    static final Category<LockInfo> LOCK_INFO_CATEGORY =
            new Category<>("vm-thread-lock", LockInfo.class,
                    Arrays.<Key<?>>asList(
                            Key.AGENT_ID,
                            Key.VM_ID,
                            Key.TIMESTAMP,
                            CONTENDED_LOCK_ATTEMPTS_KEY,
                            DEFLATIONS_KEY,
                            EMPTY_NOTIFICATIONS_KEY,
                            FAILED_SPINS_KEY,
                            FUTILE_WAKEUPS_KEY,
                            INFLATIONS_KEY,
                            MONITORS_EXTANT_KEY,
                            MONITORS_IN_CIRCULATION_KEY,
                            MONITORS_SCAVENGED_KEY,
                            NOTIFICATIONS_KEY,
                            PARKS_KEY,
                            PRIVATE_A_KEY,
                            PRIVATE_B_KEY,
                            SLOW_ENTER_KEY,
                            SLOW_EXIT_KEY,
                            SLOW_NOTIFY_KEY,
                            SLOW_NOTIFY_ALL_KEY,
                            SUCCESSFUL_SPINS_KEY),
                    Arrays.<Key<?>>asList(Key.TIMESTAMP));

    void saveLockInfo(LockInfo info);
    List<LockInfo> getLockInfo(VmRef ref, Range<Long> range);
    LockInfo getLatestLockInfo(VmRef ref);

}
