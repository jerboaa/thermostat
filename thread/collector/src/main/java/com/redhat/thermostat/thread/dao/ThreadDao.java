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

package com.redhat.thermostat.thread.dao;

import java.util.Arrays;
import java.util.List;

import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.core.experimental.statement.ResultHandler;
import com.redhat.thermostat.thread.dao.internal.ThreadDaoKeys;
import com.redhat.thermostat.thread.model.SessionID;
import com.redhat.thermostat.thread.model.ThreadContentionSample;
import com.redhat.thermostat.thread.model.ThreadHarvestingStatus;
import com.redhat.thermostat.thread.model.ThreadSession;
import com.redhat.thermostat.thread.model.ThreadState;
import com.redhat.thermostat.thread.model.ThreadSummary;
import com.redhat.thermostat.thread.model.VmDeadLockData;

public interface ThreadDao {

    public static enum Sort {
        ASCENDING,
        DESCENDING,
    }

    /*
     * vm-thread-harvesting schema
     */
    static final Key<Boolean> HARVESTING_STATUS_KEY = new Key<Boolean>("harvesting");
    static final Category<ThreadHarvestingStatus> THREAD_HARVESTING_STATUS =
            new Category<>("vm-thread-harvesting", ThreadHarvestingStatus.class,
                    Arrays.<Key<?>>asList(
                            Key.AGENT_ID,
                            Key.VM_ID,
                            Key.TIMESTAMP,
                            HARVESTING_STATUS_KEY),
                    Arrays.<Key<?>>asList(Key.TIMESTAMP));

    /*
     * vm-deadlock-data schema
     */
    static final Key<String> DEADLOCK_DESCRIPTION_KEY = new Key<>("deadLockDescription");
    static final Category<VmDeadLockData> DEADLOCK_INFO = new Category<>("vm-deadlock-data", VmDeadLockData.class,
            Key.AGENT_ID, Key.VM_ID, Key.TIMESTAMP,
            DEADLOCK_DESCRIPTION_KEY);


    /*
     * THREAD_CONTENTION_SAMPLE
     */
    static final Key<String> THREAD_CONTENTION_BLOCKED_COUNT_KEY = new Key<>("blockedCount");
    static final Key<String> THREAD_CONTENTION_BLOCKED_TIME_KEY = new Key<>("blockedTime");
    static final Key<String> THREAD_CONTENTION_WAITED_COUNT_KEY = new Key<>("waitedCount");
    static final Key<String> THREAD_CONTENTION_WAITED_TIME_KEY = new Key<>("waitedTime");
    static final Category<ThreadContentionSample> THREAD_CONTENTION_SAMPLE =
            new Category<>("thread-contention-sample", ThreadContentionSample.class,
                           Arrays.<Key<?>>asList(Key.AGENT_ID, Key.VM_ID,
                                                 THREAD_CONTENTION_BLOCKED_COUNT_KEY,
                                                 THREAD_CONTENTION_BLOCKED_TIME_KEY,
                                                 THREAD_CONTENTION_WAITED_COUNT_KEY,
                                                 THREAD_CONTENTION_WAITED_TIME_KEY,
                                                 ThreadDaoKeys.THREAD_HEADER_UUID, Key.TIMESTAMP),
                           Arrays.<Key<?>>asList(ThreadDaoKeys.THREAD_HEADER_UUID, Key.TIMESTAMP));

    void saveSummary(ThreadSummary summary);
    List<ThreadSummary> getSummary(VmRef ref, Range<Long> range, int limit);

    /**
     * Returns a list of sessions registered by thread sampling.
     */
    List<ThreadSession> getSessions(VmRef ref, Range<Long> range, int limit, Sort order);

    /**
     * Save the given session to the database.
     */
    void saveSession(ThreadSession session);

    ThreadHarvestingStatus getLatestHarvestingStatus(VmRef vm);
    void saveHarvestingStatus(ThreadHarvestingStatus status);

    void saveDeadLockStatus(VmDeadLockData deadLockInfo);

    /**
     * Returns the latest vm deadlock data
     *
     * @return the latest data or null if there is none
     */
    VmDeadLockData loadLatestDeadLockStatus(VmRef ref);

    /**
     * Adds the given {@link ThreadState} to storage.
     */
    void addThreadState(ThreadState thread);

    void getThreadStates(VmRef ref, SessionID session,
                         ResultHandler<ThreadState> handler,
                         Range<Long> range, int limit, Sort order);

    long getDeadLockCount();
}
